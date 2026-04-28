/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.search

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.zrc.model.ZaakListParameters
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.ZaakUuid
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.search.converter.AbstractZoekObjectConverter
import nl.info.zac.search.converter.ZaakZoekObjectConverter
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
    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Two zaken") {
        val ctx = setupContext()
        val zaakType = createZaakType()
        val zaaktypeURI = URI("http://example.com/${zaakType.url}")
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

        When(
            """The indexeer direct method is called to index the two zaken"""
        ) {
            ctx.indexingService.indexeerDirect(zaken.map { it.uuid.toString() }, ZoekObjectType.ZAAK, false)

            Then(
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

    Given("Dual-write is active and a zaak is indexed") {
        val ctx = setupContext()
        // relaxed = true so close() and other unstubbed calls work without explicit stubs
        val secondarySolrClient = mockk<Http2SolrClient>(relaxed = true)
        val zaakUUID = UUID.randomUUID()
        val zaakZoekObject = createZaakZoekObject()

        // setupContext() consumed the first build() call during IndexingService init;
        // override now so startDualWrite gets secondarySolrClient as the dual-write client
        every { anyConstructed<Http2SolrClient.Builder>().build() } returns secondarySolrClient
        ctx.indexingService.startDualWrite("zac_b")

        every { ctx.zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
        every { ctx.converterInstances.iterator() } returns ctx.converterInstancesIterator
        every { ctx.converterInstancesIterator.hasNext() } returns true andThen false
        every { ctx.converterInstancesIterator.next() } returns ctx.zaakZoekObjectConverter
        every { ctx.zaakZoekObjectConverter.convert(zaakUUID.toString()) } returns zaakZoekObject
        every { ctx.solrClient.addBeans(listOf(zaakZoekObject)) } returns UpdateResponse()

        ctx.indexingService.addOrUpdateZaak(zaakUUID, false)

        When("the zaak is indexed") {
            Then("the document is written to both primary and secondary Solr clients") {
                verify(exactly = 1) { ctx.solrClient.addBeans(any<Collection<*>>()) }
                verify(exactly = 1) { secondarySolrClient.addBeans(any<Collection<*>>()) }
            }
        }
    }

    Given("Dual-write is active and a zaak is removed") {
        val ctx = setupContext()
        val secondarySolrClient = mockk<Http2SolrClient>(relaxed = true)
        val zaakUUID = UUID.randomUUID()

        every { anyConstructed<Http2SolrClient.Builder>().build() } returns secondarySolrClient
        ctx.indexingService.startDualWrite("zac_b")
        every { ctx.solrClient.deleteById(zaakUUID.toString()) } returns UpdateResponse()

        ctx.indexingService.removeZaak(zaakUUID)

        When("the zaak is removed from the Solr index") {
            Then("the delete is sent to both primary and secondary Solr clients") {
                verify(exactly = 1) { ctx.solrClient.deleteById(zaakUUID.toString()) }
                verify(exactly = 1) { secondarySolrClient.deleteById(zaakUUID.toString()) }
            }
        }
    }

    Given("Solr indexing exists") {
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

        When("reindexing of zaken is called") {
            every { ctx.solrClient.addBeans(zaakZoekObjecten) } returns UpdateResponse()

            ctx.indexingService.reindex(ZoekObjectType.ZAAK)

            Then("it finishes successfully") {
                verify(exactly = 1) {
                    ctx.solrClient.deleteById(any<List<String>>())
                    ctx.solrClient.addBeans(any<Collection<*>>())
                }
            }
        }

        When("adding beans in Solr errors") {
            val solrException = SolrServerException("Solr exception")
            every { ctx.solrClient.addBeans(any<Collection<*>>()) } throws solrException

            ctx.indexingService.reindex(ZoekObjectType.ZAAK)

            Then("ignores errors") {
                verify(exactly = 1) {
                    ctx.solrClient.deleteById(any<List<String>>())
                    ctx.solrClient.addBeans(any<Collection<*>>())
                }
            }
        }
    }

    Given("Solr indexing exists and zaak count cannot be obtained") {
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

        When("reading zaak count throws an error") {
            ctx.indexingService.reindex(ZoekObjectType.ZAAK)

            Then("aborts and does not try to list zaken") {
                verify(exactly = 1) {
                    ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>())
                }
            }
        }
    }

    Given("Solr indexing exists and zaak count is available") {
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

        When("reading zaak list throws an `IOException`") {
            every {
                ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 2 })
            } throws IOException("exception")

            ctx.indexingService.reindex(ZoekObjectType.ZAAK)

            Then("continues without exception") {
                verify(exactly = 3) {
                    ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>())
                }
            }
        }
    }
})
