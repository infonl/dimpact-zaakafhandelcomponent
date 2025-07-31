/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.FUNCTIONELE_GEBRUIKER_ID
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_1_BRON_KENMERK
import nl.info.zac.itest.config.ItestConfiguration.OPEN_FORMULIEREN_FORMULIER_BRON_NAAM
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_2_BSN
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_3_BSN
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_2_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_TOELICHTING
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields

@Order(TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED)
class ZaakRestServiceHistoryTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("A zaak exists for which there is an audit trail in OpenZaak") {
        When("zaak history is requested") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakProductaanvraag1Uuid/historie"
            )

            Then("the response should be ok") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true

                val expectedResponse = """[   
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
                    "nieuweWaarde": "fakeTitel",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "fakeTitel",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "fakeTitel",
                    "toelichting": ""
                  },
                  {
                    "actie": "ONTKOPPELD",
                    "attribuutLabel": "Behandelaar",
                    "door": "$FUNCTIONELE_GEBRUIKER_ID",
                    "oudeWaarde": "$TEST_USER_1_NAME",
                    "toelichting": "fakeLijstVrijgevenReason"
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Behandelaar",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "$TEST_USER_1_NAME",
                    "toelichting": "fakeAssignToMeFromListReason"
                  },
                  {
                    "actie": "ONTKOPPELD",
                    "attribuutLabel": "Behandelaar",
                    "door": "$TEST_USER_1_NAME",
                    "oudeWaarde": "$TEST_USER_2_NAME",
                    "toelichting": "fakeAssignToMeFromListReason"
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Behandelaar",
                    "door": "$FUNCTIONELE_GEBRUIKER_ID",
                    "nieuweWaarde": "$TEST_USER_2_NAME",
                    "toelichting": "fakeLijstVerdelenReason"
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
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "$FUNCTIONELE_GEBRUIKER_ID",
                    "nieuweWaarde": "Ontvangstbevestiging van zaak $ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Bewindvoerder",
                    "door": "$FUNCTIONELE_GEBRUIKER_ID",
                    "nieuweWaarde": "$TEST_PERSON_2_BSN",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Bewindvoerder",
                    "door": "$FUNCTIONELE_GEBRUIKER_ID",
                    "nieuweWaarde": "$TEST_PERSON_3_BSN",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Medeaanvrager",
                    "door": "$FUNCTIONELE_GEBRUIKER_ID",
                    "nieuweWaarde": "$TEST_PERSON_2_BSN",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Melder",
                    "door": "$FUNCTIONELE_GEBRUIKER_ID",
                    "nieuweWaarde": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                    "toelichting": ""
                  },                
                 {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "zaakinformatieobject",
                    "door": "$FUNCTIONELE_GEBRUIKER_ID",
                    "nieuweWaarde": "Aanvraag PDF",
                    "toelichting": "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag"
                  },
                  {
                    "actie": "GEKOPPELD",
                    "attribuutLabel": "Behandelaar",
                    "door": "$FUNCTIONELE_GEBRUIKER_ID",
                    "nieuweWaarde": "$TEST_GROUP_A_DESCRIPTION",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEWIJZIGD",
                    "attribuutLabel": "status",
                    "door": "$FUNCTIONELE_GEBRUIKER_ID",
                    "nieuweWaarde": "Intake",
                    "toelichting": "Status gewijzigd"
                  },
                  {
                    "actie": "AANGEMAAKT",
                    "attribuutLabel": "zaak",
                    "door": "$FUNCTIONELE_GEBRUIKER_ID",
                    "nieuweWaarde": "$ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION",
                    "toelichting": "Aangemaakt vanuit $OPEN_FORMULIEREN_FORMULIER_BRON_NAAM met kenmerk '$OBJECT_PRODUCTAANVRAAG_1_BRON_KENMERK'. $ZAAK_PRODUCTAANVRAAG_1_TOELICHTING"
                  }]
                """.trimIndent()

                responseBody shouldEqualJsonIgnoringExtraneousFields expectedResponse
                responseBody shouldContainJsonKey("$[0].datumTijd")
            }
        }
    }
})
