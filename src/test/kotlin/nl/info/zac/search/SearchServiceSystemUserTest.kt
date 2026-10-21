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
import nl.info.zac.app.search.model.createZoekParameters
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.runAsSystemUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.SolrClientFactory
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.params.SolrParams

class SearchServiceSystemUserTest : BehaviorSpec({
    val solrClient = mockk<Http2SolrClient>()
    val solrClientFactory = mockk<SolrClientFactory> {
        every { createSolrClient(any()) } returns solrClient
    }
    val searchService = SearchService(
        loggedInUserInstance = mockk<Instance<LoggedInUser>>(),
        policyService = mockk<PolicyService>(),
        solrClientFactory = solrClientFactory
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("Work of the system user and one object of type ZAAK in the search index") {
        val queryResponse = mockk<QueryResponse>()
        val solrDocumentList = mockk<SolrDocumentList>()
        val solrParamsSlot = slot<SolrParams>()
        every { solrClient.query(capture(solrParamsSlot), SolrRequest.METHOD.POST) } returns queryResponse
        every { queryResponse.results } returns solrDocumentList
        every { solrDocumentList.size } returns 0
        every { solrDocumentList.iterator() } returns mutableListOf<SolrDocument>().iterator()
        every { solrDocumentList.numFound } returns 0
        every { queryResponse.facetFields } returns emptyList()

        `when`("the system user searches for zaken") {
            runAsSystemUser { searchService.search(createZoekParameters(zoekObjectType = ZoekObjectType.ZAAK)) }

            then("the search is not narrowed to the zaaktypen of a user, because the system user acts for none") {
                solrParamsSlot.captured.getParams("fq").toList() shouldBe listOf("type:ZAAK")
            }
        }
    }
})
