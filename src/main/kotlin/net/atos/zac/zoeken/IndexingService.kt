/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.ZaakListParameters
import net.atos.zac.app.task.model.TaakSortering
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.shared.model.SorteerRichting
import net.atos.zac.util.UriUtil
import net.atos.zac.zoeken.converter.AbstractZoekObjectConverter
import net.atos.zac.zoeken.model.ZoekObject
import net.atos.zac.zoeken.model.index.ZoekObjectType
import nl.lifely.zac.util.AllOpen
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.common.params.CursorMarkParams
import org.eclipse.microprofile.config.ConfigProvider
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

@Singleton
@AllOpen
@Suppress("TooManyFunctions")
class IndexingService @Inject constructor(
    private val converterInstances: Instance<AbstractZoekObjectConverter<out ZoekObject>>,
    private val zrcClientService: ZrcClientService,
    private val drcClientService: DrcClientService,
    private val flowableTaskService: FlowableTaskService
) {
    companion object {
        const val SOLR_CORE = "zac"

        private const val SOLR_MAX_RESULTS = 100
        private const val TAKEN_MAX_RESULTS = 50

        const val MAX_SEQUENTIAL_ERRORS = 20

        private val LOG = Logger.getLogger(IndexingService::class.java.name)
        private val reindexingViewfinder = ConcurrentHashMap.newKeySet<ZoekObjectType>()
        private val failureCounters = mutableMapOf<ZoekObjectType, Int>()

        private lateinit var solrClient: SolrClient
    }

    init {
        solrClient = Http2SolrClient.Builder(
            "${ConfigProvider.getConfig().getValue("solr.url", String::class.java)}/solr/$SOLR_CORE"
        ).build()
    }

    /**
     * Adds objectId to the Solr index and optionally performs a (hard) Solr commit so
     * that the Solr index is updated immediately.
     * Beware that hard Solr commits are relatively expensive operations.
     *
     * @param objectId      the object id to be indexed
     * @param objectType    the object type
     * @param performCommit whether to perform a hard Solr commit
     */
    fun indexeerDirect(objectId: String, objectType: ZoekObjectType, performCommit: Boolean) =
        addToSolrIndex(listOf(getConverter(objectType).convert(objectId)), performCommit)

    /**
     * Add a list of objectIds to the Solr index and optionally performs a (hard) Solr commit so
     * that the Solr index is updated immediately.
     * Beware that hard Solr commits are relatively expensive operations.
     *
     * @param objectIds     the list of object ids to be indexed
     * @param objectType    the object type
     * @param performCommit whether to perform a hard Solr commit
     */
    fun indexeerDirect(objectIds: List<String>, objectType: ZoekObjectType, performCommit: Boolean) =
        addToSolrIndex(objectIds.map { getConverter(objectType).convert(it) }, performCommit)

    fun reindex(objectType: ZoekObjectType) {
        if (reindexingViewfinder.contains(objectType)) {
            logTypeMessage(objectType, "Reindexing not started, still in progress")
            return
        }
        reindexingViewfinder.add(objectType)
        try {
            logTypeMessage(objectType, "Reindexing started ...")
            removeEntitiesFromSolrIndex(objectType)
            when (objectType) {
                ZoekObjectType.ZAAK -> reindexAllZaken()
                ZoekObjectType.DOCUMENT -> reindexAllInformatieobjecten()
                ZoekObjectType.TAAK -> reindexAllTaken()
            }
            logTypeMessage(objectType, "Reindexing finished successfully")
        } finally {
            reindexingViewfinder.remove(objectType)
            failureCounters.remove(objectType)
        }
    }

    fun addOrUpdateZaak(zaakUUID: UUID?, inclusiefTaken: Boolean) {
        indexeerDirect(zaakUUID.toString(), ZoekObjectType.ZAAK, false)
        if (inclusiefTaken) {
            flowableTaskService.listOpenTasksForZaak(zaakUUID)
                .map { it.id }
                .forEach(this::addOrUpdateTaak)
        }
    }

    fun addOrUpdateInformatieobject(informatieobjectUUID: UUID) =
        indexeerDirect(informatieobjectUUID.toString(), ZoekObjectType.DOCUMENT, false)

    fun addOrUpdateInformatieobjectByZaakinformatieobject(zaakinformatieobjectUUID: UUID) =
        addOrUpdateInformatieobject(
            UriUtil.uuidFromURI(
                zrcClientService.readZaakinformatieobject(zaakinformatieobjectUUID).informatieobject
            )
        )

    fun addOrUpdateTaak(taskID: String) =
        indexeerDirect(taskID, ZoekObjectType.TAAK, false)

    fun removeZaak(zaakUUID: UUID) =
        removeFromSolrIndex(zaakUUID.toString())

    fun removeInformatieobject(informatieobjectUUID: UUID) =
        removeFromSolrIndex(informatieobjectUUID.toString())

    fun removeTaak(taskID: String) =
        removeFromSolrIndex(taskID.toString())

    fun commit() {
        runTranslatingToIndexingException {
            // this overload waits until the solr searcher is done, which is what we want
            solrClient.commit(null, true, true)
        }
    }

    private fun getConverter(objectType: ZoekObjectType): AbstractZoekObjectConverter<out ZoekObject> =
        converterInstances
            .firstOrNull { it.supports(objectType) }
            ?: throw IndexingException("[$objectType] No converter found")

    private fun addToSolrIndex(zoekObjecten: List<ZoekObject?>, performCommit: Boolean) {
        val beansToBeAdded = zoekObjecten.filterNotNull()
        if (beansToBeAdded.isEmpty()) {
            return
        }
        runTranslatingToIndexingException {
            solrClient.addBeans(beansToBeAdded)
            if (performCommit) {
                commit()
            }
        }
    }

    private fun removeFromSolrIndex(idsToBeDeleted: List<String>): Boolean {
        if (idsToBeDeleted.isNotEmpty()) {
            runTranslatingToIndexingException {
                solrClient.deleteById(idsToBeDeleted)
            }
        }

        return true
    }

    private fun removeFromSolrIndex(id: String) {
        runTranslatingToIndexingException {
            solrClient.deleteById(id)
        }
    }

    private fun removeEntitiesFromSolrIndex(objectType: ZoekObjectType) {
        val query = SolrQuery("*:*").apply {
            setFields("id")
            addFilterQuery("type:$objectType")
            addSort("id", SolrQuery.ORDER.asc)
            rows = SOLR_MAX_RESULTS
        }
        var cursorMark = CursorMarkParams.CURSOR_MARK_START
        while (true) {
            query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark)
            val response = runTranslatingToIndexingException { solrClient.query(query) }
            if (!continueOnExceptions(objectType) {
                    removeFromSolrIndex(response.results.mapNotNull { it["id"].toString() })
                } || cursorMark == response.nextCursorMark
            ) {
                break
            }
            cursorMark = response.nextCursorMark
        }
    }

    private fun reindexAllZaken() {
        val listParameters = ZaakListParameters().apply {
            ordering = "-identificatie"
            page = ZGWApiService.FIRST_PAGE_NUMBER_ZGW_APIS
        }
        while (continueOnExceptions(ZoekObjectType.ZAAK) { reindexZaken(listParameters) }) {
            listParameters.page++
        }
    }

    private fun reindexAllInformatieobjecten() {
        val listParameters = EnkelvoudigInformatieobjectListParameters().apply {
            page = ZGWApiService.FIRST_PAGE_NUMBER_ZGW_APIS
        }
        while (continueOnExceptions(ZoekObjectType.DOCUMENT) { reindexInformatieobjecten(listParameters) }) {
            listParameters.page++
        }
    }

    private fun reindexAllTaken() {
        val numberOfTasks = flowableTaskService.countOpenTasks()
        var page = 0
        while (continueOnExceptions(ZoekObjectType.TAAK) { reindexTaken(page, numberOfTasks) }) {
            page++
        }
    }

    private fun reindexZaken(listParameters: ZaakListParameters): Boolean {
        val zaakResults = zrcClientService.listZaken(listParameters)
        indexeerDirect(
            zaakResults.results.map { it.uuid.toString() },
            ZoekObjectType.ZAAK,
            performCommit = false
        )
        logProgress(
            objectType = ZoekObjectType.ZAAK,
            progress = (listParameters.page - ZGWApiService.FIRST_PAGE_NUMBER_ZGW_APIS) * Results.NUM_ITEMS_PER_PAGE +
                zaakResults.results.size,
            totalSize = zaakResults.count.toLong()
        )
        return zaakResults.next != null
    }

    private fun reindexInformatieobjecten(
        listParameters: EnkelvoudigInformatieobjectListParameters
    ): Boolean {
        val enkelvoudigInformatieObjectenResults = drcClientService.listEnkelvoudigInformatieObjecten(listParameters)
        indexeerDirect(
            enkelvoudigInformatieObjectenResults.results.map { URIUtil.parseUUIDFromResourceURI(it.url).toString() },
            ZoekObjectType.DOCUMENT,
            performCommit = false
        )
        logProgress(
            ZoekObjectType.DOCUMENT,
            (listParameters.page - ZGWApiService.FIRST_PAGE_NUMBER_ZGW_APIS) * Results.NUM_ITEMS_PER_PAGE +
                enkelvoudigInformatieObjectenResults.results.size,
            enkelvoudigInformatieObjectenResults.count.toLong()
        )
        return enkelvoudigInformatieObjectenResults.next != null
    }

    private fun reindexTaken(page: Int, numberOfTasks: Long): Boolean {
        val firstResult = page * TAKEN_MAX_RESULTS
        val tasks = flowableTaskService.listOpenTasks(
            TaakSortering.CREATIEDATUM,
            SorteerRichting.DESCENDING,
            firstResult,
            TAKEN_MAX_RESULTS
        )
        indexeerDirect(
            tasks.map { it.id },
            ZoekObjectType.TAAK,
            performCommit = false
        )
        if (tasks.isNotEmpty()) {
            logProgress(ZoekObjectType.TAAK, firstResult.toLong() + tasks.size, numberOfTasks)
            return tasks.size == TAKEN_MAX_RESULTS
        }
        return false
    }

    private fun logTypeMessage(objectType: ZoekObjectType, message: String) =
        LOG.info("[$objectType] $message")

    private fun logTypeError(objectType: ZoekObjectType, failureNumber: Int, error: Throwable) =
        LOG.log(Level.WARNING, "[$objectType] Error ($failureNumber/$MAX_SEQUENTIAL_ERRORS) during indexing", error)

    private fun logProgress(objectType: ZoekObjectType, progress: Long, totalSize: Long) =
        logTypeMessage(objectType, "reindexed: $progress / $totalSize")

    private fun <T> runTranslatingToIndexingException(fn: () -> T): T {
        try {
            return fn()
        } catch (solrServerException: SolrServerException) {
            throw IndexingException(solrServerException)
        } catch (ioException: IOException) {
            throw IndexingException(ioException)
        }
    }

    private fun continueOnExceptions(objectType: ZoekObjectType, fn: () -> Boolean): Boolean {
        var currentFailureCount = failureCounters[objectType] ?: 0
        return try {
            runTranslatingToIndexingException { fn() }.also {
                failureCounters[objectType] = 0
            }
        } catch (indexingException: IndexingException) {
            failureCounters[objectType] = ++currentFailureCount
            logTypeError(objectType, currentFailureCount, indexingException)
            currentFailureCount < MAX_SEQUENTIAL_ERRORS
        }
    }
}
