/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
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
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.ZaakListParameters
import nl.info.client.zgw.zrc.util.isZaakspecifiekGeautoriseerd
import nl.info.zac.app.task.model.TaakSortering
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

internal sealed interface ConversionOutcome {
    data class Converted(val zoekObject: ZoekObject) : ConversionOutcome
    object Skipped : ConversionOutcome
    object Errored : ConversionOutcome
}

internal fun List<ConversionOutcome>.zoekObjecten(): List<ZoekObject> =
    mapNotNull { (it as? ConversionOutcome.Converted)?.zoekObject }

internal data class ReindexCounts(val successCount: Int = 0, val skippedCount: Int = 0) {
    operator fun plus(other: ReindexCounts) =
        ReindexCounts(successCount + other.successCount, skippedCount + other.skippedCount)
}

internal data class ReindexSummary(val successCount: Int, val skippedCount: Int, val totalCount: Int)

/**
 * Owns the Solr client and the generic machinery shared by every reindex path in this package: converting
 * and adding/removing Solr documents, the [ConversionOutcome]/[ReindexCounts]/[ReindexSummary] accounting
 * types, "Reindexing started/finished" message building, and the independent per-object-type reindex
 * algorithms ([reindexAllZaken]/[reindexAllInformatieobjecten]/[reindexAllTaken]).
 */
