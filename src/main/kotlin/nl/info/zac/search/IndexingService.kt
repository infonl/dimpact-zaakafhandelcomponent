/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.zrc.model.ZaakListParameters
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.task.model.TaakSortering
import nl.info.zac.authentication.LoggedInUserProvider.Companion.systemUser
import nl.info.zac.search.converter.AbstractZoekObjectConverter
import nl.info.zac.search.model.zoekobject.ZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.util.AllOpen
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.common.params.CursorMarkParams
import org.eclipse.microprofile.config.ConfigProvider
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
        const val SOLR_INDEXING_ERROR_MESSAGE = "Error occurred during Solr indexing"

        private const val SOLR_MAX_RESULTS = 100
        private const val TAKEN_MAX_RESULTS = 50

        private val LOG = Logger.getLogger(IndexingService::class.java.name)
        private val reindexingViewfinder = ConcurrentHashMap.newKeySet<ZoekObjectType>()

        private lateinit var solrClient: SolrClient
        private lateinit var solrUrl: String
    }

    init {
        solrUrl = ConfigProvider.getConfig().getValue("solr.url", String::class.java)
        solrClient = Http2SolrClient.Builder("$solrUrl/solr/$SOLR_CORE").build()
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
        indexeerDirectWith(
            objectIds = listOf(objectId),
            objectType = objectType,
            performCommit = performCommit,
            client = solrClient
        )

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
        indexeerDirectWith(
            objectIds = objectIds,
            objectType = objectType,
            performCommit = performCommit,
            client = solrClient
        )

    fun reindex(objectType: ZoekObjectType) = reindexWith(objectType, solrClient)

    /**
     * Reindexes all objects of the given type into the specified Solr collection.
     * Used for alias-based blue-green reindexing where data is built in an inactive
     * collection before switching the alias.
     */
    fun reindex(objectType: ZoekObjectType, targetCollection: String) {
        Http2SolrClient.Builder("$solrUrl/solr/$targetCollection").build().use { targetClient ->
            reindexWith(objectType, targetClient)
        }
    }

    fun addOrUpdateZaak(zaakUUID: UUID, inclusiefTaken: Boolean) {
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
            zrcClientService.readZaakinformatieobject(zaakinformatieobjectUUID).informatieobject.extractUuid()
        )

    fun addOrUpdateTaak(taskID: String) = indexeerDirect(taskID, ZoekObjectType.TAAK, false)

    fun removeZaak(zaakUUID: UUID) = removeFromSolrIndex(zaakUUID.toString())

    fun removeInformatieobject(informatieobjectUUID: UUID) = removeFromSolrIndex(informatieobjectUUID.toString())

    fun removeTaak(taskID: String) = removeFromSolrIndex(taskID)

    fun commit() {
        runTranslatingToIndexingException {
            // this overload waits until the solr searcher is done, which is what we want
            solrClient.commit(null, true, true)
        }
    }

    private fun reindexWith(objectType: ZoekObjectType, client: SolrClient) {
        if (reindexingViewfinder.contains(objectType)) {
            LOG.warning("[$objectType] Reindexing not started, still in progress")
            return
        }
        reindexingViewfinder.add(objectType)
        try {
            systemUser.set(true)
            LOG.info("[$objectType] Reindexing started")
            removeEntitiesFromSolrIndex(objectType, client)
            when (objectType) {
                ZoekObjectType.ZAAK -> reindexAllZaken(client)
                ZoekObjectType.DOCUMENT -> reindexAllInformatieobjecten(client)
                ZoekObjectType.TAAK -> reindexAllTaken(client)
            }
            LOG.info("[$objectType] Reindexing finished")
        } finally {
            reindexingViewfinder.remove(objectType)
            systemUser.remove()
        }
    }

    private fun getConverter(objectType: ZoekObjectType): AbstractZoekObjectConverter<out ZoekObject> =
        converterInstances
            .firstOrNull { it.supports(objectType) }
            ?: throw IndexingException("[$objectType] No converter found")

    private fun indexeerDirectWith(
        objectIds: List<String>,
        objectType: ZoekObjectType,
        performCommit: Boolean,
        client: SolrClient
    ) = addToSolrIndex(
        getConverter(objectType).let { converter ->
            objectIds.map { continueOnExceptions(objectType) { converter.convert(it) } }
        },
        performCommit,
        client
    )

    private fun addToSolrIndex(zoekObjecten: List<ZoekObject?>, performCommit: Boolean, client: SolrClient = solrClient) {
        val beansToBeAdded = zoekObjecten.filterNotNull()
        if (beansToBeAdded.isEmpty()) {
            return
        }
        runTranslatingToIndexingException {
            client.addBeans(beansToBeAdded)
            if (performCommit) {
                commit()
            }
        }
    }

    private fun removeFromSolrIndex(idsToBeDeleted: List<String>) {
        if (idsToBeDeleted.isEmpty()) {
            return
        }
        runTranslatingToIndexingException {
            solrClient.deleteById(idsToBeDeleted)
        }
    }

    private fun removeFromSolrIndex(id: String) {
        runTranslatingToIndexingException {
            solrClient.deleteById(id)
        }
    }

    private fun removeEntitiesFromSolrIndex(objectType: ZoekObjectType, client: SolrClient = solrClient) {
        val query = SolrQuery("*:*").apply {
            setFields("id")
            addFilterQuery("type:$objectType")
            addSort("id", SolrQuery.ORDER.asc)
            rows = SOLR_MAX_RESULTS
        }
        var cursorMark = CursorMarkParams.CURSOR_MARK_START
        while (true) {
            query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark)
            val response = continueOnExceptions(objectType) { client.query(query) }
            if (response == null) {
                LOG.warning(
                    "[$objectType] Cannot fetch next page. " +
                        "Aborting removal of entities after cursor mark $cursorMark"
                )
                return
            }

            continueOnExceptions(objectType) {
                val ids = response.results.mapNotNull { it["id"].toString() }
                if (ids.isNotEmpty()) {
                    client.deleteById(ids)
                }
            }
            if (cursorMark == response.nextCursorMark) {
                break
            }
            cursorMark = response.nextCursorMark
        }
    }

    private fun reindexAllZaken(client: SolrClient = solrClient) {
        val numberOfZaken = continueOnExceptions(ZoekObjectType.ZAAK) {
            zrcClientService.listZakenUuids(
                ZaakListParameters().apply {
                    ordering = "-identificatie"
                    page = ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS
                }
            ).count()
        }
        if (numberOfZaken == null) {
            LOG.warning("[${ZoekObjectType.ZAAK}] Cannot find zaken count! Aborting reindexing")
            return
        }

        val numberOfPages: Int = numberOfZaken / Results.NUM_ITEMS_PER_PAGE.toInt() +
            ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS

        for (pageNumber in ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS..numberOfPages) {
            continueOnExceptions(ZoekObjectType.ZAAK) { reindexZakenPage(pageNumber, numberOfZaken, client) }
        }
    }

    private fun reindexZakenPage(pageNumber: Int, totalCount: Int, client: SolrClient = solrClient) {
        val zaakResults = zrcClientService.listZakenUuids(
            ZaakListParameters().apply {
                ordering = "-identificatie"
                page = pageNumber
            }
        )
        val ids = zaakResults.results().map { it.uuid.toString() }
        indexeerDirectWith(
            objectIds = ids,
            objectType = ZoekObjectType.ZAAK,
            performCommit = false,
            client = client
        )
        val progress = (pageNumber - ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS) * Results.NUM_ITEMS_PER_PAGE + ids.size
        LOG.info("[${ZoekObjectType.ZAAK}] Reindexed: $progress / $totalCount ")
    }

    private fun reindexAllInformatieobjecten(client: SolrClient = solrClient) {
        val numberOfInformatieobjecten = continueOnExceptions(ZoekObjectType.DOCUMENT) {
            drcClientService.listEnkelvoudigInformatieObjecten(
                EnkelvoudigInformatieobjectListParameters().apply {
                    page = ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS
                }
            ).count()
        }
        if (numberOfInformatieobjecten == null) {
            LOG.warning("[${ZoekObjectType.DOCUMENT}] Cannot find information objects count! Aborting reindexing")
            return
        }

        val numberOfPages: Int = numberOfInformatieobjecten / Results.NUM_ITEMS_PER_PAGE.toInt() +
            ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS

        for (pageNumber in ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS..numberOfPages) {
            continueOnExceptions(ZoekObjectType.DOCUMENT) {
                reindexInformatieobjectenPage(pageNumber, numberOfInformatieobjecten, client)
            }
        }
    }

    private fun reindexInformatieobjectenPage(pageNumber: Int, totalCount: Int, client: SolrClient = solrClient) {
        val informationObjectsResults = drcClientService.listEnkelvoudigInformatieObjecten(
            EnkelvoudigInformatieobjectListParameters().apply { page = ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS }
        )
        val ids = informationObjectsResults.results().map { it.url.extractUuid().toString() }
        indexeerDirectWith(
            objectIds = ids,
            objectType = ZoekObjectType.DOCUMENT,
            performCommit = false,
            client = client
        )
        val progress = (pageNumber - ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS) * Results.NUM_ITEMS_PER_PAGE + ids.size
        LOG.info("[${ZoekObjectType.DOCUMENT}] Reindexed: $progress / $totalCount")
    }

    private fun reindexAllTaken(client: SolrClient = solrClient) {
        val numberOfTasks = continueOnExceptions(ZoekObjectType.TAAK) { flowableTaskService.countOpenTasks() }
        if (numberOfTasks == null) {
            LOG.warning("[${ZoekObjectType.TAAK}] Cannot find tasks count. Aborting reindexing")
            return
        }
        val numberOfPages: Int = numberOfTasks.toInt() / TAKEN_MAX_RESULTS

        for (pageNumber in 0..numberOfPages) {
            continueOnExceptions(ZoekObjectType.TAAK) { reindexTakenPage(pageNumber, numberOfTasks.toInt(), client) }
        }
    }

    private fun reindexTakenPage(pageNumber: Int, totalCount: Int, client: SolrClient = solrClient): Boolean {
        val firstResult = pageNumber * TAKEN_MAX_RESULTS
        val tasks = flowableTaskService.listOpenTasks(
            TaakSortering.CREATIEDATUM,
            SorteerRichting.DESCENDING,
            firstResult,
            TAKEN_MAX_RESULTS
        )
        if (tasks.isEmpty()) {
            return false
        }
        indexeerDirectWith(
            objectIds = tasks.map { it.id },
            objectType = ZoekObjectType.TAAK,
            performCommit = false,
            client = client
        )
        val progress = firstResult + tasks.size
        LOG.info("[${ZoekObjectType.TAAK}] Reindexed: $progress / $totalCount")
        return tasks.size == TAKEN_MAX_RESULTS
    }

    @Suppress("TooGenericExceptionCaught")
    private fun <T> runTranslatingToIndexingException(fn: () -> T): T {
        try {
            return fn()
        } catch (exception: Exception) {
            throw IndexingException(SOLR_INDEXING_ERROR_MESSAGE, exception)
        }
    }

    private fun <T> continueOnExceptions(objectType: ZoekObjectType, fn: () -> T): T? =
        try {
            runTranslatingToIndexingException { fn() }
        } catch (indexingException: IndexingException) {
            LOG.log(Level.WARNING, "[$objectType] Error during indexing", indexingException)
            null
        }
}
