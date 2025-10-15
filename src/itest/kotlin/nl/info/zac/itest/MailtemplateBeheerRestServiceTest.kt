package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_ALGEMEEN_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_INITIALIZATION
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
                responseBody = response.body.string()
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
                      "mail": "TAAK_ONTVANGSTBEVESTIGING",
                      "mailTemplateNaam": "Ontvangstbevestiging"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "SIGNALERING_TAAK_OP_NAAM",
                      "mailTemplateNaam": "Signalering taak op naam"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "SIGNALERING_TAAK_VERLOPEN",
                      "mailTemplateNaam": "Signalering taak verlopen"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "SIGNALERING_ZAAK_DOCUMENT_TOEGEVOEGD",
                      "mailTemplateNaam": "Signalering zaak document toegevoegd"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "SIGNALERING_ZAAK_OP_NAAM",
                      "mailTemplateNaam": "Signalering zaak op naam"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM",
                      "mailTemplateNaam": "Signalering zaak verlopend fatale datum"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM",
                      "mailTemplateNaam": "Signalering zaak verlopend streefdatum"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "TAAK_AANVULLENDE_INFORMATIE",
                      "mailTemplateNaam": "Taak formulierdefinitie: Aanvullende informatie"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "TAAK_ADVIES_EXTERN",
                      "mailTemplateNaam": "Taak formulierdefinitie: Extern advies (met e-mail)"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "ZAAK_AFGEHANDELD",
                      "mailTemplateNaam": "Zaak afgehandeld"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "ZAAK_NIET_ONTVANKELIJK",
                      "mailTemplateNaam": "Zaak niet ontvankelijk"
                    },
                    {
                      "defaultMailtemplate": true,
                      "mail": "ZAAK_ONTVANKELIJK",
                      "mailTemplateNaam": "Zaak ontvankelijk"
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
                responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
            }

            And("all templates are returned") {
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                  [
                    {
                      "id": 1,
                      "mailtemplate": {
                        "defaultMailtemplate": true,
                        "mail": "ZAAK_NIET_ONTVANKELIJK",
                        "mailTemplateNaam": "Zaak niet ontvankelijk",
                      },
                      "zaakafhandelParameters": {
                        "afrondenMail": "BESCHIKBAAR_UIT",
                        "automaticEmailConfirmation": {
                          "emailReply": "reply@info.nl",
                          "emailSender": "GEMEENTE",
                          "enabled": true,
                          "templateName": "Ontvangstbevestiging"
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
                        "defaultGroepId": "test-group-a",
                        "humanTaskParameters": [],
                        "intakeMail": "BESCHIKBAAR_UIT",
                        "mailtemplateKoppelingen": [],
                        "productaanvraagtype": "productaanvraag-type-1",
                        "smartDocuments": {
                          "enabledForZaaktype": true,
                          "enabledGlobally": true
                        },
                        "userEventListenerParameters": [],
                        "valide": true,
                        "zaakAfzenders": [],
                        "zaakbeeindigParameters": [],
                        "zaaktype": {
                          "beginGeldigheid": "2023-09-21",
                          "doel": "Test zaaktype 3",
                          "identificatie": "test-zaaktype-3",
                          "nuGeldig": true,
                          "omschrijving": "Test zaaktype 3",
                          "servicenorm": false,
                          "uuid": "448356ff-dcfb-4504-9501-7fe929077c4f",
                          "versiedatum": "2023-09-21",
                          "vertrouwelijkheidaanduiding": "openbaar"
                        }
                      }
                    },
                    {
                      "mailtemplate": {
                        "defaultMailtemplate": true,
                        "id": 2,
                        "mail": "ZAAK_NIET_ONTVANKELIJK",
                        "mailTemplateNaam": "Zaak niet ontvankelijk",
                      },
                      "zaakafhandelParameters": {
                        "afrondenMail": "BESCHIKBAAR_UIT",
                        "automaticEmailConfirmation": {
                          "emailReply": "reply@info.nl",
                          "emailSender": "sender@info.nl",
                          "enabled": true,
                          "templateName": "Ontvangstbevestiging"
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
                        "creatiedatum": "2025-10-15T16:19:09.962077Z",
                        "defaultGroepId": "test-group-a",
                        "domein": "domein_test_1",
                        "humanTaskParameters": [],
                        "intakeMail": "BESCHIKBAAR_UIT",
                        "mailtemplateKoppelingen": [],
                        "productaanvraagtype": "productaanvraag-type-2",
                        "smartDocuments": {
                          "enabledForZaaktype": true,
                          "enabledGlobally": true
                        },
                        "userEventListenerParameters": [],
                        "valide": true,
                        "zaakAfzenders": [],
                        "zaakbeeindigParameters": [],
                        "zaaktype": {
                          "beginGeldigheid": "2023-10-01",
                          "doel": "Test zaaktype 2",
                          "identificatie": "test-zaaktype-2",
                          "nuGeldig": true,
                          "omschrijving": "Test zaaktype 2",
                          "servicenorm": false,
                          "uuid": "fd2bf643-c98a-4b00-b2b3-9ae0c41ed425",
                          "versiedatum": "2023-10-01",
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
