/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.search

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import jakarta.enterprise.inject.Instance
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.zrc.model.ZaakListParameters
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakEigenschap
import nl.info.client.zgw.model.createZaakInformatieobjectForReads
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.ZaakUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.task.model.TaakSortering
import nl.info.zac.search.converter.AbstractZoekObjectConverter
import nl.info.zac.search.converter.DocumentZoekObjectConverter
import nl.info.zac.search.converter.TaakZoekObjectConverter
import nl.info.zac.search.converter.ZaakZoekObjectConverter
import nl.info.zac.search.model.createDocumentZoekObject
import nl.info.zac.search.model.createTaakZoekObject
import nl.info.zac.search.model.createZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.shared.model.SorteerRichting
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.params.CursorMarkParams
import org.eclipse.microprofile.config.ConfigProvider
import org.flowable.task.api.Task
import java.io.IOException
import java.net.URI
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.Logger

private data class TestContext(
    val solrClient: Http2SolrClient,
    val zaakZoekObjectConverter: ZaakZoekObjectConverter,
    val taakZoekObjectConverter: TaakZoekObjectConverter,
    val converterInstances: Instance<AbstractZoekObjectConverter<out ZoekObject>>,
    val converterInstancesIterator: MutableIterator<AbstractZoekObjectConverter<out ZoekObject>>,
    val drcClientService: DrcClientService,
    val flowableTaskService: FlowableTaskService,
    val zrcClientService: ZrcClientService,
    val documentZoekObjectConverter: DocumentZoekObjectConverter,
    val indexingService: IndexingService,
    val testDispatcher: TestDispatcher
)

