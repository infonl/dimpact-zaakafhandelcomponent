/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_COMPLETED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.zaak1UUID
import nl.lifely.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields

@Order(TEST_SPEC_ORDER_AFTER_TASK_COMPLETED)
class HistorieTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("ZAC Docker container is running") {
        When("historie is requested") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaak1UUID/historie"
            )

            Then("the response should be ok") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true

                val expectedResponse = """[{
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "Test User1 Špëçîâl Characters",
                    "nieuweWaarde": "dummyTitel",
                    "toelichting": ""
                  },
                  {
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "Test User1 Špëçîâl Characters",
                    "nieuweWaarde": "dummyTitel",
                    "toelichting": ""
                  },
                  {
                    "attribuutLabel": "Behandelaar",
                    "door": "Functionele gebruiker",
                    "oudeWaarde": "Test User1 Špëçîâl Characters",
                    "toelichting": "Behandelaar: dummyLijstVrijgevenReason"
                  },
                  {
                    "attribuutLabel": "Behandelaar",
                    "door": "Test User1 Špëçîâl Characters",
                    "nieuweWaarde": "Test User1 Špëçîâl Characters",
                    "toelichting": "Behandelaar: dummyAssignToMeFromListReason"
                  },
                  {
                    "attribuutLabel": "Behandelaar",
                    "door": "Test User1 Špëçîâl Characters",
                    "oudeWaarde": "Test User2",
                    "toelichting": "Behandelaar: dummyAssignToMeFromListReason"
                  },
                  {
                    "attribuutLabel": "Behandelaar",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "Test User2",
                    "toelichting": "Behandelaar: dummyLijstVerdelenReason"
                  },
                  {
                    "attribuutLabel": "status",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "Intake",
                    "toelichting": "Status gewijzigd vanuit Case"
                  },
                  {
                    "attribuutLabel": "Melder",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "999993896",
                    "toelichting": "Melder: null"
                  },
                  {
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "Aanvraag PDF",
                    "toelichting": "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag"
                  },
                  {
                    "attribuutLabel": "overige",
                    "door": "Functionele gebruiker",
                    "toelichting": ""
                  },
                  {
                    "attribuutLabel": "Behandelaar",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "Test group A",
                    "toelichting": "Behandelaar: null"
                  },
                  {
                    "attribuutLabel": "zaak",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "ZAAK-2023-0000000001",
                    "toelichting": ""
                  }]"""

                responseBody shouldEqualJsonIgnoringExtraneousFields expectedResponse
                responseBody shouldContainJsonKey("$[0].datumTijd")
            }
        }
    }
})
