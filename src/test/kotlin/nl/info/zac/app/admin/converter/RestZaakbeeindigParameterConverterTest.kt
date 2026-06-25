/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import java.util.UUID

class RestZaakbeeindigParameterConverterTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val restZaakbeeindigParameterConverter = RestZaakbeeindigParameterConverter(ztcClientService)

    afterEach {
        checkUnnecessaryStub()
    }

    Context("convertZaakbeeindigParameters") {
        Given("a set of ZaaktypeCompletionParameters") {
            val resultaattypeUUID = UUID.randomUUID()
            val zaakbeeindigReden = ZaakbeeindigReden().apply {
                id = 5L
                naam = "fakeZaakbeeindigReden"
            }
            val completionParams = ZaaktypeCompletionParameters().apply {
                id = 10L
                this.zaakbeeindigReden = zaakbeeindigReden
                resultaattype = resultaattypeUUID
            }
            val resultaattype = createResultaatType(omschrijving = "fakeResultaattypeOmschrijving")
            every { ztcClientService.readResultaattype(resultaattypeUUID) } returns resultaattype

            When("convertZaakbeeindigParameters is called") {
                val result = restZaakbeeindigParameterConverter.convertZaakbeeindigParameters(setOf(completionParams))

                Then("it returns one RestZaakbeeindigParameter") {
                    result.size shouldBe 1
                }

                Then("the zaakbeeindigReden is converted correctly") {
                    result[0].zaakbeeindigReden.id shouldBe "5"
                    result[0].zaakbeeindigReden.naam shouldBe "fakeZaakbeeindigReden"
                }

                Then("the resultaattype is fetched and converted") {
                    result[0].resultaattype.naam shouldBe "fakeResultaattypeOmschrijving"
                }

                Then("the id is carried over") {
                    result[0].id shouldBe 10L
                }
            }
        }

        Given("an empty set of ZaaktypeCompletionParameters") {
            When("convertZaakbeeindigParameters is called") {
                val result = restZaakbeeindigParameterConverter.convertZaakbeeindigParameters(emptySet())

                Then("it returns an empty list") {
                    result shouldBe emptyList()
                }
            }
        }
    }
})
