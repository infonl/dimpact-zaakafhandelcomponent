/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.runBlocking
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForReads
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.ZaakListParameters
import nl.info.client.zgw.zrc.model.ZaakUuid
import nl.info.zac.search.converter.DocumentZoekObjectConverter
import nl.info.zac.search.converter.TaakZoekObjectConverter
import nl.info.zac.search.converter.ZaakZoekObjectConverter
import nl.info.zac.search.model.createDocumentZoekObject
import nl.info.zac.search.model.createTaakZoekObject
import nl.info.zac.search.model.createZaakAutorisatieGegevens
import nl.info.zac.search.model.createZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import org.flowable.task.api.Task

private data class ZaakGedrevenReindexServiceTestContext(
    val reindexSupportService: ReindexSupportService,
    val zrcClientService: ZrcClientService,
    val drcClientService: DrcClientService,
    val flowableTaskService: FlowableTaskService,
    val documentZoekObjectConverter: DocumentZoekObjectConverter,
    val zaakZoekObjectConverter: ZaakZoekObjectConverter,
    val taakZoekObjectConverter: TaakZoekObjectConverter,
    val zaakGedrevenReindexService: ZaakGedrevenReindexService
)

/**
 * Stubs [ReindexSupportService.continueOnExceptions] on the mocked [ReindexSupportService] to forward to
 * (and swallow exceptions from) the captured lambda, instead of actually running the real implementation -
 * the real class already has its own dedicated test coverage in ReindexSupportServiceTest. Every [reindex]
 * call reaches this at least once (for the zaken count), so this is safe to call unconditionally in every
 * test.
 */
@Suppress("SwallowedException")
private fun ZaakGedrevenReindexServiceTestContext.stubContinueOnExceptionsForwarding() {
    val continueOnExceptionsSlot = slot<() -> Any?>()
    every {
        reindexSupportService.continueOnExceptions<Any?>(any(), any(), capture(continueOnExceptionsSlot))
    } answers {
        try {
            continueOnExceptionsSlot.captured()
        } catch (exception: Exception) {
            null
        }
    }
}

/**
 * Stubs [ReindexSupportService.runTranslatingToIndexingException] on the mocked [ReindexSupportService] to
 * forward to, and translate exceptions from, the captured lambda, mirroring the real implementation. Only
 * reached once the zaak-driven pass actually walks at least one zaak, so this is a separate stub from
 * [stubContinueOnExceptionsForwarding] - added only by tests that reach that path, to avoid failing
 * `checkUnnecessaryStub()` in tests that never do (e.g. a count failure).
 */
private fun ZaakGedrevenReindexServiceTestContext.stubRunTranslatingToIndexingExceptionForwarding() {
    val runTranslatingSlot = slot<() -> Any?>()
    every {
        reindexSupportService.runTranslatingToIndexingException<Any?>(capture(runTranslatingSlot))
    } answers {
        try {
            runTranslatingSlot.captured()
        } catch (indexingException: IndexingException) {
            throw indexingException
        } catch (exception: Exception) {
            throw IndexingException(IndexingService.SOLR_INDEXING_ERROR_MESSAGE, exception)
        }
    }
}

/**
 * Stubs [ReindexSupportService.runConcurrentPageConversions] on the mocked [ReindexSupportService] to
 * forward to the captured items/conversion, running them sequentially rather than actually concurrently -
 * the real dispatcher-backed implementation already has its own dedicated test coverage in
 * ReindexSupportServiceTest.
 */
private fun ZaakGedrevenReindexServiceTestContext.stubRunConcurrentPageConversionsForwarding() {
    val itemsSlot = slot<List<Any?>>()
    val convertSlot = slot<suspend (Any?) -> Any?>()
    every {
        reindexSupportService.runConcurrentPageConversions<Any?, Any?>(capture(itemsSlot), capture(convertSlot))
    } answers {
        runBlocking { itemsSlot.captured.map { convertSlot.captured(it) } }
    }
}

