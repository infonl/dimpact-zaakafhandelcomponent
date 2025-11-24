/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DOMEIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_2
import nl.info.zac.itest.config.ItestConfiguration.RESULTAAT_TYPE_GEWEIGERD_UUID
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_REFERENCE_TABLES_UPDATED
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_BEEINDIG_VERZOEK_IS_BIJ_VERKEERDE_ORGANISATIE_INGEDIEND_ID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_BEEINDIG_VERZOEK_IS_BIJ_VERKEERDE_ORGANISATIE_INGEDIEND_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_BEEINDIG_VERZOEK_IS_DOOR_INITIATOR_INGETROKKEN_ID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_BEEINDIG_VERZOEK_IS_DOOR_INITIATOR_INGETROKKEN_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_BEEINDIG_ZAAK_IS_EEN_DUPLICAAT_ID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_BEEINDIG_ZAAK_IS_EEN_DUPLICAAT_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrder
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import java.net.HttpURLConnection.HTTP_OK

@Order(TEST_SPEC_ORDER_AFTER_REFERENCE_TABLES_UPDATED)
class ZaaktypeCmmnConfigurationRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given(
        """
        Zaaktype CMMN configuration have been created for the CMMN test zaaktypes,
        and a test domein exists in the domein reference table 
        """.trimIndent()
    ) {
        When("the list zaakafhandelparameters endpoint is called for the '$ZAAKTYPE_TEST_3_DESCRIPTION' zaaktype") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaakafhandelparameters/$ZAAKTYPE_TEST_3_UUID"
            )
            Then("the response should be ok and it should return the zaakafhandelparameters") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(responseBody) {
                    shouldContainJsonKeyValue(
                        "zaaktype.identificatie",
                        ZAAKTYPE_TEST_3_IDENTIFICATIE
                    )
                }
            }
        }
        When("the list zaakafhandelparameters endpoint is called for the '$ZAAKTYPE_TEST_2_DESCRIPTION' zaaktype") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaakafhandelparameters/$ZAAKTYPE_TEST_2_UUID"
            )
            Then(
                "the response should be ok and it should return the zaakafhandelparameters with the configured domein"
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "afrondenMail" : "BESCHIKBAAR_UIT",
                      "caseDefinition" : {
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
                          "defaultFormulierDefinitie" : "EXTERN_ADVIES_VASTLEGGEN",
                          "id" : "ADVIES_EXTERN",
                          "naam" : "Advies extern",
                          "type" : "HUMAN_TASK"
                        }, {
                          "defaultFormulierDefinitie" : "DOCUMENT_VERZENDEN_POST",
                          "id" : "DOCUMENT_VERZENDEN_POST",
                          "naam" : "Document verzenden",
                          "type" : "HUMAN_TASK"
                        }, {
                          "defaultFormulierDefinitie" : "ADVIES",
                          "id" : "ADVIES_INTERN",
                          "naam" : "Advies intern",
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
                      },
                      "defaultGroepId" : "${BEHANDELAARS_DOMAIN_TEST_1.name}",
                      "domein" : "$DOMEIN_TEST_1",
                      "humanTaskParameters" : [ {
                        "actief" : true,
                        "formulierDefinitieId" : "AANVULLENDE_INFORMATIE",
                        "planItemDefinition" : {
                          "defaultFormulierDefinitie" : "AANVULLENDE_INFORMATIE",
                          "id" : "AANVULLENDE_INFORMATIE",
                          "naam" : "Aanvullende informatie",
                          "type" : "HUMAN_TASK"
                        },
                        "referentieTabellen" : [ ]
                      }, {
                        "actief" : true,
                        "formulierDefinitieId" : "GOEDKEUREN",
                        "planItemDefinition" : {
                          "defaultFormulierDefinitie" : "GOEDKEUREN",
                          "id" : "GOEDKEUREN",
                          "naam" : "Goedkeuren",
                          "type" : "HUMAN_TASK"
                        },
                        "referentieTabellen" : [ ]
                      }, {
                        "actief" : true,
                        "formulierDefinitieId" : "EXTERN_ADVIES_VASTLEGGEN",
                        "planItemDefinition" : {
                          "defaultFormulierDefinitie" : "EXTERN_ADVIES_VASTLEGGEN",
                          "id" : "ADVIES_EXTERN",
                          "naam" : "Advies extern",
                          "type" : "HUMAN_TASK"
                        },
                        "referentieTabellen" : [ ]
                      }, {
                        "actief" : true,
                        "formulierDefinitieId" : "DOCUMENT_VERZENDEN_POST",
                        "planItemDefinition" : {
                          "defaultFormulierDefinitie" : "DOCUMENT_VERZENDEN_POST",
                          "id" : "DOCUMENT_VERZENDEN_POST",
                          "naam" : "Document verzenden",
                          "type" : "HUMAN_TASK"
                        },
                        "referentieTabellen" : [ ]
                      }, {
                        "actief" : true,
                        "formulierDefinitieId" : "ADVIES",
                        "planItemDefinition" : {
                          "defaultFormulierDefinitie" : "ADVIES",
                          "id" : "ADVIES_INTERN",
                          "naam" : "Advies intern",
                          "type" : "HUMAN_TASK"
                        },
                        "referentieTabellen" : [ {
                          "tabel" : {
                            "aantalWaarden" : 5,
                            "code" : "ADVIES",
                            "id" : 1,
                            "naam" : "Advies",
                            "systeem" : true,
                            "waarden" : [ ]
                          },
                          "veld" : "ADVIES"
                        } ]
                      } ],
                      "intakeMail" : "BESCHIKBAAR_UIT",
                      "mailtemplateKoppelingen" : [
                        {
                          "mailtemplate": {
                            "body": "<p>Beste {ZAAK_INITIATOR},</p><p></p><p>Uw verzoek over {ZAAK_TYPE} met zaaknummer {ZAAK_NUMMER} wordt niet in behandeling genomen. Voor meer informatie gaat u naar Mijn Loket.</p><p></p><p>Met vriendelijke groet,</p><p></p><p>Gemeente Dommeldam</p>",
                            "defaultMailtemplate": true,
                            "id": 2,
                            "mail": "ZAAK_NIET_ONTVANKELIJK",
                            "mailTemplateNaam": "Zaak niet ontvankelijk",
                            "onderwerp": "<p>Wij hebben uw verzoek niet in behandeling genomen (zaaknummer: {ZAAK_NUMMER})</p>",
                            "variabelen": [
                              "GEMEENTE",
                              "ZAAK_NUMMER",
                              "ZAAK_TYPE",
                              "ZAAK_STATUS",
                              "ZAAK_REGISTRATIEDATUM",
                              "ZAAK_STARTDATUM",
                              "ZAAK_STREEFDATUM",
                              "ZAAK_FATALEDATUM",
                              "ZAAK_OMSCHRIJVING",
                              "ZAAK_TOELICHTING",
                              "ZAAK_INITIATOR",
                              "ZAAK_INITIATOR_ADRES"
                            ]
                          }
                        }
                      ],
                      "productaanvraagtype" : "$PRODUCTAANVRAAG_TYPE_2",
                      "smartDocuments" : {
                        "enabledForZaaktype" : true,
                        "enabledGlobally" : true
                      },
                      "userEventListenerParameters" : [ {
                        "id" : "INTAKE_AFRONDEN",
                        "naam" : "Intake afronden"
                      }, {
                        "id" : "ZAAK_AFHANDELEN",
                        "naam" : "Zaak afhandelen"
                      } ],
                      "valide" : true,
                      "zaakAfzenders" : [ {
                        "defaultMail" : false,
                        "mail" : "GEMEENTE",
                        "speciaal" : true
                      }, {
                        "defaultMail" : false,
                        "mail" : "MEDEWERKER",
                        "speciaal" : true
                      } ],
                      "zaakNietOntvankelijkResultaattype" : {
                        "archiefNominatie" : "VERNIETIGEN",
                        "archiefTermijn" : "5 jaren",
                        "besluitVerplicht" : false,
                        "id" : "$RESULTAAT_TYPE_GEWEIGERD_UUID",
                        "naam" : "Geweigerd",
                        "naamGeneriek" : "Geweigerd",
                        "toelichting" : "Het door het orgaan behandelen van een aanvraag, melding of verzoek om toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen",
                        "vervaldatumBesluitVerplicht" : false
                      },
                      "zaakbeeindigParameters" : [ ],
                      "zaaktype" : {
                        "beginGeldigheid" : "2023-10-01",
                        "doel" : "$ZAAKTYPE_TEST_2_DESCRIPTION",
                        "identificatie" : "$ZAAKTYPE_TEST_2_IDENTIFICATIE",
                        "nuGeldig" : true,
                        "omschrijving" : "$ZAAKTYPE_TEST_2_DESCRIPTION",
                        "servicenorm" : false,
                        "uuid" : "$ZAAKTYPE_TEST_2_UUID",
                        "versiedatum" : "2023-10-01",
                        "vertrouwelijkheidaanduiding" : "openbaar"
                      }
                    }
                """.trimIndent()
            }
        }
        When("the list case definitions endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaakafhandelparameters/case-definitions"
            )
            Then("the response should be ok and it should return all available case definitions") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
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
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
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
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
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
