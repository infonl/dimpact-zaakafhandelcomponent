/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.search

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldInclude
import io.kotest.matchers.string.shouldNotInclude
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import jakarta.enterprise.inject.Instance
import nl.info.zac.app.search.createZoekParameters
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.search.model.DatumRange
import nl.info.zac.search.model.DatumVeld
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.SorteerVeld
import nl.info.zac.search.model.ZoekVeld
import nl.info.zac.search.model.zoekobject.DocumentZoekObject
import nl.info.zac.search.model.zoekobject.TaakZoekObject
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.shared.model.SorteerRichting
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
import kotlin.collections.get

class SearchServiceTest : BehaviorSpec({
    // add static mocking for config provider because the IndexeerService class
    // references the config provider statically
    val solrUrl = "http://localhost/fakeSolrUrl"
    mockkStatic(ConfigProvider::class)
    every {
        ConfigProvider.getConfig().getValue("solr.url", String::class.java)
    } returns solrUrl

    val solrClient = mockk<Http2SolrClient>()
    mockkConstructor(Http2SolrClient.Builder::class)
    every { anyConstructed<Http2SolrClient.Builder>().build() } returns solrClient
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val loggedInUser = mockk<LoggedInUser>()
    val zoekService = SearchService(loggedInUserInstance)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A logged-in user authorised for all zaaktypes and two objects of type ZAAK in the search index") {
        val zaakDescriptionSearchField = "fakeZaakDescription"
        val behandelaarFilterValue1 = "fakeBehandelaarFilterValue1"
        val behandelaarFilterValue2 = "fakeBehandelaarFilterValue2"
        val zaakType1 = "fakeZaaktype1"
        val zaakType2 = "fakeZaaktype2"
        val queryResponse = mockk<QueryResponse>()
        val solrDocumentList = mockk<SolrDocumentList>()
        val solrDocument1 = mockk<SolrDocument>()
        val solrDocument2 = mockk<SolrDocument>()
        val documentObjectBinder = mockk<DocumentObjectBinder>()
        val zaakZoekObject1 = mockk<ZaakZoekObject>()
        val zaakZoekObject2 = mockk<ZaakZoekObject>()
        val solrParamsSlot = slot<SolrParams>()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { loggedInUser.pabcIntegrationEnabled } returns false
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
            val zaakSearchStartDate = LocalDate.of(2000, 1, 1)
            val zaakSearchEndDate = LocalDate.of(2000, 2, 1)
            val zaakSearchDateRange = DatumRange(zaakSearchStartDate, zaakSearchEndDate)
            val zoekResultaat = zoekService.zoek(
                createZoekParameters(
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
            )

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
    Given("A logged-in user authorised for a zaaktype and one object of type TAAK in the search index") {
        val zaakType1 = "fakeZaaktype1"
        val queryResponse = mockk<QueryResponse>()
        val solrDocumentList = mockk<SolrDocumentList>()
        val solrDocument1 = mockk<SolrDocument>()
        val documentObjectBinder = mockk<DocumentObjectBinder>()
        val taakZoekObject1 = mockk<TaakZoekObject>()
        val solrParamsSlot = slot<SolrParams>()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { loggedInUser.pabcIntegrationEnabled } returns false
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
            val zaakSearchStartDate = LocalDate.of(2000, 1, 1)
            val zaakSearchEndDate = LocalDate.of(2000, 2, 1)
            val zaakSearchDateRange = DatumRange(zaakSearchStartDate, zaakSearchEndDate)
            val zoekResultaat = zoekService.zoek(
                createZoekParameters(
                    zoekObjectType = ZoekObjectType.TAAK,
                    datums = EnumMap<DatumVeld, DatumRange>(DatumVeld::class.java).apply {
                        put(DatumVeld.STARTDATUM, zaakSearchDateRange)
                    }
                ).apply {
                    addDatum(DatumVeld.STARTDATUM, zaakSearchDateRange)
                    addFilter(FilterVeld.ZAAKTYPE, FilterParameters(listOf(zaakType1), false))
                }
            )

            Then("it should return a zoekresultaat containing a taak zoek object") {
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
    Given("A logged-in user authorised for a zaaktype and one object of type DOCUMENT in the search index") {
        val zaakType1 = "fakeZaaktype1"
        val queryResponse = mockk<QueryResponse>()
        val solrDocumentList = mockk<SolrDocumentList>()
        val solrDocument1 = mockk<SolrDocument>()
        val documentObjectBinder = mockk<DocumentObjectBinder>()
        val documentZoekObject1 = mockk<DocumentZoekObject>()
        val solrParamsSlot = slot<SolrParams>()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { loggedInUser.pabcIntegrationEnabled } returns false
        every { loggedInUser.isAuthorisedForAllZaaktypen() } returns false
        every { loggedInUser.geautoriseerdeZaaktypen } returns setOf(zaakType1)
        every { solrClient.query(capture(solrParamsSlot)) } returns queryResponse
        every { queryResponse.results } returns solrDocumentList
        every { solrDocumentList.size } returns 1
        every {
            solrDocumentList.iterator()
        } returns listOf<SolrDocument>(solrDocument1,).iterator() as MutableIterator<SolrDocument>
        every { solrDocument1["type"] } returns "DOCUMENT"
        every { solrClient.binder } returns documentObjectBinder
        every { documentObjectBinder.getBean(DocumentZoekObject::class.java, solrDocument1) } returns documentZoekObject1
        every { solrDocumentList.numFound } returns 1
        every { queryResponse.facetFields } returns emptyList()

        When("searching for all documents of type DOCUMENT with a sort field and sort direction") {
            val zoekResultaat = zoekService.zoek(
                createZoekParameters(
                    zoekObjectType = ZoekObjectType.DOCUMENT,
                    sorteerVeld = SorteerVeld.INFORMATIEOBJECT_TITEL,
                    sorteerRichting = SorteerRichting.DESCENDING
                )
            )

            Then("it should return a zoekresultaat containing a document zoek object with the correct sorting") {
                with(zoekResultaat) {
                    count shouldBe 1
                    with(items) {
                        size shouldBe 1
                        items[0] shouldBe documentZoekObject1
                    }
                }
                with(solrParamsSlot.captured) {
                    get("q") shouldBe "*:*"
                    getParams("fq") shouldBe arrayOf(
                        """zaaktypeOmschrijving:"$zaakType1"""",
                        "type:DOCUMENT"
                    )
                    get("facet") shouldBe "true"
                    get("facet.mincount") shouldBe "1"
                    get("facet.missing") shouldBe "true"
                    getParams("facet.field") shouldBe arrayOf(
                        "{!ex=ZAAKTYPE}zaaktypeOmschrijving",
                        "{!ex=DOCUMENT_STATUS}informatieobject_status",
                        "{!ex=DOCUMENT_TYPE}informatieobject_documentType",
                        "{!ex=DOCUMENT_VERGRENDELD_DOOR}informatieobject_vergrendeldDoorNaam",
                        "{!ex=DOCUMENT_INDICATIES}informatieobject_indicaties"
                    )
                    get("q.op") shouldBe "AND"
                    get("rows") shouldBe "0"
                    get("start") shouldBe "0"
                    get("sort") shouldBe "informatieobject_titel_sort desc,created desc,zaak_identificatie desc,id desc"
                }
            }
        }
    }
    Given("A PABC-enabled user with per-zaaktype roles and one object of type TAAK in the search index") {
        val zaakType1 = "fakeZaaktype1"
        val queryResponse = mockk<QueryResponse>()
        val solrDocumentList = mockk<SolrDocumentList>()
        val solrDocument1 = mockk<SolrDocument>()
        val documentObjectBinder = mockk<DocumentObjectBinder>()
        val taakZoekObject1 = mockk<TaakZoekObject>()
        val solrParamsSlot = slot<SolrParams>()

        every { loggedInUserInstance.get() } returns loggedInUser
        every { loggedInUser.pabcIntegrationEnabled } returns true
        every { loggedInUser.applicationRolesPerZaaktype } returns mapOf(
            zaakType1 to setOf("fakeApplicationRole1", "fakeApplicationRole2")
        )

        every { solrClient.query(capture(solrParamsSlot)) } returns queryResponse
        every { queryResponse.results } returns solrDocumentList
        every { solrDocumentList.size } returns 1
        every { solrDocumentList.iterator() } returns
            listOf(solrDocument1).iterator() as MutableIterator<SolrDocument>
        every { solrDocument1["type"] } returns "TAAK"
        every { solrClient.binder } returns documentObjectBinder
        every { documentObjectBinder.getBean(TaakZoekObject::class.java, solrDocument1) } returns taakZoekObject1
        every { solrDocumentList.numFound } returns 1
        every { queryResponse.facetFields } returns emptyList()

        When("searching for all documents of type TAAK with a specific filter") {
            val zaakSearchStartDate = LocalDate.of(2000, 1, 1)
            val zaakSearchEndDate = LocalDate.of(2000, 2, 1)
            val zaakSearchDateRange = DatumRange(zaakSearchStartDate, zaakSearchEndDate)
            val zoekResultaat = zoekService.zoek(
                createZoekParameters(
                    zoekObjectType = ZoekObjectType.TAAK,
                    datums = EnumMap<DatumVeld, DatumRange>(DatumVeld::class.java).apply {
                        put(DatumVeld.STARTDATUM, zaakSearchDateRange)
                    }
                ).apply {
                    addDatum(DatumVeld.STARTDATUM, zaakSearchDateRange)
                    addFilter(FilterVeld.ZAAKTYPE, FilterParameters(listOf(zaakType1), false))
                }
            )

            Then("it should return a zoekresultaat containing a taak zoek object and filter using PABC zaaktypen") {
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
    Given("A users sorts") {
        val queryResponse = mockk<QueryResponse>()
        val solrDocumentList = mockk<SolrDocumentList>()
        val solrDocument1 = mockk<SolrDocument>()
        val solrDocument2 = mockk<SolrDocument>()
        val documentObjectBinder = mockk<DocumentObjectBinder>()
        val zaakZoekObject1 = mockk<ZaakZoekObject>()
        val zaakZoekObject2 = mockk<ZaakZoekObject>()
        val solrParamsSlot = slot<SolrParams>()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { loggedInUser.pabcIntegrationEnabled } returns false
        every { loggedInUser.isAuthorisedForAllZaaktypen() } returns true
        every { solrClient.query(capture(solrParamsSlot)) } returns queryResponse
        every { queryResponse.results } returns solrDocumentList
        every { solrDocumentList.size } returns 2
        every { solrDocumentList.iterator() } returns listOf(
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

        When("searching ${SorteerRichting.ASCENDING.value}") {
            zoekService.zoek(
                createZoekParameters(
                    zoekObjectType = ZoekObjectType.ZAAK,
                    sorteerRichting = SorteerRichting.ASCENDING,
                    sorteerVeld = SorteerVeld.ZAAK_STATUS
                )
            )

            Then("it should add the correct sort to the query") {
                with(solrParamsSlot.captured) {
                    get("sort") shouldInclude "zaak_statustypeOmschrijving asc"
                }
            }
        }

        When("searching ${SorteerRichting.DESCENDING.value}") {
            zoekService.zoek(
                createZoekParameters(
                    zoekObjectType = ZoekObjectType.ZAAK,
                    sorteerRichting = SorteerRichting.DESCENDING,
                    sorteerVeld = SorteerVeld.ZAAK_STATUS
                )
            )

            Then("it should add the correct sort to the query") {
                with(solrParamsSlot.captured) {
                    get("sort") shouldInclude "zaak_statustypeOmschrijving desc"
                }
            }
        }
        When("searching ${SorteerRichting.NONE.value}") {
            zoekService.zoek(
                createZoekParameters(
                    zoekObjectType = ZoekObjectType.ZAAK,
                    sorteerRichting = SorteerRichting.NONE,
                    sorteerVeld = SorteerVeld.ZAAK_STATUS
                )
            )

            Then("it should not add a sort to the query") {
                with(solrParamsSlot.captured) {
                    get("sort") shouldNotInclude "zaak_statustypeOmschrijving"
                }
            }
        }
    }
})
