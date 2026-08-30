/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.zrc.model.ZaakListParameters
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
        private const val TAKEN_MAX_RESULTS = 100
        private const val PAGE_CONVERSION_PARALLELISM = 8

        private val LOG = Logger.getLogger(IndexingService::class.java.name)
        private val reindexingViewfinder = ConcurrentHashMap.newKeySet<ZoekObjectType>()

        private lateinit var solrClient: SolrClient
    }

    private val pageConversionDispatcher = Dispatchers.IO.limitedParallelism(PAGE_CONVERSION_PARALLELISM)

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
        addToSolrIndex(
            getConverter(objectType).let { converter ->
                listOf(
                    continueOnExceptions(objectType) { converter.convert(objectId) }
                )
            },
            performCommit
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
        addToSolrIndex(convertObjects(objectIds, objectType), performCommit)

    /**
     * Reindexes all object types (`ZAAK`, `TAAK`, `DOCUMENT`) as a single, complete reindexing
     * process: logs when the complete process starts and finishes, in addition to the existing
     * per-object-type logging performed by [reindex], and logs the current Solr document counts
     * for [objectTypes] both before and after reindexing.
     *
     * @param objectTypes the object types to reindex; defaults to all object types
     */
    fun reindexAll(objectTypes: Set<ZoekObjectType> = ZoekObjectType.entries.toSet()) {
        LOG.info("Complete reindexing process started for object types: $objectTypes")
        logSolrIndexCounts(objectTypes)
        objectTypes.forEach(::reindex)
        logSolrIndexCounts(objectTypes)
        LOG.info("Complete reindexing process finished for object types: $objectTypes")
    }

    fun reindex(objectType: ZoekObjectType) {
        if (reindexingViewfinder.contains(objectType)) {
            LOG.warning("[$objectType] Reindexing not started, still in progress")
            return
        }
        reindexingViewfinder.add(objectType)
        try {
            systemUser.set(true)
            LOG.info("[$objectType] Reindexing started")
            removeEntitiesFromSolrIndex(objectType)
            val summary = when (objectType) {
                ZoekObjectType.ZAAK -> reindexAllZaken()
                ZoekObjectType.DOCUMENT -> reindexAllInformatieobjecten()
                ZoekObjectType.TAAK -> reindexAllTaken()
            }
            LOG.info(reindexFinishedMessage(objectType, summary))
        } finally {
            reindexingViewfinder.remove(objectType)
            systemUser.remove()
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

    private fun getConverter(objectType: ZoekObjectType): AbstractZoekObjectConverter<out ZoekObject> =
        converterInstances
            .firstOrNull { it.supports(objectType) }
            ?: throw IndexingException("[$objectType] No converter found")

    private fun convertObjects(objectIds: List<String>, objectType: ZoekObjectType): List<ZoekObject?> =
        getConverter(objectType).let { converter ->
            runBlocking(pageConversionDispatcher) {
                objectIds.map { objectId ->
                    async { continueOnExceptions(objectType) { converter.convert(objectId) } }
                }.awaitAll()
            }
        }

    /**
     * Converts and adds [objectIds] to the Solr index, returning the number of objects that were
     * successfully converted and added (as opposed to the number of [objectIds] passed in), so
     * that callers can track how many objects failed to reindex due to errors.
     */
    private fun indexeerDirectCountingSuccesses(objectIds: List<String>, objectType: ZoekObjectType): Int {
        val zoekObjecten = convertObjects(objectIds, objectType)
        addToSolrIndex(zoekObjecten, performCommit = false)
        return zoekObjecten.count { it != null }
    }

    private fun countInSolrIndex(objectType: ZoekObjectType): Long =
        runTranslatingToIndexingException {
            solrClient.query(
                SolrQuery("*:*").apply {
                    addFilterQuery("type:$objectType")
                    rows = 0
                }
            ).results.numFound
        }

    private fun logSolrIndexCounts(objectTypes: Set<ZoekObjectType>) {
        objectTypes.forEach { objectType ->
            LOG.info("[$objectType] Solr index contains ${countInSolrIndex(objectType)} documents")
        }
    }

    /**
     * Builds the "Reindexing finished" log message for [objectType], including the reindexed/total/error
     * totals from [summary] when reindexing was actually attempted (i.e. [summary] is not `null`).
     */
    private fun reindexFinishedMessage(objectType: ZoekObjectType, summary: Pair<Int, Int>?): String {
        val message = "[$objectType] Reindexing finished"
        return summary?.let { (successCount, totalCount) ->
            val errorCount = totalCount - successCount
            "$message. Reindexed: $successCount / $totalCount, not reindexed because of errors: $errorCount"
        } ?: message
    }

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
            val response = continueOnExceptions(objectType) { solrClient.query(query) }
            if (response == null) {
                LOG.warning(
                    "[$objectType] Cannot fetch next page. " +
                        "Aborting removal of entities after cursor mark $cursorMark"
                )
                return
            }

            continueOnExceptions(objectType) {
                removeFromSolrIndex(response.results.mapNotNull { it["id"].toString() })
            }
            if (cursorMark == response.nextCursorMark) {
                break
            }
            cursorMark = response.nextCursorMark
        }
    }

    private fun reindexAllZaken(): Pair<Int, Int>? {
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
            return null
        }

        val numberOfPages: Int = numberOfZaken / Results.DEFAULT_ZGW_PAGE_SIZE.toInt() +
            ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS

        var successCount = 0
        for (pageNumber in ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS..numberOfPages) {
            successCount += continueOnExceptions(ZoekObjectType.ZAAK) {
                reindexZakenPage(pageNumber, numberOfZaken)
            } ?: 0
        }
        return successCount to numberOfZaken
    }

    private fun reindexZakenPage(pageNumber: Int, totalCount: Int): Int {
        val zaakResults = zrcClientService.listZakenUuids(
            ZaakListParameters().apply {
                ordering = "-identificatie"
                page = pageNumber
            }
        )
        val ids = zaakResults.results().map { it.uuid.toString() }
        val successCount = indexeerDirectCountingSuccesses(ids, ZoekObjectType.ZAAK)
        val progress = (pageNumber - ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS) * Results.DEFAULT_ZGW_PAGE_SIZE + ids.size
        LOG.info("[${ZoekObjectType.ZAAK}] Reindexed: $progress / $totalCount ")
        return successCount
    }

    private fun reindexAllInformatieobjecten(): Pair<Int, Int>? {
        val numberOfInformatieobjecten = continueOnExceptions(ZoekObjectType.DOCUMENT) {
            drcClientService.listEnkelvoudigInformatieObjecten(
                EnkelvoudigInformatieobjectListParameters().apply {
                    page = ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS
                }
            ).count()
        }
        if (numberOfInformatieobjecten == null) {
            LOG.warning("[${ZoekObjectType.DOCUMENT}] Cannot find information objects count! Aborting reindexing")
            return null
        }

        val numberOfPages: Int = numberOfInformatieobjecten / Results.DEFAULT_ZGW_PAGE_SIZE.toInt() +
            ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS

        var successCount = 0
        for (pageNumber in ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS..numberOfPages) {
            successCount += continueOnExceptions(ZoekObjectType.DOCUMENT) {
                reindexInformatieobjectenPage(pageNumber, numberOfInformatieobjecten)
            } ?: 0
        }
        return successCount to numberOfInformatieobjecten
    }

    private fun reindexInformatieobjectenPage(pageNumber: Int, totalCount: Int): Int {
        val informationObjectsResults = drcClientService.listEnkelvoudigInformatieObjecten(
            EnkelvoudigInformatieobjectListParameters().apply { page = pageNumber }
        )
        val ids = informationObjectsResults.results().map { it.url.extractUuid().toString() }
        val successCount = indexeerDirectCountingSuccesses(ids, ZoekObjectType.DOCUMENT)
        val progress = (pageNumber - ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS) * Results.DEFAULT_ZGW_PAGE_SIZE + ids.size
        LOG.info("[${ZoekObjectType.DOCUMENT}] Reindexed: $progress / $totalCount")
        return successCount
    }

    private fun reindexAllTaken(): Pair<Int, Int>? {
        val numberOfTasks = continueOnExceptions(ZoekObjectType.TAAK) { flowableTaskService.countOpenTasks() }
        if (numberOfTasks == null) {
            LOG.warning("[${ZoekObjectType.TAAK}] Cannot find tasks count. Aborting reindexing")
            return null
        }
        val numberOfPages: Int = numberOfTasks.toInt() / TAKEN_MAX_RESULTS

        var successCount = 0
        for (pageNumber in 0..numberOfPages) {
            successCount += continueOnExceptions(ZoekObjectType.TAAK) {
                reindexTakenPage(pageNumber, numberOfTasks.toInt())
            } ?: 0
        }
        return successCount to numberOfTasks.toInt()
    }

    private fun reindexTakenPage(pageNumber: Int, totalCount: Int): Int {
        val firstResult = pageNumber * TAKEN_MAX_RESULTS
        val tasks = flowableTaskService.listOpenTasks(
            TaakSortering.CREATIEDATUM,
            SorteerRichting.DESCENDING,
            firstResult,
            TAKEN_MAX_RESULTS
        )
        if (tasks.isEmpty()) {
            return 0
        }
        val successCount = indexeerDirectCountingSuccesses(tasks.map { it.id }, ZoekObjectType.TAAK)
        val progress = firstResult + tasks.size
        LOG.info("[${ZoekObjectType.TAAK}] Reindexed: $progress / $totalCount")
        return successCount
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
