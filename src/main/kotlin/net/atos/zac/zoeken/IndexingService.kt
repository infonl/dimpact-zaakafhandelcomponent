/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
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
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.params.CursorMarkParams
import org.eclipse.microprofile.config.ConfigProvider
import org.flowable.task.api.Task
import java.io.IOException
import java.util.UUID
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

        private const val SOLR_MAX_RESULT = 100
        private const val TAKEN_MAX_RESULTS = 50

        private val LOG = Logger.getLogger(IndexingService::class.java.name)

        private val reindexingViewfinder: MutableSet<ZoekObjectType> = HashSet()
    }

    private val solrClient: SolrClient = Http2SolrClient.Builder(
        "${ConfigProvider.getConfig().getValue("solr.url", String::class.java)}/solr/$SOLR_CORE"
    ).build()

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
        addToSolrIndex(objectIds.map { objectId: String -> getConverter(objectType).convert(objectId) }, performCommit)

    @Transactional(Transactional.TxType.NEVER)
    fun reindex(objectType: ZoekObjectType) {
        if (reindexingViewfinder.contains(objectType)) {
            logTypeMessage(objectType, "Herindexeren niet gestart, is nog bezig")
            return
        }
        reindexingViewfinder.add(objectType)
        try {
            logTypeMessage(objectType, "Herindexeren gestart...")
            removeEntitiesFromSolrIndex(objectType)
            when (objectType) {
                ZoekObjectType.ZAAK -> reindexAllZaken()
                ZoekObjectType.DOCUMENT -> reindexAllInformatieobjecten()
                ZoekObjectType.TAAK -> reindexAllTaken()
            }
            logTypeMessage(objectType, "Herindexeren gestopt")
        } finally {
            reindexingViewfinder.remove(objectType)
        }
    }

    fun addOrUpdateZaak(zaakUUID: UUID?, inclusiefTaken: Boolean) {
        indexeerDirect(zaakUUID.toString(), ZoekObjectType.ZAAK, false)
        if (inclusiefTaken) {
            flowableTaskService.listOpenTasksForZaak(zaakUUID).stream()
                .map { obj: Task -> obj.id }
                .forEach { taskID: String -> this.addOrUpdateTaak(taskID) }
        }
    }

    fun addOrUpdateInformatieobject(informatieobjectUUID: UUID) {
        indexeerDirect(informatieobjectUUID.toString(), ZoekObjectType.DOCUMENT, false)
    }

    fun addOrUpdateInformatieobjectByZaakinformatieobject(zaakinformatieobjectUUID: UUID) {
        addOrUpdateInformatieobject(
            UriUtil.uuidFromURI(
                zrcClientService.readZaakinformatieobject(zaakinformatieobjectUUID).informatieobject
            )
        )
    }

    fun addOrUpdateTaak(taskID: String) {
        indexeerDirect(taskID, ZoekObjectType.TAAK, false)
    }

    fun removeZaak(zaakUUID: UUID) {
        removeFromSolrIndex(zaakUUID.toString())
    }

    fun removeInformatieobject(informatieobjectUUID: UUID) {
        removeFromSolrIndex(informatieobjectUUID.toString())
    }

    fun removeTaak(taskID: String) {
        removeFromSolrIndex(taskID.toString())
    }

    fun commit() {
        try {
            // this overload waits until the solr searcher is done, which is what we want
            solrClient.commit(null, true, true)
        } catch (solrServerException: SolrServerException) {
            throw IndexingException(solrServerException)
        } catch (ioException: IOException) {
            throw IndexingException(ioException)
        }
    }

    private fun getConverter(objectType: ZoekObjectType): AbstractZoekObjectConverter<out ZoekObject> {
        for (converter in converterInstances) {
            if (converter.supports(objectType)) {
                return converter
            }
        }
        throw IndexingException("[$objectType] No converter found")
    }

    private fun addToSolrIndex(zoekObjecten: List<ZoekObject?>, performCommit: Boolean) {
        val beansToBeAdded = zoekObjecten.filterNotNull()
        if (beansToBeAdded.isEmpty()) {
            return
        }
        try {
            solrClient.addBeans(beansToBeAdded)
            if (performCommit) {
                commit()
            }
        } catch (solrServerException: SolrServerException) {
            throw IndexingException(solrServerException)
        } catch (ioException: IOException) {
            throw IndexingException(ioException)
        }
    }

    private fun removeFromSolrIndex(idsToBeDeleted: List<String>) {
        if (idsToBeDeleted.isEmpty()) {
            return
        }
        try {
            solrClient.deleteById(idsToBeDeleted)
        } catch (solrServerException: SolrServerException) {
            throw IndexingException(solrServerException)
        } catch (ioException: IOException) {
            throw IndexingException(ioException)
        }
    }

    private fun removeFromSolrIndex(id: String) {
        try {
            solrClient.deleteById(id)
        } catch (solrServerException: SolrServerException) {
            throw IndexingException(solrServerException)
        } catch (ioException: IOException) {
            throw IndexingException(ioException)
        }
    }

    private fun logProgress(objectType: ZoekObjectType, voortgang: Long, grootte: Long) {
        logTypeMessage(objectType, "reindexed: $voortgang / $grootte")
    }

    private fun removeEntitiesFromSolrIndex(objectType: ZoekObjectType) {
        val query = SolrQuery("*:*")
        query.setFields("id")
        query.addFilterQuery("type:$objectType")
        query.addSort("id", SolrQuery.ORDER.asc)
        query.setRows(SOLR_MAX_RESULT)
        var cursorMark = CursorMarkParams.CURSOR_MARK_START
        var done = false
        while (!done) {
            query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark)
            val response: QueryResponse
            try {
                response = solrClient.query(query)
            } catch (solrServerException: SolrServerException) {
                throw IndexingException(solrServerException)
            } catch (ioException: IOException) {
                throw IndexingException(ioException)
            }
            removeFromSolrIndex(
                response.results
                    .mapNotNull { document: SolrDocument -> document["id"] }
                    .map { obj: kotlin.Any -> obj.toString() }
            )
            val nextCursorMark = response.nextCursorMark
            if (cursorMark == nextCursorMark) {
                done = true
            } else {
                cursorMark = nextCursorMark
            }
        }
    }

    private fun reindexAllZaken() {
        val listParameters = ZaakListParameters().apply {
            ordering = "-identificatie"
            page = ZGWApiService.FIRST_PAGE_NUMBER_ZGW_APIS
        }
        while (reindexZaken(listParameters)) {
            listParameters.page++
        }
    }

    private fun reindexAllInformatieobjecten() {
        val listParameters = EnkelvoudigInformatieobjectListParameters().apply {
            page = ZGWApiService.FIRST_PAGE_NUMBER_ZGW_APIS
        }
        while (reindexInformatieobjecten(listParameters)) {
            listParameters.page++
        }
    }

    private fun reindexAllTaken() {
        val numberOfTasks = flowableTaskService.countOpenTasks()
        var page = 0
        while (reindexTaken(page, numberOfTasks)) {
            page++
        }
    }

    private fun reindexZaken(listParameters: ZaakListParameters): Boolean {
        val zaakResults = zrcClientService.listZaken(listParameters)
        indexeerDirect(
            zaakResults.results
                .map { obj: Zaak -> obj.uuid }
                .map { obj: UUID -> obj.toString() },
            ZoekObjectType.ZAAK,
            false
        )
        logProgress(
            ZoekObjectType.ZAAK,
            (listParameters.page - ZGWApiService.FIRST_PAGE_NUMBER_ZGW_APIS) * Results.NUM_ITEMS_PER_PAGE +
                zaakResults.results.size,
            zaakResults.count.toLong()
        )
        return zaakResults.next != null
    }

    private fun reindexInformatieobjecten(
        listParameters: EnkelvoudigInformatieobjectListParameters
    ): Boolean {
        val enkelvoudigInformatieObjectenResults = drcClientService.listEnkelvoudigInformatieObjecten(listParameters)
        indexeerDirect(
            enkelvoudigInformatieObjectenResults.results
                .map { URIUtil.parseUUIDFromResourceURI(it.url) }
                .map { it.toString() },
            ZoekObjectType.DOCUMENT,
            false
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
        indexeerDirect(tasks.map { it.id }, ZoekObjectType.TAAK, false)
        if (tasks.isNotEmpty()) {
            logProgress(ZoekObjectType.TAAK, firstResult.toLong() + tasks.size, numberOfTasks)
            return tasks.size == TAKEN_MAX_RESULTS
        }
        return false
    }

    private fun logTypeMessage(objectType: ZoekObjectType, message: String) =
        LOG.info("[$objectType] $message")
}
