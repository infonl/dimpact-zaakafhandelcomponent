/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.search

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.BulkResponse
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse
import co.elastic.clients.util.ObjectBuilder
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
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
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import java.io.IOException
import java.net.URI
import java.util.UUID
import java.util.function.Function

private data class TestContext(
    val elasticsearchClient: ElasticsearchClient,
    val zaakZoekObjectConverter: ZaakZoekObjectConverter,
    val converterInstances: Instance<AbstractZoekObjectConverter<out ZoekObject>>,
    val converterInstancesIterator: MutableIterator<AbstractZoekObjectConverter<out ZoekObject>>,
    val drcClientService: DrcClientService,
    val flowableTaskService: FlowableTaskService,
    val zrcClientService: ZrcClientService,
    val indexingService: IndexingService
)

private fun setupContext(): TestContext {
    val elasticsearchClient = mockk<ElasticsearchClient>()
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
        flowableTaskService,
        elasticsearchClient
    )

    return TestContext(
        elasticsearchClient,
        zaakZoekObjectConverter,
        converterInstances,
        converterInstancesIterator,
        drcClientService,
        flowableTaskService,
        zrcClientService,
        indexingService
    )
}

private fun TestContext.stubConverter(uuids: List<String>, zaakZoekObjecten: List<ZaakZoekObject>) {
    every { zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
    every { converterInstances.iterator() } returns converterInstancesIterator
    every { converterInstancesIterator.hasNext() } returns true andThen true andThen false
    every { converterInstancesIterator.next() } returns zaakZoekObjectConverter andThen zaakZoekObjectConverter
    uuids.forEachIndexed { index, uuid ->
        every { zaakZoekObjectConverter.convert(uuid) } returns zaakZoekObjecten[index]
    }
}

class IndexingServiceTest : BehaviorSpec({
    afterEach {
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
        val zaakZoekObjecten = listOf(createZaakZoekObject(), createZaakZoekObject())
        ctx.stubConverter(zaken.map { it.uuid.toString() }, zaakZoekObjecten)
        every { ctx.elasticsearchClient.bulk(any<BulkRequest>()) } returns mockk<BulkResponse>()

        When("the indexeerDirect method is called to index the two zaken") {
            ctx.indexingService.indexeerDirect(zaken.map { it.uuid.toString() }, ZoekObjectType.ZAAK, false)

            Then("the two zaak zoek objecten should be added to the Elasticsearch index in a single bulk request") {
                verify(exactly = 1) { ctx.elasticsearchClient.bulk(any<BulkRequest>()) }
            }
        }
    }

    Given("Elasticsearch indexing exists") {
        val ctx = setupContext()
        val zakenUuid = listOf(ZaakUuid(UUID.randomUUID()), ZaakUuid(UUID.randomUUID()))
        val zaakZoekObjecten = listOf(createZaakZoekObject(), createZaakZoekObject())

        beforeContainer {
            clearMocks(ctx.elasticsearchClient)
            every {
                ctx.elasticsearchClient.deleteByQuery(
                    any<Function<DeleteByQueryRequest.Builder, ObjectBuilder<DeleteByQueryRequest>>>()
                )
            } returns mockk<DeleteByQueryResponse>()
            every { ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>()) } returnsMany listOf(
                Results(zakenUuid, 2),
                Results(zakenUuid, 2),
                Results(emptyList(), 0)
            )
            ctx.stubConverter(zakenUuid.map { it.uuid.toString() }, zaakZoekObjecten)
        }

        When("reindexing of zaken is called") {
            every { ctx.elasticsearchClient.bulk(any<BulkRequest>()) } returns mockk<BulkResponse>()

            ctx.indexingService.reindex(ZoekObjectType.ZAAK)

            Then("it removes the existing entities and indexes the new ones") {
                verify(exactly = 1) {
                    ctx.elasticsearchClient.deleteByQuery(
                        any<Function<DeleteByQueryRequest.Builder, ObjectBuilder<DeleteByQueryRequest>>>()
                    )
                    ctx.elasticsearchClient.bulk(any<BulkRequest>())
                }
            }
        }

        When("bulk indexing in Elasticsearch errors") {
            every { ctx.elasticsearchClient.bulk(any<BulkRequest>()) } throws RuntimeException("Elasticsearch exception")

            ctx.indexingService.reindex(ZoekObjectType.ZAAK)

            Then("it ignores the error") {
                verify(exactly = 1) {
                    ctx.elasticsearchClient.deleteByQuery(
                        any<Function<DeleteByQueryRequest.Builder, ObjectBuilder<DeleteByQueryRequest>>>()
                    )
                    ctx.elasticsearchClient.bulk(any<BulkRequest>())
                }
            }
        }
    }

    Given("Elasticsearch indexing exists and zaak count cannot be obtained") {
        val ctx = setupContext()
        every {
            ctx.elasticsearchClient.deleteByQuery(
                any<Function<DeleteByQueryRequest.Builder, ObjectBuilder<DeleteByQueryRequest>>>()
            )
        } returns mockk<DeleteByQueryResponse>()
        every { ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>()) } throws IOException("IO exception")

        When("reading zaak count throws an error") {
            ctx.indexingService.reindex(ZoekObjectType.ZAAK)

            Then("it aborts and does not try to list zaken again") {
                verify(exactly = 1) {
                    ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>())
                }
            }
        }
    }

    Given("Elasticsearch indexing exists and zaak count is available") {
        val ctx = setupContext()
        val zakenUuid = listOf(ZaakUuid(UUID.randomUUID()), ZaakUuid(UUID.randomUUID()))
        val zaakZoekObjecten = listOf(createZaakZoekObject(), createZaakZoekObject())

        every {
            ctx.elasticsearchClient.deleteByQuery(
                any<Function<DeleteByQueryRequest.Builder, ObjectBuilder<DeleteByQueryRequest>>>()
            )
        } returns mockk<DeleteByQueryResponse>()
        every { ctx.elasticsearchClient.bulk(any<BulkRequest>()) } returns mockk<BulkResponse>()
        every {
            ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 1 })
        } returns Results(zakenUuid, 102)
        ctx.stubConverter(zakenUuid.map { it.uuid.toString() }, zaakZoekObjecten)

        When("reading the zaak list throws an `IOException` for a later page") {
            every {
                ctx.zrcClientService.listZakenUuids(match<ZaakListParameters> { it.page == 2 })
            } throws IOException("exception")

            ctx.indexingService.reindex(ZoekObjectType.ZAAK)

            Then("it continues without exception") {
                verify(exactly = 3) {
                    ctx.zrcClientService.listZakenUuids(any<ZaakListParameters>())
                }
            }
        }
    }
})
