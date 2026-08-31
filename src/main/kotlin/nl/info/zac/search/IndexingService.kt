/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
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
        addToSolrIndex(convertObjects(objectIds, objectType).zoekObjecten(), performCommit)

    /**
     * Reindexes all object types (`ZAAK`, `TAAK`, `DOCUMENT`) as a single, complete reindexing
     * process: logs when the complete process starts and finishes, in addition to the existing
     * per-object-type logging (including Solr document counts) performed by [reindex].
     *
     * @param objectTypes the object types to reindex; defaults to all object types
     */
    fun reindexAll(objectTypes: Set<ZoekObjectType> = ZoekObjectType.entries.toSet()) {
        val orderedObjectTypes = objectTypes.sorted()
        LOG.info("Complete reindexing process started for object types: $orderedObjectTypes")
        orderedObjectTypes.forEach { objectType ->
            try {
                reindex(objectType)
            } catch (indexingException: IndexingException) {
                LOG.log(
                    Level.SEVERE,
                    "[$objectType] Reindexing failed, continuing with remaining object types",
                    indexingException
                )
            }
        }
        LOG.info("Complete reindexing process finished for object types: $orderedObjectTypes")
    }

    fun reindex(objectType: ZoekObjectType) {
        if (reindexingViewfinder.contains(objectType)) {
            LOG.warning("[$objectType] Reindexing not started, still in progress")
            return
        }
        reindexingViewfinder.add(objectType)
        try {
            systemUser.set(true)
            LOG.info(reindexStartedMessage(objectType))
            val summary = when (objectType) {
                ZoekObjectType.ZAAK -> reindexAllZaken()
                ZoekObjectType.DOCUMENT -> reindexAllInformatieobjecten()
                ZoekObjectType.TAAK -> reindexAllTaken()
            }
            // only commit when reindexing was actually attempted: existing entities are only
            // deleted once the total count is known (see e.g. reindexAllZaken), so a null summary
            // means nothing was deleted either, and there is nothing to make visible
            if (summary != null) {
                // ensure the removed/reindexed entities are visible to the searcher before reporting
                // the finished Solr document count, since bulk (re)indexing never commits per page
                commit()
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

    private sealed interface ConversionOutcome {
        data class Converted(val zoekObject: ZoekObject) : ConversionOutcome
        object Skipped : ConversionOutcome
        object Errored : ConversionOutcome
    }

    private fun List<ConversionOutcome>.zoekObjecten(): List<ZoekObject> =
        mapNotNull { (it as? ConversionOutcome.Converted)?.zoekObject }

    private data class ReindexCounts(val successCount: Int = 0, val skippedCount: Int = 0) {
        operator fun plus(other: ReindexCounts) =
            ReindexCounts(successCount + other.successCount, skippedCount + other.skippedCount)
    }

    private data class ReindexSummary(val successCount: Int, val skippedCount: Int, val totalCount: Int)

    private fun convert(
        converter: AbstractZoekObjectConverter<out ZoekObject>,
        objectType: ZoekObjectType,
        objectId: String
    ): ConversionOutcome =
        try {
            runTranslatingToIndexingException { converter.convert(objectId) }
                ?.let { ConversionOutcome.Converted(it) }
                ?: ConversionOutcome.Skipped
        } catch (indexingException: IndexingException) {
            LOG.log(Level.WARNING, "[$objectType] Error during indexing", indexingException)
            ConversionOutcome.Errored
        }

    private fun convertObjects(objectIds: List<String>, objectType: ZoekObjectType): List<ConversionOutcome> =
        getConverter(objectType).let { converter ->
            runBlocking(pageConversionDispatcher) {
                objectIds.map { objectId ->
                    async { convert(converter, objectType, objectId) }
                }.awaitAll()
            }
        }

    /**
     * Converts and adds [objectIds] to the Solr index, returning how many were successfully
     * converted and added versus legitimately skipped by the converter (as opposed to the
     * number of [objectIds] passed in), so that callers can report skips separately from
     * objects that failed to reindex due to errors.
     */
    private fun indexeerDirectCountingSuccesses(objectIds: List<String>, objectType: ZoekObjectType): ReindexCounts {
        val outcomes = convertObjects(objectIds, objectType)
        addToSolrIndex(outcomes.zoekObjecten(), performCommit = false)
        return ReindexCounts(
            successCount = outcomes.count { it is ConversionOutcome.Converted },
            skippedCount = outcomes.count { it is ConversionOutcome.Skipped }
        )
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

    /**
     * Builds the "Reindexing started" log message for [objectType], including the current Solr
     * document count for that type (i.e. before any entities are removed or reindexed).
     */
    private fun reindexStartedMessage(objectType: ZoekObjectType): String =
        "[$objectType] Reindexing started. Solr index currently contains ${countInSolrIndex(objectType)} " +
            "documents of type '$objectType'."

    /**
     * Deletes the existing Solr documents of [objectType]. Only called once the total count for
     * that type is known, so that an early abort (e.g. the count could not be determined) never
     * deletes entities that reindexing will not get a chance to replace.
     */
    private fun deleteExistingEntities(objectType: ZoekObjectType) {
        LOG.info("[$objectType] Deleting existing documents of type '$objectType' before reindexing.")
        removeEntitiesFromSolrIndex(objectType)
    }

    /**
     * Builds the "Reindexing finished" log message for [objectType], including the reindexed/skipped/error
     * totals from [summary] when reindexing was actually attempted (i.e. [summary] is not `null`), and
     * the current Solr document count for that type (i.e. after reindexing has completed). Skipped
     * objects (the converter legitimately decided not to index them) are reported separately from
     * errors, so an "errors" count only ever reflects an actual failure.
     */
    private fun reindexFinishedMessage(objectType: ZoekObjectType, summary: ReindexSummary?): String {
        val message = "[$objectType] Reindexing finished"
        val withSummary = summary?.let { (successCount, skippedCount, totalCount) ->
            val errorCount = totalCount - successCount - skippedCount
            "$message. Reindexed: $successCount / $totalCount, skipped: $skippedCount, " +
                "not reindexed because of errors: $errorCount"
        } ?: message
        return "$withSummary. Solr index contains ${countInSolrIndex(objectType)} documents of type '$objectType'."
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

    private fun reindexAllZaken(): ReindexSummary? {
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
        deleteExistingEntities(ZoekObjectType.ZAAK)

        val numberOfPages: Int = (numberOfZaken + Results.DEFAULT_ZGW_PAGE_SIZE.toInt() - 1) /
            Results.DEFAULT_ZGW_PAGE_SIZE.toInt()

        var counts = ReindexCounts()
        for (pageNumber in ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS..numberOfPages) {
            continueOnExceptions(ZoekObjectType.ZAAK) {
                reindexZakenPage(pageNumber, numberOfZaken)
            }?.let { counts += it }
        }
        return ReindexSummary(counts.successCount, counts.skippedCount, numberOfZaken)
    }

    private fun reindexZakenPage(pageNumber: Int, totalCount: Int): ReindexCounts {
        val zaakResults = zrcClientService.listZakenUuids(
            ZaakListParameters().apply {
                ordering = "-identificatie"
                page = pageNumber
            }
        )
        val ids = zaakResults.results().map { it.uuid.toString() }
        val counts = indexeerDirectCountingSuccesses(ids, ZoekObjectType.ZAAK)
        val progress = (pageNumber - ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS) * Results.DEFAULT_ZGW_PAGE_SIZE + ids.size
        LOG.info("[${ZoekObjectType.ZAAK}] Reindexed: $progress / $totalCount ")
        return counts
    }

    private fun reindexAllInformatieobjecten(): ReindexSummary? {
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
        deleteExistingEntities(ZoekObjectType.DOCUMENT)

        val numberOfPages: Int = (numberOfInformatieobjecten + Results.DEFAULT_ZGW_PAGE_SIZE.toInt() - 1) /
            Results.DEFAULT_ZGW_PAGE_SIZE.toInt()

        var counts = ReindexCounts()
        for (pageNumber in ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS..numberOfPages) {
            continueOnExceptions(ZoekObjectType.DOCUMENT) {
                reindexInformatieobjectenPage(pageNumber, numberOfInformatieobjecten)
            }?.let { counts += it }
        }
        return ReindexSummary(counts.successCount, counts.skippedCount, numberOfInformatieobjecten)
    }

    private fun reindexInformatieobjectenPage(pageNumber: Int, totalCount: Int): ReindexCounts {
        val informationObjectsResults = drcClientService.listEnkelvoudigInformatieObjecten(
            EnkelvoudigInformatieobjectListParameters().apply { page = pageNumber }
        )
        val ids = informationObjectsResults.results().map { it.url.extractUuid().toString() }
        val counts = indexeerDirectCountingSuccesses(ids, ZoekObjectType.DOCUMENT)
        val progress = (pageNumber - ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS) * Results.DEFAULT_ZGW_PAGE_SIZE + ids.size
        LOG.info("[${ZoekObjectType.DOCUMENT}] Reindexed: $progress / $totalCount")
        return counts
    }

    private fun reindexAllTaken(): ReindexSummary? {
        val numberOfTasks = continueOnExceptions(ZoekObjectType.TAAK) { flowableTaskService.countOpenTasks() }
        if (numberOfTasks == null) {
            LOG.warning("[${ZoekObjectType.TAAK}] Cannot find tasks count. Aborting reindexing")
            return null
        }
        deleteExistingEntities(ZoekObjectType.TAAK)

        val numberOfPages: Int = numberOfTasks.toInt() / TAKEN_MAX_RESULTS

        var counts = ReindexCounts()
        for (pageNumber in 0..numberOfPages) {
            continueOnExceptions(ZoekObjectType.TAAK) {
                reindexTakenPage(pageNumber, numberOfTasks.toInt())
            }?.let { counts += it }
        }
        return ReindexSummary(counts.successCount, counts.skippedCount, numberOfTasks.toInt())
    }

    private fun reindexTakenPage(pageNumber: Int, totalCount: Int): ReindexCounts {
        val firstResult = pageNumber * TAKEN_MAX_RESULTS
        val tasks = flowableTaskService.listOpenTasks(
            TaakSortering.CREATIEDATUM,
            SorteerRichting.DESCENDING,
            firstResult,
            TAKEN_MAX_RESULTS
        )
        if (tasks.isEmpty()) {
            return ReindexCounts()
        }
        val counts = indexeerDirectCountingSuccesses(tasks.map { it.id }, ZoekObjectType.TAAK)
        val progress = firstResult + tasks.size
        LOG.info("[${ZoekObjectType.TAAK}] Reindexed: $progress / $totalCount")
        return counts
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
