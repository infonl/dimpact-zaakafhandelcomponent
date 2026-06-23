/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.zac.app.search.model.RestTaakZoekObject
import nl.info.zac.app.search.model.RestZaakKoppelenZoekObject
import nl.info.zac.app.search.model.RestZaakZoekObject
import nl.info.zac.app.search.model.createRestZoekParameters
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createDocumentRechten
import nl.info.zac.policy.output.createTaakRechten
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterResultaat
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.createTaakZoekObject
import nl.info.zac.search.model.createZaakZoekObject
import nl.info.zac.search.model.zoekobject.DocumentZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType

class RestZoekResultaatConverterTest : BehaviorSpec({
    val policyService = mockk<PolicyService>()
    val restZoekResultaatConverter = RestZoekResultaatConverter(policyService = policyService)

    afterEach { checkUnnecessaryStub() }

    Context("convert with ZoekResultaat and RestZoekParameters") {
        Given("a ZoekResultaat containing a TaakZoekObject") {
            val taakZoekObject = createTaakZoekObject()
            val zoekResultaat = ZoekResultaat(listOf(taakZoekObject), 1L)
            val zoekParameters = createRestZoekParameters(type = ZoekObjectType.TAAK, filters = emptyMap())
            val taakRechten = createTaakRechten()

            every { policyService.readTaakRechten(taakZoekObject) } returns taakRechten

            When("convert is called") {
                val result = restZoekResultaatConverter.convert(zoekResultaat, zoekParameters)

                Then("it returns a RestZoekResultaat with the correct count") {
                    result.resultCount shouldBe 1L
                    result.results shouldHaveSize 1
                    (result.results.first() is RestTaakZoekObject) shouldBe true
                }
            }
        }

        Given("a ZoekResultaat containing a ZaakZoekObject") {
            val zaakZoekObject = createZaakZoekObject()
            val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1L)
            val zoekParameters = createRestZoekParameters(type = ZoekObjectType.ZAAK, filters = emptyMap())
            val zaakRechten = createZaakRechten()

            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns zaakRechten

            When("convert is called") {
                val result = restZoekResultaatConverter.convert(zoekResultaat, zoekParameters)

                Then("it returns a RestZoekResultaat with the correct count") {
                    result.resultCount shouldBe 1L
                    result.results shouldHaveSize 1
                    (result.results.first() is RestZaakZoekObject) shouldBe true
                }
            }
        }

        Given("a ZoekResultaat containing a DocumentZoekObject") {
            val documentZoekObject = DocumentZoekObject(
                id = "fakeDocumentId",
                type = ZoekObjectType.DOCUMENT.name
            )
            val zoekResultaat = ZoekResultaat(listOf(documentZoekObject), 1L)
            val zoekParameters = createRestZoekParameters(type = ZoekObjectType.DOCUMENT, filters = emptyMap())
            val documentRechten = createDocumentRechten()

            every { policyService.readDocumentRechten(documentZoekObject) } returns documentRechten

            When("convert is called") {
                val result = restZoekResultaatConverter.convert(zoekResultaat, zoekParameters)

                Then("it returns a RestZoekResultaat with the correct count") {
                    result.resultCount shouldBe 1L
                    result.results shouldHaveSize 1
                }
            }
        }

        Given("a ZoekResultaat with existing filters") {
            val zaakZoekObject = createZaakZoekObject()
            val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1L).also {
                it.addFilter(FilterVeld.ZAAKTYPE, mutableListOf(FilterResultaat("fakeZaaktype", 5)))
            }
            val zoekParameters = createRestZoekParameters(type = ZoekObjectType.ZAAK, filters = emptyMap())
            val zaakRechten = createZaakRechten()

            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns zaakRechten

            When("convert is called") {
                val result = restZoekResultaatConverter.convert(zoekResultaat, zoekParameters)

                Then("filters from zoekResultaat are carried over") {
                    val zaaktypeFilters = result.filters[FilterVeld.ZAAKTYPE]!!
                    zaaktypeFilters shouldHaveSize 1
                    zaaktypeFilters.first().naam shouldBe "fakeZaaktype"
                    zaaktypeFilters.first().aantal shouldBe 5
                }
            }
        }

        Given("zoekParameters with filter values not in zoekResultaat") {
            val zaakZoekObject = createZaakZoekObject()
            val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1L)
            val zoekParameters = createRestZoekParameters(
                type = ZoekObjectType.ZAAK,
                filters = mapOf(FilterVeld.BEHANDELAAR to FilterParameters(listOf("fakeMissingBehandelaar"), false))
            )
            val zaakRechten = createZaakRechten()

            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns zaakRechten

            When("convert is called") {
                val result = restZoekResultaatConverter.convert(zoekResultaat, zoekParameters)

                Then("missing filter values are added with count 0") {
                    val behandelaarFilters = result.filters[FilterVeld.BEHANDELAAR]!!
                    behandelaarFilters shouldHaveSize 1
                    behandelaarFilters.first().naam shouldBe "fakeMissingBehandelaar"
                    behandelaarFilters.first().aantal shouldBe 0
                }
            }
        }
    }

    Context("convert for koppelen") {
        Given("a ZoekResultaat containing ZaakZoekObjects for koppelen") {
            val zaakZoekObject = createZaakZoekObject()
            val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1L)
            val documentLinkableList = listOf(true)
            val zaakRechten = createZaakRechten()

            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns zaakRechten

            When("convert is called with documentLinkableList") {
                val result = restZoekResultaatConverter.convert(zoekResultaat, documentLinkableList)

                Then("it returns RestZaakKoppelenZoekObjects") {
                    result.resultCount shouldBe 1L
                    result.results shouldHaveSize 1
                    (result.results.first() is RestZaakKoppelenZoekObject) shouldBe true
                }
            }
        }
    }
})
