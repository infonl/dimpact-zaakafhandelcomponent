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
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.DOMEIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_1
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_2
import nl.info.zac.itest.config.ItestConfiguration.RESULTAAT_TYPE_GEWEIGERD_UUID
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_REFERENCE_TABLES_UPDATED
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BEHANDELEN_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
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
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields

@Order(TEST_SPEC_ORDER_AFTER_REFERENCE_TABLES_UPDATED)
class ZaakafhandelParametersRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    Given(
        """
        ZAC Docker container is running and no zaakafhandelparameters have been created
        and a test domein exists in the domein reference table 
        """.trimIndent()
    ) {
        When(
            """
            the create zaakafhandelparameters endpoint is called to create a new zaakafhandelparameters
            for the 'melding klein evenement' zaaktype without specifying a 'domein'
            """.trimIndent()
        ) {
            val response = zacClient.createZaakAfhandelParameters(
                zaakTypeIdentificatie = ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE,
                zaakTypeUuid = ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID,
                zaakTypeDescription = ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION,
                productaanvraagType = PRODUCTAANVRAAG_TYPE_1
            )
            Then("the response should be ok") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
            }
        }
        When(
            """
            the create zaakafhandelparameters endpoint is called to create a new zaakafhandelparameters
            for the 'indienen aansprakelijkheidstelling' zaaktype with specifying a the existing test domein
            """.trimIndent()
        ) {
            val response = zacClient.createZaakAfhandelParameters(
                zaakTypeIdentificatie = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BEHANDELEN_IDENTIFICATIE,
                zaakTypeUuid = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID,
                zaakTypeDescription = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION,
                productaanvraagType = PRODUCTAANVRAAG_TYPE_2,
                domein = DOMEIN_TEST_1
            )
            Then("the response should be ok") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
            }
        }
        When("the list zaakafhandelparameters endpoint is called for the 'melding klein evenement' zaaktype") {
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
        When(
            """
                the list zaakafhandelparameters endpoint is called for the 
                'indienen aansprakelijkheidstelling door derden' zaaktype"
            """.trimIndent()
        ) {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaakafhandelparameters/$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID"
            )
            Then(
                "the response should be ok and it should return the zaakafhandelparameters with the configured domein"
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
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
                      "defaultGroepId" : "$TEST_GROUP_A_ID",
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
                      "mailtemplateKoppelingen" : [ ],
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
                        "doel" : "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                        "identificatie" : "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BEHANDELEN_IDENTIFICATIE",
                        "nuGeldig" : true,
                        "omschrijving" : "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                        "servicenorm" : false,
                        "uuid" : "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID",
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
