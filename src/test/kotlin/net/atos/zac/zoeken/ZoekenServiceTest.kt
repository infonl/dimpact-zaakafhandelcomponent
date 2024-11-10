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
import net.atos.zac.zoeken.model.DatumRange
import net.atos.zac.zoeken.model.DatumVeld
import net.atos.zac.zoeken.model.FilterParameters
import net.atos.zac.zoeken.model.FilterVeld
import net.atos.zac.zoeken.model.ZoekVeld
import net.atos.zac.zoeken.model.index.ZoekObjectType
import net.atos.zac.zoeken.model.zoekobject.TaakZoekObject
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject
import org.apache.solr.client.solrj.beans.DocumentObjectBinder
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.params.SolrParams
import org.eclipse.microprofile.config.ConfigProvider
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
        val zaakDescriptionSearchField = "dummyZaakDescription"
        val behandelaarFilterValue1 = "dummyBehandelaarFilterValue1"
        val behandelaarFilterValue2 = "dummyBehandelaarFilterValue2"
        val zaakType1 = "dummyZaaktype1"
        val zaakType2 = "dummyZaaktype2"
        val zaakSearchStartDate = LocalDate.of(2000, 1, 1)
        val zaakSearchEndDate = LocalDate.of(2000, 2, 1)
        val zaakSearchDateRange = DatumRange(zaakSearchStartDate, zaakSearchEndDate)
        val zoekParameters = createZoekParameters(
            zoekObjectType = ZoekObjectType.ZAAK,
            datums = EnumMap<DatumVeld, DatumRange>(DatumVeld::class.java).apply {
                put(DatumVeld.STARTDATUM, zaakSearchDateRange)
            }
        ).apply {
            addFilter(
                FilterVeld.BEHANDELAAR,
                FilterParameters(listOf(behandelaarFilterValue1, behandelaarFilterValue2), false)
            )
            addFilter(FilterVeld.ZAAKTYPE, FilterParameters(listOf(zaakType1, zaakType2), false))
            addZoekVeld(ZoekVeld.ZAAK_OMSCHRIJVING, zaakDescriptionSearchField)
        }
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

        When("searching for all documents of type ZAAK for a specific behandelaar and zaaktypen") {
            val zoekResultaat = zoekService.zoek(zoekParameters)

            Then("it should return a zoekresultaat containing the two zaak zoek objecten") {
                with(zoekResultaat) {
                    count shouldBe 2
                    with(items) {
                        size shouldBe 2
                        items[0] shouldBe zaakZoekObject1
                        items[1] shouldBe zaakZoekObject2
                    }
                }
                val zaakSearchStartDateString = DateTimeFormatter.ISO_INSTANT.format(
                    zaakSearchStartDate.atStartOfDay(ZoneId.systemDefault())
                )
                val zaakSearchEndDateString = DateTimeFormatter.ISO_INSTANT.format(
                    zaakSearchEndDate.atStartOfDay(ZoneId.systemDefault())
                )
                with(solrParamsSlot.captured) {
                    get("q") shouldBe "*:*"
                    getParams("fq") shouldBe arrayOf(
                        "type:ZAAK",
                        "zaak_omschrijving:($zaakDescriptionSearchField)",
                        "startdatum:[$zaakSearchStartDateString TO $zaakSearchEndDateString]",
                        """{!tag=ZAAKTYPE}zaaktypeOmschrijving:("$zaakType1" OR "$zaakType2")""",
                        """{!tag=BEHANDELAAR}behandelaarNaam:("$behandelaarFilterValue1" OR "$behandelaarFilterValue2")"""
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
    Given("A logged-in user authorised for a zaaktype and one objects of type TAAK in the search index") {
        val zaakType1 = "dummyZaaktype1"
        val zaakSearchStartDate = LocalDate.of(2000, 1, 1)
        val zaakSearchEndDate = LocalDate.of(2000, 2, 1)
        val zaakSearchDateRange = DatumRange(zaakSearchStartDate, zaakSearchEndDate)
        val zoekParameters = createZoekParameters(
            zoekObjectType = ZoekObjectType.TAAK,
            datums = EnumMap<DatumVeld, DatumRange>(DatumVeld::class.java).apply {
                put(DatumVeld.STARTDATUM, zaakSearchDateRange)
            }
        ).apply {
            addDatum(DatumVeld.STARTDATUM, zaakSearchDateRange)
            addFilter(FilterVeld.ZAAKTYPE, FilterParameters(listOf(zaakType1), false))
        }
        val queryResponse = mockk<QueryResponse>()
        val solrDocumentList = mockk<SolrDocumentList>()
        val solrDocument1 = mockk<SolrDocument>()
        val documentObjectBinder = mockk<DocumentObjectBinder>()
        val taakZoekObject1 = mockk<TaakZoekObject>()
        val solrParamsSlot = slot<SolrParams>()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { loggedInUser.isAuthorisedForAllZaaktypen() } returns false
        every { loggedInUser.geautoriseerdeZaaktypen } returns setOf(zaakType1)
        every { solrClient.query(capture(solrParamsSlot)) } returns queryResponse
        every { queryResponse.results } returns solrDocumentList
        every { solrDocumentList.size } returns 1
        every {
            solrDocumentList.iterator()
        } returns listOf<SolrDocument>(solrDocument1,).iterator() as MutableIterator<SolrDocument>
        every { solrDocument1["type"] } returns "TAAK"
        every { solrClient.binder } returns documentObjectBinder
        every { documentObjectBinder.getBean(TaakZoekObject::class.java, solrDocument1) } returns taakZoekObject1
        every { solrDocumentList.numFound } returns 1
        every { queryResponse.facetFields } returns emptyList()

        When("searching for all documents of type TAAK with a specific filter") {
            val zoekResultaat = zoekService.zoek(zoekParameters)

            Then("it should return a zoekresultaat containing two zaak zoek objecten") {
                with(zoekResultaat) {
                    count shouldBe 1
                    with(items) {
                        size shouldBe 1
                        items[0] shouldBe taakZoekObject1
                    }
                }
                val zaakSearchStartDateString = DateTimeFormatter.ISO_INSTANT.format(
                    zaakSearchStartDate.atStartOfDay(ZoneId.systemDefault())
                )
                val zaakSearchEndDateString = DateTimeFormatter.ISO_INSTANT.format(
                    zaakSearchEndDate.atStartOfDay(ZoneId.systemDefault())
                )
                with(solrParamsSlot.captured) {
                    get("q") shouldBe "*:*"
                    getParams("fq") shouldBe arrayOf(
                        """zaaktypeOmschrijving:"$zaakType1"""",
                        "type:TAAK",
                        "startdatum:[$zaakSearchStartDateString TO $zaakSearchEndDateString]",
                        """{!tag=ZAAKTYPE}zaaktypeOmschrijving:("$zaakType1")"""
                    )
                    get("facet") shouldBe "true"
                    get("facet.mincount") shouldBe "1"
                    get("facet.missing") shouldBe "true"
                    getParams("facet.field") shouldBe arrayOf(
                        "{!ex=ZAAKTYPE}zaaktypeOmschrijving",
                        "{!ex=BEHANDELAAR}behandelaarNaam",
                        "{!ex=GROEP}groepNaam",
                        "{!ex=TAAK_NAAM}taak_naam",
                        "{!ex=TAAK_STATUS}taak_status"
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
