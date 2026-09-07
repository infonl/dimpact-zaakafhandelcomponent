/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search

import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.authentication.LoggedInUserProvider.Companion.systemUser
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
 * Public entry point for indexing/reindexing ZAC's Solr search index. The generic Solr/conversion
 * machinery and the independent per-object-type reindex algorithms live in [ReindexSupportService]; the
 * `ZAAK`+`TAAK`+`DOCUMENT` combined zaak-driven reindex pass lives in [ZaakGedrevenReindexService]. This
 * class owns the public API surface, the reindex reservation bookkeeping ([reindexingViewfinder]), and the
 * background-launch coroutine machinery, delegating the actual indexing work to those two collaborators.
 */
@Singleton
@AllOpen
@Suppress("TooManyFunctions")
class IndexingService @Inject constructor(
    private val reindexSupportService: ReindexSupportService,
    private val zaakGedrevenReindexService: ZaakGedrevenReindexService,
    private val zrcClientService: ZrcClientService,
    private val flowableTaskService: FlowableTaskService,
    private val documentZoekObjectConverter: DocumentZoekObjectConverter,
    private val zaakZoekObjectConverter: ZaakZoekObjectConverter,
    private val taakZoekObjectConverter: TaakZoekObjectConverter,

    /**
     * Declare a Kotlin coroutine dispatcher here so that it can be overridden in unit tests with a test dispatcher
     * while in normal operation it will be injected using [nl.info.zac.util.CoroutineDispatcherProducer].
     */
    private val dispatcher: CoroutineDispatcher
) {
    companion object {
        const val SOLR_CORE = "zac"
        const val SOLR_INDEXING_ERROR_MESSAGE = "Error occurred during Solr indexing"

        private val LOG = Logger.getLogger(IndexingService::class.java.name)
        private val reindexingViewfinder = ConcurrentHashMap.newKeySet<ZoekObjectType>()
    }

    /**
     * Owns every background reindex launched via [reindexAsync]/[reindexAllAsync], so that they can be
     * cancelled together on [shutdown] instead of leaking daemon coroutines (and the deployment classloader
     * they pin) past application undeploy. [exceptionHandler] is the single backstop that logs any failure
     * that escapes a launched reindex since a fire-and-forget coroutine has no caller to propagate to.
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        LOG.log(Level.SEVERE, "Unexpected failure while reindexing", throwable)
    }
    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher + exceptionHandler)

    @PreDestroy
    fun shutdown() {
        coroutineScope.cancel()
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
        reindexSupportService.addToSolrIndex(
            reindexSupportService.getConverter(objectType).let { converter ->
                listOf(
                    reindexSupportService.continueOnExceptions(objectType) { converter.convert(objectId) }
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
        reindexSupportService.addToSolrIndex(
            reindexSupportService.convertObjects(objectIds, objectType).zoekObjecten(),
            performCommit
        )

    /**
     * Launches [reindexAll] in the background on [coroutineScope] and returns immediately. Any
     * failure that escapes [reindexAll] itself (it already catches per object type) is logged by
     * [exceptionHandler], the same backstop used by [reindexAsync].
     *
     * @param objectTypes the object types to reindex; defaults to all object types
     */
    fun reindexAllAsync(objectTypes: Set<ZoekObjectType> = ZoekObjectType.entries.toSet()) {
        coroutineScope.launch {
            reindexAll(objectTypes)
        }
    }

    /**
     * Reindexes all object types (`ZAAK`, `TAAK`, `DOCUMENT`) as a single, complete reindexing
     * process: logs when the complete process starts and finishes, in addition to the existing
     * per-object-type logging (including Solr document counts) performed by [reindex].
     *
     * When [objectTypes] includes `ZAAK` together with `TAAK` and/or `DOCUMENT`, those are reindexed
     * through [reindexCombined], which retrieves each zaak from ZGW at most once and reuses it for that
     * zaak's own reindex as well as its taken and documenten, instead of each taak/document independently
     * retrieving the same zaak again. Any object type not covered by that combination (e.g. `TAAK` or
     * `DOCUMENT` requested without `ZAAK`) still goes through the independent [reindex] path.
     *
     * @param objectTypes the object types to reindex; defaults to all object types
     */
    @Suppress("TooGenericExceptionCaught")
    fun reindexAll(objectTypes: Set<ZoekObjectType> = ZoekObjectType.entries.toSet()) {
        val orderedObjectTypes = objectTypes.sorted()
        LOG.info("Complete reindexing process started for object types: $orderedObjectTypes")
        val combinedObjectTypes = combinableObjectTypes(objectTypes)
        if (combinedObjectTypes.isNotEmpty()) {
            try {
                reindexCombined(combinedObjectTypes)
            } catch (exception: Exception) {
                LOG.log(
                    Level.SEVERE,
                    "[$combinedObjectTypes] Reindexing failed, continuing with remaining object types",
                    exception
                )
            }
        }
        orderedObjectTypes.filterNot { it in combinedObjectTypes }.forEach(::reindexOrLogFailure)
        LOG.info("Complete reindexing process finished for object types: $orderedObjectTypes")
    }

    /**
     * The subset of [objectTypes] that [reindexCombined] can reindex together: `ZAAK` together with
     * `TAAK` and/or `DOCUMENT`. Returns an empty set when `ZAAK` is not requested together with at least
     * one of the other two, since there is then no zaak-pass retrieval for `TAAK`/`DOCUMENT` to reuse.
     */
    private fun combinableObjectTypes(objectTypes: Set<ZoekObjectType>): Set<ZoekObjectType> =
        if (ZoekObjectType.ZAAK in objectTypes &&
            (ZoekObjectType.TAAK in objectTypes || ZoekObjectType.DOCUMENT in objectTypes)
        ) {
            objectTypes.intersect(setOf(ZoekObjectType.ZAAK, ZoekObjectType.TAAK, ZoekObjectType.DOCUMENT))
        } else {
            emptySet()
        }

    /**
     * Runs [reindex] for [objectType], logging (rather than propagating) any failure that escapes it, so
     * that [reindexAll] continues with the remaining object types regardless.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun reindexOrLogFailure(objectType: ZoekObjectType) {
        try {
            reindex(objectType)
        } catch (exception: Exception) {
            // catches more than IndexingException on purpose
            // an unguarded exception anywhere in reindex() must not abort the reindexing process
            LOG.log(
                Level.SEVERE,
                "[$objectType] Reindexing failed, continuing with remaining object types",
                exception
            )
        }
    }

    /**
     * Reindexes [objectTypes] (`ZAAK` together with `TAAK` and/or `DOCUMENT`) as one zaak-driven combined
     * pass via [ZaakGedrevenReindexService.reindex], reserving all of [objectTypes] in
     * [reindexingViewfinder] together so that a standalone trigger for any of them is rejected as "still
     * in progress" for the duration of the combined pass, consistent with [reindex]'s own single-type
     * reservation. If any of [objectTypes] is already in progress, none are reserved here; each is instead
     * reindexed independently via [reindexOrLogFailure], so the free ones still reindex and the busy one
     * logs "still in progress" exactly as [reindex] does on its own.
     */
    private fun reindexCombined(objectTypes: Set<ZoekObjectType>) {
        val reserved = mutableSetOf<ZoekObjectType>()
        val allReserved = objectTypes.all { objectType ->
            reindexingViewfinder.add(objectType).also { added -> if (added) reserved += objectType }
        }
        if (!allReserved) {
            reserved.forEach(reindexingViewfinder::remove)
            objectTypes.sorted().forEach(::reindexOrLogFailure)
            return
        }
        try {
            systemUser.set(true)
            zaakGedrevenReindexService.reindex(
                ReindexScope(
                    includeTaken = ZoekObjectType.TAAK in objectTypes,
                    includeDocumenten = ZoekObjectType.DOCUMENT in objectTypes
                )
            )
        } finally {
            systemUser.remove()
            reserved.forEach(reindexingViewfinder::remove)
        }
    }

    /**
     * Launches [objectType]'s reindex in the background on [coroutineScope] and returns immediately,
     * unless it is already in progress, in which case nothing is launched.
     *
     * @return `true` if reindexing was started, `false` if it was already running for [objectType]
     */
    fun reindexAsync(objectType: ZoekObjectType): Boolean {
        if (!reindexingViewfinder.add(objectType)) {
            LOG.warning("[$objectType] Reindexing not started, still in progress")
            return false
        }
        coroutineScope.launch {
            try {
                reindexReserved(objectType)
            } finally {
                reindexingViewfinder.remove(objectType)
            }
        }
        return true
    }

    fun reindex(objectType: ZoekObjectType) {
        if (!reindexingViewfinder.add(objectType)) {
            LOG.warning("[$objectType] Reindexing not started, still in progress")
            return
        }
        try {
            reindexReserved(objectType)
        } finally {
            reindexingViewfinder.remove(objectType)
        }
    }

    /**
     * Performs [objectType]'s reindex. Only called once [objectType] has been reserved in
     * [reindexingViewfinder], by either [reindex] or [reindexAsync].
     */
    private fun reindexReserved(objectType: ZoekObjectType) {
        try {
            systemUser.set(true)
            LOG.info(reindexSupportService.reindexStartedMessage(objectType))
            val summary = when (objectType) {
                ZoekObjectType.ZAAK -> reindexSupportService.reindexAllZaken()
                ZoekObjectType.DOCUMENT -> reindexSupportService.reindexAllInformatieobjecten()
                ZoekObjectType.TAAK -> reindexSupportService.reindexAllTaken()
            }
            reindexSupportService.finishReindex(objectType, summary)
        } finally {
            systemUser.remove()
        }
    }

    /**
     * Reindexes the zaak and, when [inclusiefTaken], its open taken, sharing one memoized
     * `isZaakspecifiekGeautoriseerd` lookup between the zaak and all of its open taken instead of
     * each conversion deriving the flag on its own.
     *
     * @return `true` if the zaak itself was indexed successfully, `false` if that failed (already
     * logged by [ReindexSupportService.continueOnExceptions]). Whether its taken were reindexed
     * successfully is not part of this signal: a taak failure never aborts the remaining taken either,
     * consistent with [addOrUpdateTakenForZaak].
     */
    fun addOrUpdateZaak(zaakUUID: UUID, inclusiefTaken: Boolean): Boolean {
        val isZaakspecifiekGeautoriseerd = reindexSupportService.memoizedIsZaakspecifiekGeautoriseerd()
        val zaakIndexed = reindexSupportService.continueOnExceptions(ZoekObjectType.ZAAK) {
            reindexSupportService.addToSolrIndex(
                listOf(
                    reindexSupportService.continueOnExceptions(ZoekObjectType.ZAAK) {
                        zaakZoekObjectConverter.convert(zaakUUID.toString(), isZaakspecifiekGeautoriseerd)
                    }
                ),
                performCommit = false
            )
        } != null
        if (inclusiefTaken) {
            flowableTaskService.listOpenTasksForZaak(zaakUUID)
                .map { it.id }
                .forEach { addOrUpdateTaak(it, isZaakspecifiekGeautoriseerd) }
        }
        return zaakIndexed
    }

    /**
     * Like [addOrUpdateZaak], but throws [IndexingException] when indexing the zaak itself failed,
     * for REST callers that must surface a Solr failure as an HTTP 500 instead of silently
     * responding with success.
     */
    fun addOrUpdateZaakOrThrow(zaakUUID: UUID, inclusiefTaken: Boolean) {
        if (!addOrUpdateZaak(zaakUUID, inclusiefTaken)) {
            throw IndexingException("[${ZoekObjectType.ZAAK}] Failed to index zaak '$zaakUUID'")
        }
    }

    /**
     * Reindexes both the open and the completed taken of a zaak, sharing one memoized
     * `isZaakspecifiekGeautoriseerd` lookup across all of them. Unlike [addOrUpdateZaak]'s
     * `inclusiefTaken` flag, this also covers completed taken, since a taak-level flag (such as
     * `taak_zaakspecifiekGeautoriseerd`) can go stale on a completed taak just as easily as on an
     * open one. Calling this on every zaak update would add a `HistoricTaskInstanceQuery` per
     * notificatie, so it is reserved for triggers where a completed taak can plausibly go stale,
     * such as a zaakeigenschap change.
     */
    fun addOrUpdateTakenForZaak(zaakUUID: UUID) {
        val isZaakspecifiekGeautoriseerd = reindexSupportService.memoizedIsZaakspecifiekGeautoriseerd()
        flowableTaskService.listTasksForZaak(zaakUUID)
            .map { it.id }
            .forEach { addOrUpdateTaak(it, isZaakspecifiekGeautoriseerd) }
    }

    fun addOrUpdateInformatieobject(informatieobjectUUID: UUID) =
        indexeerDirect(informatieobjectUUID.toString(), ZoekObjectType.DOCUMENT, false)

    fun addOrUpdateInformatieobjectByZaakinformatieobject(zaakinformatieobjectUUID: UUID) =
        addOrUpdateInformatieobject(
            zrcClientService.readZaakinformatieobject(zaakinformatieobjectUUID).informatieobject.extractUuid()
        )

    /**
     * Reindexes every document of a zaak, memoizing the `isZaakspecifiekGeautoriseerd` flag per zaak
     * UUID so that documents linked to the same zaak share one lookup, instead of each document's
     * conversion deriving the flag on its own. The flag is still looked up for whichever zaak
     * [DocumentZoekObjectConverter.convert] actually resolves the document against, since a document
     * can be linked to a zaak other than [zaakUUID].
     */
    fun addOrUpdateInformatieobjectenForZaak(zaakUUID: UUID) {
        val isZaakspecifiekGeautoriseerd = reindexSupportService.memoizedIsZaakspecifiekGeautoriseerd()
        zrcClientService.listZaakinformatieobjecten(zrcClientService.readZaak(zaakUUID)).forEach {
            reindexSupportService.continueOnExceptions(ZoekObjectType.DOCUMENT) {
                reindexSupportService.addToSolrIndex(
                    listOf(
                        reindexSupportService.continueOnExceptions(ZoekObjectType.DOCUMENT) {
                            documentZoekObjectConverter.convert(
                                it.informatieobject.extractUuid().toString(),
                                isZaakspecifiekGeautoriseerd
                            )
                        }
                    ),
                    performCommit = false
                )
            }
        }
    }

    /**
     * Launches [addOrUpdateInformatieobjectenForZaak] in the background on [coroutineScope] and returns
     * immediately. Any failure that escapes it is logged by [exceptionHandler], the same backstop used
     * by [reindexAsync]/[reindexAllAsync].
     */
    fun addOrUpdateInformatieobjectenForZaakAsync(zaakUUID: UUID) {
        coroutineScope.launch {
            addOrUpdateInformatieobjectenForZaak(zaakUUID)
        }
    }

    fun addOrUpdateTaak(taskID: String) = indexeerDirect(taskID, ZoekObjectType.TAAK, false)

    /**
     * Converts and indexes [taskID], looking up the zaakspecifiek geautoriseerd flag through
     * [isZaakspecifiekGeautoriseerd] instead of always deriving it directly. Used by [addOrUpdateZaak]
     * and [addOrUpdateTakenForZaak] to share one memoized lookup across the taken of one zaak.
     */
    private fun addOrUpdateTaak(taskID: String, isZaakspecifiekGeautoriseerd: (UUID) -> Boolean) =
        reindexSupportService.continueOnExceptions(ZoekObjectType.TAAK) {
            reindexSupportService.addToSolrIndex(
                listOf(
                    reindexSupportService.continueOnExceptions(ZoekObjectType.TAAK) {
                        taakZoekObjectConverter.convert(taskID, isZaakspecifiekGeautoriseerd)
                    }
                ),
                performCommit = false
            )
        }

    fun removeZaak(zaakUUID: UUID) = reindexSupportService.removeFromSolrIndex(zaakUUID.toString())

    fun removeInformatieobject(informatieobjectUUID: UUID) = reindexSupportService.removeFromSolrIndex(informatieobjectUUID.toString())

    fun removeTaak(taskID: String) = reindexSupportService.removeFromSolrIndex(taskID)

    fun commit() = reindexSupportService.commit()
}