/**
 * Stubs the [ReindexSupportService] members touched once the zaak-driven pass actually walks at least one
 * page of zaken, so that tests covering that path do not have to repeat this bundle individually.
 */
private fun ZaakGedrevenReindexServiceTestContext.stubZaakPageProcessing() {
    every { reindexSupportService.memoizedZaakAutorisatieGegevens() } returns { createZaakAutorisatieGegevens() }
    stubRunConcurrentPageConversionsForwarding()
    every { reindexSupportService.deleteExistingEntities(any()) } just Runs
    every { reindexSupportService.addToSolrIndex(any(), any()) } just Runs
}

private fun setupContext(): ZaakGedrevenReindexServiceTestContext {
    val reindexSupportService = mockk<ReindexSupportService>()
    val zrcClientService = mockk<ZrcClientService>()
    val drcClientService = mockk<DrcClientService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val documentZoekObjectConverter = mockk<DocumentZoekObjectConverter>()
    val zaakZoekObjectConverter = mockk<ZaakZoekObjectConverter>()
    val taakZoekObjectConverter = mockk<TaakZoekObjectConverter>()

    every { reindexSupportService.reindexStartedMessage(any()) } returns "fakeReindexStartedMessage"
    every { reindexSupportService.finishReindex(any(), any()) } just Runs
    every { reindexSupportService.zaakListParameters(any()) } answers {
        ZaakListParameters().apply {
            ordering = "-identificatie"
            page = firstArg()
        }
    }

    val zaakGedrevenReindexService = ZaakGedrevenReindexService(
        reindexSupportService,
        zrcClientService,
        drcClientService,
        flowableTaskService,
        documentZoekObjectConverter,
        zaakZoekObjectConverter,
        taakZoekObjectConverter
    )

    return ZaakGedrevenReindexServiceTestContext(
        reindexSupportService,
        zrcClientService,
        drcClientService,
        flowableTaskService,
        documentZoekObjectConverter,
        zaakZoekObjectConverter,
        taakZoekObjectConverter,
        zaakGedrevenReindexService
    )
}