private fun captureLogRecords(block: () -> Unit): List<LogRecord> {
    val logger = Logger.getLogger(IndexingService::class.java.name)
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

private fun setupContext(): TestContext {
    val solrUrl = "http://localhost/fakeSolrUrl"
    mockkStatic(ConfigProvider::class)
    every {
        ConfigProvider.getConfig().getValue("solr.url", String::class.java)
    } returns solrUrl

    val solrClient = mockk<Http2SolrClient>()
    mockkConstructor(Http2SolrClient.Builder::class)
    every { anyConstructed<Http2SolrClient.Builder>().build() } returns solrClient

    val zaakZoekObjectConverter = mockk<ZaakZoekObjectConverter>()
    val taakZoekObjectConverter = mockk<TaakZoekObjectConverter>()
    val converterInstances = mockk<Instance<AbstractZoekObjectConverter<out ZoekObject>>>()
    val converterInstancesIterator = mockk<MutableIterator<AbstractZoekObjectConverter<out ZoekObject>>>()
    val drcClientService = mockk<DrcClientService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val zrcClientService = mockk<ZrcClientService>()
    val testDispatcher = StandardTestDispatcher()
    val documentZoekObjectConverter = mockk<DocumentZoekObjectConverter>()

    val indexingService = IndexingService(
        converterInstances,
        zrcClientService,
        drcClientService,
        flowableTaskService,
        documentZoekObjectConverter,
        zaakZoekObjectConverter,
        taakZoekObjectConverter,
        testDispatcher
    )

    return TestContext(
        solrClient,
        zaakZoekObjectConverter,
        taakZoekObjectConverter,
        converterInstances,
        converterInstancesIterator,
        drcClientService,
        flowableTaskService,
        zrcClientService,
        documentZoekObjectConverter,
        indexingService,
        testDispatcher
    )
}

@Suppress("LargeClass")
class IndexingServiceTest : BehaviorSpec({
    afterEach {
        checkUnnecessaryStub()
    }

    given("Two zaken") {
        val ctx = setupContext()
        val zaakType = createZaakType()
        val zaaktypeURI = URI("https://example.com/${zaakType.url}")
        val zaken = listOf(
            createZaak(zaaktypeUri = zaaktypeURI),
            createZaak(zaaktypeUri = zaaktypeURI)
        )
        val zaakZoekObjecten = listOf(
            createZaakZoekObject(),
            createZaakZoekObject()
        )
        every { ctx.zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen true andThen false
        every { ctx.converterInstancesIterator.next() } returns ctx.zaakZoekObjectConverter andThen ctx.zaakZoekObjectConverter
        zaken.forEachIndexed { index, zaak ->
            every { ctx.zaakZoekObjectConverter.convert(zaak.uuid.toString(), any()) } returns zaakZoekObjecten[index]
        }
        every { ctx.solrClient.addBeans(zaakZoekObjecten) } returns UpdateResponse()

        `when`(
            """The indexeer direct method is called to index the two zaken"""
        ) {
            ctx.indexingService.indexeerDirect(zaken.map { it.uuid.toString() }, ZoekObjectType.ZAAK, false)

            then(
                """
                two zaak zoek objecten should be added to the Solr client and 
                both related object ids should be removed as 'marked for indexing'                
                """
            ) {
                verify(exactly = 1) {
                    ctx.solrClient.addBeans(any<Collection<*>>())
                }
            }
        }
    }

    given("Three zaken where one fails to convert") {
        val ctx = setupContext()
        val zaakType = createZaakType()
        val zaaktypeURI = URI("https://example.com/${zaakType.url}")
        val zaken = listOf(
            createZaak(zaaktypeUri = zaaktypeURI),
            createZaak(zaaktypeUri = zaaktypeURI),
            createZaak(zaaktypeUri = zaaktypeURI)
        )
        val zaakZoekObjecten = listOf(
            createZaakZoekObject(),
            createZaakZoekObject()
        )
        every { ctx.zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns ctx.zaakZoekObjectConverter
        every { ctx.zaakZoekObjectConverter.convert(zaken[0].uuid.toString(), any()) } returns zaakZoekObjecten[0]
        every { ctx.zaakZoekObjectConverter.convert(zaken[1].uuid.toString(), any()) } throws
            RuntimeException("fake conversion failure")
        every { ctx.zaakZoekObjectConverter.convert(zaken[2].uuid.toString(), any()) } returns zaakZoekObjecten[1]
        every { ctx.solrClient.addBeans(zaakZoekObjecten) } returns UpdateResponse()

        `when`(
            """The indexeer direct method is called to index the three zaken"""
        ) {
            ctx.indexingService.indexeerDirect(zaken.map { it.uuid.toString() }, ZoekObjectType.ZAAK, false)

            then(
                """
                the two successfully converted zaak zoek objecten are still added to the Solr client,
                even though one zaak failed to convert
                """
            ) {
                verify(exactly = 1) {
                    ctx.solrClient.addBeans(zaakZoekObjecten)
                }
            }
        }
    }

    given("A page with more zaken than the configured conversion concurrency limit") {
        val ctx = setupContext()
        val zaakType = createZaakType()
        val zaaktypeURI = URI("https://example.com/${zaakType.url}")
        val pageSize = 20
        // must stay in sync with IndexingService.PAGE_CONVERSION_PARALLELISM
        val expectedConcurrencyLimit = 8
        val zaken = List(pageSize) { createZaak(zaaktypeUri = zaaktypeURI) }
        val zaakZoekObjecten = List(pageSize) { createZaakZoekObject() }
        val activeConversions = AtomicInteger(0)
        val maxObservedConcurrency = AtomicInteger(0)

        every { ctx.zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns ctx.zaakZoekObjectConverter
        zaken.forEachIndexed { index, zaak ->
            every { ctx.zaakZoekObjectConverter.convert(zaak.uuid.toString(), any()) } answers {
                val current = activeConversions.incrementAndGet()
                maxObservedConcurrency.updateAndGet { previousMax -> maxOf(previousMax, current) }
                try {
                    Thread.sleep(50)
                } finally {
                    activeConversions.decrementAndGet()
                }
                zaakZoekObjecten[index]
            }
        }
        every { ctx.solrClient.addBeans(any<Collection<*>>()) } returns UpdateResponse()

        `when`("indexeerDirect is called for the page") {
            ctx.indexingService.indexeerDirect(zaken.map { it.uuid.toString() }, ZoekObjectType.ZAAK, false)

            then("no more conversions than the configured limit run concurrently") {
                (maxObservedConcurrency.get() <= expectedConcurrencyLimit) shouldBe true
            }

            then("all zaken in the page are still converted and added to the Solr index") {
                verify(exactly = 1) {
                    ctx.solrClient.addBeans(match<Collection<*>> { it.size == pageSize })
                }
            }
        }
    }

    given("A page of documenten, all linked to the same zaak, exceeding the conversion concurrency limit") {
        val ctx = setupContext()
        val pageSize = 20
        val zaakUUID = UUID.randomUUID()
        val informatieobjectUUIDs = List(pageSize) { UUID.randomUUID().toString() }
        val documentZoekObjecten = List(pageSize) { createDocumentZoekObject() }

        every { ctx.documentZoekObjectConverter.supports(ZoekObjectType.DOCUMENT) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns ctx.documentZoekObjectConverter
        every { ctx.zrcClientService.listZaakeigenschappen(zaakUUID) } returns listOf(
            createZaakEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD, waarde = "true")
        )
        informatieobjectUUIDs.forEachIndexed { index, informatieobjectUUID ->
            every { ctx.documentZoekObjectConverter.convert(informatieobjectUUID, any()) } answers {
                // simulates the converter resolving the memoized flag lookup for the shared zaak,
                // concurrently with the other documents on this page
                secondArg<(UUID) -> Boolean>().invoke(zaakUUID)
                documentZoekObjecten[index]
            }
        }
        every { ctx.solrClient.addBeans(any<Collection<*>>()) } returns UpdateResponse()

        `when`("indexeerDirect is called for the page") {
            ctx.indexingService.indexeerDirect(informatieobjectUUIDs, ZoekObjectType.DOCUMENT, false)

            then(
                "the shared zaak's zaakspecifiek geautoriseerd flag is looked up only once, despite " +
                    "every document on the page resolving it concurrently"
            ) {
                verify(exactly = 1) {
                    ctx.zrcClientService.listZaakeigenschappen(zaakUUID)
                }
            }
        }
    }

    given("A zaak with two documenten attached") {
        val ctx = setupContext()
        val zaak = createZaak()
        val zaakInformatieobjecten = listOf(
            createZaakInformatieobjectForReads(zaak = zaak.url),
            createZaakInformatieobjectForReads(zaak = zaak.url)
        )
        val documentZoekObjecten = listOf(createDocumentZoekObject(), createDocumentZoekObject())

        every { ctx.zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ctx.zrcClientService.listZaakinformatieobjecten(zaak) } returns zaakInformatieobjecten
        every { ctx.zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(
            createZaakEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD, waarde = "true")
        )
        every { ctx.solrClient.addBeans(any<Collection<*>>()) } returns UpdateResponse()
        zaakInformatieobjecten.forEachIndexed { index, zaakInformatieobject ->
            every {
                ctx.documentZoekObjectConverter.convert(
                    zaakInformatieobject.informatieobject.extractUuid().toString(),
                    any()
                )
            } answers {
                // simulates the converter resolving the document against this same zaak,
                // which is what actually triggers the memoized flag lookup
                secondArg<(UUID) -> Boolean>().invoke(zaak.uuid)
                documentZoekObjecten[index]
            }
        }

        `when`("addOrUpdateInformatieobjectenForZaak is called for the zaak's UUID") {
            ctx.indexingService.addOrUpdateInformatieobjectenForZaak(zaak.uuid)

            then("both of the zaak's documenten are (re)indexed in Solr") {
                verify(exactly = 1) {
                    ctx.documentZoekObjectConverter.convert(
                        zaakInformatieobjecten[0].informatieobject.extractUuid().toString(),
                        any()
                    )
                    ctx.documentZoekObjectConverter.convert(
                        zaakInformatieobjecten[1].informatieobject.extractUuid().toString(),
                        any()
                    )
                }
            }

            then("the zaak's zaakspecifiek geautoriseerd flag is read only once, not once per document") {
                verify(exactly = 1) {
                    ctx.zrcClientService.listZaakeigenschappen(zaak.uuid)
                }
            }
        }
    }

    given("A zaak with two documenten, one of which the converter resolves against a different zaak") {
        val ctx = setupContext()
        val zaak = createZaak()
        val otherZaakUUID = UUID.randomUUID()
        val zaakInformatieobjecten = listOf(
            createZaakInformatieobjectForReads(zaak = zaak.url),
            createZaakInformatieobjectForReads(zaak = zaak.url)
        )
        val documentZoekObjecten = listOf(createDocumentZoekObject(), createDocumentZoekObject())

        every { ctx.zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ctx.zrcClientService.listZaakinformatieobjecten(zaak) } returns zaakInformatieobjecten
        every { ctx.zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(
            createZaakEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD, waarde = "true")
        )
        every { ctx.zrcClientService.listZaakeigenschappen(otherZaakUUID) } returns listOf(
            createZaakEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD, waarde = "false")
        )
        every { ctx.solrClient.addBeans(any<Collection<*>>()) } returns UpdateResponse()
        every {
            ctx.documentZoekObjectConverter.convert(
                zaakInformatieobjecten[0].informatieobject.extractUuid().toString(),
                any()
            )
        } answers {
            // resolves against the iterated zaak, like a document only linked to this one zaak
            secondArg<(UUID) -> Boolean>().invoke(zaak.uuid)
            documentZoekObjecten[0]
        }
        every {
            ctx.documentZoekObjectConverter.convert(
                zaakInformatieobjecten[1].informatieobject.extractUuid().toString(),
                any()
            )
        } answers {
            // resolves against a different zaak, like a document linked to more than one zaak
            secondArg<(UUID) -> Boolean>().invoke(otherZaakUUID)
            documentZoekObjecten[1]
        }

        `when`("addOrUpdateInformatieobjectenForZaak is called for the zaak's UUID") {
            ctx.indexingService.addOrUpdateInformatieobjectenForZaak(zaak.uuid)

            then(
                "the flag is looked up for the zaak the second document actually resolves against, " +
                    "not forced to the iterated zaak's own flag"
            ) {
                verify(exactly = 1) {
                    ctx.zrcClientService.listZaakeigenschappen(zaak.uuid)
                    ctx.zrcClientService.listZaakeigenschappen(otherZaakUUID)
                }
            }
        }
    }

    given("A zaak with one open taak, called with inclusiefTaken true") {
        val ctx = setupContext()
        val zaakUUID = UUID.randomUUID()
        val openTask = mockk<Task>().apply { every { id } returns "fakeOpenTaskId" }
        val zaakZoekObject = createZaakZoekObject()
        val taakZoekObject = createTaakZoekObject()

        every { ctx.zrcClientService.listZaakeigenschappen(zaakUUID) } returns listOf(
            createZaakEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD, waarde = "true")
        )
        every { ctx.zaakZoekObjectConverter.convert(zaakUUID.toString(), any()) } answers {
            // simulates the converter resolving the memoized flag lookup for this same zaak
            secondArg<(UUID) -> Boolean>().invoke(zaakUUID)
            zaakZoekObject
        }
        every { ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", any()) } answers {
            secondArg<(UUID) -> Boolean>().invoke(zaakUUID)
            taakZoekObject
        }
        every { ctx.flowableTaskService.listOpenTasksForZaak(zaakUUID) } returns listOf(openTask)
        every { ctx.solrClient.addBeans(any<Collection<*>>()) } returns UpdateResponse()

        `when`("addOrUpdateZaak is called") {
            val zaakIndexed = ctx.indexingService.addOrUpdateZaak(zaakUUID, true)

            then("it reports that the zaak itself was indexed successfully") {
                zaakIndexed shouldBe true
            }

            then("the zaak and only its open taken are reindexed, without listing its completed taken") {
                verify(exactly = 1) {
                    ctx.zaakZoekObjectConverter.convert(zaakUUID.toString(), any())
                    ctx.flowableTaskService.listOpenTasksForZaak(zaakUUID)
                    ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", any())
                }
                verify(exactly = 0) {
                    ctx.flowableTaskService.listTasksForZaak(any())
                }
            }

            then("the zaak's zaakspecifiek geautoriseerd flag is read only once, shared by the zaak and its taak") {
                verify(exactly = 1) {
                    ctx.zrcClientService.listZaakeigenschappen(zaakUUID)
                }
            }
        }
    }

    given("A zaak with one open and one completed taak") {
        val ctx = setupContext()
        val zaakUUID = UUID.randomUUID()
        val openTask = mockk<Task>().apply { every { id } returns "fakeOpenTaskId" }
        val completedTask = mockk<Task>().apply { every { id } returns "fakeCompletedTaskId" }
        val taakZoekObjecten = listOf(createTaakZoekObject(), createTaakZoekObject())

        every { ctx.zrcClientService.listZaakeigenschappen(zaakUUID) } returns listOf(
            createZaakEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD, waarde = "true")
        )
        every { ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", any()) } answers {
            // simulates the converter resolving the memoized flag lookup for this same zaak
            secondArg<(UUID) -> Boolean>().invoke(zaakUUID)
            taakZoekObjecten[0]
        }
        every { ctx.taakZoekObjectConverter.convert("fakeCompletedTaskId", any()) } answers {
            secondArg<(UUID) -> Boolean>().invoke(zaakUUID)
            taakZoekObjecten[1]
        }
        every { ctx.flowableTaskService.listTasksForZaak(zaakUUID) } returns listOf(openTask, completedTask)
        every { ctx.solrClient.addBeans(any<Collection<*>>()) } returns UpdateResponse()

        `when`("addOrUpdateTakenForZaak is called") {
            ctx.indexingService.addOrUpdateTakenForZaak(zaakUUID)

            then("both the open and the completed taak are reindexed") {
                verify(exactly = 1) {
                    ctx.flowableTaskService.listTasksForZaak(zaakUUID)
                    ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", any())
                    ctx.taakZoekObjectConverter.convert("fakeCompletedTaskId", any())
                }
            }

            then("the zaak's zaakspecifiek geautoriseerd flag is read only once, not once per taak") {
                verify(exactly = 1) {
                    ctx.zrcClientService.listZaakeigenschappen(zaakUUID)
                }
            }
        }
    }

    given("Solr indexing exists") {
        val ctx = setupContext()
        val queryResponse = mockk<QueryResponse>()

        val documentList = SolrDocumentList().apply {
            addAll(
                listOf(
                    SolrDocument(mapOf("id" to 1)),
                    SolrDocument(mapOf("id" to 2))
                )
            )
        }

        val zakenUuid = listOf(
            ZaakUuid(UUID.randomUUID()),
            ZaakUuid(UUID.randomUUID())
        )
        val zaakZoekObjecten = listOf(
            createZaakZoekObject(),
            createZaakZoekObject()
        )

        beforeContainer {
            clearMocks(ctx.solrClient)

            every { queryResponse.results } returns documentList
            every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START

            every { ctx.solrClient.query(any()) } returns queryResponse
            every { ctx.solrClient.deleteById(listOf("1", "2")) } returns UpdateResponse()
            every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

            every { ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>()) } returnsMany listOf(
                Results(zakenUuid, 2),
                Results(zakenUuid, 2),
                Results(emptyList(), 0)
            )

            every { ctx.zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
            every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
            every { ctx.converterInstancesIterator.hasNext() } returns true andThen true andThen false
            every { ctx.converterInstancesIterator.next() } returns ctx.zaakZoekObjectConverter andThen ctx.zaakZoekObjectConverter
            zakenUuid.forEachIndexed { index, zaak ->
                every { ctx.zaakZoekObjectConverter.convert(zaak.uuid.toString(), any()) } returns zaakZoekObjecten[index]
            }
        }

        `when`("reindexing of zaken is called") {
            every { ctx.solrClient.addBeans(zaakZoekObjecten) } returns UpdateResponse()

            ctx.indexingService.reindex(ZoekObjectType.ZAAK)

            then("it finishes successfully") {
                verify(exactly = 1) {
                    ctx.solrClient.deleteById(any<List<String>>())
                    ctx.solrClient.addBeans(any<Collection<*>>())
                }
            }
        }

        `when`("adding beans in Solr errors") {
            val solrException = SolrServerException("Solr exception")
            every { ctx.solrClient.addBeans(any<Collection<*>>()) } throws solrException

            ctx.indexingService.reindex(ZoekObjectType.ZAAK)

            then("ignores errors") {
                verify(exactly = 1) {
                    ctx.solrClient.deleteById(any<List<String>>())
                    ctx.solrClient.addBeans(any<Collection<*>>())
                }
            }
        }
    }

    given("Solr indexing exists and zaak count cannot be obtained") {
        val ctx = setupContext()
        val queryResponse = mockk<QueryResponse>()
        val documentList = SolrDocumentList().apply {
            addAll(
                listOf(
                    SolrDocument(mapOf("id" to 1)),
                    SolrDocument(mapOf("id" to 2))
                )
            )
        }
        every { queryResponse.results } returns documentList
        every { ctx.solrClient.query(any()) } returns queryResponse

        val ioException = IOException("IO exception")
        every { ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>()) } throws ioException

        `when`("reading zaak count throws an error") {
            ctx.indexingService.reindex(ZoekObjectType.ZAAK)

            then("aborts and does not try to list zaken") {
                verify(exactly = 1) {
                    ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>())
                }
            }

            then("existing Solr documents of that type are left untouched") {
                verify(exactly = 0) {
                    ctx.solrClient.deleteById(any<List<String>>())
                    ctx.solrClient.commit(null, true, true)
                }
            }
        }
    }

    given("Solr indexing exists and zaak count is available") {
        val ctx = setupContext()
        val queryResponse = mockk<QueryResponse>()
        val documentList = SolrDocumentList().apply {
            addAll(
                listOf(
                    SolrDocument(mapOf("id" to 1)),
                    SolrDocument(mapOf("id" to 2))
                )
            )
        }
        val zakenUuid = listOf(
            ZaakUuid(UUID.randomUUID()),
            ZaakUuid(UUID.randomUUID())
        )
        val zaakZoekObjecten = listOf(
            createZaakZoekObject(),
            createZaakZoekObject()
        )

        every { queryResponse.results } returns documentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.deleteById(listOf("1", "2")) } returns UpdateResponse()
        every { ctx.solrClient.addBeans(zaakZoekObjecten) } returns UpdateResponse()
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(zakenUuid, 102)

        every { ctx.zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen true andThen false
        every { ctx.converterInstancesIterator.next() } returns ctx.zaakZoekObjectConverter andThen ctx.zaakZoekObjectConverter
        zakenUuid.forEachIndexed { index, zaak ->
            every { ctx.zaakZoekObjectConverter.convert(zaak.uuid.toString(), any()) } returns zaakZoekObjecten[index]
        }

        `when`("reading zaak list throws an `IOException`") {
            every {
                ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 2 })
            } throws IOException("exception")

            ctx.indexingService.reindex(ZoekObjectType.ZAAK)

            then("continues without exception") {
                verify(exactly = 3) {
                    ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>())
                }
            }
        }
    }

    given("Solr indexing exists and informatieobjecten count is available") {
        val ctx = setupContext()
        val queryResponse = mockk<QueryResponse>()
        val documentList = SolrDocumentList().apply {
            addAll(
                listOf(
                    SolrDocument(mapOf("id" to 1)),
                    SolrDocument(mapOf("id" to 2))
                )
            )
        }
        val documentZoekObjectConverter = mockk<DocumentZoekObjectConverter>()
        val informatieobjectenPage1 = listOf(
            createEnkelvoudigInformatieObject(),
            createEnkelvoudigInformatieObject()
        )
        val informatieobjectPage2 = createEnkelvoudigInformatieObject()
        val documentZoekObjectenPage1 = listOf(
            createDocumentZoekObject(),
            createDocumentZoekObject()
        )
        val documentZoekObjectPage2 = createDocumentZoekObject()

        every { queryResponse.results } returns documentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.deleteById(listOf("1", "2")) } returns UpdateResponse()
        every { ctx.solrClient.addBeans(any<Collection<*>>()) } returns UpdateResponse()
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(
                match<EnkelvoudigInformatieobjectListParameters> { it.page == 1 }
            )
        } returns Results(informatieobjectenPage1, 102)
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(
                match<EnkelvoudigInformatieobjectListParameters> { it.page == 2 }
            )
        } returns Results(listOf(informatieobjectPage2), 102)

        every { documentZoekObjectConverter.supports(ZoekObjectType.DOCUMENT) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true
        every { ctx.converterInstancesIterator.next() } returns documentZoekObjectConverter
        informatieobjectenPage1.forEachIndexed { index, informatieobject ->
            every {
                documentZoekObjectConverter.convert(informatieobject.url.extractUuid().toString(), any())
            } returns documentZoekObjectenPage1[index]
        }
        every {
            documentZoekObjectConverter.convert(informatieobjectPage2.url.extractUuid().toString(), any())
        } returns documentZoekObjectPage2

        `when`("reindexing of informatieobjecten is called") {
            ctx.indexingService.reindex(ZoekObjectType.DOCUMENT)

            then("the second page is fetched using its own page number instead of always page one") {
                verify(exactly = 1) {
                    ctx.drcClientService.listEnkelvoudigInformatieObjecten(
                        match<EnkelvoudigInformatieobjectListParameters> { it.page == 2 }
                    )
                    documentZoekObjectConverter.convert(informatieobjectPage2.url.extractUuid().toString(), any())
                }
            }
        }
    }

    given("Two documenten on separate reindex pages, both linked to the same zaak") {
        val ctx = setupContext()
        val queryResponse = mockk<QueryResponse>()
        val documentList = SolrDocumentList().apply {
            addAll(listOf(SolrDocument(mapOf("id" to 1)), SolrDocument(mapOf("id" to 2))))
        }
        val documentZoekObjectConverter = mockk<DocumentZoekObjectConverter>()
        val zaakUUID = UUID.randomUUID()
        val informatieobjectPage1 = createEnkelvoudigInformatieObject()
        val informatieobjectPage2 = createEnkelvoudigInformatieObject()

        every { queryResponse.results } returns documentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.deleteById(listOf("1", "2")) } returns UpdateResponse()
        every { ctx.solrClient.addBeans(any<Collection<*>>()) } returns UpdateResponse()
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()
        every { ctx.zrcClientService.listZaakeigenschappen(zaakUUID) } returns listOf(
            createZaakEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD, waarde = "true")
        )

        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(
                match<EnkelvoudigInformatieobjectListParameters> { it.page == 1 }
            )
        } returns Results(listOf(informatieobjectPage1), 102)
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(
                match<EnkelvoudigInformatieobjectListParameters> { it.page == 2 }
            )
        } returns Results(listOf(informatieobjectPage2), 102)

        every { documentZoekObjectConverter.supports(ZoekObjectType.DOCUMENT) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true
        every { ctx.converterInstancesIterator.next() } returns documentZoekObjectConverter
        listOf(informatieobjectPage1, informatieobjectPage2).forEach { informatieobject ->
            every {
                documentZoekObjectConverter.convert(informatieobject.url.extractUuid().toString(), any())
            } answers {
                secondArg<(UUID) -> Boolean>().invoke(zaakUUID)
                createDocumentZoekObject()
            }
        }

        `when`("reindexing of informatieobjecten is called") {
            ctx.indexingService.reindex(ZoekObjectType.DOCUMENT)

            then(
                "the shared zaak's zaakspecifiek geautoriseerd flag is looked up only once across both pages"
            ) {
                verify(exactly = 1) {
                    ctx.zrcClientService.listZaakeigenschappen(zaakUUID)
                }
            }
        }
    }

    given("Reindexing exactly one page's worth of taken through reindex()") {
        val ctx = setupContext()
        val queryResponse = mockk<QueryResponse>()
        val documentList = SolrDocumentList()
        every { queryResponse.results } returns documentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        val takenMaxResults = 100
        val taakZoekObjectConverter = mockk<TaakZoekObjectConverter>()
        val tasks = List(takenMaxResults) { index ->
            mockk<Task>().apply { every { id } returns "fakeTaskId$index" }
        }

        every { ctx.flowableTaskService.countOpenTasks() } returns takenMaxResults.toLong()
        every {
            ctx.flowableTaskService.listOpenTasks(
                TaakSortering.CREATIEDATUM,
                SorteerRichting.DESCENDING,
                0,
                takenMaxResults
            )
        } returns tasks

        every { taakZoekObjectConverter.supports(ZoekObjectType.TAAK) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns taakZoekObjectConverter
        every { taakZoekObjectConverter.convert(any(), any()) } throws RuntimeException("fake conversion failure")

        `when`("reindexing of taken is called") {
            ctx.indexingService.reindex(ZoekObjectType.TAAK)

            then("only one page is fetched, since the task count exactly fills one page") {
                verify(exactly = 1) {
                    ctx.flowableTaskService.listOpenTasks(any(), any(), any(), any())
                }
            }
        }
    }

    given("Reindexing informatieobjecten where one converts, one is skipped and one errors") {
        val ctx = setupContext()
        val queryResponse = mockk<QueryResponse>()
        val documentList = SolrDocumentList().apply {
            addAll(listOf(SolrDocument(mapOf("id" to 1)), SolrDocument(mapOf("id" to 2))))
        }
        every { queryResponse.results } returns documentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.deleteById(listOf("1", "2")) } returns UpdateResponse()
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        val documentZoekObjectConverter = mockk<DocumentZoekObjectConverter>()
        val informatieobjecten = listOf(
            createEnkelvoudigInformatieObject(),
            createEnkelvoudigInformatieObject(),
            createEnkelvoudigInformatieObject()
        )
        val convertedDocumentZoekObject = createDocumentZoekObject()

        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(
                match<EnkelvoudigInformatieobjectListParameters> { it.page == 1 }
            )
        } returns Results(informatieobjecten, 3)

        every { documentZoekObjectConverter.supports(ZoekObjectType.DOCUMENT) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns documentZoekObjectConverter
        every {
            documentZoekObjectConverter.convert(informatieobjecten[0].url.extractUuid().toString(), any())
        } returns convertedDocumentZoekObject
        every {
            documentZoekObjectConverter.convert(informatieobjecten[1].url.extractUuid().toString(), any())
        } returns null
        every {
            documentZoekObjectConverter.convert(informatieobjecten[2].url.extractUuid().toString(), any())
        } throws RuntimeException("fake conversion failure")
        every { ctx.solrClient.addBeans(listOf(convertedDocumentZoekObject)) } returns UpdateResponse()

        `when`("reindexing of informatieobjecten is called") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindex(ZoekObjectType.DOCUMENT)
            }

            then(
                "the finished log reports the skipped informatieobject separately from the errored one"
            ) {
                logRecords.map { it.message } shouldContain
                    "[DOCUMENT] Reindexing finished. Reindexed: 1 / 3, skipped: 1, not reindexed because of errors: 1. " +
                    "Solr index contains 0 documents of type 'DOCUMENT'."
            }
        }
    }

    given("Reindexing zaken through reindex() where one of three conversions fails") {
        val ctx = setupContext()
        val queryResponse = mockk<QueryResponse>()
        val documentList = SolrDocumentList().apply {
            addAll(listOf(SolrDocument(mapOf("id" to 1)), SolrDocument(mapOf("id" to 2))))
        }
        every { queryResponse.results } returns documentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.deleteById(listOf("1", "2")) } returns UpdateResponse()
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        val zakenUuid = listOf(
            ZaakUuid(UUID.randomUUID()),
            ZaakUuid(UUID.randomUUID()),
            ZaakUuid(UUID.randomUUID())
        )
        val zaakZoekObjecten = listOf(createZaakZoekObject(), createZaakZoekObject())

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(zakenUuid, 3)

        every { ctx.zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns ctx.zaakZoekObjectConverter
        every { ctx.zaakZoekObjectConverter.convert(zakenUuid[0].uuid.toString(), any()) } returns zaakZoekObjecten[0]
        every { ctx.zaakZoekObjectConverter.convert(zakenUuid[1].uuid.toString(), any()) } throws
            RuntimeException("fake conversion failure")
        every { ctx.zaakZoekObjectConverter.convert(zakenUuid[2].uuid.toString(), any()) } returns zaakZoekObjecten[1]
        every { ctx.solrClient.addBeans(zaakZoekObjecten) } returns UpdateResponse()

        `when`("reindexing of zaken is called") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindex(ZoekObjectType.ZAAK)
            }

            then("the finished log includes a summary reporting 2 out of 3 zaken reindexed with 1 error") {
                logRecords.map { it.message } shouldContain
                    "[ZAAK] Reindexing finished. Reindexed: 2 / 3, skipped: 0, not reindexed because of errors: 1. " +
                    "Solr index contains 0 documents of type 'ZAAK'."
            }
        }
    }

    given("Reindexing zaken through reindex() where all conversions succeed") {
        val ctx = setupContext()
        val queryResponse = mockk<QueryResponse>()
        val documentList = SolrDocumentList().apply {
            addAll(listOf(SolrDocument(mapOf("id" to 1)), SolrDocument(mapOf("id" to 2))))
        }
        every { queryResponse.results } returns documentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.deleteById(listOf("1", "2")) } returns UpdateResponse()
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        val zakenUuid = listOf(ZaakUuid(UUID.randomUUID()), ZaakUuid(UUID.randomUUID()))
        val zaakZoekObjecten = listOf(createZaakZoekObject(), createZaakZoekObject())

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(zakenUuid, 2)

        every { ctx.zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns ctx.zaakZoekObjectConverter
        zakenUuid.forEachIndexed { index, zaak ->
            every { ctx.zaakZoekObjectConverter.convert(zaak.uuid.toString(), any()) } returns zaakZoekObjecten[index]
        }
        every { ctx.solrClient.addBeans(zaakZoekObjecten) } returns UpdateResponse()

        `when`("reindexing of zaken is called") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindex(ZoekObjectType.ZAAK)
            }

            then("the finished log includes a summary reporting all zaken reindexed with 0 errors") {
                logRecords.map { it.message } shouldContain
                    "[ZAAK] Reindexing finished. Reindexed: 2 / 2, skipped: 0, not reindexed because of errors: 0. " +
                    "Solr index contains 0 documents of type 'ZAAK'."
            }

            then("Solr is committed before the finished Solr document count is queried") {
                verifyOrder {
                    ctx.solrClient.addBeans(zaakZoekObjecten)
                    ctx.solrClient.commit(null, true, true)
                    ctx.solrClient.query(match<SolrQuery> { it.rows == 0 })
                }
            }
        }
    }

    given("Reindexing zaken through reindex() where more zaken are returned than the count snapshot reported") {
        val ctx = setupContext()
        val queryResponse = mockk<QueryResponse>()
        val documentList = SolrDocumentList().apply {
            addAll(listOf(SolrDocument(mapOf("id" to 1)), SolrDocument(mapOf("id" to 2))))
        }
        every { queryResponse.results } returns documentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.deleteById(listOf("1", "2")) } returns UpdateResponse()
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        val zakenUuid = listOf(ZaakUuid(UUID.randomUUID()), ZaakUuid(UUID.randomUUID()))
        val zaakZoekObjecten = listOf(createZaakZoekObject(), createZaakZoekObject())

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(zakenUuid, 1)

        every { ctx.zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns ctx.zaakZoekObjectConverter
        zakenUuid.forEachIndexed { index, zaak ->
            every { ctx.zaakZoekObjectConverter.convert(zaak.uuid.toString(), any()) } returns zaakZoekObjecten[index]
        }
        every { ctx.solrClient.addBeans(zaakZoekObjecten) } returns UpdateResponse()

        `when`("reindexing of zaken is called") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindex(ZoekObjectType.ZAAK)
            }

            then("the finished log clamps the negative error count to zero instead of reporting it") {
                logRecords.map { it.message } shouldContain
                    "[ZAAK] Reindexing finished. Reindexed: 2 / 1, skipped: 0, not reindexed because of errors: 0. " +
                    "Solr index contains 0 documents of type 'ZAAK'."
            }
        }
    }

    given("Reindexing zaken through reindex() where the zaak count cannot be determined") {
        val ctx = setupContext()
        val queryResponse = mockk<QueryResponse>()
        val documentList = SolrDocumentList().apply {
            addAll(listOf(SolrDocument(mapOf("id" to 1)), SolrDocument(mapOf("id" to 2))))
        }
        every { queryResponse.results } returns documentList
        every { ctx.solrClient.query(any()) } returns queryResponse
        every {
            ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>())
        } throws IOException("exception")

        `when`("reindexing of zaken is called") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindex(ZoekObjectType.ZAAK)
            }

            then("the log reports the reindex as aborted, not finished, with no reindexed/error summary") {
                logRecords.map { it.message } shouldContain
                    "[ZAAK] Reindexing aborted. Solr index contains 0 documents of type 'ZAAK'."
                logRecords.any { it.message.contains("Reindexed:") } shouldBe false
                logRecords.any { it.message.contains("Reindexing finished") } shouldBe false
            }
        }
    }

    given("reindexAll() reindexes every object type as one complete process") {
        val ctx = setupContext()
        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        every { ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>()) } returns Results(emptyList(), 0)
        every { ctx.flowableTaskService.countOpenTasks() } returns 0
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(any<EnkelvoudigInformatieobjectListParameters>())
        } returns Results(emptyList(), 0)

        `when`("reindexAll is called with the default set of all object types") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAll()
            }

            then("the complete reindexing process logs its own start and finish, in addition to the per-type ones") {
                logRecords.first().message shouldBe
                    "Complete reindexing process started for object types: [ZAAK, TAAK, DOCUMENT]"
                logRecords.last().message shouldBe
                    "Complete reindexing process finished for object types: [ZAAK, TAAK, DOCUMENT]"
                logRecords.map { it.message } shouldContain
                    "[ZAAK] Reindexing started. Solr index currently contains 0 documents of type 'ZAAK'."
                logRecords.map { it.message } shouldContain
                    "[TAAK] Reindexing started. Solr index currently contains 0 documents of type 'TAAK'."
                logRecords.map { it.message } shouldContain
                    "[DOCUMENT] Reindexing started. Solr index currently contains 0 documents of type 'DOCUMENT'."
            }

            then("Solr document counts are queried before and after reindexing, for every object type") {
                verify(exactly = 6) {
                    ctx.solrClient.query(match<SolrQuery> { it.rows == 0 })
                }
            }
        }

        `when`("reindexAll is called with an unordered Set of object types") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAll(hashSetOf(ZoekObjectType.DOCUMENT, ZoekObjectType.ZAAK, ZoekObjectType.TAAK))
            }

            then("the reindex order and logged object type list are still ZAAK, TAAK, DOCUMENT") {
                logRecords.first().message shouldBe
                    "Complete reindexing process started for object types: [ZAAK, TAAK, DOCUMENT]"
                logRecords.last().message shouldBe
                    "Complete reindexing process finished for object types: [ZAAK, TAAK, DOCUMENT]"
            }
        }
    }

    given("reindexAll() where the zaak count cannot be determined and its reindex aborts early") {
        val ctx = setupContext()
        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        every { ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>()) } throws IOException("exception")
        every { ctx.flowableTaskService.countOpenTasks() } returns 0
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(any<EnkelvoudigInformatieobjectListParameters>())
        } returns Results(emptyList(), 0)

        `when`("reindexAll is called with the default set of all object types") {
            ctx.indexingService.reindexAll()

            then("the remaining object types are still reindexed") {
                verify(exactly = 1) {
                    ctx.flowableTaskService.countOpenTasks()
                }
                verify(exactly = 1) {
                    ctx.drcClientService.listEnkelvoudigInformatieObjecten(any<EnkelvoudigInformatieobjectListParameters>())
                }
            }
        }
    }

    given("reindexAll() where the Solr commit fails for one object type") {
        val ctx = setupContext()
        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        // ZAAK is reindexed first; only its commit fails, so TAAK and DOCUMENT commits still succeed
        every { ctx.solrClient.commit(null, true, true) } throws
            SolrServerException("fake commit failure") andThen UpdateResponse() andThen UpdateResponse()

        every { ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>()) } returns Results(emptyList(), 0)
        every { ctx.flowableTaskService.countOpenTasks() } returns 0
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(any<EnkelvoudigInformatieobjectListParameters>())
        } returns Results(emptyList(), 0)

        `when`("reindexAll is called with the default set of all object types") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAll()
            }

            then("the remaining object types are still reindexed") {
                verify(exactly = 1) {
                    ctx.flowableTaskService.countOpenTasks()
                }
                verify(exactly = 1) {
                    ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>())
                }
                verify(exactly = 1) {
                    ctx.drcClientService.listEnkelvoudigInformatieObjecten(any<EnkelvoudigInformatieobjectListParameters>())
                }
            }

            then("ZAAK itself is still reported as finished, not as a failed type") {
                logRecords.map { it.message } shouldContain
                    "[ZAAK] Reindexing finished. Reindexed: 0 / 0, skipped: 0, not reindexed because of errors: 0. " +
                    "Solr index contains 0 documents of type 'ZAAK'."
                logRecords.any { it.message.contains("[ZAAK] Reindexing failed") } shouldBe false
            }

            then("the complete reindexing process still reports it finished") {
                logRecords.last().message shouldBe
                    "Complete reindexing process finished for object types: [ZAAK, TAAK, DOCUMENT]"
            }
        }
    }

    given("reindexAll() where a Solr document count query fails once for one object type") {
        val ctx = setupContext()
        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        // ZAAK is reindexed first; its "Reindexing started" Solr document count query fails once,
        // but that count is purely informational, so ZAAK is still reindexed despite it
        every { ctx.solrClient.query(any()) } throws
            SolrServerException("fake count failure") andThen queryResponse andThen queryResponse andThen
            queryResponse andThen queryResponse andThen queryResponse andThen queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        every { ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>()) } returns Results(emptyList(), 0)
        every { ctx.flowableTaskService.countOpenTasks() } returns 0
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(any<EnkelvoudigInformatieobjectListParameters>())
        } returns Results(emptyList(), 0)

        `when`("reindexAll is called with the default set of all object types") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAll()
            }

            then("ZAAK is still reindexed despite its started-message document count failing") {
                verify(exactly = 1) {
                    ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>())
                }
                logRecords.map { it.message } shouldContain
                    "[ZAAK] Reindexing started. Solr index currently contains unknown documents of type 'ZAAK'."
                logRecords.map { it.message } shouldContain
                    "[ZAAK] Reindexing finished. Reindexed: 0 / 0, skipped: 0, not reindexed because of errors: 0. " +
                    "Solr index contains 0 documents of type 'ZAAK'."
            }

            then("the remaining object types are still reindexed") {
                verify(exactly = 1) {
                    ctx.flowableTaskService.countOpenTasks()
                }
                verify(exactly = 1) {
                    ctx.drcClientService.listEnkelvoudigInformatieObjecten(any<EnkelvoudigInformatieobjectListParameters>())
                }
            }

            then("the complete reindexing process still reports it finished") {
                logRecords.last().message shouldBe
                    "Complete reindexing process finished for object types: [ZAAK, TAAK, DOCUMENT]"
            }
        }
    }

    given("An object type to reindex asynchronously") {
        val ctx = setupContext()
        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()
        every { ctx.flowableTaskService.countOpenTasks() } returns 0

        `when`("reindexAsync is called") {
            val started = ctx.indexingService.reindexAsync(ZoekObjectType.TAAK)

            then(
                """reindexing is reported as started, but does not run until the coroutine dispatcher
                   is advanced"""
            ) {
                started shouldBe true
                verify(exactly = 0) {
                    ctx.flowableTaskService.countOpenTasks()
                }

                ctx.testDispatcher.scheduler.advanceUntilIdle()

                verify(exactly = 1) {
                    ctx.flowableTaskService.countOpenTasks()
                }
            }
        }
    }

    given("An object type whose reindex is already in progress") {
        val ctx = setupContext()
        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()
        every { ctx.flowableTaskService.countOpenTasks() } returns 0
        ctx.indexingService.reindexAsync(ZoekObjectType.TAAK)

        `when`("reindexAsync is called again before the first launch has run") {
            val startedAgain = ctx.indexingService.reindexAsync(ZoekObjectType.TAAK)

            then("the second call is rejected instead of running a duplicate reindex") {
                startedAgain shouldBe false

                // let the still-pending launch from the first call run, so it releases its viewfinder
                // entry and does not leak into any other test relying on the
                // (companion-object-shared) viewfinder
                ctx.testDispatcher.scheduler.advanceUntilIdle()
            }
        }
    }

    given("An asynchronously launched reindex that fails with an error not caught anywhere internally") {
        val ctx = setupContext()
        every { ctx.solrClient.query(any()) } throws Error("fakeUnexpectedFailure")

        `when`("reindexAsync is called and the coroutine dispatcher is advanced") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAsync(ZoekObjectType.TAAK)
                ctx.testDispatcher.scheduler.advanceUntilIdle()
            }

            then("the failure is logged instead of crashing the coroutine, and the object type is released") {
                logRecords.any {
                    it.message == "Unexpected failure while reindexing" && it.thrown?.message == "fakeUnexpectedFailure"
                } shouldBe true

                val startedAgain = ctx.indexingService.reindexAsync(ZoekObjectType.TAAK)
                startedAgain shouldBe true
                ctx.testDispatcher.scheduler.advanceUntilIdle()
            }
        }
    }

    given("A shut down IndexingService") {
        // uses reindexAllAsync, not reindexAsync: reindexAllAsync does not reserve a viewfinder
        // entry synchronously before launching, so a cancelled, never-run launch here cannot leak
        // a permanently "in progress" object type into the (companion-object-shared) viewfinder
        // that other tests rely on
        val ctx = setupContext()
        ctx.indexingService.shutdown()

        `when`("reindexAllAsync is called after shutdown") {
            ctx.indexingService.reindexAllAsync()
            ctx.testDispatcher.scheduler.advanceUntilIdle()

            then("the launched reindex never runs, since its coroutine scope was cancelled") {
                verify(exactly = 0) {
                    ctx.flowableTaskService.countOpenTasks()
                }
            }
        }
    }

    given("A zaak with an open taak, where indexing the zaak itself to Solr fails") {
        val ctx = setupContext()
        val zaakUUID = UUID.randomUUID()
        val openTask = mockk<Task>().apply { every { id } returns "fakeOpenTaskId" }
        val zaakZoekObject = createZaakZoekObject()
        val taakZoekObject = createTaakZoekObject()

        every { ctx.zaakZoekObjectConverter.convert(zaakUUID.toString(), any()) } returns zaakZoekObject
        every { ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", any()) } returns taakZoekObject
        every { ctx.flowableTaskService.listOpenTasksForZaak(zaakUUID) } returns listOf(openTask)
        every { ctx.solrClient.addBeans(listOf(zaakZoekObject)) } throws SolrServerException("fake Solr failure")
        every { ctx.solrClient.addBeans(listOf(taakZoekObject)) } returns UpdateResponse()

        `when`("addOrUpdateZaak is called") {
            var zaakIndexed = true
            val logRecords = captureLogRecords {
                zaakIndexed = ctx.indexingService.addOrUpdateZaak(zaakUUID, true)
            }

            then("the zaak's open taak is still indexed despite the zaak's own Solr indexing failing") {
                verify(exactly = 1) {
                    ctx.solrClient.addBeans(listOf(taakZoekObject))
                }
            }

            then("the zaak's Solr indexing failure is logged instead of being thrown to the caller") {
                logRecords.any {
                    it.message == "[ZAAK] Error during indexing" && it.thrown?.cause?.message == "fake Solr failure"
                } shouldBe true
            }

            then("the return value reports that indexing the zaak itself failed") {
                zaakIndexed shouldBe false
            }
        }
    }

    given("A zaak with an open taak, where indexing the zaak itself to Solr fails, called via addOrUpdateZaakOrThrow") {
        val ctx = setupContext()
        val zaakUUID = UUID.randomUUID()
        val openTask = mockk<Task>().apply { every { id } returns "fakeOpenTaskId" }
        val zaakZoekObject = createZaakZoekObject()
        val taakZoekObject = createTaakZoekObject()

        every { ctx.zaakZoekObjectConverter.convert(zaakUUID.toString(), any()) } returns zaakZoekObject
        every { ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", any()) } returns taakZoekObject
        every { ctx.flowableTaskService.listOpenTasksForZaak(zaakUUID) } returns listOf(openTask)
        every { ctx.solrClient.addBeans(listOf(zaakZoekObject)) } throws SolrServerException("fake Solr failure")
        every { ctx.solrClient.addBeans(listOf(taakZoekObject)) } returns UpdateResponse()

        `when`("addOrUpdateZaakOrThrow is called") {
            val indexingException = shouldThrow<IndexingException> {
                ctx.indexingService.addOrUpdateZaakOrThrow(zaakUUID, true)
            }

            then("it throws instead of silently reporting success to the caller") {
                indexingException.message shouldBe "[ZAAK] Failed to index zaak '$zaakUUID'"
            }

            then("the zaak's open taak is still indexed despite the zaak itself failing to index") {
                verify(exactly = 1) {
                    ctx.solrClient.addBeans(listOf(taakZoekObject))
                }
            }
        }
    }

    given("A zaak with two documenten, where indexing the first document to Solr fails") {
        val ctx = setupContext()
        val zaak = createZaak()
        val zaakInformatieobjecten = listOf(
            createZaakInformatieobjectForReads(zaak = zaak.url),
            createZaakInformatieobjectForReads(zaak = zaak.url)
        )
        val documentZoekObjecten = listOf(createDocumentZoekObject(), createDocumentZoekObject())

        every { ctx.zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ctx.zrcClientService.listZaakinformatieobjecten(zaak) } returns zaakInformatieobjecten
        zaakInformatieobjecten.forEachIndexed { index, zaakInformatieobject ->
            every {
                ctx.documentZoekObjectConverter.convert(
                    zaakInformatieobject.informatieobject.extractUuid().toString(),
                    any()
                )
            } returns documentZoekObjecten[index]
        }
        every { ctx.solrClient.addBeans(listOf(documentZoekObjecten[0])) } throws
            SolrServerException("fake Solr failure")
        every { ctx.solrClient.addBeans(listOf(documentZoekObjecten[1])) } returns UpdateResponse()

        `when`("addOrUpdateInformatieobjectenForZaak is called for the zaak's UUID") {
            val logRecords = captureLogRecords {
                ctx.indexingService.addOrUpdateInformatieobjectenForZaak(zaak.uuid)
            }

            then("the second document is still indexed despite the first document's Solr indexing failing") {
                verify(exactly = 1) {
                    ctx.solrClient.addBeans(listOf(documentZoekObjecten[1]))
                }
            }

            then("the first document's Solr indexing failure is logged instead of being thrown to the caller") {
                logRecords.any {
                    it.message == "[DOCUMENT] Error during indexing" && it.thrown?.cause?.message == "fake Solr failure"
                } shouldBe true
            }
        }
    }

    given("A zaak with an open and a completed taak, where indexing the open taak to Solr fails") {
        val ctx = setupContext()
        val zaakUUID = UUID.randomUUID()
        val openTask = mockk<Task>().apply { every { id } returns "fakeOpenTaskId" }
        val completedTask = mockk<Task>().apply { every { id } returns "fakeCompletedTaskId" }
        val taakZoekObjecten = listOf(createTaakZoekObject(), createTaakZoekObject())

        every { ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", any()) } returns taakZoekObjecten[0]
        every { ctx.taakZoekObjectConverter.convert("fakeCompletedTaskId", any()) } returns taakZoekObjecten[1]
        every { ctx.flowableTaskService.listTasksForZaak(zaakUUID) } returns listOf(openTask, completedTask)
        every { ctx.solrClient.addBeans(listOf(taakZoekObjecten[0])) } throws
            SolrServerException("fake Solr failure")
        every { ctx.solrClient.addBeans(listOf(taakZoekObjecten[1])) } returns UpdateResponse()

        `when`("addOrUpdateTakenForZaak is called") {
            val logRecords = captureLogRecords {
                ctx.indexingService.addOrUpdateTakenForZaak(zaakUUID)
            }

            then("the completed taak is still indexed despite the open taak's Solr indexing failing") {
                verify(exactly = 1) {
                    ctx.solrClient.addBeans(listOf(taakZoekObjecten[1]))
                }
            }

            then("the open taak's Solr indexing failure is logged instead of being thrown to the caller") {
                logRecords.any {
                    it.message == "[TAAK] Error during indexing" && it.thrown?.cause?.message == "fake Solr failure"
                } shouldBe true
            }
        }
    }

    given("A zaak's documenten to reindex asynchronously via addOrUpdateInformatieobjectenForZaakAsync") {
        val ctx = setupContext()
        val zaak = createZaak()
        every { ctx.zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ctx.zrcClientService.listZaakinformatieobjecten(zaak) } returns emptyList()

        `when`("addOrUpdateInformatieobjectenForZaakAsync is called") {
            ctx.indexingService.addOrUpdateInformatieobjectenForZaakAsync(zaak.uuid)

            then(
                "nothing runs yet, since it launches on the coroutine dispatcher, until that " +
                    "dispatcher is advanced"
            ) {
                verify(exactly = 0) {
                    ctx.zrcClientService.readZaak(zaak.uuid)
                }

                ctx.testDispatcher.scheduler.advanceUntilIdle()

                verify(exactly = 1) {
                    ctx.zrcClientService.readZaak(zaak.uuid)
                }
            }
        }
    }

    given("A zaak whose asynchronous documenten reindex fails with an error not caught anywhere internally") {
        val ctx = setupContext()
        val zaak = createZaak()
        every { ctx.zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ctx.zrcClientService.listZaakinformatieobjecten(zaak) } throws
            RuntimeException("fake unexpected failure")

        `when`("addOrUpdateInformatieobjectenForZaakAsync is called and the coroutine dispatcher is advanced") {
            val logRecords = captureLogRecords {
                ctx.indexingService.addOrUpdateInformatieobjectenForZaakAsync(zaak.uuid)
                ctx.testDispatcher.scheduler.advanceUntilIdle()
            }

            then("the failure is logged by the same backstop used by reindexAsync, instead of crashing the coroutine") {
                logRecords.any {
                    it.message == "Unexpected failure while reindexing" &&
                        it.thrown?.message == "fake unexpected failure"
                } shouldBe true
            }
        }
    }

    given("All object types to reindex asynchronously") {
        val ctx = setupContext()
        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()
        every { ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>()) } returns Results(emptyList(), 0)
        every { ctx.flowableTaskService.countOpenTasks() } returns 0
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(any<EnkelvoudigInformatieobjectListParameters>())
        } returns Results(emptyList(), 0)

        `when`("reindexAllAsync is called") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAllAsync()
            }

            then(
                """nothing has run yet, since it launches on the coroutine dispatcher, and the complete
                   reindexing process runs once that dispatcher is advanced"""
            ) {
                logRecords shouldBe emptyList()

                ctx.testDispatcher.scheduler.advanceUntilIdle()

                verify(exactly = 1) {
                    ctx.flowableTaskService.countOpenTasks()
                }
            }
        }
    }

    given("reindexAll() combining ZAAK, TAAK and DOCUMENT for a zaak with one open taak and one linked document") {
        val ctx = setupContext()
        val zaak = createZaak()
        val documentUUID = UUID.randomUUID()
        val openTask = mockk<Task>().apply { every { id } returns "fakeOpenTaskId" }
        val zaakInformatieobject = createZaakInformatieobjectForReads(
            zaak = zaak.url,
            informatieobject = URI("https://example.com/$documentUUID")
        )
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(uuid = documentUUID)
        val zaakZoekObject = createZaakZoekObject()
        val taakZoekObject = createTaakZoekObject()
        val documentZoekObject = createDocumentZoekObject()

        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()
        every { ctx.solrClient.addBeans(any<Collection<*>>()) } returns UpdateResponse()

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(listOf(ZaakUuid(zaak.uuid)), 1)
        every { ctx.zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ctx.flowableTaskService.listOpenTasksForZaak(zaak.uuid) } returns listOf(openTask)
        every { ctx.flowableTaskService.countOpenTasks() } returns 1
        every { ctx.zrcClientService.listZaakinformatieobjecten(zaak) } returns listOf(zaakInformatieobject)
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(match<EnkelvoudigInformatieobjectListParameters> { it.page == 1 })
        } returns Results(listOf(enkelvoudigInformatieObject), 1)

        every { ctx.zaakZoekObjectConverter.convert(zaak, any()) } returns zaakZoekObject
        every { ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", zaak, any()) } returns taakZoekObject
        every { ctx.documentZoekObjectConverter.convert(documentUUID.toString(), zaak, any()) } returns documentZoekObject

        every { ctx.documentZoekObjectConverter.supports(ZoekObjectType.DOCUMENT) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns ctx.documentZoekObjectConverter

        `when`("reindexAll is called with the default set of all object types") {
            ctx.indexingService.reindexAll()

            then("the zaak is retrieved from the ZRC API exactly once, shared by the zaak, its taak and its document") {
                verify(exactly = 1) {
                    ctx.zrcClientService.readZaak(zaak.uuid)
                }
            }

            then("the zaak, its open taak and its linked document are all reindexed") {
                verify(exactly = 1) {
                    ctx.zaakZoekObjectConverter.convert(zaak, any())
                    ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", zaak, any())
                    ctx.documentZoekObjectConverter.convert(documentUUID.toString(), zaak, any())
                }
                verify(exactly = 1) {
                    ctx.solrClient.addBeans(listOf(zaakZoekObject))
                    ctx.solrClient.addBeans(listOf(taakZoekObject))
                    ctx.solrClient.addBeans(listOf(documentZoekObject))
                }
            }

            then("the document is not reconverted by the orphan sweep, since it was already reindexed via its zaak") {
                verify(exactly = 0) {
                    ctx.documentZoekObjectConverter.convert(any<String>(), any<(UUID) -> Boolean>())
                }
            }
        }
    }

    given("reindexAll() combining ZAAK and TAAK only, without DOCUMENT") {
        val ctx = setupContext()
        val zaak = createZaak()
        val openTask = mockk<Task>().apply { every { id } returns "fakeOpenTaskId" }
        val zaakZoekObject = createZaakZoekObject()
        val taakZoekObject = createTaakZoekObject()

        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()
        every { ctx.solrClient.addBeans(any<Collection<*>>()) } returns UpdateResponse()

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(listOf(ZaakUuid(zaak.uuid)), 1)
        every { ctx.zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ctx.flowableTaskService.listOpenTasksForZaak(zaak.uuid) } returns listOf(openTask)
        every { ctx.flowableTaskService.countOpenTasks() } returns 1
        every { ctx.zaakZoekObjectConverter.convert(zaak, any()) } returns zaakZoekObject
        every { ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", zaak, any()) } returns taakZoekObject

        `when`("reindexAll is called for ZAAK and TAAK") {
            ctx.indexingService.reindexAll(setOf(ZoekObjectType.ZAAK, ZoekObjectType.TAAK))

            then("the zaak and its open taak are reindexed, without ever listing its documenten") {
                verify(exactly = 1) {
                    ctx.zrcClientService.readZaak(zaak.uuid)
                    ctx.zaakZoekObjectConverter.convert(zaak, any())
                    ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", zaak, any())
                }
                verify(exactly = 0) {
                    ctx.zrcClientService.listZaakinformatieobjecten(any<Zaak>())
                }
            }
        }
    }

    given("reindexAll() combining ZAAK and TAAK where one zaak's own conversion fails") {
        val ctx = setupContext()
        val failingZaakUUID = UUID.randomUUID()
        val succeedingZaak = createZaak()
        val openTask = mockk<Task>().apply { every { id } returns "fakeOpenTaskId" }
        val zaakZoekObject = createZaakZoekObject()
        val taakZoekObject = createTaakZoekObject()

        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()
        every { ctx.solrClient.addBeans(any<Collection<*>>()) } returns UpdateResponse()

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(listOf(ZaakUuid(failingZaakUUID), ZaakUuid(succeedingZaak.uuid)), 2)
        every { ctx.zrcClientService.readZaak(failingZaakUUID) } throws RuntimeException("fake read zaak failure")
        every { ctx.zrcClientService.readZaak(succeedingZaak.uuid) } returns succeedingZaak
        every { ctx.flowableTaskService.listOpenTasksForZaak(succeedingZaak.uuid) } returns listOf(openTask)
        every { ctx.flowableTaskService.countOpenTasks() } returns 1
        every { ctx.zaakZoekObjectConverter.convert(succeedingZaak, any()) } returns zaakZoekObject
        every { ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", succeedingZaak, any()) } returns taakZoekObject

        `when`("reindexAll is called for ZAAK and TAAK") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAll(setOf(ZoekObjectType.ZAAK, ZoekObjectType.TAAK))
            }

            then("the failing zaak's taken are never attempted, but the other zaak and its taak are still reindexed") {
                verify(exactly = 0) {
                    ctx.flowableTaskService.listOpenTasksForZaak(failingZaakUUID)
                }
                verify(exactly = 1) {
                    ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", succeedingZaak, any())
                }
                logRecords.any {
                    it.message == "[ZAAK] Error during indexing" && it.thrown?.cause?.message == "fake read zaak failure"
                } shouldBe true
            }
        }
    }

    given("reindexAll() combining ZAAK and TAAK where the taak's own conversion fails") {
        val ctx = setupContext()
        val zaak = createZaak()
        val openTask = mockk<Task>().apply { every { id } returns "fakeOpenTaskId" }
        val zaakZoekObject = createZaakZoekObject()

        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()
        every { ctx.solrClient.addBeans(any<Collection<*>>()) } returns UpdateResponse()

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(listOf(ZaakUuid(zaak.uuid)), 1)
        every { ctx.zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ctx.flowableTaskService.listOpenTasksForZaak(zaak.uuid) } returns listOf(openTask)
        every { ctx.flowableTaskService.countOpenTasks() } returns 1
        every { ctx.zaakZoekObjectConverter.convert(zaak, any()) } returns zaakZoekObject
        every { ctx.taakZoekObjectConverter.convert("fakeOpenTaskId", zaak, any()) } throws
            RuntimeException("fake taak conversion failure")

        `when`("reindexAll is called for ZAAK and TAAK") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAll(setOf(ZoekObjectType.ZAAK, ZoekObjectType.TAAK))
            }

            then("the zaak is still reindexed despite its taak failing to convert") {
                verify(exactly = 1) {
                    ctx.solrClient.addBeans(listOf(zaakZoekObject))
                }
                logRecords.any {
                    it.message == "[TAAK] Error during indexing" && it.thrown?.cause?.message == "fake taak conversion failure"
                } shouldBe true
            }
        }
    }

    given("reindexAll() combining ZAAK and DOCUMENT where the document's own conversion fails") {
        val ctx = setupContext()
        val zaak = createZaak()
        val documentUUID = UUID.randomUUID()
        val zaakInformatieobject = createZaakInformatieobjectForReads(
            zaak = zaak.url,
            informatieobject = URI("https://example.com/$documentUUID")
        )
        val zaakZoekObject = createZaakZoekObject()

        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()
        every { ctx.solrClient.addBeans(any<Collection<*>>()) } returns UpdateResponse()

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(listOf(ZaakUuid(zaak.uuid)), 1)
        every { ctx.zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ctx.zrcClientService.listZaakinformatieobjecten(zaak) } returns listOf(zaakInformatieobject)
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(match<EnkelvoudigInformatieobjectListParameters> { it.page == 1 })
        } returns Results(emptyList(), 0)
        every { ctx.zaakZoekObjectConverter.convert(zaak, any()) } returns zaakZoekObject
        every {
            ctx.documentZoekObjectConverter.convert(documentUUID.toString(), zaak, any())
        } throws RuntimeException("fake document conversion failure")

        `when`("reindexAll is called for ZAAK and DOCUMENT") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAll(setOf(ZoekObjectType.ZAAK, ZoekObjectType.DOCUMENT))
            }

            then("the zaak is still reindexed despite its document failing to convert") {
                verify(exactly = 1) {
                    ctx.solrClient.addBeans(listOf(zaakZoekObject))
                }
                logRecords.any {
                    it.message == "[DOCUMENT] Error during indexing" && it.thrown?.cause?.message == "fake document conversion failure"
                } shouldBe true
            }
        }
    }

    given("reindexAll() combining TAAK and DOCUMENT, without ZAAK") {
        val ctx = setupContext()
        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        every { ctx.flowableTaskService.countOpenTasks() } returns 0
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(any<EnkelvoudigInformatieobjectListParameters>())
        } returns Results(emptyList(), 0)

        `when`("reindexAll is called for TAAK and DOCUMENT, which cannot be combined without ZAAK") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAll(setOf(ZoekObjectType.TAAK, ZoekObjectType.DOCUMENT))
            }

            then("TAAK and DOCUMENT are still each fully reindexed independently, and ZAAK is never touched") {
                verify(exactly = 0) {
                    ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>())
                }
                logRecords.map { it.message } shouldContain
                    "[TAAK] Reindexing finished. Reindexed: 0 / 0, skipped: 0, not reindexed because of errors: 0. " +
                    "Solr index contains 0 documents of type 'TAAK'."
                logRecords.map { it.message } shouldContain
                    "[DOCUMENT] Reindexing finished. Reindexed: 0 / 0, skipped: 0, not reindexed because of errors: 0. " +
                    "Solr index contains 0 documents of type 'DOCUMENT'."
            }
        }
    }

    given("reindexAll() combining ZAAK and TAAK where the taak count itself cannot be determined") {
        val ctx = setupContext()
        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        every { ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>()) } returns Results(emptyList(), 0)
        every { ctx.flowableTaskService.countOpenTasks() } throws RuntimeException("fake taak count failure")

        `when`("reindexAll is called for ZAAK and TAAK") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAll(setOf(ZoekObjectType.ZAAK, ZoekObjectType.TAAK))
            }

            then("ZAAK still finishes normally while TAAK is reported as aborted") {
                logRecords.map { it.message } shouldContain
                    "[ZAAK] Reindexing finished. Reindexed: 0 / 0, skipped: 0, not reindexed because of errors: 0. " +
                    "Solr index contains 0 documents of type 'ZAAK'."
                logRecords.map { it.message } shouldContain
                    "[TAAK] Reindexing aborted. Solr index contains 0 documents of type 'TAAK'."
            }
        }
    }

    given("reindexAll() combining ZAAK and DOCUMENT where the informatieobjecten count itself cannot be determined") {
        val ctx = setupContext()
        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        every { ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>()) } returns Results(emptyList(), 0)
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(any<EnkelvoudigInformatieobjectListParameters>())
        } throws RuntimeException("fake informatieobjecten count failure")

        `when`("reindexAll is called for ZAAK and DOCUMENT") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAll(setOf(ZoekObjectType.ZAAK, ZoekObjectType.DOCUMENT))
            }

            then("ZAAK still finishes normally while DOCUMENT is reported as aborted") {
                logRecords.map { it.message } shouldContain
                    "[ZAAK] Reindexing finished. Reindexed: 0 / 0, skipped: 0, not reindexed because of errors: 0. " +
                    "Solr index contains 0 documents of type 'ZAAK'."
                logRecords.map { it.message } shouldContain
                    "[DOCUMENT] Reindexing aborted. Solr index contains 0 documents of type 'DOCUMENT'."
            }
        }
    }

    given("reindexAll() combining ZAAK and DOCUMENT where the orphan sweep's own page fetch fails") {
        val ctx = setupContext()
        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        every { ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>()) } returns Results(emptyList(), 0)
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(match<EnkelvoudigInformatieobjectListParameters> { it.page == 1 })
        } returns Results(emptyList(), 5) andThenThrows RuntimeException("fake sweep page failure")

        `when`("reindexAll is called for ZAAK and DOCUMENT") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAll(setOf(ZoekObjectType.ZAAK, ZoekObjectType.DOCUMENT))
            }

            then("DOCUMENT still reports finished, with the sweep's page failure logged instead of propagating") {
                logRecords.map { it.message } shouldContain
                    "[DOCUMENT] Reindexing finished. Reindexed: 0 / 5, skipped: 0, not reindexed because of errors: 5. " +
                    "Solr index contains 0 documents of type 'DOCUMENT'."
            }
        }
    }

    given("reindexAll() combining ZAAK and DOCUMENT where one document has no linked zaak") {
        val ctx = setupContext()
        val zaak = createZaak()
        val orphanDocumentUUID = UUID.randomUUID()
        val orphanEnkelvoudigInformatieObject = createEnkelvoudigInformatieObject(uuid = orphanDocumentUUID)
        val zaakZoekObject = createZaakZoekObject()

        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()
        every { ctx.solrClient.addBeans(any<Collection<*>>()) } returns UpdateResponse()

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(listOf(ZaakUuid(zaak.uuid)), 1)
        every { ctx.zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ctx.zrcClientService.listZaakinformatieobjecten(zaak) } returns emptyList()
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(match<EnkelvoudigInformatieobjectListParameters> { it.page == 1 })
        } returns Results(listOf(orphanEnkelvoudigInformatieObject), 1)
        // the orphan sweep falls back to the id-only convert(), which itself looks for a linked zaak
        // and returns null (skipped) when there is none - exactly like today's independent DOCUMENT pass
        every {
            ctx.documentZoekObjectConverter.convert(orphanDocumentUUID.toString(), any<(UUID) -> Boolean>())
        } returns null

        every { ctx.zaakZoekObjectConverter.convert(zaak, any()) } returns zaakZoekObject
        every { ctx.documentZoekObjectConverter.supports(ZoekObjectType.DOCUMENT) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns ctx.documentZoekObjectConverter

        `when`("reindexAll is called for ZAAK and DOCUMENT") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAll(setOf(ZoekObjectType.ZAAK, ZoekObjectType.DOCUMENT))
            }

            then("the orphan document is still found by the sweep and counted as skipped") {
                verify(exactly = 1) {
                    ctx.documentZoekObjectConverter.convert(orphanDocumentUUID.toString(), any<(UUID) -> Boolean>())
                }
                logRecords.map { it.message } shouldContain
                    "[DOCUMENT] Reindexing finished. Reindexed: 0 / 1, skipped: 1, not reindexed because of errors: 0. " +
                    "Solr index contains 0 documents of type 'DOCUMENT'."
            }
        }
    }

    given("reindexAll() combining all object types while TAAK is already being reindexed") {
        val ctx = setupContext()
        val emptyDocumentList = SolrDocumentList()
        val queryResponse = mockk<QueryResponse>()
        every { queryResponse.results } returns emptyDocumentList
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.commit(null, true, true) } returns UpdateResponse()

        every { ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>()) } returns Results(emptyList(), 0)
        every { ctx.flowableTaskService.countOpenTasks() } returns 0
        every {
            ctx.drcClientService.listEnkelvoudigInformatieObjecten(any<EnkelvoudigInformatieobjectListParameters>())
        } returns Results(emptyList(), 0)
        ctx.indexingService.reindexAsync(ZoekObjectType.TAAK)

        `when`("reindexAll is called while TAAK is already reserved by another in-progress reindex") {
            val logRecords = captureLogRecords {
                ctx.indexingService.reindexAll()
            }

            then("ZAAK and DOCUMENT are still reindexed independently, and TAAK logs that it is still in progress") {
                logRecords.map { it.message } shouldContain
                    "[TAAK] Reindexing not started, still in progress"
                logRecords.map { it.message } shouldContain
                    "[ZAAK] Reindexing finished. Reindexed: 0 / 0, skipped: 0, not reindexed because of errors: 0. " +
                    "Solr index contains 0 documents of type 'ZAAK'."
                logRecords.map { it.message } shouldContain
                    "[DOCUMENT] Reindexing finished. Reindexed: 0 / 0, skipped: 0, not reindexed because of errors: 0. " +
                    "Solr index contains 0 documents of type 'DOCUMENT'."

                // let the still-pending TAAK reindex launched above actually run, so it releases its
                // viewfinder entry and does not leak into any other test relying on the
                // (companion-object-shared) viewfinder
                ctx.testDispatcher.scheduler.advanceUntilIdle()
            }
        }
    }
})
