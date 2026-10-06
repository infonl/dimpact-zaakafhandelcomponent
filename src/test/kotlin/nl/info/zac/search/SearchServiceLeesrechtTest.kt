/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.search

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.enterprise.inject.Instance
import nl.info.client.pabc.ROLE_NAME_BRP_ZOEKEN
import nl.info.client.pabc.ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD
import nl.info.zac.app.search.model.createZoekParameters
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.SolrClientFactory
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.params.SolrParams

class SearchServiceLeesrechtTest : BehaviorSpec({
    val solrClient = mockk<Http2SolrClient>()
    val solrClientFactory = mockk<SolrClientFactory> {
        every { createSolrClient(any()) } returns solrClient
    }
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val leesrollen = setOf("raadpleger", "behandelaar", "coordinator", "recordmanager", "beheerder")
    val zoekService = SearchService(
        loggedInUserInstance = loggedInUserInstance,
        policyService = policyService,
        solrClientFactory = solrClientFactory
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("A logged-in user who holds only brp_zoeken for one zaaktype and behandelaar for another") {
        val zaaktypeWithoutReadRole = "fakeZaaktypeWithoutReadRole"
        val zaaktypeWithReadRole = "fakeZaaktypeWithReadRole"
        val queryResponse = mockk<QueryResponse>()
        val solrDocumentList = mockk<SolrDocumentList>()
        val solrParamsSlot = slot<SolrParams>()
        val loggedInUser = createLoggedInUser(
            applicationRolesPerZaaktype = mapOf(
                zaaktypeWithoutReadRole to setOf(ROLE_NAME_BRP_ZOEKEN),
                zaaktypeWithReadRole to setOf("behandelaar")
            )
        )

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readLeesrollen() } returns leesrollen
        every { solrClient.query(capture(solrParamsSlot), SolrRequest.METHOD.POST) } returns queryResponse
        every { queryResponse.results } returns solrDocumentList
        every { solrDocumentList.size } returns 0
        every { solrDocumentList.iterator() } returns mutableListOf<SolrDocument>().iterator()
        every { solrDocumentList.numFound } returns 0
        every { queryResponse.facetFields } returns emptyList()

        `when`("searching for all documents of type ZAAK") {
            zoekService.search(createZoekParameters(zoekObjectType = ZoekObjectType.ZAAK))

            then("only the zaaktype with the read role is allowed") {
                with(solrParamsSlot.captured) {
                    getParams("fq") shouldBe arrayOf(
                        "{!terms f=zaaktypeOmschrijving separator='\n'}$zaaktypeWithReadRole",
                        "-({!terms f=zaaktypeOmschrijving separator='\n' v=\$zaaktypenZonderZaakspecifiekGeautoriseerd} " +
                            "AND zaakspecifiekGeautoriseerd:true AND -zaakGeautoriseerdeMedewerkers:\"fakeId\")",
                        "type:ZAAK"
                    )
                    get("zaaktypenZonderZaakspecifiekGeautoriseerd") shouldBe "$zaaktypeWithoutReadRole\n$zaaktypeWithReadRole"
                }
            }
        }
    }

    given("A logged-in user who holds only zaakspecifiek_geautoriseerd for one zaaktype and behandelaar for another") {
        val zaaktypeWithoutReadRole = "fakeZaaktypeWithoutReadRole"
        val zaaktypeWithReadRole = "fakeZaaktypeWithReadRole"
        val queryResponse = mockk<QueryResponse>()
        val solrDocumentList = mockk<SolrDocumentList>()
        val solrParamsSlot = slot<SolrParams>()
        val loggedInUser = createLoggedInUser(
            applicationRolesPerZaaktype = mapOf(
                zaaktypeWithoutReadRole to setOf(ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD),
                zaaktypeWithReadRole to setOf("behandelaar")
            )
        )

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readLeesrollen() } returns leesrollen
        every { solrClient.query(capture(solrParamsSlot), SolrRequest.METHOD.POST) } returns queryResponse
        every { queryResponse.results } returns solrDocumentList
        every { solrDocumentList.size } returns 0
        every { solrDocumentList.iterator() } returns mutableListOf<SolrDocument>().iterator()
        every { solrDocumentList.numFound } returns 0
        every { queryResponse.facetFields } returns emptyList()

        `when`("searching for all documents of type TAAK") {
            zoekService.search(createZoekParameters(zoekObjectType = ZoekObjectType.TAAK))

            then("only the zaaktype with the read role is allowed and the zaakspecifiek geautoriseerd filter is unchanged") {
                with(solrParamsSlot.captured) {
                    getParams("fq") shouldBe arrayOf(
                        "{!terms f=zaaktypeOmschrijving separator='\n'}$zaaktypeWithReadRole",
                        "-({!terms f=zaaktypeOmschrijving separator='\n' v=\$zaaktypenZonderZaakspecifiekGeautoriseerd} " +
                            "AND zaakspecifiekGeautoriseerd:true AND -zaakGeautoriseerdeMedewerkers:\"fakeId\")",
                        "type:TAAK"
                    )
                    get("zaaktypenZonderZaakspecifiekGeautoriseerd") shouldBe "$zaaktypeWithReadRole"
                }
            }
        }
    }

    given("A logged-in user who holds raadpleger as an overall role and only brp_zoeken for a zaaktype") {
        val zaaktype = "fakeZaaktype"
        val queryResponse = mockk<QueryResponse>()
        val solrDocumentList = mockk<SolrDocumentList>()
        val solrParamsSlot = slot<SolrParams>()
        val loggedInUser = createLoggedInUser(
            applicationRolesPerZaaktype = mapOf(zaaktype to setOf(ROLE_NAME_BRP_ZOEKEN)),
            overallRoles = setOf("raadpleger")
        )

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readLeesrollen() } returns leesrollen
        every { solrClient.query(capture(solrParamsSlot), SolrRequest.METHOD.POST) } returns queryResponse
        every { queryResponse.results } returns solrDocumentList
        every { solrDocumentList.size } returns 0
        every { solrDocumentList.iterator() } returns mutableListOf<SolrDocument>().iterator()
        every { solrDocumentList.numFound } returns 0
        every { queryResponse.facetFields } returns emptyList()

        `when`("searching for all documents of type ZAAK") {
            zoekService.search(createZoekParameters(zoekObjectType = ZoekObjectType.ZAAK))

            then("the zaaktype is allowed because the overall read role applies to it") {
                with(solrParamsSlot.captured) {
                    getParams("fq")[0] shouldBe "{!terms f=zaaktypeOmschrijving separator='\n'}$zaaktype"
                }
            }
        }
    }

    given("A logged-in user who may read a zaaktype whose omschrijving contains a comma") {
        val zaaktypeWithComma = "fakeZaaktype, with a comma"
        val otherZaaktype = "fakeOtherZaaktype"
        val queryResponse = mockk<QueryResponse>()
        val solrDocumentList = mockk<SolrDocumentList>()
        val solrParamsSlot = slot<SolrParams>()
        val loggedInUser = createLoggedInUser(
            applicationRolesPerZaaktype = mapOf(
                zaaktypeWithComma to setOf("behandelaar"),
                otherZaaktype to setOf("behandelaar")
            )
        )

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readLeesrollen() } returns leesrollen
        every { solrClient.query(capture(solrParamsSlot), SolrRequest.METHOD.POST) } returns queryResponse
        every { queryResponse.results } returns solrDocumentList
        every { solrDocumentList.size } returns 0
        every { solrDocumentList.iterator() } returns mutableListOf<SolrDocument>().iterator()
        every { solrDocumentList.numFound } returns 0
        every { queryResponse.facetFields } returns emptyList()

        `when`("searching for all documents of type ZAAK") {
            zoekService.search(createZoekParameters(zoekObjectType = ZoekObjectType.ZAAK))

            then("the zaaktypen are separated by a newline so that the comma does not split the zaaktype in two") {
                with(solrParamsSlot.captured) {
                    getParams("fq")[0] shouldBe
                        "{!terms f=zaaktypeOmschrijving separator='\n'}$zaaktypeWithComma\n$otherZaaktype"
                }
            }
        }
    }

    given("A logged-in user who holds no read role for any zaaktype nor as an overall role") {
        val zaaktype = "fakeZaaktype"
        val queryResponse = mockk<QueryResponse>()
        val solrDocumentList = mockk<SolrDocumentList>()
        val solrParamsSlot = slot<SolrParams>()
        val loggedInUser = createLoggedInUser(
            applicationRolesPerZaaktype = mapOf(zaaktype to setOf(ROLE_NAME_BRP_ZOEKEN)),
            overallRoles = setOf(ROLE_NAME_BRP_ZOEKEN)
        )

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readLeesrollen() } returns leesrollen
        every { solrClient.query(capture(solrParamsSlot), SolrRequest.METHOD.POST) } returns queryResponse
        every { queryResponse.results } returns solrDocumentList
        every { solrDocumentList.size } returns 0
        every { solrDocumentList.iterator() } returns mutableListOf<SolrDocument>().iterator()
        every { solrDocumentList.numFound } returns 0
        every { queryResponse.facetFields } returns emptyList()

        `when`("searching for all documents of type DOCUMENT") {
            zoekService.search(createZoekParameters(zoekObjectType = ZoekObjectType.DOCUMENT))

            then("only a non-existing zaaktype is allowed, so nothing is found") {
                with(solrParamsSlot.captured) {
                    getParams("fq")[0] shouldBe """zaaktypeOmschrijving:"\-NON\-EXISTING\-ZAAKTYPE\-""""
                }
            }
        }
    }
})
