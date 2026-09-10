/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakEigenschap
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.ZaakListParameters
import nl.info.client.zgw.zrc.model.ZaakUuid
import nl.info.client.zgw.zrc.util.ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD
import nl.info.zac.app.task.model.TaakSortering
import nl.info.zac.authentication.LoggedInUserProvider
import nl.info.zac.authentication.runAsSystemUser
import nl.info.zac.search.converter.AbstractZoekObjectConverter
import nl.info.zac.search.model.createZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.shared.model.SorteerRichting
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.params.CursorMarkParams
import org.eclipse.microprofile.config.ConfigProvider
import org.flowable.task.api.Task
import java.util.UUID
import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.Logger

private data class ReindexSupportServiceTestContext(
    val solrClient: Http2SolrClient,
    val converterInstances: Instance<AbstractZoekObjectConverter<out ZoekObject>>,
    val converterInstancesIterator: MutableIterator<AbstractZoekObjectConverter<out ZoekObject>>,
    val zrcClientService: ZrcClientService,
    val drcClientService: DrcClientService,
    val flowableTaskService: FlowableTaskService,
    val reindexSupportService: ReindexSupportService
)

private fun captureLogRecords(block: () -> Unit): List<LogRecord> {
    val logger = Logger.getLogger(ReindexSupportService::class.java.name)
    val records = mutableListOf<LogRecord>()
    val handler = object : Handler() {
        override fun publish(record: LogRecord) {
            records.add(record)
        }
        override fun flush() = Unit
        override fun close() = Unit
    }
    logger.addHandler(handler)
    try {
        block()
    } finally {
        logger.removeHandler(handler)
    }
    return records
}

private fun setupContext(): ReindexSupportServiceTestContext {
    val solrUrl = "http://localhost/fakeSolrUrl"
    mockkStatic(ConfigProvider::class)
    every {
        ConfigProvider.getConfig().getValue("solr.url", String::class.java)
    } returns solrUrl

    val solrClient = mockk<Http2SolrClient>()
    mockkConstructor(Http2SolrClient.Builder::class)
    every { anyConstructed<Http2SolrClient.Builder>().build() } returns solrClient

    val converterInstances = mockk<Instance<AbstractZoekObjectConverter<out ZoekObject>>>()
    val converterInstancesIterator = mockk<MutableIterator<AbstractZoekObjectConverter<out ZoekObject>>>()
    val zrcClientService = mockk<ZrcClientService>()
    val drcClientService = mockk<DrcClientService>()
    val flowableTaskService = mockk<FlowableTaskService>()

    val reindexSupportService = ReindexSupportService(
        converterInstances,
        zrcClientService,
        drcClientService,
        flowableTaskService
    )

    return ReindexSupportServiceTestContext(
        solrClient,
        converterInstances,
        converterInstancesIterator,
        zrcClientService,
        drcClientService,
        flowableTaskService,
        reindexSupportService
    )
}

