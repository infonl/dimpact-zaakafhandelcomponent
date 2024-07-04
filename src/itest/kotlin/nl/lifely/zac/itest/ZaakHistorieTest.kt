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
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_COMPLETED
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_2_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_1_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.zaak1UUID
import nl.lifely.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields

@Order(TEST_SPEC_ORDER_AFTER_TASK_COMPLETED)
class ZaakHistorieTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("A zaak exists for which there is an audit trail in OpenZaak") {
        When("zaakhistorie is requested") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaak1UUID/historie"
            )

            Then("the response should be ok") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true

                val expectedResponse = """[{
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "dummyTitel",
                    "toelichting": ""
                  },
                  {
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "dummyTitel",
                    "toelichting": ""
                  },
                  {
                    "attribuutLabel": "Behandelaar",
                    "door": "Functionele gebruiker",
                    "oudeWaarde": "$TEST_USER_1_NAME",
                    "toelichting": "Behandelaar: dummyLijstVrijgevenReason"
                  },
                  {
                    "attribuutLabel": "Behandelaar",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "$TEST_USER_1_NAME",
                    "toelichting": "Behandelaar: dummyAssignToMeFromListReason"
                  },
                  {
                    "attribuutLabel": "Behandelaar",
                    "door": "$TEST_USER_1_NAME",
                    "oudeWaarde": "$TEST_USER_2_NAME",
                    "toelichting": "Behandelaar: dummyAssignToMeFromListReason"
                  },
                  {
                    "attribuutLabel": "Behandelaar",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "$TEST_USER_2_NAME",
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
                    "nieuweWaarde": "$TEST_GROUP_A_DESCRIPTION"",
                    "toelichting": "Behandelaar: null"
                  },
                  {
                    "attribuutLabel": "zaak",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "$ZAAK_1_IDENTIFICATION",
                    "toelichting": ""
                  }]"""

                responseBody shouldEqualJsonIgnoringExtraneousFields expectedResponse
                responseBody shouldContainJsonKey("$[0].datumTijd")
            }
        }
    }
})
