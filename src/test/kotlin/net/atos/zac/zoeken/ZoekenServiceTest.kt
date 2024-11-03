/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.zoeken

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import jakarta.enterprise.inject.Instance
import net.atos.zac.app.zoeken.createZoekParameters
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.zoeken.model.ZoekVeld
import net.atos.zac.zoeken.model.index.ZoekObjectType
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject
import org.apache.solr.client.solrj.beans.DocumentObjectBinder
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.params.SolrParams
import org.eclipse.microprofile.config.ConfigProvider
import java.util.EnumMap

class ZoekenServiceTest : BehaviorSpec({
    // add static mocking for config provider because the IndexeerService class
    // references the config provider statically
    val solrUrl = "http://localhost/dummySolrUrl"
    mockkStatic(ConfigProvider::class)
    every {
        ConfigProvider.getConfig().getValue("solr.url", String::class.java)
    } returns solrUrl

    val solrClient = mockk<Http2SolrClient>()
    mockkConstructor(Http2SolrClient.Builder::class)
    every { anyConstructed<Http2SolrClient.Builder>().build() } returns solrClient
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val loggedInUser = mockk<LoggedInUser>()
    val zoekService = ZoekenService(loggedInUserInstance)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A logged-in user authorised for all zaaktypes and wo objects of type ZAAK in the search index") {
        val zoekParameters = createZoekParameters(
            zoekObjectType = ZoekObjectType.ZAAK,
            zoeken = EnumMap<ZoekVeld, String>(ZoekVeld::class.java).apply {
                put(ZoekVeld.ZAAK_OMSCHRIJVING, "dummyOmchrijving")
            }
        )
        val queryResponse = mockk<QueryResponse>()
        val solrDocumentList = mockk<SolrDocumentList>()
        val solrDocument1 = mockk<SolrDocument>()
        val solrDocument2 = mockk<SolrDocument>()
        val documentObjectBinder = mockk<DocumentObjectBinder>()
        val zaakZoekObject1 = mockk<ZaakZoekObject>()
        val zaakZoekObject2 = mockk<ZaakZoekObject>()
        val solrParamsSlot = slot<SolrParams>()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { loggedInUser.isAuthorisedForAllZaaktypen() } returns true
        every { solrClient.query(capture(solrParamsSlot)) } returns queryResponse
        every { queryResponse.results } returns solrDocumentList
        every { solrDocumentList.size } returns 2
        every { solrDocumentList.iterator() } returns listOf<SolrDocument>(
            solrDocument1,
            solrDocument2
        ).iterator() as MutableIterator<SolrDocument>
        every { solrDocument1["type"] } returns "ZAAK"
        every { solrDocument2["type"] } returns "ZAAK"
        every { solrClient.binder } returns documentObjectBinder
        every { documentObjectBinder.getBean(ZaakZoekObject::class.java, solrDocument1) } returns zaakZoekObject1
        every { documentObjectBinder.getBean(ZaakZoekObject::class.java, solrDocument2) } returns zaakZoekObject2
        every { solrDocumentList.numFound } returns 2
        every { queryResponse.facetFields } returns emptyList()

        When("searching for all documents of type ZAAK") {
            val zoekResultaat = zoekService.zoek(zoekParameters)

            Then("it should return a zoekresultaat containing two zaak zoek objecten") {
                with(zoekResultaat) {
                    count shouldBe 2
                    with(items) {
                        size shouldBe 2
                        items[0] shouldBe zaakZoekObject1
                        items[1] shouldBe zaakZoekObject2
                    }
                }
                with(solrParamsSlot.captured) {
                    get("q") shouldBe "*:*"
                    getParams("fq") shouldBe arrayOf(
                        "type:ZAAK",
                        "zaak_omschrijving:(dummyOmchrijving)"
                    )
                    get("facet") shouldBe "true"
                    get("facet.mincount") shouldBe "1"
                    get("facet.missing") shouldBe "true"
                    getParams("facet.field") shouldBe arrayOf(
                        "{!ex=ZAAKTYPE}zaaktypeOmschrijving",
                        "{!ex=BEHANDELAAR}behandelaarNaam",
                        "{!ex=GROEP}groepNaam",
                        "{!ex=ZAAK_STATUS}zaak_statustypeOmschrijving",
                        "{!ex=ZAAK_RESULTAAT}zaak_resultaattypeOmschrijving",
                        "{!ex=ZAAK_INDICATIES}zaak_indicaties",
                        "{!ex=ZAAK_COMMUNICATIEKANAAL}zaak_communicatiekanaal",
                        "{!ex=ZAAK_VERTROUWELIJKHEIDAANDUIDING}zaak_vertrouwelijkheidaanduiding",
                        "{!ex=ZAAK_ARCHIEF_NOMINATIE}zaak_archiefNominatie"
                    )
                    get("q.op") shouldBe "AND"
                    get("rows") shouldBe "0"
                    get("start") shouldBe "0"
                    get("sort") shouldBe "created asc,zaak_identificatie desc,id desc"
                }
            }
        }
    }
})