class ReindexSupportServiceTest : BehaviorSpec({
    afterEach { checkUnnecessaryStub() }

    given("getConverter for an object type a registered converter supports") {
        val ctx = setupContext()
        val zaakZoekObjectConverter = mockk<AbstractZoekObjectConverter<out ZoekObject>>()
        every { zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns zaakZoekObjectConverter

        `when`("getConverter is called for ZAAK") {
            val converter = ctx.reindexSupportService.getConverter(ZoekObjectType.ZAAK)

            then("the converter that supports ZAAK is returned") {
                converter shouldBe zaakZoekObjectConverter
            }
        }
    }

    given("getConverter when no registered converter supports the object type") {
        val ctx = setupContext()
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns false

        `when`("getConverter is called for ZAAK") {
            val indexingException = shouldThrow<IndexingException> {
                ctx.reindexSupportService.getConverter(ZoekObjectType.ZAAK)
            }

            then("it throws an IndexingException naming the object type") {
                indexingException.message shouldBe "[${ZoekObjectType.ZAAK}] No converter found"
            }
        }
    }

    given("addToSolrIndex with an empty list of zoekobjecten") {
        val ctx = setupContext()

        `when`("addToSolrIndex is called") {
            ctx.reindexSupportService.addToSolrIndex(emptyList(), performCommit = false)

            then("nothing is sent to the Solr client") {
                verify(exactly = 0) { ctx.solrClient.addBeans(any<Collection<*>>()) }
            }
        }
    }

    given("addToSolrIndex with performCommit set to true") {
        val ctx = setupContext()
        val zaakZoekObject = createZaakZoekObject()
        every { ctx.solrClient.addBeans(listOf(zaakZoekObject)) } returns UpdateResponse()
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        `when`("addToSolrIndex is called") {
            ctx.reindexSupportService.addToSolrIndex(listOf(zaakZoekObject), performCommit = true)

            then("the zoekobject is added and a hard Solr commit is performed") {
                verify(exactly = 1) {
                    ctx.solrClient.addBeans(listOf(zaakZoekObject))
                    ctx.solrClient.commit(null, true, true)
                }
            }
        }
    }

    given("removeFromSolrIndex for a single id") {
        val ctx = setupContext()
        val taskID = "fakeTaskId1"
        every { ctx.solrClient.deleteById(taskID) } returns UpdateResponse()

        `when`("removeFromSolrIndex is called") {
            ctx.reindexSupportService.removeFromSolrIndex(taskID)

            then("the id is deleted from the Solr client") {
                verify(exactly = 1) { ctx.solrClient.deleteById(taskID) }
            }
        }
    }

    given("commit") {
        val ctx = setupContext()
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        `when`("commit is called") {
            ctx.reindexSupportService.commit()

            then("a hard Solr commit is performed") {
                verify(exactly = 1) { ctx.solrClient.commit(null, true, true) }
            }
        }
    }

    given("finishReindex when reindexing was aborted, i.e. the summary is null") {
        val ctx = setupContext()
        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { ctx.solrClient.query(any()) } returns queryResponse

        `when`("finishReindex is called with a null summary") {
            ctx.reindexSupportService.finishReindex(ZoekObjectType.TAAK, null)

            then("no Solr commit is performed") {
                verify(exactly = 0) { ctx.solrClient.commit(null, true, true) }
            }
        }
    }

    given("finishReindex when reindexing finished, i.e. the summary is non-null") {
        val ctx = setupContext()
        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        `when`("finishReindex is called with a non-null summary") {
            ctx.reindexSupportService.finishReindex(
                ZoekObjectType.TAAK,
                ReindexSummary(successCount = 1, skippedCount = 0, totalCount = 1)
            )

            then("a Solr commit is performed so the reindexed entities become visible") {
                verify(exactly = 1) { ctx.solrClient.commit(null, true, true) }
            }
        }
    }

    given("continueOnExceptions when the wrapped function completes normally") {
        val ctx = setupContext()

        `when`("continueOnExceptions is called") {
            val result = ctx.reindexSupportService.continueOnExceptions(ZoekObjectType.ZAAK) { "fakeResult" }

            then("the function's result is returned") {
                result shouldBe "fakeResult"
            }
        }
    }

    given("continueOnExceptions when the wrapped function throws") {
        val ctx = setupContext()

        `when`("continueOnExceptions is called") {
            var result: String? = "not yet overwritten"
            val logRecords = captureLogRecords {
                result = ctx.reindexSupportService.continueOnExceptions(ZoekObjectType.ZAAK) {
                    throw IllegalStateException("fake failure")
                }
            }

            then("null is returned instead of the exception propagating") {
                result shouldBe null
            }

            then("the failure is logged") {
                logRecords.any {
                    it.message == "[${ZoekObjectType.ZAAK}] Error during indexing" &&
                        it.thrown?.cause?.message == "fake failure"
                } shouldBe true
            }
        }
    }

    given("memoizedIsZaakspecifiekGeautoriseerd invoked twice for the same zaak") {
        val ctx = setupContext()
        val zaakUUID = UUID.randomUUID()
        every { ctx.zrcClientService.listZaakeigenschappen(zaakUUID) } returns listOf(
            createZaakEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD, waarde = "true")
        )

        `when`("the returned lookup is invoked twice for the same zaak UUID") {
            val isZaakspecifiekGeautoriseerd = ctx.reindexSupportService.memoizedIsZaakspecifiekGeautoriseerd()
            val firstResult = isZaakspecifiekGeautoriseerd(zaakUUID)
            val secondResult = isZaakspecifiekGeautoriseerd(zaakUUID)

            then("both calls return true") {
                firstResult shouldBe true
                secondResult shouldBe true
            }

            then("the ZGW API is only queried once for that zaak") {
                verify(exactly = 1) { ctx.zrcClientService.listZaakeigenschappen(zaakUUID) }
            }
        }
    }

    given("reindexAllZaken when the zaken count cannot be determined") {
        val ctx = setupContext()
        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } throws RuntimeException("fake zaken count failure")

        `when`("reindexAllZaken is called") {
            val summary = ctx.reindexSupportService.reindexAllZaken()

            then("it returns null without deleting any existing ZAAK entities") {
                summary shouldBe null
                verify(exactly = 0) { ctx.solrClient.query(any()) }
            }
        }
    }

    given("reindexAllZaken with a single zaak") {
        val ctx = setupContext()
        val zaak = createZaak()
        val zaakZoekObject = createZaakZoekObject()
        val zaakZoekObjectConverter = mockk<AbstractZoekObjectConverter<out ZoekObject>>()

        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.addBeans(listOf(zaakZoekObject)) } returns UpdateResponse()

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(listOf(ZaakUuid(zaak.uuid)), 1)
        every { zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns zaakZoekObjectConverter
        every { zaakZoekObjectConverter.convert(zaak.uuid.toString(), any()) } returns zaakZoekObject

        `when`("reindexAllZaken is called") {
            val summary = ctx.reindexSupportService.reindexAllZaken()

            then("existing ZAAK entities are deleted and the zaak is reindexed") {
                verify(exactly = 1) {
                    ctx.solrClient.addBeans(listOf(zaakZoekObject))
                }
                summary shouldBe ReindexSummary(successCount = 1, skippedCount = 0, totalCount = 1)
            }
        }
    }

    given("reindexAllInformatieobjecten when the informatieobjecten count cannot be determined") {
        val ctx = setupContext()
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(any<EnkelvoudigInformatieobjectListParameters>())
        } throws RuntimeException("fake informatieobjecten count failure")

        `when`("reindexAllInformatieobjecten is called") {
            val summary = ctx.reindexSupportService.reindexAllInformatieobjecten()

            then("it returns null without deleting any existing DOCUMENT entities") {
                summary shouldBe null
                verify(exactly = 0) { ctx.solrClient.query(any()) }
            }
        }
    }

    given("reindexAllInformatieobjecten with a single informatieobject") {
        val ctx = setupContext()
        val informatieobjectUUID = UUID.randomUUID()
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(uuid = informatieobjectUUID)
        val documentZoekObject = createZaakZoekObject()
        val documentZoekObjectConverter = mockk<AbstractZoekObjectConverter<out ZoekObject>>()

        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.addBeans(listOf(documentZoekObject)) } returns UpdateResponse()

        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(match<EnkelvoudigInformatieobjectListParameters> { it.page == 1 })
        } returns Results(listOf(enkelvoudigInformatieObject), 1)
        every { documentZoekObjectConverter.supports(ZoekObjectType.DOCUMENT) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns documentZoekObjectConverter
        every {
            documentZoekObjectConverter.convert(informatieobjectUUID.toString(), any())
        } returns documentZoekObject

        `when`("reindexAllInformatieobjecten is called") {
            val summary = ctx.reindexSupportService.reindexAllInformatieobjecten()

            then("existing DOCUMENT entities are deleted and the informatieobject is reindexed") {
                verify(exactly = 1) {
                    ctx.solrClient.addBeans(listOf(documentZoekObject))
                }
                summary shouldBe ReindexSummary(successCount = 1, skippedCount = 0, totalCount = 1)
            }
        }
    }

    given("reindexAllTaken when the taken count cannot be determined") {
        val ctx = setupContext()
        every { ctx.flowableTaskService.countOpenTasks() } throws RuntimeException("fake taken count failure")

        `when`("reindexAllTaken is called") {
            val summary = ctx.reindexSupportService.reindexAllTaken()

            then("it returns null without deleting any existing TAAK entities") {
                summary shouldBe null
                verify(exactly = 0) { ctx.solrClient.query(any()) }
            }
        }
    }

    given("reindexAllTaken with a single open taak") {
        val ctx = setupContext()
        val openTask = mockk<Task>().apply { every { id } returns "fakeOpenTaskId" }
        val taakZoekObject = createZaakZoekObject()
        val taakZoekObjectConverter = mockk<AbstractZoekObjectConverter<out ZoekObject>>()

        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.addBeans(listOf(taakZoekObject)) } returns UpdateResponse()

        every { ctx.flowableTaskService.countOpenTasks() } returns 1
        every {
            ctx.flowableTaskService.listOpenTasks(TaakSortering.CREATIEDATUM, SorteerRichting.DESCENDING, 0, 100)
        } returns listOf(openTask)
        every { taakZoekObjectConverter.supports(ZoekObjectType.TAAK) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns taakZoekObjectConverter
        every { taakZoekObjectConverter.convert("fakeOpenTaskId", any()) } returns taakZoekObject

        `when`("reindexAllTaken is called") {
            val summary = ctx.reindexSupportService.reindexAllTaken()

            then("existing TAAK entities are deleted and the open taak is reindexed") {
                verify(exactly = 1) {
                    ctx.solrClient.addBeans(listOf(taakZoekObject))
                }
                summary shouldBe ReindexSummary(successCount = 1, skippedCount = 0, totalCount = 1)
            }
        }
    }

    context("Running page conversions as the system user") {
        given("page conversions started inside system user work") {
            val reindexSupportService = setupContext().reindexSupportService

            `when`("a conversion runs on the page conversion dispatcher") {
                var wasSystemUserDuringConversion: Boolean? = null
                runAsSystemUser {
                    reindexSupportService.runConcurrentPageConversions(listOf("fakeItem")) {
                        wasSystemUserDuringConversion = LoggedInUserProvider.systemUser.get()
                    }
                }

                then("the conversion runs as the system user too, not on an unattributed worker thread") {
                    wasSystemUserDuringConversion shouldBe true
                }
            }
        }
    }
})
