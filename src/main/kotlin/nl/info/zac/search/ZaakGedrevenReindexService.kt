/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search

import jakarta.inject.Inject
import jakarta.inject.Singleton
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
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.generated.ZaakInformatieObject
import nl.info.zac.search.converter.DocumentZoekObjectConverter
import nl.info.zac.search.converter.TaakZoekObjectConverter
import nl.info.zac.search.converter.ZaakZoekObjectConverter
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Which of `TAAK`/`DOCUMENT` to reindex together with `ZAAK` in [ZaakGedrevenReindexService.reindex] and the
 * functions it drives. Bundling the two flags in one value type, instead of passing them as adjacent
 * `Boolean` parameters through every function in that call chain, removes the risk of one being swapped
 * for the other undetected at any call site.
 */
internal data class ReindexScope(val includeTaken: Boolean, val includeDocumenten: Boolean)

/**
 * Reindexes `ZAAK` together with, when requested, `TAAK` and/or `DOCUMENT` as one zaak-driven combined pass,
 * retrieving each zaak from ZGW at most once and reusing it for the zaak's own reindex as well as its open
 * taken and its linked documenten, instead of each taak/document independently retrieving the same zaak
 * again. Invoked from `IndexingService.reindexCombined`, which reserves the requested object types and
 * wraps the call in `systemUser`.
 */
