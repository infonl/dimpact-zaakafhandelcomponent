/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.zac.admin.ResultaattypeReferenceService
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.app.admin.model.createRestResultaattype
import nl.info.zac.app.admin.model.createRestZaakbeeindigParameter
import nl.info.zac.app.admin.model.createRestZaakbeeindigReden

class RestZaakbeeindigParameterConverterTest : BehaviorSpec({
    val resultaattypeReferenceService = mockk<ResultaattypeReferenceService>()
    val restZaakbeeindigParameterConverter = RestZaakbeeindigParameterConverter(resultaattypeReferenceService)

    afterEach {
        checkUnnecessaryStub()
    }

    fun createCompletionParameters() = ZaaktypeCompletionParameters().apply {
        id = 10L
        zaakbeeindigReden = ZaakbeeindigReden().apply {
            id = 5L
            naam = "fakeZaakbeeindigReden"
        }
        resultaattypeOmschrijving = "fakeResultaattypeOmschrijving"
    }

    context("convertZaakbeeindigParameters") {
        given("a zaakbeeindig parameter whose resultaattype is found in the zaaktype version") {
            val completionParameters = createCompletionParameters()
            val resultaattype = createResultaatType(omschrijving = "fakeResultaattypeOmschrijving")
            every { resultaattypeReferenceService.findResultaattype(completionParameters) } returns resultaattype

            `when`("convertZaakbeeindigParameters is called") {
                val result = restZaakbeeindigParameterConverter.convertZaakbeeindigParameters(setOf(completionParameters))

                then("it returns the parameter with its id, reden, and the found resultaattype") {
                    result.size shouldBe 1
                    with(result.single()) {
                        id shouldBe 10L
                        zaakbeeindigReden.id shouldBe "5"
                        zaakbeeindigReden.naam shouldBe "fakeZaakbeeindigReden"
                        this.resultaattype.naam shouldBe "fakeResultaattypeOmschrijving"
                    }
                }
            }
        }

        given("a zaakbeeindig parameter whose resultaattype is not found in the zaaktype version") {
            val completionParameters = createCompletionParameters()
            every { resultaattypeReferenceService.findResultaattype(completionParameters) } returns null

            `when`("convertZaakbeeindigParameters is called") {
                val result = restZaakbeeindigParameterConverter.convertZaakbeeindigParameters(setOf(completionParameters))

                then("the parameter is left out") {
                    result.shouldBeEmpty()
                }
            }
        }

        given("an empty set of ZaaktypeCompletionParameters") {
            `when`("convertZaakbeeindigParameters is called") {
                val result = restZaakbeeindigParameterConverter.convertZaakbeeindigParameters(emptySet())

                then("it returns an empty list") {
                    result shouldBe emptyList()
                }
            }
        }
    }

    context("toZaaktypeCompletionParameters") {
        given("a REST zaakbeeindig parameter with a resultaattype UUID") {
            val restResultaattype = createRestResultaattype()
            val restZaakbeeindigParameter = createRestZaakbeeindigParameter(
                id = 7L,
                zaakbeeindigReden = createRestZaakbeeindigReden(id = "5", name = "fakeZaakbeeindigReden"),
                resultaattype = restResultaattype
            )
            every { resultaattypeReferenceService.readOmschrijving(restResultaattype.id) } returns "fakeToegekend"

            `when`("it is converted") {
                val result = restZaakbeeindigParameterConverter.toZaaktypeCompletionParameters(
                    listOf(restZaakbeeindigParameter)
                )

                then("the parameter references the resultaattype by the omschrijving that ZTC has for the UUID") {
                    with(result.single()) {
                        id shouldBe 7L
                        zaakbeeindigReden.id shouldBe 5L
                        zaakbeeindigReden.naam shouldBe "fakeZaakbeeindigReden"
                        resultaattypeOmschrijving shouldBe "fakeToegekend"
                    }
                }
            }
        }
    }
})
