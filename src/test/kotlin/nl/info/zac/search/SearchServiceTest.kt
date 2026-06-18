/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.search

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.core.SearchResponse
import co.elastic.clients.elasticsearch.core.search.Hit
import co.elastic.clients.elasticsearch.core.search.HitsMetadata
import co.elastic.clients.elasticsearch.core.search.TotalHits
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.enterprise.inject.Instance
import nl.info.zac.app.search.model.createZoekParameters
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.search.elasticsearch.SearchIndex
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.SorteerVeld
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.shared.model.SorteerRichting

private fun mockSearchResponse(
    hits: List<Hit<Map<String, Any?>>> = emptyList(),
    total: Long = 0,
    aggregations: Map<String, Aggregate> = emptyMap()
): SearchResponse<Map<String, Any?>> {
    val hitsMetadata = mockk<HitsMetadata<Map<String, Any?>>>()
    every { hitsMetadata.hits() } returns hits
    every { hitsMetadata.total() } returns TotalHits.of { it.value(total).relation(TotalHitsRelation.Eq) }
    val response = mockk<SearchResponse<Map<String, Any?>>>()
    every { response.hits() } returns hitsMetadata
    every { response.aggregations() } returns aggregations
    return response
}

private fun zaakHit(id: String): Hit<Map<String, Any?>> {
    val hit = mockk<Hit<Map<String, Any?>>>()
    every { hit.index() } returns SearchIndex.ZAAK.indexName
    every { hit.source() } returns mapOf(
        "id" to id,
        "type" to "ZAAK",
        "zaak_identificatie" to "ZAAK-$id",
        "zaak_zaaktypeUuid" to "fakeZaaktypeUuid",
        "zaak_zaaktypeIdentificatie" to "fakeZaaktypeIdentificatie",
        "zaak_zaaktypeOmschrijving" to "fakeZaaktypeOmschrijving"
    )
    return hit
}

class SearchServiceTest : BehaviorSpec({
    val elasticsearchClient = mockk<ElasticsearchClient>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val searchService = SearchService(loggedInUserInstance, elasticsearchClient)

    @Suppress("UNCHECKED_CAST")
    fun stubSearch(response: SearchResponse<Map<String, Any?>>): CapturingSlot<SearchRequest> {
        val requestSlot = slot<SearchRequest>()
        every {
            elasticsearchClient.search(capture(requestSlot), Map::class.java)
        } returns response as SearchResponse<Map<*, *>>
        return requestSlot
    }

    Given("A logged-in user authorised for all zaaktypes and two zaken in the search index") {
        every { loggedInUserInstance.get() } returns createLoggedInUser()
        val requestSlot = stubSearch(mockSearchResponse(hits = listOf(zaakHit("1"), zaakHit("2")), total = 2))

        When("searching for zaken") {
            val zoekResultaat = searchService.zoek(createZoekParameters(zoekObjectType = ZoekObjectType.ZAAK))

            Then("it queries all three indices and maps the hits back to zaak zoek objecten") {
                zoekResultaat.count shouldBe 2
                zoekResultaat.items.size shouldBe 2
                zoekResultaat.items.forEach { it.shouldBeTypeOfZaak() }
                requestSlot.captured.index() shouldContainExactly SearchIndex.ALL_INDEX_NAMES
            }
        }
    }

    Given("A logged-in user and a default (descending) sort") {
        every { loggedInUserInstance.get() } returns createLoggedInUser()
        val requestSlot = stubSearch(mockSearchResponse())

        When("searching sorted on a specific field") {
            searchService.zoek(
                createZoekParameters(
                    zoekObjectType = ZoekObjectType.ZAAK,
                    sorteerVeld = SorteerVeld.ZAAK_STATUS,
                    sorteerRichting = SorteerRichting.DESCENDING
                )
            )

            Then("the requested sort is applied with the deterministic created/identificatie/id fallback") {
                val sortFields = requestSlot.captured.sort().map { it.field().field() }
                sortFields shouldContainExactly listOf(
                    "zaak_statustypeOmschrijving",
                    "created",
                    "zaak_identificatie",
                    "id"
                )
            }
        }

        When("searching with sort direction NONE") {
            val noneRequestSlot = stubSearch(mockSearchResponse())
            searchService.zoek(
                createZoekParameters(
                    zoekObjectType = ZoekObjectType.ZAAK,
                    sorteerVeld = SorteerVeld.ZAAK_STATUS,
                    sorteerRichting = SorteerRichting.NONE
                )
            )

            Then("only the deterministic fallback sort is applied") {
                val sortFields = noneRequestSlot.captured.sort().map { it.field().field() }
                sortFields shouldContainExactly listOf("created", "zaak_identificatie", "id")
            }
        }
    }

    Given("A logged-in user selecting two facet values for different facets") {
        every { loggedInUserInstance.get() } returns createLoggedInUser()
        val requestSlot = stubSearch(mockSearchResponse())

        When("searching with both a ZAAKTYPE and a BEHANDELAAR filter selected") {
            searchService.zoek(
                createZoekParameters(zoekObjectType = ZoekObjectType.ZAAK).apply {
                    addFilter(FilterVeld.ZAAKTYPE, FilterParameters(listOf("fakeZaaktype"), false))
                    addFilter(FilterVeld.BEHANDELAAR, FilterParameters(listOf("fakeBehandelaar"), false))
                }
            )

            Then("each facet aggregation re-applies the other facet's selection but not its own") {
                val aggregations = requestSlot.captured.aggregations()
                // an aggregation exists for every available zaak facet field
                aggregations.keys shouldContainAll listOf(
                    "zaaktypeOmschrijving",
                    "behandelaarNaam",
                    "groepNaam",
                    "zaak_statustypeOmschrijving"
                )
                // the BEHANDELAAR aggregation re-applies the ZAAKTYPE selection but excludes its own
                val behandelaarFilters = aggregations["behandelaarNaam"]!!.filter().bool().filter()
                behandelaarFilters.size shouldBe 1
                behandelaarFilters[0].terms().field() shouldBe "zaaktypeOmschrijving"
                aggregations["behandelaarNaam"]!!
                    .aggregations()["values"]!!.terms().field() shouldBe "behandelaarNaam"

                // and the ZAAKTYPE aggregation re-applies the BEHANDELAAR selection but excludes its own
                val zaaktypeFilters = aggregations["zaaktypeOmschrijving"]!!.filter().bool().filter()
                zaaktypeFilters.size shouldBe 1
                zaaktypeFilters[0].terms().field() shouldBe "behandelaarNaam"

                // the selected values are applied as a post filter so they do not collapse the facet counts
                requestSlot.captured.postFilter() shouldNotBe null
            }
        }
    }
})

private fun Any.shouldBeTypeOfZaak() {
    (this as ZaakZoekObject).getType() shouldBe ZoekObjectType.ZAAK
}
