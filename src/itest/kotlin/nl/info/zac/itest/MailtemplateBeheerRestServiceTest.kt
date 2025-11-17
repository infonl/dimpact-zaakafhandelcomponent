/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_2023_09_21
import nl.info.zac.itest.config.ItestConfiguration.DATE_2023_10_01
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
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_ONTVANKELIJK_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_ONTVANKELIJK_NAME
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_1
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_2
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_INITIALIZATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import java.net.HttpURLConnection.HTTP_OK

/**
 * This test assumes zaakafhandelparameters are created successfully.
 */
@Order(TEST_SPEC_ORDER_AFTER_INITIALIZATION)
class MailtemplateBeheerRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("A zaakafhandelparameters are configured") {
        When("mailtempalte list if fetched") {
            val response = itestHttpClient.performGetRequest(url = "$ZAC_API_URI/beheer/mailtemplates")
            lateinit var responseBody: String

            Then("the response should be a 200 HTTP response") {
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

        When("mailtemplatekoppeling list if fetched") {
            val response = itestHttpClient.performGetRequest(url = "$ZAC_API_URI/beheer/mailtemplatekoppeling")
            lateinit var responseBody: String

            Then("the response should be a 200 HTTP response") {
                response.code shouldBe HTTP_OK
                responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
            }

            And("all mailtemplatekoppeling are returned") {
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                  [
                    {
                      "mailtemplate": {
                        "defaultMailtemplate": true,
                        "mail": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL",
                        "mailTemplateNaam": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME"
                      },
                      "zaakafhandelParameters": {
                        "afrondenMail": "BESCHIKBAAR_UIT",
                        "automaticEmailConfirmation": {
                          "emailReply": "reply@info.nl",
                          "emailSender": "GEMEENTE",
                          "enabled": true,
                          "templateName": "$MAIL_TEMPLATE_TAAK_ONTVANGSTBEVESTIGING_NAME"
                        },
                        "betrokkeneKoppelingen": {
                          "brpKoppelen": true,
                          "kvkKoppelen": true
                        },
                        "brpDoelbindingen": {
                          "raadpleegWaarde": "BRPACT-Totaal",
                          "zoekWaarde": "BRPACT-ZoekenAlgemeen"
                        },
                        "caseDefinition": {
                          "key": "generiek-zaakafhandelmodel",
                          "naam": "Generiek zaakafhandelmodel"
                        },
                        "defaultGroepId": "${BEHANDELAARS_DOMAIN_TEST_1.name}",
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
                          "doel": "$ZAAKTYPE_TEST_3_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_TEST_3_IDENTIFICATIE",
                          "nuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_TEST_3_DESCRIPTION",
                          "servicenorm": false,
                          "uuid": "$ZAAKTYPE_TEST_3_UUID",
                          "versiedatum": "$DATE_2023_09_21",
                          "vertrouwelijkheidaanduiding": "openbaar"
                        }
                      }
                    },
                    {
                      "mailtemplate": {
                        "defaultMailtemplate": true,
                        "mail": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL",
                        "mailTemplateNaam": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME"
                      },
                      "zaakafhandelParameters": {
                        "afrondenMail": "BESCHIKBAAR_UIT",
                        "automaticEmailConfirmation": {
                          "emailReply": "reply@info.nl",
                          "emailSender": "sender@info.nl",
                          "enabled": true,
                          "templateName": "$MAIL_TEMPLATE_TAAK_ONTVANGSTBEVESTIGING_NAME"
                        },
                        "betrokkeneKoppelingen": {
                          "brpKoppelen": true,
                          "kvkKoppelen": true
                        },
                        "brpDoelbindingen": {
                          "raadpleegWaarde": "BRPACT-Totaal",
                          "zoekWaarde": "BRPACT-ZoekenAlgemeen"
                        },
                        "caseDefinition": {
                          "key": "generiek-zaakafhandelmodel",
                          "naam": "Generiek zaakafhandelmodel"
                        },
                        "defaultGroepId": "${BEHANDELAARS_DOMAIN_TEST_1.name}",
                        "domein": "domein_test_1",
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
                          "doel": "$ZAAKTYPE_TEST_2_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_TEST_2_IDENTIFICATIE",
                          "nuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_TEST_2_DESCRIPTION",
                          "servicenorm": false,
                          "uuid": "$ZAAKTYPE_TEST_2_UUID",
                          "versiedatum": "$DATE_2023_10_01",
                          "vertrouwelijkheidaanduiding": "openbaar"
                        }
                      }
                    }
                  ]
                """.trimIndent()
            }
        }
    }
})
