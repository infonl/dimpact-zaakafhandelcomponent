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
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_FILE_TITLE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_2_BSN
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_3_BSN
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_LAST
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_2_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_TOELICHTING
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import nl.lifely.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields

@Order(TEST_SPEC_ORDER_LAST)
class ZaakRestServiceHistoryTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("A zaak exists for which there is an audit trail in OpenZaak") {
        When("zaak history is requested") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakProductaanvraag1Uuid/historie"
            )

            Then("the response should be ok") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true

                val expectedResponse = """[{
                    "actie": "GEWIJZIGD",
                    "attribuutLabel": "status",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "Intake",
                    "toelichting": "Status gewijzigd"
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Behandelaar",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "$TEST_USER_1_NAME",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "subject",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "$SMART_DOCUMENTS_FILE_TITLE",
                    "toelichting": ""
                  },                
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "$SMART_DOCUMENTS_FILE_TITLE",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "dummyTitel",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "dummyTitel",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "dummyTitel",
                    "toelichting": ""
                  },
                  {
                    "actie": "ONTKOPPELD",
                    "attribuutLabel": "Behandelaar",
                    "door": "Functionele gebruiker",
                    "oudeWaarde": "$TEST_USER_1_NAME",
                    "toelichting": "dummyLijstVrijgevenReason"
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Behandelaar",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "$TEST_USER_1_NAME",
                    "toelichting": "dummyAssignToMeFromListReason"
                  },
                  {
                    "actie": "ONTKOPPELD",
                    "attribuutLabel": "Behandelaar",
                    "door": "$TEST_USER_1_NAME",
                    "oudeWaarde": "$TEST_USER_2_NAME",
                    "toelichting": "dummyAssignToMeFromListReason"
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Behandelaar",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "$TEST_USER_2_NAME",
                    "toelichting": "dummyLijstVerdelenReason"
                  },
                  {
                    "actie": "GEWIJZIGD",
                    "attribuutLabel": "uiterlijkeEinddatumAfdoening",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "17-01-1970",
                    "oudeWaarde": "15-01-1970",
                    "toelichting": "Aanvullende informatie opgevraagd"
                  },
                  {
                    "actie": "GEWIJZIGD",
                    "attribuutLabel": "status",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "Wacht op aanvullende informatie",
                    "toelichting": "Status gewijzigd"
                  },
                  {
                    "actie": "GEWIJZIGD",
                    "attribuutLabel": "status",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "Intake",
                    "toelichting": "Status gewijzigd"
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Bewindvoerder",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "$TEST_PERSON_2_BSN",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Bewindvoerder",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "$TEST_PERSON_3_BSN",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Medeaanvrager",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "$TEST_PERSON_2_BSN",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Melder",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "Aanvraag PDF",
                    "toelichting": "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag"
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Behandelaar",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "$TEST_GROUP_A_DESCRIPTION",
                    "toelichting": ""
                  },
                  {
                    "actie": "AANGEMAAKT",
                    "attribuutLabel": "zaak",
                    "door": "Functionele gebruiker",
                    "nieuweWaarde": "$ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION",
                    "toelichting": "$ZAAK_PRODUCTAANVRAAG_1_TOELICHTING"
                  }]
                """.trimIndent()

                responseBody shouldEqualJsonIgnoringExtraneousFields expectedResponse
                responseBody shouldContainJsonKey("$[0].datumTijd")
            }
        }
    }
})