@Singleton
@AllOpen
@Suppress("TooManyFunctions")
class ZaakGedrevenReindexService @Inject constructor(
    private val reindexSupportService: ReindexSupportService,
    private val zrcClientService: ZrcClientService,
    private val drcClientService: DrcClientService,
    private val flowableTaskService: FlowableTaskService,
    private val documentZoekObjectConverter: DocumentZoekObjectConverter,
    private val zaakZoekObjectConverter: ZaakZoekObjectConverter,
    private val taakZoekObjectConverter: TaakZoekObjectConverter
) {
    companion object {
        // deliberately keeps the pre-split logger name/identity so that log-based tests and any external
        // log-based tooling keyed off "nl.info.zac.search.IndexingService" keep working unchanged
        private val LOG = Logger.getLogger(IndexingService::class.java.name)
    }

    private data class ZakenTakenDocumentenCounts(
        val zaakCounts: ReindexCounts = ReindexCounts(),
        val takenCounts: ReindexCounts = ReindexCounts(),
        val documentenCounts: ReindexCounts = ReindexCounts()
    ) {
        operator fun plus(other: ZakenTakenDocumentenCounts) =
            ZakenTakenDocumentenCounts(
                zaakCounts + other.zaakCounts,
                takenCounts + other.takenCounts,
                documentenCounts + other.documentenCounts
            )
    }

    /**
     * The per-zaak conversion outcomes produced by [reindexZaakTakenDocumenten]: the zaak itself, and the
     * outcomes of its open taken and its linked documenten.
     */
    private data class ReindexZaakTakenDocumentenOutcome(
        val zaakOutcome: ConversionOutcome,
        val takenOutcomes: List<ConversionOutcome>,
        val documentenOutcomes: List<ConversionOutcome>
    )

    private data class TaakDocumentReindexPlan(
        val numberOfTasks: Long?,
        val numberOfInformatieobjecten: Int?,
        val effectiveScope: ReindexScope
    )

    /**
     * Reindexes `ZAAK` together with, when requested by [scope], `TAAK` and/or `DOCUMENT`, retrieving each
     * zaak from ZGW at most once and reusing it for the zaak's own reindex as well as its open taken and
     * its linked documenten (see [reindexZaakTakenDocumenten]), instead of each taak/document independently
     * retrieving the same zaak again.
     *
     * When the zaak count cannot be determined, `ZAAK` is reported as aborted (as
     * [ReindexSupportService.reindexAllZaken] does on its own), and `TAAK`/`DOCUMENT` fall back to their
     * existing independent [ReindexSupportService.reindexAllTaken]/[ReindexSupportService.reindexAllInformatieobjecten]
     * passes for this run instead of being skipped, consistent with `solr-reindexing-observability`'s
     * "remaining object types still reindex" behavior.
     *
     * When the zaak count succeeds but the `TAAK` and/or `DOCUMENT` count fails, that object type is left
     * untouched for this run - neither deleted nor repopulated - and reported as aborted, the same way
     * [ReindexSupportService.reindexAllTaken]/[ReindexSupportService.reindexAllInformatieobjecten] abort
     * without touching existing data when their own count fails.
     */
    internal fun reindex(scope: ReindexScope) {
        LOG.info(reindexSupportService.reindexStartedMessage(ZoekObjectType.ZAAK))
        if (scope.includeTaken) LOG.info(reindexSupportService.reindexStartedMessage(ZoekObjectType.TAAK))
        if (scope.includeDocumenten) LOG.info(reindexSupportService.reindexStartedMessage(ZoekObjectType.DOCUMENT))

        val numberOfZaken = reindexSupportService.continueOnExceptions(ZoekObjectType.ZAAK) { countZaken() }
        if (numberOfZaken == null) {
            reindexFallback(scope)
            return
        }

        // captured before any deletion happens, consistent with reindexAllTaken/reindexAllInformatieobjecten
        val plan = determineTaakDocumentReindexPlan(scope)

        reindexSupportService.deleteExistingEntities(ZoekObjectType.ZAAK)
        if (plan.effectiveScope.includeTaken) reindexSupportService.deleteExistingEntities(ZoekObjectType.TAAK)
        if (plan.effectiveScope.includeDocumenten) reindexSupportService.deleteExistingEntities(ZoekObjectType.DOCUMENT)

        // tracks which informatieobjecten the zaak-driven stage already indexed, so the orphan sweep
        // below does not reconvert them - see reindexInformatieobjectenOrphanSweep
        val alreadyIndexedInformatieobjectUUIDs = ConcurrentHashMap.newKeySet<UUID>()
        val counts = reindexPages(
            numberOfZaken,
            plan.effectiveScope,
            alreadyIndexedInformatieobjectUUIDs
        )

        reindexSupportService.finishReindex(
            ZoekObjectType.ZAAK,
            ReindexSummary(counts.zaakCounts.successCount, counts.zaakCounts.skippedCount, numberOfZaken)
        )
        if (scope.includeTaken) {
            reindexSupportService.finishReindex(
                ZoekObjectType.TAAK,
                plan.numberOfTasks?.let {
                    ReindexSummary(counts.takenCounts.successCount, counts.takenCounts.skippedCount, it.toInt())
                }
            )
        }
        if (scope.includeDocumenten) {
            reindexSupportService.finishReindex(
                ZoekObjectType.DOCUMENT,
                plan.numberOfInformatieobjecten?.let { total ->
                    val documentenCounts = counts.documentenCounts +
                        reindexInformatieobjectenOrphanSweep(total, alreadyIndexedInformatieobjectUUIDs)
                    ReindexSummary(documentenCounts.successCount, documentenCounts.skippedCount, total)
                }
            )
        }
    }

    /**
     * Determines the `TAAK`/`DOCUMENT` counts for the combined zaak-driven pass, and the [ReindexScope]
     * that should actually be reindexed for this run - only the parts of [requestedScope] whose own count
     * succeeded, so a count failure leaves that type untouched (see [reindex]'s KDoc) instead of deleting
     * and repopulating data whose true total is unknown.
     */
    private fun determineTaakDocumentReindexPlan(requestedScope: ReindexScope): TaakDocumentReindexPlan {
        val numberOfTasks = if (requestedScope.includeTaken) {
            reindexSupportService.continueOnExceptions(ZoekObjectType.TAAK) { flowableTaskService.countOpenTasks() }
        } else {
            null
        }
        val numberOfInformatieobjecten = if (requestedScope.includeDocumenten) {
            reindexSupportService.continueOnExceptions(ZoekObjectType.DOCUMENT) { countInformatieobjecten() }
        } else {
            null
        }
        if (requestedScope.includeTaken && numberOfTasks == null) {
            LOG.warning("[${ZoekObjectType.TAAK}] Cannot find tasks count! Aborting reindexing")
        }
        if (requestedScope.includeDocumenten && numberOfInformatieobjecten == null) {
            LOG.warning("[${ZoekObjectType.DOCUMENT}] Cannot find information objects count! Aborting reindexing")
        }
        return TaakDocumentReindexPlan(
            numberOfTasks = numberOfTasks,
            numberOfInformatieobjecten = numberOfInformatieobjecten,
            effectiveScope = ReindexScope(
                includeTaken = requestedScope.includeTaken && numberOfTasks != null,
                includeDocumenten = requestedScope.includeDocumenten && numberOfInformatieobjecten != null
            )
        )
    }

    /**
     * Falls back to [ReindexSupportService.reindexAllTaken]/[ReindexSupportService.reindexAllInformatieobjecten]
     * for whichever of [scope]'s `TAAK`/`DOCUMENT` were requested, since the zaak count being unavailable
     * means there is no zaak-driven pass for them to be part of - see [reindex]'s KDoc.
     */
    private fun reindexFallback(scope: ReindexScope) {
        LOG.warning("[${ZoekObjectType.ZAAK}] Cannot find zaken count! Aborting reindexing")
        reindexSupportService.finishReindex(ZoekObjectType.ZAAK, null)
        if (scope.includeTaken) {
            reindexSupportService.finishReindex(ZoekObjectType.TAAK, reindexSupportService.reindexAllTaken())
        }
        if (scope.includeDocumenten) {
            reindexSupportService.finishReindex(ZoekObjectType.DOCUMENT, reindexSupportService.reindexAllInformatieobjecten())
        }
    }

    private fun countZaken(): Int =
        zrcClientService.listZakenUuids(
            ZaakListParameters().apply {
                ordering = "-identificatie"
                page = ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS
            }
        ).count()

    private fun countInformatieobjecten(): Int =
        drcClientService.listEnkelvoudigInformatieObjecten(
            EnkelvoudigInformatieobjectListParameters().apply { page = ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS }
        ).count()

    private fun reindexPages(
        numberOfZaken: Int,
        scope: ReindexScope,
        alreadyIndexedInformatieobjectUUIDs: MutableSet<UUID>
    ): ZakenTakenDocumentenCounts {
        val numberOfPages: Int = (numberOfZaken + Results.DEFAULT_ZGW_PAGE_SIZE.toInt() - 1) /
            Results.DEFAULT_ZGW_PAGE_SIZE.toInt()
        var counts = ZakenTakenDocumentenCounts()
        for (pageNumber in ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS..numberOfPages) {
            reindexSupportService.continueOnExceptions(ZoekObjectType.ZAAK) {
                reindexPage(
                    pageNumber,
                    numberOfZaken,
                    scope,
                    alreadyIndexedInformatieobjectUUIDs
                )
            }?.let { counts += it }
        }
        return counts
    }

    private fun reindexPage(
        pageNumber: Int,
        totalCount: Int,
        scope: ReindexScope,
        alreadyIndexedInformatieobjectUUIDs: MutableSet<UUID>
    ): ZakenTakenDocumentenCounts {
        val zaakUUIDs = zrcClientService.listZakenUuids(
            ZaakListParameters().apply {
                ordering = "-identificatie"
                page = pageNumber
            }
        ).results().map { it.uuid }
        val isZaakspecifiekGeautoriseerd = reindexSupportService.memoizedIsZaakspecifiekGeautoriseerd()

        val pageResults = runBlocking(reindexSupportService.pageConversionDispatcher) {
            zaakUUIDs.map { zaakUUID ->
                async {
                    reindexZaakTakenDocumenten(
                        zaakUUID,
                        scope,
                        isZaakspecifiekGeautoriseerd,
                        alreadyIndexedInformatieobjectUUIDs
                    )
                }
            }.awaitAll()
        }

        val zaakOutcomes = pageResults.map { it.zaakOutcome }
        val takenOutcomes = pageResults.flatMap { it.takenOutcomes }
        val documentenOutcomes = pageResults.flatMap { it.documentenOutcomes }
        reindexSupportService.addToSolrIndex(zaakOutcomes.zoekObjecten(), performCommit = false)
        reindexSupportService.addToSolrIndex(takenOutcomes.zoekObjecten(), performCommit = false)
        reindexSupportService.addToSolrIndex(documentenOutcomes.zoekObjecten(), performCommit = false)

        val progress = (pageNumber - ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS) * Results.DEFAULT_ZGW_PAGE_SIZE + zaakUUIDs.size
        LOG.info("[${ZoekObjectType.ZAAK}] Reindexed: $progress / $totalCount ")

        return ZakenTakenDocumentenCounts(
            zaakCounts = ReindexCounts(
                successCount = zaakOutcomes.count { it is ConversionOutcome.Converted },
                skippedCount = zaakOutcomes.count { it is ConversionOutcome.Skipped }
            ),
            takenCounts = ReindexCounts(
                successCount = takenOutcomes.count { it is ConversionOutcome.Converted },
                skippedCount = takenOutcomes.count { it is ConversionOutcome.Skipped }
            ),
            documentenCounts = ReindexCounts(
                successCount = documentenOutcomes.count { it is ConversionOutcome.Converted },
                skippedCount = documentenOutcomes.count { it is ConversionOutcome.Skipped }
            )
        )
    }

    /**
     * Reindexes [zaakUUID] and, when requested, its open taken and its linked documenten, retrieving the
     * zaak once via [ZrcClientService.readZaak] and reusing it for all three conversions. If retrieving or
     * converting the zaak itself fails, its taken and documenten are not attempted either for that zaak -
     * consistent with them belonging to the zaak, and avoiding retrieval calls likely to fail again for
     * the same zaak. A listing failure for the taken or documenten of an otherwise successfully indexed
     * zaak only drops that piece for this zaak (logged, not counted as a conversion error), the same way a
     * page-listing failure is handled by [ReindexSupportService.reindexAllTaken]/
     * [ReindexSupportService.reindexAllInformatieobjecten].
     */
    private fun reindexZaakTakenDocumenten(
        zaakUUID: UUID,
        scope: ReindexScope,
        isZaakspecifiekGeautoriseerd: (UUID) -> Boolean,
        alreadyIndexedInformatieobjectUUIDs: MutableSet<UUID>
    ): ReindexZaakTakenDocumentenOutcome {
        val zaakConversion = try {
            reindexSupportService.runTranslatingToIndexingException {
                val zaak = zrcClientService.readZaak(zaakUUID)
                zaak to zaakZoekObjectConverter.convert(zaak, isZaakspecifiekGeautoriseerd)
            }
        } catch (indexingException: IndexingException) {
            LOG.log(Level.WARNING, "[${ZoekObjectType.ZAAK}] Error during indexing", indexingException)
            null
        }
        if (zaakConversion == null) {
            return ReindexZaakTakenDocumentenOutcome(ConversionOutcome.Errored, emptyList(), emptyList())
        }
        val (zaak, zaakZoekObject) = zaakConversion

        val takenOutcomes = if (scope.includeTaken) {
            reindexSupportService.continueOnExceptions(ZoekObjectType.TAAK) {
                flowableTaskService.listOpenTasksForZaak(zaakUUID)
            }
                .orEmpty()
                .map { task -> convertTaak(task.id, zaak, isZaakspecifiekGeautoriseerd) }
        } else {
            emptyList()
        }

        val documentenOutcomes = if (scope.includeDocumenten) {
            reindexSupportService.continueOnExceptions(ZoekObjectType.DOCUMENT) {
                zrcClientService.listZaakinformatieobjecten(zaak)
            }
                .orEmpty()
                .mapNotNull { zaakInformatieobject ->
                    val informatieobjectUUID = zaakInformatieobject.informatieobject.extractUuid()
                    // an informatieobject can be linked to more than one zaak in ZGW; claiming the UUID
                    // here ensures it is only converted/counted once for this run, via whichever of its
                    // zaken is processed first, instead of once per zaak it is linked to
                    if (alreadyIndexedInformatieobjectUUIDs.add(informatieobjectUUID)) {
                        convertDocument(zaakInformatieobject, zaak, isZaakspecifiekGeautoriseerd)
                    } else {
                        null
                    }
                }
        } else {
            emptyList()
        }

        return ReindexZaakTakenDocumentenOutcome(ConversionOutcome.Converted(zaakZoekObject), takenOutcomes, documentenOutcomes)
    }

    private fun convertTaak(taskId: String, zaak: Zaak, isZaakspecifiekGeautoriseerd: (UUID) -> Boolean): ConversionOutcome =
        try {
            ConversionOutcome.Converted(
                reindexSupportService.runTranslatingToIndexingException {
                    taakZoekObjectConverter.convert(taskId, zaak, isZaakspecifiekGeautoriseerd)
                }
            )
        } catch (indexingException: IndexingException) {
            LOG.log(Level.WARNING, "[${ZoekObjectType.TAAK}] Error during indexing", indexingException)
            ConversionOutcome.Errored
        }

    private fun convertDocument(
        zaakInformatieobject: ZaakInformatieObject,
        zaak: Zaak,
        isZaakspecifiekGeautoriseerd: (UUID) -> Boolean
    ): ConversionOutcome =
        try {
            ConversionOutcome.Converted(
                reindexSupportService.runTranslatingToIndexingException {
                    documentZoekObjectConverter.convert(zaakInformatieobject, zaak, isZaakspecifiekGeautoriseerd)
                }
            )
        } catch (indexingException: IndexingException) {
            LOG.log(Level.WARNING, "[${ZoekObjectType.DOCUMENT}] Error during indexing", indexingException)
            ConversionOutcome.Errored
        }

    /**
     * Finds and accounts for documents that have no linked zaak ("orphans"), by paging through the DRC's
     * full informatieobject listing exactly as [ReindexSupportService.reindexAllInformatieobjecten] does,
     * but skipping any informatieobject UUID already reindexed via a zaak in
     * [alreadyIndexedInformatieobjectUUIDs]. Only an orphan (or a document created after the zaak-driven
     * stage already passed its zaak) reaches [DocumentZoekObjectConverter.convert] here, which still
     * returns `null` for a document with no linked zaak, counted as skipped exactly as it is today.
     */
    private fun reindexInformatieobjectenOrphanSweep(
        totalCount: Int,
        alreadyIndexedInformatieobjectUUIDs: Set<UUID>
    ): ReindexCounts {
        val numberOfPages: Int = (totalCount + Results.DEFAULT_ZGW_PAGE_SIZE.toInt() - 1) /
            Results.DEFAULT_ZGW_PAGE_SIZE.toInt()
        var counts = ReindexCounts()
        for (pageNumber in ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS..numberOfPages) {
            reindexSupportService.continueOnExceptions(ZoekObjectType.DOCUMENT) {
                reindexInformatieobjectenOrphanSweepPage(pageNumber, alreadyIndexedInformatieobjectUUIDs)
            }?.let { counts += it }
        }
        return counts
    }

    private fun reindexInformatieobjectenOrphanSweepPage(
        pageNumber: Int,
        alreadyIndexedInformatieobjectUUIDs: Set<UUID>
    ): ReindexCounts {
        val ids = drcClientService.listEnkelvoudigInformatieObjecten(
            EnkelvoudigInformatieobjectListParameters().apply { page = pageNumber }
        ).results()
            .map { it.url.extractUuid() }
            .filterNot { it in alreadyIndexedInformatieobjectUUIDs }
            .map { it.toString() }
        return reindexSupportService.indexeerDirectCountingSuccesses(ids, ZoekObjectType.DOCUMENT)
    }
}