@Singleton
@AllOpen
@Suppress("TooManyFunctions")
class ReindexSupportService @Inject constructor(
    private val converterInstances: Instance<AbstractZoekObjectConverter<out ZoekObject>>,
    private val zrcClientService: ZrcClientService,
    private val drcClientService: DrcClientService,
    private val flowableTaskService: FlowableTaskService
) {
    companion object {
        private const val SOLR_MAX_RESULTS = 100
        private const val TAKEN_MAX_RESULTS = 100
        private const val PAGE_CONVERSION_PARALLELISM = 8

        private val LOG = Logger.getLogger(ReindexSupportService::class.java.name)
    }

    internal val pageConversionDispatcher = Dispatchers.IO.limitedParallelism(PAGE_CONVERSION_PARALLELISM)

    private val solrClient: SolrClient = Http2SolrClient.Builder(
        "${ConfigProvider.getConfig().getValue("solr.url", String::class.java)}/solr/${IndexingService.SOLR_CORE}"
    ).build()

    fun commit() {
        runTranslatingToIndexingException {
            solrClient.commit(null, true, true)
        }
    }

    internal fun getConverter(objectType: ZoekObjectType): AbstractZoekObjectConverter<out ZoekObject> =
        converterInstances
            .firstOrNull { it.supports(objectType) }
            ?: throw IndexingException("[$objectType] No converter found")

    private fun convert(
        converter: AbstractZoekObjectConverter<out ZoekObject>,
        objectType: ZoekObjectType,
        objectId: String,
        isZaakspecifiekGeautoriseerd: (UUID) -> Boolean
    ): ConversionOutcome =
        try {
            runTranslatingToIndexingException { converter.convert(objectId, isZaakspecifiekGeautoriseerd) }
                ?.let { ConversionOutcome.Converted(it) }
                ?: ConversionOutcome.Skipped
        } catch (indexingException: IndexingException) {
            LOG.log(Level.WARNING, "[$objectType] Error during indexing", indexingException)
            ConversionOutcome.Errored
        }

    /**
     * Converts [objectIds] concurrently, sharing one
     * [isZaakspecifiekGeautoriseerd] lookup across all of them by default, memoized per zaak UUID via
     * [memoizedIsZaakspecifiekGeautoriseerd] so that objects linked to the same zaak (e.g. several
     * documents of one zaak within a reindex page) share one ZGW call instead of each deriving the flag
     * on its own.
     */
    internal fun convertObjects(
        objectIds: List<String>,
        objectType: ZoekObjectType,
        isZaakspecifiekGeautoriseerd: (UUID) -> Boolean = memoizedIsZaakspecifiekGeautoriseerd()
    ): List<ConversionOutcome> =
        getConverter(objectType).let { converter ->
            runBlocking(pageConversionDispatcher) {
                objectIds.map { objectId ->
                    async { convert(converter, objectType, objectId, isZaakspecifiekGeautoriseerd) }
                }.awaitAll()
            }
        }

    /**
     * Converts and adds [objectIds] to the Solr index, returning how many were successfully
     * converted and added versus legitimately skipped by the converter (as opposed to the
     * number of [objectIds] passed in), so that callers can report skips separately from
     * objects that failed to reindex due to errors.
     */
    internal fun indexeerDirectCountingSuccesses(
        objectIds: List<String>,
        objectType: ZoekObjectType,
        isZaakspecifiekGeautoriseerd: (UUID) -> Boolean = memoizedIsZaakspecifiekGeautoriseerd()
    ): ReindexCounts {
        val outcomes = convertObjects(objectIds, objectType, isZaakspecifiekGeautoriseerd)
        addToSolrIndex(outcomes.zoekObjecten(), performCommit = false)
        return ReindexCounts(
            successCount = outcomes.count { it is ConversionOutcome.Converted },
            skippedCount = outcomes.count { it is ConversionOutcome.Skipped }
        )
    }

    /**
     * Best-effort: this count is purely informational, so a Solr hiccup here must not abort
     * reindexing itself (nor, via [continueOnExceptions], the remaining object types in
     * `IndexingService.reindexAll`).
     */
    private fun countInSolrIndex(objectType: ZoekObjectType): Long? =
        continueOnExceptions(objectType, "Error counting Solr documents") {
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
    internal fun reindexStartedMessage(objectType: ZoekObjectType): String =
        "[$objectType] Reindexing started. Solr index currently contains " +
            "${countInSolrIndex(objectType) ?: "unknown"} documents of type '$objectType'."

    /**
     * Deletes the existing Solr documents of [objectType]. Only called once the total count for
     * that type is known, so that an early abort (e.g. the count could not be determined) never
     * deletes entities that reindexing will not get a chance to replace.
     */
    internal fun deleteExistingEntities(objectType: ZoekObjectType) {
        LOG.info("[$objectType] Deleting existing documents of type '$objectType' before reindexing.")
        removeEntitiesFromSolrIndex(objectType)
    }

    /**
     * Builds the "Reindexing finished" (or, when [summary] is `null`, "Reindexing aborted") log message
     * for [objectType], including the reindexed/skipped/error totals from [summary] when reindexing was
     * actually attempted, and the current Solr document count for that type (i.e. after reindexing has
     * completed, or was aborted). Skipped objects (the converter legitimately decided not to index them)
     * are reported separately from errors, so an "errors" count only ever reflects an actual per-object
     * failure - with one caveat: [ReindexSummary.totalCount] is the ZGW API's total count captured once
     * before paging starts, while successCount/skippedCount accumulate as paging proceeds afterwards.
     * Objects created or deleted in the ZGW API while a reindex is running can therefore surface as a
     * phantom error count here (or mask a real one), purely from that drift, not from a conversion
     * failure. Since paging can overshoot the snapshot (e.g. more objects created after the page count
     * was fixed), the raw difference can go negative, so it is clamped to zero rather than logged as-is.
     */
    private fun reindexFinishedMessage(objectType: ZoekObjectType, summary: ReindexSummary?): String {
        val withSummary = summary?.let { (successCount, skippedCount, totalCount) ->
            val errorCount = (totalCount - successCount - skippedCount).coerceAtLeast(0)
            "[$objectType] Reindexing finished. Reindexed: $successCount / $totalCount, skipped: $skippedCount, " +
                "not reindexed because of errors: $errorCount"
        } ?: "[$objectType] Reindexing aborted"
        return "$withSummary. Solr index contains ${countInSolrIndex(objectType) ?: "unknown"} " +
            "documents of type '$objectType'."
    }

    /**
     * Commits (when [summary] is non-null) and logs the "Reindexing finished" message for [objectType].
     * Shared by `IndexingService.reindexReserved` (a single independently reindexed object type) and
     * [ZaakGedrevenReindexService] (each object type covered by the zaak-driven combined pass), so both
     * report completion identically.
     */
    internal fun finishReindex(objectType: ZoekObjectType, summary: ReindexSummary?) {
        // only commit when reindexing was actually attempted: existing entities are only
        // deleted once the total count is known (see e.g. reindexAllZaken), so a null summary
        // means nothing was deleted either, and there is nothing to make visible
        if (summary != null) {
            // ensure the removed/reindexed entities are visible to the searcher before reporting
            // the finished Solr document count, since bulk (re)indexing never commits per page.
            // Best-effort: a commit failure here must not discard the reindexing work already
            // done, nor abort the remaining object types in reindexAll() - the new/removed
            // entities simply become visible whenever Solr's own autoCommit next fires instead.
            continueOnExceptions(objectType) { commit() }
        }
        LOG.info(reindexFinishedMessage(objectType, summary))
    }

    internal fun addToSolrIndex(zoekObjecten: List<ZoekObject?>, performCommit: Boolean) {
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

    internal fun removeFromSolrIndex(id: String) {
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

    /**
     * Returns an `isZaakspecifiekGeautoriseerd` lookup that memoizes [ZrcClientService.isZaakspecifiekGeautoriseerd]
     * per zaak UUID, so that converting several zoekobjecten linked to the same zaak shares one call. Backed
     * by a [ConcurrentHashMap] so that it is also safe to share across [convertObjects]' concurrent page
     * conversions, not just sequential callers.
     */
    internal fun memoizedIsZaakspecifiekGeautoriseerd(): (UUID) -> Boolean {
        val isZaakspecifiekGeautoriseerdByZaakUUID = ConcurrentHashMap<UUID, Boolean>()
        return { zaakUUID ->
            isZaakspecifiekGeautoriseerdByZaakUUID.computeIfAbsent(zaakUUID, zrcClientService::isZaakspecifiekGeautoriseerd)
        }
    }

    internal fun reindexAllZaken(): ReindexSummary? {
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

    /**
     * Reindexes every informatieobject, sharing one memoized `isZaakspecifiekGeautoriseerd` lookup across
     * every page of the reindex, instead of each document's conversion deriving the flag on its own — this
     * reindex can cover every informatieobject in the environment, so several documents linked to the same
     * zaak sharing one ZGW call matters here far more than within a single page.
     */
    internal fun reindexAllInformatieobjecten(): ReindexSummary? {
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

        val isZaakspecifiekGeautoriseerd = memoizedIsZaakspecifiekGeautoriseerd()
        var counts = ReindexCounts()
        for (pageNumber in ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS..numberOfPages) {
            continueOnExceptions(ZoekObjectType.DOCUMENT) {
                reindexInformatieobjectenPage(pageNumber, numberOfInformatieobjecten, isZaakspecifiekGeautoriseerd)
            }?.let { counts += it }
        }
        return ReindexSummary(counts.successCount, counts.skippedCount, numberOfInformatieobjecten)
    }

    private fun reindexInformatieobjectenPage(
        pageNumber: Int,
        totalCount: Int,
        isZaakspecifiekGeautoriseerd: (UUID) -> Boolean
    ): ReindexCounts {
        val informationObjectsResults = drcClientService.listEnkelvoudigInformatieObjecten(
            EnkelvoudigInformatieobjectListParameters().apply { page = pageNumber }
        )
        val ids = informationObjectsResults.results().map { it.url.extractUuid().toString() }
        val counts = indexeerDirectCountingSuccesses(ids, ZoekObjectType.DOCUMENT, isZaakspecifiekGeautoriseerd)
        val progress = (pageNumber - ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS) * Results.DEFAULT_ZGW_PAGE_SIZE + ids.size
        LOG.info("[${ZoekObjectType.DOCUMENT}] Reindexed: $progress / $totalCount")
        return counts
    }

    /**
     * Reindexes every open taak, sharing one memoized `isZaakspecifiekGeautoriseerd` lookup across every
     * page of the reindex, instead of each taak's conversion deriving the flag on its own — several open
     * taken of the same zaak landing in different pages still share one ZGW call this way.
     */
    internal fun reindexAllTaken(): ReindexSummary? {
        val numberOfTasks = continueOnExceptions(ZoekObjectType.TAAK) { flowableTaskService.countOpenTasks() }
        if (numberOfTasks == null) {
            LOG.warning("[${ZoekObjectType.TAAK}] Cannot find tasks count. Aborting reindexing")
            return null
        }
        deleteExistingEntities(ZoekObjectType.TAAK)

        val numberOfPages: Int = (numberOfTasks.toInt() + TAKEN_MAX_RESULTS - 1) / TAKEN_MAX_RESULTS

        val isZaakspecifiekGeautoriseerd = memoizedIsZaakspecifiekGeautoriseerd()
        var counts = ReindexCounts()
        for (pageNumber in 0 until numberOfPages) {
            continueOnExceptions(ZoekObjectType.TAAK) {
                reindexTakenPage(pageNumber, numberOfTasks.toInt(), isZaakspecifiekGeautoriseerd)
            }?.let { counts += it }
        }
        return ReindexSummary(counts.successCount, counts.skippedCount, numberOfTasks.toInt())
    }

    private fun reindexTakenPage(
        pageNumber: Int,
        totalCount: Int,
        isZaakspecifiekGeautoriseerd: (UUID) -> Boolean
    ): ReindexCounts {
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
        val counts = indexeerDirectCountingSuccesses(tasks.map { it.id }, ZoekObjectType.TAAK, isZaakspecifiekGeautoriseerd)
        val progress = firstResult + tasks.size
        LOG.info("[${ZoekObjectType.TAAK}] Reindexed: $progress / $totalCount")
        return counts
    }

    @Suppress("TooGenericExceptionCaught")
    internal fun <T> runTranslatingToIndexingException(fn: () -> T): T {
        try {
            return fn()
        } catch (indexingException: IndexingException) {
            throw indexingException
        } catch (exception: Exception) {
            throw IndexingException(IndexingService.SOLR_INDEXING_ERROR_MESSAGE, exception)
        }
    }

    internal fun <T> continueOnExceptions(
        objectType: ZoekObjectType,
        message: String = "Error during indexing",
        fn: () -> T
    ): T? =
        try {
            runTranslatingToIndexingException { fn() }
        } catch (indexingException: IndexingException) {
            LOG.log(Level.WARNING, "[$objectType] $message", indexingException)
            null
        }
}
