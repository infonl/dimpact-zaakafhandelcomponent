/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.search

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.zrc.model.ZaakListParameters
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.ZaakUuid
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.search.converter.AbstractZoekObjectConverter
import nl.info.zac.search.converter.DocumentZoekObjectConverter
import nl.info.zac.search.converter.ZaakZoekObjectConverter
import nl.info.zac.search.model.createDocumentZoekObject
import nl.info.zac.search.model.createZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.params.CursorMarkParams
import org.eclipse.microprofile.config.ConfigProvider
import java.io.IOException
import java.net.URI
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

private data class TestContext(
    val solrClient: Http2SolrClient,
    val zaakZoekObjectConverter: ZaakZoekObjectConverter,
    val converterInstances: Instance<AbstractZoekObjectConverter<out ZoekObject>>,
    val converterInstancesIterator: MutableIterator<AbstractZoekObjectConverter<out ZoekObject>>,
    val drcClientService: DrcClientService,
    val flowableTaskService: FlowableTaskService,
    val zrcClientService: ZrcClientService,
    val indexingService: IndexingService
)

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
    val converterInstances = mockk<Instance<AbstractZoekObjectConverter<out ZoekObject>>>()
    val converterInstancesIterator = mockk<MutableIterator<AbstractZoekObjectConverter<out ZoekObject>>>()
    val drcClientService = mockk<DrcClientService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val zrcClientService = mockk<ZrcClientService>()

    val indexingService = IndexingService(
        converterInstances,
        zrcClientService,
        drcClientService,
        flowableTaskService
    )

    return TestContext(
        solrClient,
        zaakZoekObjectConverter,
        converterInstances,
        converterInstancesIterator,
        drcClientService,
        flowableTaskService,
        zrcClientService,
        indexingService
    )
}

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
            every { ctx.zaakZoekObjectConverter.convert(zaak.uuid.toString()) } returns zaakZoekObjecten[index]
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
        every { ctx.zaakZoekObjectConverter.convert(zaken[0].uuid.toString()) } returns zaakZoekObjecten[0]
        every { ctx.zaakZoekObjectConverter.convert(zaken[1].uuid.toString()) } throws
            RuntimeException("fake conversion failure")
        every { ctx.zaakZoekObjectConverter.convert(zaken[2].uuid.toString()) } returns zaakZoekObjecten[1]
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
            every { ctx.zaakZoekObjectConverter.convert(zaak.uuid.toString()) } answers {
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
                every { ctx.zaakZoekObjectConverter.convert(zaak.uuid.toString()) } returns zaakZoekObjecten[index]
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
        every { queryResponse.nextCursorMark } returns CursorMarkParams.CURSOR_MARK_START
        every { ctx.solrClient.query(any()) } returns queryResponse
        every { ctx.solrClient.deleteById(listOf("1", "2")) } returns UpdateResponse()

        val ioException = IOException("IO exception")
        every { ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>()) } throws ioException

        `when`("reading zaak count throws an error") {
            ctx.indexingService.reindex(ZoekObjectType.ZAAK)

            then("aborts and does not try to list zaken") {
                verify(exactly = 1) {
                    ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>())
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

        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(zakenUuid, 102)

        every { ctx.zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen true andThen false
        every { ctx.converterInstancesIterator.next() } returns ctx.zaakZoekObjectConverter andThen ctx.zaakZoekObjectConverter
        zakenUuid.forEachIndexed { index, zaak ->
            every { ctx.zaakZoekObjectConverter.convert(zaak.uuid.toString()) } returns zaakZoekObjecten[index]
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
                documentZoekObjectConverter.convert(informatieobject.url.extractUuid().toString())
            } returns documentZoekObjectenPage1[index]
        }
        every {
            documentZoekObjectConverter.convert(informatieobjectPage2.url.extractUuid().toString())
        } returns documentZoekObjectPage2

        `when`("reindexing of informatieobjecten is called") {
            ctx.indexingService.reindex(ZoekObjectType.DOCUMENT)

            then("the second page is fetched using its own page number instead of always page one") {
                verify(exactly = 1) {
                    ctx.drcClientService.listEnkelvoudigInformatieObjecten(
                        match<EnkelvoudigInformatieobjectListParameters> { it.page == 2 }
                    )
                    documentZoekObjectConverter.convert(informatieobjectPage2.url.extractUuid().toString())
                }
            }
        }
    }
})