class ZaakGedrevenReindexServiceTest : BehaviorSpec({
    afterEach { checkUnnecessaryStub() }

    given("reindex() when the zaken count cannot be determined, with TAAK and DOCUMENT both requested") {
        val ctx = setupContext()
        ctx.stubContinueOnExceptionsForwarding()
        every {
            ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>())
        } throws RuntimeException("fake zaken count failure")
        every { ctx.reindexSupportService.reindexAllTaken() } returns ReindexSummary(1, 0, 1)
        every { ctx.reindexSupportService.reindexAllInformatieobjecten() } returns ReindexSummary(2, 0, 2)

        `when`("reindex is called for ZAAK, TAAK and DOCUMENT") {
            ctx.zaakGedrevenReindexService.reindex(ReindexScope(includeTaken = true, includeDocumenten = true))

            then("ZAAK is reported as aborted, and TAAK/DOCUMENT fall back to their independent passes") {
                verify(exactly = 1) {
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.ZAAK, null)
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.TAAK, ReindexSummary(1, 0, 1))
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.DOCUMENT, ReindexSummary(2, 0, 2))
                    ctx.reindexSupportService.reindexAllTaken()
                    ctx.reindexSupportService.reindexAllInformatieobjecten()
                }
            }
        }
    }

    given("reindex() when the zaken count cannot be determined, with only TAAK requested") {
        val ctx = setupContext()
        ctx.stubContinueOnExceptionsForwarding()
        every {
            ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>())
        } throws RuntimeException("fake zaken count failure")
        every { ctx.reindexSupportService.reindexAllTaken() } returns null

        `when`("reindex is called for ZAAK and TAAK only") {
            ctx.zaakGedrevenReindexService.reindex(ReindexScope(includeTaken = true, includeDocumenten = false))

            then("only TAAK falls back to its independent pass; DOCUMENT is never touched") {
                verify(exactly = 1) {
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.ZAAK, null)
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.TAAK, null)
                    ctx.reindexSupportService.reindexAllTaken()
                }
                verify(exactly = 0) {
                    ctx.reindexSupportService.reindexAllInformatieobjecten()
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.DOCUMENT, any())
                }
            }
        }
    }

    given("reindex() with zero zaken, where the taak count cannot be determined") {
        val ctx = setupContext()
        ctx.stubContinueOnExceptionsForwarding()
        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(emptyList(), 0)
        every { ctx.flowableTaskService.countOpenTasks() } throws RuntimeException("fake taak count failure")
        every { ctx.reindexSupportService.deleteExistingEntities(any()) } just Runs

        `when`("reindex is called for ZAAK and TAAK") {
            ctx.zaakGedrevenReindexService.reindex(ReindexScope(includeTaken = true, includeDocumenten = false))

            then("ZAAK finishes normally while TAAK is left untouched and reported as aborted") {
                verify(exactly = 1) {
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.ZAAK, ReindexSummary(0, 0, 0))
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.TAAK, null)
                    ctx.reindexSupportService.deleteExistingEntities(ZoekObjectType.ZAAK)
                }
                verify(exactly = 0) {
                    ctx.reindexSupportService.deleteExistingEntities(ZoekObjectType.TAAK)
                }
            }
        }
    }

    given("reindex() with zero zaken, where the informatieobjecten count cannot be determined") {
        val ctx = setupContext()
        ctx.stubContinueOnExceptionsForwarding()
        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(emptyList(), 0)
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(any<EnkelvoudigInformatieobjectListParameters>())
        } throws RuntimeException("fake informatieobjecten count failure")
        every { ctx.reindexSupportService.deleteExistingEntities(any()) } just Runs

        `when`("reindex is called for ZAAK and DOCUMENT") {
            ctx.zaakGedrevenReindexService.reindex(ReindexScope(includeTaken = false, includeDocumenten = true))

            then("ZAAK finishes normally while DOCUMENT is left untouched, and the orphan sweep never runs") {
                verify(exactly = 1) {
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.ZAAK, ReindexSummary(0, 0, 0))
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.DOCUMENT, null)
                }
                verify(exactly = 0) {
                    ctx.reindexSupportService.deleteExistingEntities(ZoekObjectType.DOCUMENT)
                    ctx.reindexSupportService.indexeerDirectCountingSuccesses(any(), any(), any())
                }
            }
        }
    }

    given("reindex() with a single zaak that has an open taak and a linked document") {
        val ctx = setupContext()
        ctx.stubContinueOnExceptionsForwarding()
        ctx.stubZaakPageProcessing()
        ctx.stubRunTranslatingToIndexingExceptionForwarding()
        val zaak = createZaak()
        val zaakZoekObject = createZaakZoekObject()
        val openTask = mockk<Task>().apply { every { id } returns "fakeOpenTaskId" }
        val taakZoekObject = createTaakZoekObject()
        val documentUUID = UUID.randomUUID()
        val zaakInformatieobject = createZaakInformatieobjectForReads(
            zaak = zaak.url,
            informatieobject = URI("https://example.com/$documentUUID")
        )
        val documentZoekObject = createDocumentZoekObject()

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(listOf(ZaakUuid(zaak.uuid)), 1)
        every { ctx.flowableTaskService.countOpenTasks() } returns 1
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(match<EnkelvoudigInformatieobjectListParameters> { it.page == 1 })
        } returns Results(listOf(createEnkelvoudigInformatieObject(uuid = documentUUID)), 1)

        every { ctx.zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ctx.zaakZoekObjectConverter.convert(zaak, any()) } returns zaakZoekObject
        every { ctx.flowableTaskService.listOpenTasksForZaak(zaak.uuid) } returns listOf(openTask)
        every { ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", zaak, any()) } returns taakZoekObject
        every { ctx.zrcClientService.listZaakinformatieobjecten(zaak) } returns listOf(zaakInformatieobject)
        every { ctx.documentZoekObjectConverter.convert(zaakInformatieobject, zaak, any()) } returns documentZoekObject
        every {
            ctx.reindexSupportService.indexeerDirectCountingSuccesses(emptyList(), ZoekObjectType.DOCUMENT, any())
        } returns ReindexCounts()

        `when`("reindex is called for ZAAK, TAAK and DOCUMENT") {
            ctx.zaakGedrevenReindexService.reindex(ReindexScope(includeTaken = true, includeDocumenten = true))

            then("the zaak is retrieved from the ZRC API exactly once, shared by its taak and its document") {
                verify(exactly = 1) {
                    ctx.zrcClientService.readZaak(zaak.uuid)
                    ctx.zaakZoekObjectConverter.convert(zaak, any())
                    ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", zaak, any())
                    ctx.documentZoekObjectConverter.convert(zaakInformatieobject, zaak, any())
                }
            }

            then("the orphan sweep does not reconvert the document already indexed via its zaak") {
                verify(exactly = 1) {
                    ctx.reindexSupportService.indexeerDirectCountingSuccesses(emptyList(), ZoekObjectType.DOCUMENT, any())
                }
            }

            then("ZAAK, TAAK and DOCUMENT are all reported as finished with the correct totals") {
                verify(exactly = 1) {
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.ZAAK, ReindexSummary(1, 0, 1))
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.TAAK, ReindexSummary(1, 0, 1))
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.DOCUMENT, ReindexSummary(1, 0, 1))
                }
            }
        }
    }

    given("reindex() where retrieving the zaak itself fails") {
        val ctx = setupContext()
        ctx.stubContinueOnExceptionsForwarding()
        ctx.stubZaakPageProcessing()
        ctx.stubRunTranslatingToIndexingExceptionForwarding()
        val zaakUUID = UUID.randomUUID()

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(listOf(ZaakUuid(zaakUUID)), 1)
        every { ctx.flowableTaskService.countOpenTasks() } returns 0
        every { ctx.zrcClientService.readZaak(zaakUUID) } throws RuntimeException("fake zaak retrieval failure")

        `when`("reindex is called for ZAAK and TAAK") {
            ctx.zaakGedrevenReindexService.reindex(ReindexScope(includeTaken = true, includeDocumenten = false))

            then("the zaak's taak is never attempted") {
                verify(exactly = 0) {
                    ctx.flowableTaskService.listOpenTasksForZaak(zaakUUID)
                }
            }

            then("the zaak is counted as an error, and TAAK finds nothing to reindex") {
                verify(exactly = 1) {
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.ZAAK, ReindexSummary(0, 0, 1))
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.TAAK, ReindexSummary(0, 0, 0))
                }
            }
        }
    }

    given("reindex() where listing the zaak's open taken fails for an otherwise successfully indexed zaak") {
        val ctx = setupContext()
        ctx.stubContinueOnExceptionsForwarding()
        ctx.stubZaakPageProcessing()
        ctx.stubRunTranslatingToIndexingExceptionForwarding()
        val zaak = createZaak()
        val zaakZoekObject = createZaakZoekObject()

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(listOf(ZaakUuid(zaak.uuid)), 1)
        every { ctx.flowableTaskService.countOpenTasks() } returns 0
        every { ctx.zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ctx.zaakZoekObjectConverter.convert(zaak, any()) } returns zaakZoekObject
        every {
            ctx.flowableTaskService.listOpenTasksForZaak(zaak.uuid)
        } throws RuntimeException("fake open taken listing failure")

        `when`("reindex is called for ZAAK and TAAK") {
            ctx.zaakGedrevenReindexService.reindex(ReindexScope(includeTaken = true, includeDocumenten = false))

            then("the zaak is still reindexed successfully despite the taak listing failure") {
                verify(exactly = 1) {
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.ZAAK, ReindexSummary(1, 0, 1))
                    ctx.reindexSupportService.finishReindex(ZoekObjectType.TAAK, ReindexSummary(0, 0, 0))
                }
            }
        }
    }
})
