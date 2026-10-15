/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.search

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.SolrClientFactory
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.params.SolrParams

class SearchServiceZaakspecifiekGeautoriseerdTest : BehaviorSpec({
    val solrClient = mockk<Http2SolrClient>()
    val solrClientFactory = mockk<SolrClientFactory> {
        every { createSolrClient(any()) } returns solrClient
    }
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val zoekService = SearchService(
        loggedInUserInstance = loggedInUserInstance,
        policyService = policyService,
        solrClientFactory = solrClientFactory
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Looking up which zaakspecifiek geautoriseerde objects are in a set of values") {
        given("two task IDs of which only the first belongs to a zaakspecifiek geautoriseerde zaak") {
            val zaakspecifiekGeautoriseerdeTaakId = "fakeTaakId1"
            val otherTaakId = "fakeTaakId2"
            val queryResponse = mockk<QueryResponse>()
            val solrParamsSlot = slot<SolrParams>()
            every { solrClient.query(capture(solrParamsSlot), SolrRequest.METHOD.POST) } returns queryResponse
            every { queryResponse.results } returns SolrDocumentList().apply {
                add(SolrDocument(mutableMapOf<String, Any>("id" to zaakspecifiekGeautoriseerdeTaakId)))
            }

            `when`("the zaakspecifiek geautoriseerde task IDs are looked up") {
                val taakIds = zoekService.listZaakspecifiekGeautoriseerdeWaarden(
                    waarden = listOf(zaakspecifiekGeautoriseerdeTaakId, otherTaakId),
                    type = ZoekObjectType.TAAK,
                    veld = "id"
                )

                then("only the task ID found in the index is returned") {
                    taakIds shouldBe setOf(zaakspecifiekGeautoriseerdeTaakId)
                }

                and("the query is limited to marked taken with those IDs, without the filters of the logged-in user") {
                    with(solrParamsSlot.captured) {
                        getParams("fq") shouldBe arrayOf(
                            "type:TAAK",
                            "zaakspecifiekGeautoriseerd:true",
                            "{!terms f=id separator='\n' v=\$gezochteWaarden}"
                        )
                        get("gezochteWaarden") shouldBe "$zaakspecifiekGeautoriseerdeTaakId\n$otherTaakId"
                        get("fl") shouldBe "id"
                        get("rows") shouldBe "2"
                    }
                }
            }
        }

        given("a zaaknummer that occurs twice and belongs to a zaakspecifiek geautoriseerde zaak") {
            val zaakIdentificatie = "fakeZaakIdentificatie"
            val queryResponse = mockk<QueryResponse>()
            val solrParamsSlot = slot<SolrParams>()
            every { solrClient.query(capture(solrParamsSlot), SolrRequest.METHOD.POST) } returns queryResponse
            every { queryResponse.results } returns SolrDocumentList().apply {
                add(SolrDocument(mutableMapOf<String, Any>("zaak_identificatie" to zaakIdentificatie)))
            }

            `when`("the zaakspecifiek geautoriseerde zaaknummers are looked up") {
                val zaakIdentificaties = zoekService.listZaakspecifiekGeautoriseerdeWaarden(
                    listOf(zaakIdentificatie, zaakIdentificatie)
                )

                then("the zaaknummer is returned once") {
                    zaakIdentificaties shouldBe setOf(zaakIdentificatie)
                }

                and("the query looks for marked zaken by zaaknummer, asking for each zaaknummer once") {
                    with(solrParamsSlot.captured) {
                        getParams("fq") shouldBe arrayOf(
                            "type:ZAAK",
                            "zaakspecifiekGeautoriseerd:true",
                            "{!terms f=zaak_identificatie separator='\n' v=\$gezochteWaarden}"
                        )
                        get("gezochteWaarden") shouldBe zaakIdentificatie
                        get("rows") shouldBe "1"
                    }
                }
            }
        }

        given("no task IDs") {
            clearMocks(solrClient)

            `when`("the zaakspecifiek geautoriseerde task IDs are looked up") {
                val taakIds = zoekService.listZaakspecifiekGeautoriseerdeWaarden(
                    waarden = emptyList(),
                    type = ZoekObjectType.TAAK,
                    veld = "id"
                )

                then("nothing is returned and the search index is not queried") {
                    taakIds shouldBe emptySet()
                    verify(exactly = 0) { solrClient.query(any(), any<SolrRequest.METHOD>()) }
                }
            }
        }
    }
})
