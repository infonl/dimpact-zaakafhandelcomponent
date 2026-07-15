/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_2023_09_21
import nl.info.zac.itest.config.ItestConfiguration.DATE_2023_10_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_2025_07_01
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_SIGNALERING_TAAK_OP_NAAM_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_SIGNALERING_TAAK_OP_NAAM_NAME
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_SIGNALERING_TAAK_VERLOPEN_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_SIGNALERING_TAAK_VERLOPEN_NAME
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_SIGNALERING_ZAAK_DOCUMENT_TOEGEVOEGD_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_SIGNALERING_ZAAK_DOCUMENT_TOEGEVOEGD_NAME
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_SIGNALERING_ZAAK_OP_NAAM_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_SIGNALERING_ZAAK_OP_NAAM_NAME
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM_NAME
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM_NAME
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_TAAK_AANVULLENDE_INFORMATIE_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_TAAK_AANVULLENDE_INFORMATIE_NAME
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_TAAK_ADVIES_EXTERN_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_TAAK_ADVIES_EXTERN_NAME
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_TAAK_ONTVANGSTBEVESTIGING_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_TAAK_ONTVANGSTBEVESTIGING_NAME
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_AFGEHANDELD_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_AFGEHANDELD_NAME
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_ALGEMEEN_NAME
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_BODY
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_SUBJECT
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_ONTVANKELIJK_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_ONTVANKELIJK_NAME
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_1
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_2
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_3
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK

class MailtemplateBeheerRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    context("Listing mail templates") {
        given("The default mail templates have been provisioned on ZAC startup") {
            `when`("mail template list is fetched as a beheerder") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/beheer/mailtemplates",
                    testUser = BEHEERDER_1
                )
                lateinit var responseBody: String

                then("the response should be a 200 HTTP response") {
                    response.code shouldBe HTTP_OK
                    responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                }

