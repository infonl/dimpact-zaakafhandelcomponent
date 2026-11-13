/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.enterprise.inject.Instance
import nl.info.zac.app.search.model.createZoekParameters
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.search.model.DatumVeld
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.SolrClientFactory
import nl.info.zac.time.DatumRange
import nl.info.zac.time.UtcDefaultTimeZone
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.params.SolrParams
import java.time.LocalDate
import java.util.EnumMap

class SearchServiceDatumRangeTest : BehaviorSpec({
    extension(UtcDefaultTimeZone())

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

    given("the JVM time zone UTC, a logged-in user and a search index without results") {
        val queryResponse = mockk<QueryResponse>()
        val solrDocumentList = mockk<SolrDocumentList>()
        val solrParamsSlot = slot<SolrParams>()

        every { loggedInUserInstance.get() } returns createLoggedInUser(
            applicationRolesPerZaaktype = mapOf("fakeZaaktype1" to setOf("behandelaar"))
        )
        every { policyService.readLeesrollen() } returns leesrollen
        every { solrClient.query(capture(solrParamsSlot), SolrRequest.METHOD.POST) } returns queryResponse
        every { queryResponse.results } returns solrDocumentList
        every { solrDocumentList.size } returns 0
        every { solrDocumentList.iterator() } returns mutableListOf<SolrDocument>().iterator()
        every { solrDocumentList.numFound } returns 0
        every { queryResponse.facetFields } returns emptyList()

        `when`("searching tasks created van 30-07-2026 t/m 30-07-2026") {
            zoekService.search(
                createZoekParameters(
                    zoekObjectType = ZoekObjectType.TAAK,
                    datums = EnumMap<DatumVeld, DatumRange>(DatumVeld::class.java).apply {
                        put(
                            DatumVeld.TAAK_CREATIEDATUM,
                            DatumRange(van = LocalDate.of(2026, 7, 30), tot = LocalDate.of(2026, 7, 30))
                        )
                    }
                )
            )

            then(
                "the filter runs from the start of 30-07-2026 in Europe/Amsterdam up to, but not including, " +
                    "the start of 31-07-2026 in Europe/Amsterdam"
            ) {
                solrParamsSlot.captured.getParams("fq") shouldContain
                    "taak_creatiedatum:[2026-07-29T22:00:00Z TO 2026-07-30T22:00:00Z}"
            }
        }

        `when`("searching tasks with a fatale datum van 30-07-2026 and without a t/m date") {
            zoekService.search(
                createZoekParameters(
                    zoekObjectType = ZoekObjectType.TAAK,
                    datums = EnumMap<DatumVeld, DatumRange>(DatumVeld::class.java).apply {
                        put(DatumVeld.TAAK_FATALEDATUM, DatumRange(van = LocalDate.of(2026, 7, 30), tot = null))
                    }
                )
            )

            then("the filter has no upper bound") {
                solrParamsSlot.captured.getParams("fq") shouldContain "taak_fataledatum:[2026-07-29T22:00:00Z TO *}"
            }
        }

        `when`("searching documents registered t/m 30-07-2026 and without a van date") {
            zoekService.search(
                createZoekParameters(
                    zoekObjectType = ZoekObjectType.DOCUMENT,
                    datums = EnumMap<DatumVeld, DatumRange>(DatumVeld::class.java).apply {
                        put(
                            DatumVeld.INFORMATIEOBJECT_REGISTRATIEDATUM,
                            DatumRange(van = null, tot = LocalDate.of(2026, 7, 30))
                        )
                    }
                )
            )

            then("the filter has no lower bound") {
                solrParamsSlot.captured.getParams("fq") shouldContain
                    "informatieobject_registratiedatum:[* TO 2026-07-30T22:00:00Z}"
            }
        }
    }
})
