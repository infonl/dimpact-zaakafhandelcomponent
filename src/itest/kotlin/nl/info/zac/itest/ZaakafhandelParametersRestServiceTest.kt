/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_BEEINDIG_VERZOEK_IS_BIJ_VERKEERDE_ORGANISATIE_INGEDIEND_ID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_BEEINDIG_VERZOEK_IS_BIJ_VERKEERDE_ORGANISATIE_INGEDIEND_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_BEEINDIG_VERZOEK_IS_DOOR_INITIATOR_INGETROKKEN_ID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_BEEINDIG_VERZOEK_IS_DOOR_INITIATOR_INGETROKKEN_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_BEEINDIG_ZAAK_IS_EEN_DUPLICAAT_ID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_BEEINDIG_ZAAK_IS_EEN_DUPLICAAT_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrder

class ZaakafhandelParametersRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the list zaakafhandelparameters endpoint is called for our zaaktype under test") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaakafhandelparameters/$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID"
            )
            Then("the response should be ok and it should return the zaakafhandelparameters") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(responseBody) {
                    shouldContainJsonKeyValue(
                        "zaaktype.identificatie",
                        ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
                    )
                }
            }
        }
        When("the list case definitions endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaakafhandelparameters/case-definitions"
            )
            Then("the response should be ok and it should return all available case definitions") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody shouldEqualJsonIgnoringOrder """
                    [ {
                      "humanTaskDefinitions" : [ {
                        "defaultFormulierDefinitie" : "AANVULLENDE_INFORMATIE",
                        "id" : "AANVULLENDE_INFORMATIE",
                        "naam" : "Aanvullende informatie",
                        "type" : "HUMAN_TASK"
                      }, {
                        "defaultFormulierDefinitie" : "GOEDKEUREN",
                        "id" : "GOEDKEUREN",
                        "naam" : "Goedkeuren",
                        "type" : "HUMAN_TASK"
                      }, {
                        "defaultFormulierDefinitie" : "ADVIES",
                        "id" : "ADVIES_INTERN",
                        "naam" : "Advies intern",
                        "type" : "HUMAN_TASK"
                      }, {
                        "defaultFormulierDefinitie" : "EXTERN_ADVIES_VASTLEGGEN",
                        "id" : "ADVIES_EXTERN",
                        "naam" : "Advies extern",
                        "type" : "HUMAN_TASK"
                      }, {
                        "defaultFormulierDefinitie" : "DOCUMENT_VERZENDEN_POST",
                        "id" : "DOCUMENT_VERZENDEN_POST",
                        "naam" : "Document verzenden",
                        "type" : "HUMAN_TASK"
                      } ],
                      "key" : "generiek-zaakafhandelmodel",
                      "naam" : "Generiek zaakafhandelmodel",
                      "userEventListenerDefinitions" : [ {
                        "defaultFormulierDefinitie" : "DEFAULT_TAAKFORMULIER",
                        "id" : "INTAKE_AFRONDEN",
                        "naam" : "Intake afronden",
                        "type" : "USER_EVENT_LISTENER"
                      }, {
                        "defaultFormulierDefinitie" : "DEFAULT_TAAKFORMULIER",
                        "id" : "ZAAK_AFHANDELEN",
                        "naam" : "Zaak afhandelen",
                        "type" : "USER_EVENT_LISTENER"
                      } ]
                    } ]
                """.trimIndent()
            }
        }
        When("the list zaakbeeindigredenen endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaakafhandelparameters/zaakbeeindigredenen"
            )
            Then("the response should be ok and it should return all available zaakbeeindigredenen") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody shouldEqualJsonIgnoringOrder """
                    [ 
                        {
                          "id" : "$ZAAK_BEEINDIG_VERZOEK_IS_DOOR_INITIATOR_INGETROKKEN_ID",
                          "naam" : "$ZAAK_BEEINDIG_VERZOEK_IS_DOOR_INITIATOR_INGETROKKEN_NAME"
                        },
                        {
                          "id" : "$ZAAK_BEEINDIG_ZAAK_IS_EEN_DUPLICAAT_ID",
                          "naam" : "$ZAAK_BEEINDIG_ZAAK_IS_EEN_DUPLICAAT_NAME"
                        }, 
                        {
                          "id" : "$ZAAK_BEEINDIG_VERZOEK_IS_BIJ_VERKEERDE_ORGANISATIE_INGEDIEND_ID",
                          "naam" : "$ZAAK_BEEINDIG_VERZOEK_IS_BIJ_VERKEERDE_ORGANISATIE_INGEDIEND_NAME"
                        }
                    ]
                """.trimIndent()
            }
        }
        When("the list formulier definities endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaakafhandelparameters/formulierdefinities"
            )
            Then("the response should be ok and it should return all available formulierdefinities") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody shouldEqualJson """
                    [ {
                      "id" : "DEFAULT_TAAKFORMULIER",
                      "veldDefinities" : [ ]
                    }, {
                      "id" : "AANVULLENDE_INFORMATIE",
                      "veldDefinities" : [ ]
                    }, {
                      "id" : "ADVIES",
                      "veldDefinities" : [ {
                        "naam" : "ADVIES",
                        "waarde" : "ADVIES"
                      } ]
                    }, {
                      "id" : "EXTERN_ADVIES_VASTLEGGEN",
                      "veldDefinities" : [ ]
                    }, {
                      "id" : "EXTERN_ADVIES_MAIL",
                      "veldDefinities" : [ ]
                    }, {
                      "id" : "GOEDKEUREN",
                      "veldDefinities" : [ ]
                    }, {
                      "id" : "DOCUMENT_VERZENDEN_POST",
                      "veldDefinities" : [ ]
                    } ]
                """.trimIndent()
            }
        }
    }
})