                And("all templates are returned") {
                    responseBody shouldEqualJsonIgnoringExtraneousFields """
                  [
                    {
                      "defaultMailtemplate": true,
                      "mail": "$MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL",
                      "mailTemplateNaam": "$MAIL_TEMPLATE_ZAAK_ALGEMEEN_NAME"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "$MAIL_TEMPLATE_TAAK_ONTVANGSTBEVESTIGING_MAIL",
                      "mailTemplateNaam": "$MAIL_TEMPLATE_TAAK_ONTVANGSTBEVESTIGING_NAME"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "$MAIL_TEMPLATE_SIGNALERING_TAAK_OP_NAAM_MAIL",
                      "mailTemplateNaam": "$MAIL_TEMPLATE_SIGNALERING_TAAK_OP_NAAM_NAME"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "$MAIL_TEMPLATE_SIGNALERING_TAAK_VERLOPEN_MAIL",
                      "mailTemplateNaam": "$MAIL_TEMPLATE_SIGNALERING_TAAK_VERLOPEN_NAME"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "$MAIL_TEMPLATE_SIGNALERING_ZAAK_DOCUMENT_TOEGEVOEGD_MAIL",
                      "mailTemplateNaam": "$MAIL_TEMPLATE_SIGNALERING_ZAAK_DOCUMENT_TOEGEVOEGD_NAME"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "$MAIL_TEMPLATE_SIGNALERING_ZAAK_OP_NAAM_MAIL",
                      "mailTemplateNaam": "$MAIL_TEMPLATE_SIGNALERING_ZAAK_OP_NAAM_NAME"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "$MAIL_TEMPLATE_SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM_MAIL",
                      "mailTemplateNaam": "$MAIL_TEMPLATE_SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM_NAME"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "$MAIL_TEMPLATE_SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM_MAIL",
                      "mailTemplateNaam": "$MAIL_TEMPLATE_SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM_NAME"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "$MAIL_TEMPLATE_TAAK_AANVULLENDE_INFORMATIE_MAIL",
                      "mailTemplateNaam": "$MAIL_TEMPLATE_TAAK_AANVULLENDE_INFORMATIE_NAME"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "$MAIL_TEMPLATE_TAAK_ADVIES_EXTERN_MAIL",
                      "mailTemplateNaam": "$MAIL_TEMPLATE_TAAK_ADVIES_EXTERN_NAME"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "$MAIL_TEMPLATE_ZAAK_AFGEHANDELD_MAIL",
                      "mailTemplateNaam": "$MAIL_TEMPLATE_ZAAK_AFGEHANDELD_NAME"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL",
                      "mailTemplateNaam": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "$MAIL_TEMPLATE_ZAAK_ONTVANKELIJK_MAIL",
                      "mailTemplateNaam": "$MAIL_TEMPLATE_ZAAK_ONTVANKELIJK_NAME"
                    }
                  ]
                    """.trimIndent()
                }
            }
        }
    }

    context("Listing mail template koppelingen") {
        given(
            "The test CMMN and BPMN zaaktype configurations have been created as part of the integration test setup"
        ) {
            `when`("the mail template koppelingen list is fetched as a beheerder") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/beheer/mailtemplatekoppeling",
                    testUser = BEHEERDER_1
                )
                lateinit var responseBody: String

                then("the response should be a 200 HTTP response") {
                    response.code shouldBe HTTP_OK
                    responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                }

                And("all mail template koppelingen are returned") {
                    responseBody shouldEqualJsonIgnoringExtraneousFields """
                  [
                    {
                      "mailtemplate": {
                        "body": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_BODY",
                        "defaultMailtemplate": true,
                        "mail": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL",
                        "mailTemplateNaam": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME",
                        "onderwerp": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_SUBJECT",
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
                      },
                      "zaakafhandelParameters": {
                        "afrondenMail": "BESCHIKBAAR_UIT",
                        "automaticEmailConfirmation": {
                          "emailReply": "reply@example.com",
                          "emailSender": "GEMEENTE",
                          "enabled": true,
                          "templateName": "$MAIL_TEMPLATE_TAAK_ONTVANGSTBEVESTIGING_NAME"
                        },
                        "betrokkeneKoppelingen": {
                          "brpKoppelen": true,
                          "kvkKoppelen": true
                        },
                        "brpDoelbindingen": {
                          "raadpleegWaarde": "BRPACT-AlgemeneTaken",
                          "zoekWaarde": "BRPACT-ZoekenAlgemeen"
                        },
                        "caseDefinition": {
                          "key": "generiek-zaakafhandelmodel",
                          "naam": "Generiek zaakafhandelmodel"
                        },
                        "defaultGroepId": "${GROUP_BEHANDELAARS_TEST_1.name}",
                        "humanTaskParameters": [],
                        "intakeMail": "BESCHIKBAAR_UIT",
                        "mailtemplateKoppelingen": [],
                        "productaanvraagtype": "$PRODUCTAANVRAAG_TYPE_3",
                        "smartDocuments": {
                          "enabledForZaaktype": true,
                          "enabledGlobally": true
                        },
                        "userEventListenerParameters": [],
                        "valide": true,
                        "zaakAfzenders": [],
                        "zaakbeeindigParameters": [],
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2025_07_01",
                          "doel": "$ZAAKTYPE_CMMN_TEST_1_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_CMMN_TEST_1_IDENTIFICATIE",
                          "nuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_CMMN_TEST_1_DESCRIPTION",
                          "servicenorm": false,
                          "uuid": "$ZAAKTYPE_CMMN_TEST_1_UUID",
                          "versiedatum": "$DATE_2025_07_01",
                          "vertrouwelijkheidaanduiding": "openbaar"
                        }
                      }
                    },
                    {
                      "mailtemplate": {
                        "body": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_BODY",
                        "defaultMailtemplate": true,
                        "mail": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL",
                        "mailTemplateNaam": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME",
                        "onderwerp": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_SUBJECT",
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
                      },
                      "zaakafhandelParameters": {
                        "afrondenMail": "BESCHIKBAAR_UIT",
                        "automaticEmailConfirmation": {
                          "emailReply": "reply@example.com",
                          "emailSender": "GEMEENTE",
                          "enabled": true,
                          "templateName": "$MAIL_TEMPLATE_TAAK_ONTVANGSTBEVESTIGING_NAME"
                        },
                        "betrokkeneKoppelingen": {
                          "brpKoppelen": true,
                          "kvkKoppelen": true
                        },
                        "brpDoelbindingen": {
                          "raadpleegWaarde": "BRPACT-AlgemeneTaken",
                          "zoekWaarde": "BRPACT-ZoekenAlgemeen"
                        },
                        "caseDefinition": {
                          "key": "generiek-zaakafhandelmodel",
                          "naam": "Generiek zaakafhandelmodel"
                        },
                        "defaultGroepId": "${GROUP_BEHANDELAARS_TEST_1.name}",
                        "humanTaskParameters": [],
                        "intakeMail": "BESCHIKBAAR_UIT",
                        "mailtemplateKoppelingen": [],
                        "productaanvraagtype": "$PRODUCTAANVRAAG_TYPE_2",
                        "smartDocuments": {
                          "enabledForZaaktype": true,
                          "enabledGlobally": true
                        },
                        "userEventListenerParameters": [],
                        "valide": true,
                        "zaakAfzenders": [],
                        "zaakbeeindigParameters": [],
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2023_10_01",
                          "doel": "$ZAAKTYPE_CMMN_TEST_2_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_CMMN_TEST_2_IDENTIFICATIE",
                          "nuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_CMMN_TEST_2_DESCRIPTION",
                          "servicenorm": false,
                          "uuid": "$ZAAKTYPE_CMMN_TEST_2_UUID",
                          "versiedatum": "$DATE_2023_10_01",
                          "vertrouwelijkheidaanduiding": "openbaar"
                        }
                      }
                    },
                    {
                      "mailtemplate": {
                        "body": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_BODY",
                        "defaultMailtemplate": true,
                        "mail": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL",
                        "mailTemplateNaam": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME",
                        "onderwerp": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_SUBJECT",
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
                      },
                      "zaakafhandelParameters": {
                        "afrondenMail": "BESCHIKBAAR_UIT",
                        "automaticEmailConfirmation": {
                          "emailReply": "reply@example.com",
                          "emailSender": "GEMEENTE",
                          "enabled": true,                           
                          "templateName": "Ontvangstbevestiging"
                        },
                        "betrokkeneKoppelingen": {
                          "brpKoppelen": true,
                          "kvkKoppelen": true
                        },
                        "brpDoelbindingen": {
                          "raadpleegWaarde": "BRPACT-AlgemeneTaken",
                          "zoekWaarde": "BRPACT-ZoekenAlgemeen"
                        },
                        "caseDefinition": {
                          "key": "generiek-zaakafhandelmodel",
                          "naam": "Generiek zaakafhandelmodel"
                        },
                        "defaultGroepId": "${GROUP_BEHANDELAARS_TEST_1.name}",
                        "humanTaskParameters": [],
                        "intakeMail": "BESCHIKBAAR_UIT",
                        "mailtemplateKoppelingen": [],
                        "productaanvraagtype": "$PRODUCTAANVRAAG_TYPE_1",
                        "smartDocuments": {
                          "enabledForZaaktype": true,
                          "enabledGlobally": true
                        },
                        "userEventListenerParameters": [],
                        "valide": true,
                        "zaakAfzenders": [],
                        "zaakbeeindigParameters": [],
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2023_09_21",
                          "doel": "$ZAAKTYPE_CMMN_TEST_3_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_CMMN_TEST_3_IDENTIFICATIE",
                          "nuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_CMMN_TEST_3_DESCRIPTION",
                          "servicenorm": false,
                          "uuid": "$ZAAKTYPE_CMMN_TEST_3_UUID",
                          "versiedatum": "$DATE_2023_09_21",
                          "vertrouwelijkheidaanduiding": "openbaar"
                        }
                      }
                    }
                  ]
                    """.trimIndent()
                }
            }
        }
    }

    context("Creating, updating and deleting a mail template") {
        given("A beheerder wants to create a new mail template") {
            `when`("the full create-read-update-delete lifecycle is executed") {
                then("all operations succeed in order") {
                    val createResponse = itestHttpClient.performJSONPostRequest(
                        url = "$ZAC_API_URI/beheer/mailtemplates",
                        requestBodyAsString = """
                            {
                              "mailTemplateNaam": "itest-mailtemplate",
                              "onderwerp": "Test onderwerp",
                              "body": "<p>Test body</p>",
                              "mail": "$MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL",
                              "defaultMailtemplate": false
                            }
                        """.trimIndent(),
                        testUser = BEHEERDER_1
                    )
                    createResponse.code shouldBe HTTP_CREATED
                    logger.info { "Create response: ${createResponse.bodyAsString}" }
                    val createdTemplateId = JSONObject(createResponse.bodyAsString).run {
                        getString("mailTemplateNaam") shouldBe "itest-mailtemplate"
                        getString("onderwerp") shouldBe "Test onderwerp"
                        getString("body") shouldBe "<p>Test body</p>"
                        getString("mail") shouldBe MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL
                        getBoolean("defaultMailtemplate") shouldBe false
                        getLong("id").toString()
                    }

                    itestHttpClient.performGetRequest(
                        url = "$ZAC_API_URI/beheer/mailtemplates/$createdTemplateId",
                        testUser = BEHEERDER_1
                    ).also {
                        it.code shouldBe HTTP_OK
                        it.bodyAsString shouldEqualJsonIgnoringExtraneousFields """
                            {
                              "mailTemplateNaam": "itest-mailtemplate",
                              "onderwerp": "Test onderwerp",
                              "body": "<p>Test body</p>",
                              "mail": "$MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL",
                              "defaultMailtemplate": false
                            }
                        """.trimIndent()
                    }

                    itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/beheer/mailtemplates/$createdTemplateId",
                        requestBodyAsString = """
                            {
                              "mailTemplateNaam": "itest-mailtemplate-updated",
                              "onderwerp": "Updated onderwerp",
                              "body": "<p>Updated body</p>",
                              "mail": "$MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL",
                              "defaultMailtemplate": false
                            }
                        """.trimIndent(),
                        testUser = BEHEERDER_1
                    ).also {
                        it.code shouldBe HTTP_OK
                        it.bodyAsString shouldEqualJsonIgnoringExtraneousFields """
                            {
                              "mailTemplateNaam": "itest-mailtemplate-updated",
                              "onderwerp": "Updated onderwerp",
                              "body": "<p>Updated body</p>",
                              "mail": "$MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL",
                              "defaultMailtemplate": false
                            }
                        """.trimIndent()
                    }

                    itestHttpClient.performDeleteRequest(
                        url = "$ZAC_API_URI/beheer/mailtemplates/$createdTemplateId",
                        testUser = BEHEERDER_1
                    ).code shouldBe HTTP_NO_CONTENT

                    itestHttpClient.performGetRequest(
                        url = "$ZAC_API_URI/beheer/mailtemplates/$createdTemplateId",
                        testUser = BEHEERDER_1
                    ).code shouldBe HTTP_NOT_FOUND
                }
            }
        }
    }
})
