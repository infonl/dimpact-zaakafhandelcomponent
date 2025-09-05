/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.schema.JsonSchema
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.json.shouldNotContainJsonKey
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.ItestConfiguration
import nl.info.zac.itest.config.ItestConfiguration.ACTIE_INTAKE_AFRONDEN
import nl.info.zac.itest.config.ItestConfiguration.ACTIE_ZAAK_AFHANDELEN
import nl.info.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFACTION_TYPE_VESTIGING
import nl.info.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFICATION_TYPE_BSN
import nl.info.zac.itest.config.ItestConfiguration.BETROKKENE_ROL_TOEVOEGEN_REDEN
import nl.info.zac.itest.config.ItestConfiguration.BETROKKENE_TYPE_NATUURLIJK_PERSOON
import nl.info.zac.itest.config.ItestConfiguration.BRON_ORGANISATIE
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_2
import nl.info.zac.itest.config.ItestConfiguration.DATE_2020_01_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_2020_01_15
import nl.info.zac.itest.config.ItestConfiguration.DATE_2023_09_21
import nl.info.zac.itest.config.ItestConfiguration.DATE_2023_10_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_2025_01_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2020_01_01
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE
import nl.info.zac.itest.config.ItestConfiguration.HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.HUMAN_TASK_TYPE
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_FACTUUR_UUID
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_1
import nl.info.zac.itest.config.ItestConfiguration.RESULTAAT_TYPE_GEWEIGERD_UUID
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_NAME_BELANGHEBBENDE
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_NAME_MEDEAANVRAGER
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_UUID_BELANGHEBBENDE
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_UUID_MEDEAANVRAGER
import nl.info.zac.itest.config.ItestConfiguration.SCREEN_EVENT_TYPE_ZAKEN_VERDELEN
import nl.info.zac.itest.config.ItestConfiguration.SCREEN_EVENT_TYPE_ZAKEN_VRIJGEVEN
import nl.info.zac.itest.config.ItestConfiguration.TEST_BEHANDELAAR_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_BEHANDELAAR_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_BEHANDELAAR_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_COORDINATOR_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_COORDINATOR_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_BEHANDELAARS_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_BEHANDELAARS_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_INFORMATIE_OBJECT_TYPE_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_NUMMER_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGINGSNUMMER_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.info.zac.itest.config.ItestConfiguration.TEST_RAADPLEGER_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_RAADPLEGER_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_2_ID
import nl.info.zac.itest.config.ItestConfiguration.VERANTWOORDELIJKE_ORGANISATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BEHANDELEN_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_REFERENTIEPROCES
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_DESCRIPTION_1
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_DESCRIPTION_2
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_EXPLANATION_1
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_MANUAL_2020_01_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_MANUAL_2024_01_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Betrokkene1Uuid
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import nl.info.zac.itest.util.WebSocketTestListener
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
@Suppress("LargeClass")
class ZaakRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}
    val longitude = Random.nextFloat()
    val latitude = Random.nextFloat()
    val startDateNew = LocalDate.now()
    val fatalDateNew = startDateNew.plusDays(1)
    lateinit var zaak2UUID: UUID

    afterSpec {
        // re-authenticate using testuser1 since currently subsequent integration tests rely on this user being logged in
        authenticate(username = TEST_USER_1_USERNAME, password = TEST_USER_1_PASSWORD)
    }

    Given("ZAC Docker container is running and zaakafhandelparameters have been created and a raadpleger is logged-in") {
        authenticate(username = TEST_RAADPLEGER_1_USERNAME, password = TEST_RAADPLEGER_1_PASSWORD)
        lateinit var responseBody: String
        When("zaak types are listed") {
            val response = itestHttpClient.performGetRequest("$ZAC_API_URI/zaken/zaaktypes")
            Then("the response should be a 200 HTTP response") {
                responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
            }
            And("the response body should contain the BPMN zaak types") {
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                [
                  {
                    "beginGeldigheid": "$DATE_2025_01_01",
                    "doel": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                    "identificatie": "$ZAAKTYPE_BPMN_TEST_IDENTIFICATIE",
                    "informatieobjecttypes": [
                      "$TEST_INFORMATIE_OBJECT_TYPE_1_UUID",
                      "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID",
                      "a8dfd5b8-8657-48bf-b624-f962709f6e19",
                      "12dbb9de-6b5c-4649-b9f3-06e6190f2cc6",
                      "ce22f2f5-d8d1-4c6e-8649-3b24f6c2c38a",
                      "8ca36dd0-7da4-498b-b095-12ac50d13677",
                      "e30d5680-cce3-4e8a-b895-4d358d354198",
                      "ecdb5ee6-846e-4afe-bb87-bee2a87109a9"
                    ],
                    "nuGeldig": true,
                    "omschrijving": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                    "opschortingMogelijk": true,
                    "referentieproces": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                    "servicenorm": false,
                    "uuid": "$ZAAKTYPE_BPMN_TEST_UUID",
                    "verlengingMogelijk": true,
                    "verlengingstermijn": 30,
                    "versiedatum": "$DATE_2025_01_01",
                    "vertrouwelijkheidaanduiding": "openbaar",
                    "zaakafhandelparameters": {
                      "afrondenMail": "BESCHIKBAAR_UIT",
                      "automaticEmailConfirmation": {
                        "enabled": false
                      },
                      "betrokkeneKoppelingen": {
                        "brpKoppelen": false,
                        "kvkKoppelen": false
                      },
                      "brpDoelbindingen": {
                        "raadpleegWaarde": "",
                        "zoekWaarde": ""
                      },
                      "humanTaskParameters": [],
                      "intakeMail": "BESCHIKBAAR_UIT",
                      "mailtemplateKoppelingen": [],
                      "smartDocuments": {
                        "enabledForZaaktype": false,
                        "enabledGlobally": true
                      },
                      "userEventListenerParameters": [],
                      "valide": false,
                      "zaakAfzenders": [
                        {
                          "defaultMail": false,
                          "mail": "GEMEENTE",
                          "speciaal": true
                        },
                        {
                          "defaultMail": false,
                          "mail": "MEDEWERKER",
                          "speciaal": true
                        }
                      ],
                      "zaakbeeindigParameters": [],
                      "zaaktype": {
                        "beginGeldigheid": "$DATE_2025_01_01",
                        "doel": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                        "identificatie": "$ZAAKTYPE_BPMN_TEST_IDENTIFICATIE",
                        "nuGeldig": true,
                        "omschrijving": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                        "servicenorm": false,
                        "uuid": "$ZAAKTYPE_BPMN_TEST_UUID",
                        "versiedatum": "$DATE_2025_01_01",
                        "vertrouwelijkheidaanduiding": "openbaar"
                      }
                    },
                    "zaaktypeRelaties": []
                  }
                ]
                """.trimIndent()
            }
        }
    }

    Given("ZAC Docker container is running and zaakafhandelparameters have been created and a behandelaar is logged-in") {
        authenticate(username = TEST_BEHANDELAAR_1_USERNAME, password = TEST_BEHANDELAAR_1_PASSWORD)
        lateinit var responseBody: String
        When("zaak types are listed") {
            val response = itestHttpClient.performGetRequest("$ZAC_API_URI/zaken/zaaktypes")
            Then("the response should be a 200 HTTP response") {
                responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
            }
            And("the response body should contain the zaak types") {
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                [
                  {
                    "beginGeldigheid": "$DATE_2025_01_01",
                    "doel": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                    "identificatie": "$ZAAKTYPE_BPMN_TEST_IDENTIFICATIE",
                    "informatieobjecttypes": [
                      "$TEST_INFORMATIE_OBJECT_TYPE_1_UUID",
                      "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID",
                      "a8dfd5b8-8657-48bf-b624-f962709f6e19",
                      "12dbb9de-6b5c-4649-b9f3-06e6190f2cc6",
                      "ce22f2f5-d8d1-4c6e-8649-3b24f6c2c38a",
                      "8ca36dd0-7da4-498b-b095-12ac50d13677",
                      "e30d5680-cce3-4e8a-b895-4d358d354198",
                      "ecdb5ee6-846e-4afe-bb87-bee2a87109a9"
                    ],
                    "nuGeldig": true,
                    "omschrijving": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                    "opschortingMogelijk": true,
                    "referentieproces": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                    "servicenorm": false,
                    "uuid": "$ZAAKTYPE_BPMN_TEST_UUID",
                    "verlengingMogelijk": true,
                    "verlengingstermijn": 30,
                    "versiedatum": "$DATE_2025_01_01",
                    "vertrouwelijkheidaanduiding": "openbaar",
                    "zaakafhandelparameters": {
                      "afrondenMail": "BESCHIKBAAR_UIT",
                      "automaticEmailConfirmation": {
                        "enabled": false
                      },
                      "betrokkeneKoppelingen": {
                        "brpKoppelen": false,
                        "kvkKoppelen": false
                      },
                      "brpDoelbindingen": {
                        "raadpleegWaarde": "",
                        "zoekWaarde": ""
                      },
                      "humanTaskParameters": [],
                      "intakeMail": "BESCHIKBAAR_UIT",
                      "mailtemplateKoppelingen": [],
                      "smartDocuments": {
                        "enabledForZaaktype": false,
                        "enabledGlobally": true
                      },
                      "userEventListenerParameters": [],
                      "valide": false,
                      "zaakAfzenders": [
                        {
                          "defaultMail": false,
                          "mail": "GEMEENTE",
                          "speciaal": true
                        },
                        {
                          "defaultMail": false,
                          "mail": "MEDEWERKER",
                          "speciaal": true
                        }
                      ],
                      "zaakbeeindigParameters": [],
                      "zaaktype": {
                        "beginGeldigheid": "$DATE_2025_01_01",
                        "doel": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                        "identificatie": "$ZAAKTYPE_BPMN_TEST_IDENTIFICATIE",
                        "nuGeldig": true,
                        "omschrijving": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                        "servicenorm": false,
                        "uuid": "$ZAAKTYPE_BPMN_TEST_UUID",
                        "versiedatum": "$DATE_2025_01_01",
                        "vertrouwelijkheidaanduiding": "openbaar"
                      }
                    },
                    "zaaktypeRelaties": []
                  },
                  {
                    "beginGeldigheid": "$DATE_2023_10_01",
                    "doel": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                    "identificatie": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BEHANDELEN_IDENTIFICATIE",
                    "informatieobjecttypes": [
                      "$TEST_INFORMATIE_OBJECT_TYPE_1_UUID",
                      "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID",
                      "$INFORMATIE_OBJECT_TYPE_FACTUUR_UUID",
                      "bf9a7836-2e29-4db1-9abc-382f2d4a9e70",
                      "d01b6502-6c9b-48a0-a5f2-9825a2128952",
                      "8018c096-28c5-4175-b235-916b0318c6ef",
                      "37beaaf9-9075-4cc8-b847-a06552324c92",
                      "8a106522-c526-427d-83d0-05393e5cac9a",
                      "9ad666ea-8f17-44a4-aa2c-9e1deb1c9326",
                      "390fca6f-4f9a-41f9-998a-3e7e7fe43271",
                      "b741de57-6509-456e-94fb-6266c0079356",
                      "0a6d8317-593f-4a64-9c18-9f14277e644c",
                      "91dc9aab-0393-4ead-bdf7-0d6ff75aa8a7",
                      "7397af15-44d1-4b0d-b7ea-22b20912ed80"
                    ],
                    "nuGeldig": true,
                    "omschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                    "opschortingMogelijk": true,
                    "referentieproces": "Indienen aansprakelijkstelling door derden",
                    "servicenorm": false,
                    "uuid": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID",
                    "verlengingMogelijk": true,
                    "verlengingstermijn": 30,
                    "versiedatum": "$DATE_2023_10_01",
                    "vertrouwelijkheidaanduiding": "openbaar",
                    "zaakafhandelparameters": {
                      "afrondenMail": "BESCHIKBAAR_UIT",
                      "automaticEmailConfirmation": {
                        "emailReply": "reply@info.nl",
                        "emailSender": "sender@info.nl",
                        "enabled": true,
                        "id": 2,
                        "templateName": "Ontvangstbevestiging"
                      },
                      "betrokkeneKoppelingen": {
                        "brpKoppelen": true,
                        "id": 2,
                        "kvkKoppelen": true
                      },
                      "brpDoelbindingen": {
                        "id": 2,
                        "raadpleegWaarde": "BRPACT-Totaal",
                        "zoekWaarde": "BRPACT-ZoekenAlgemeen"
                      },
                      "caseDefinition": {
                        "humanTaskDefinitions": [
                          {
                            "defaultFormulierDefinitie": "$FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE",
                            "id": "AANVULLENDE_INFORMATIE",
                            "naam": "Aanvullende informatie",
                            "type": "HUMAN_TASK"
                          },
                          {
                            "defaultFormulierDefinitie": "GOEDKEUREN",
                            "id": "GOEDKEUREN",
                            "naam": "Goedkeuren",
                            "type": "HUMAN_TASK"
                          },
                          {
                            "defaultFormulierDefinitie": "EXTERN_ADVIES_VASTLEGGEN",
                            "id": "ADVIES_EXTERN",
                            "naam": "Advies extern",
                            "type": "HUMAN_TASK"
                          },
                          {
                            "defaultFormulierDefinitie": "DOCUMENT_VERZENDEN_POST",
                            "id": "DOCUMENT_VERZENDEN_POST",
                            "naam": "Document verzenden",
                            "type": "HUMAN_TASK"
                          },
                          {
                            "defaultFormulierDefinitie": "ADVIES",
                            "id": "ADVIES_INTERN",
                            "naam": "Advies intern",
                            "type": "HUMAN_TASK"
                          }
                        ],
                        "key": "generiek-zaakafhandelmodel",
                        "naam": "Generiek zaakafhandelmodel",
                        "userEventListenerDefinitions": [
                          {
                            "defaultFormulierDefinitie": "DEFAULT_TAAKFORMULIER",
                            "id": "INTAKE_AFRONDEN",
                            "naam": "Intake afronden",
                            "type": "USER_EVENT_LISTENER"
                          },
                          {
                            "defaultFormulierDefinitie": "DEFAULT_TAAKFORMULIER",
                            "id": "ZAAK_AFHANDELEN",
                            "naam": "Zaak afhandelen",
                            "type": "USER_EVENT_LISTENER"
                          }
                        ]
                      },
                      "defaultGroepId": "test-group-a",
                      "domein": "domein_test_1",
                      "humanTaskParameters": [
                        {
                          "actief": true,
                          "formulierDefinitieId": "AANVULLENDE_INFORMATIE",
                          "planItemDefinition": {
                            "defaultFormulierDefinitie": "AANVULLENDE_INFORMATIE",
                            "id": "AANVULLENDE_INFORMATIE",
                            "naam": "Aanvullende informatie",
                            "type": "HUMAN_TASK"
                          },
                          "referentieTabellen": []
                        },
                        {
                          "actief": true,
                          "formulierDefinitieId": "GOEDKEUREN",
                          "planItemDefinition": {
                            "defaultFormulierDefinitie": "GOEDKEUREN",
                            "id": "GOEDKEUREN",
                            "naam": "Goedkeuren",
                            "type": "HUMAN_TASK"
                          },
                          "referentieTabellen": []
                        },
                        {
                          "actief": true,
                          "formulierDefinitieId": "EXTERN_ADVIES_VASTLEGGEN",
                          "planItemDefinition": {
                            "defaultFormulierDefinitie": "EXTERN_ADVIES_VASTLEGGEN",
                            "id": "ADVIES_EXTERN",
                            "naam": "Advies extern",
                            "type": "HUMAN_TASK"
                          },
                          "referentieTabellen": []
                        },
                        {
                          "actief": true,
                          "formulierDefinitieId": "DOCUMENT_VERZENDEN_POST",
                          "planItemDefinition": {
                            "defaultFormulierDefinitie": "DOCUMENT_VERZENDEN_POST",
                            "id": "DOCUMENT_VERZENDEN_POST",
                            "naam": "Document verzenden",
                            "type": "HUMAN_TASK"
                          },
                          "referentieTabellen": []
                        },
                        {
                          "actief": true,
                          "formulierDefinitieId": "ADVIES",
                          "planItemDefinition": {
                            "defaultFormulierDefinitie": "ADVIES",
                            "id": "ADVIES_INTERN",
                            "naam": "Advies intern",
                            "type": "HUMAN_TASK"
                          },
                          "referentieTabellen": [
                            {
                              "id": 2,
                              "tabel": {
                                "aantalWaarden": 5,
                                "code": "ADVIES",
                                "id": 1,
                                "naam": "Advies",
                                "systeem": true,
                                "waarden": []
                              },
                              "veld": "ADVIES"
                            }
                          ]
                        }
                      ],
                      "id": 2,
                      "intakeMail": "BESCHIKBAAR_UIT",
                      "mailtemplateKoppelingen": [],
                      "productaanvraagtype": "productaanvraag-type-2",
                      "smartDocuments": {
                        "enabledForZaaktype": true,
                        "enabledGlobally": true
                      },
                      "userEventListenerParameters": [
                        {
                          "id": "INTAKE_AFRONDEN",
                          "naam": "Intake afronden"
                        },
                        {
                          "id": "ZAAK_AFHANDELEN",
                          "naam": "Zaak afhandelen"
                        }
                      ],
                      "valide": true,
                      "zaakAfzenders": [
                        {
                          "defaultMail": false,
                          "mail": "GEMEENTE",
                          "speciaal": true
                        },
                        {
                          "defaultMail": false,
                          "mail": "MEDEWERKER",
                          "speciaal": true
                        }
                      ],
                      "zaakNietOntvankelijkResultaattype": {
                        "archiefNominatie": "VERNIETIGEN",
                        "archiefTermijn": "5 jaren",
                        "besluitVerplicht": false,
                        "bronArchiefprocedure": {
                          "afleidingswijze": "afgehandeld",
                          "datumkenmerk": "",
                          "einddatumBekend": false,
                          "objecttype": "",
                          "registratie": ""
                        },
                        "datumKenmerkVerplicht": false,
                        "id": "dd2bcd87-ed7e-4b23-a8e3-ea7fe7ef00c6",
                        "naam": "Geweigerd",
                        "naamGeneriek": "Geweigerd",
                        "toelichting": "Het door het orgaan behandelen van een aanvraag, melding of verzoek om toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen",
                        "vervaldatumBesluitVerplicht": false
                      },
                      "zaakbeeindigParameters": [],
                      "zaaktype": {
                        "beginGeldigheid": "$DATE_2023_10_01",
                        "doel": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                        "identificatie": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BEHANDELEN_IDENTIFICATIE",
                        "nuGeldig": true,
                        "omschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                        "servicenorm": false,
                        "uuid": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID",
                        "versiedatum": "$DATE_2023_10_01",
                        "vertrouwelijkheidaanduiding": "openbaar"
                      }
                    },
                    "zaaktypeRelaties": [
                      {
                        "relatieType": "DEELZAAK",
                        "zaaktypeUuid": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID"
                      }
                    ]
                  },
                  {
                    "beginGeldigheid": "$DATE_2023_09_21",
                    "doel": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                    "identificatie": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE",
                    "informatieobjecttypes": [
                      "$TEST_INFORMATIE_OBJECT_TYPE_1_UUID",
                      "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                    ],
                    "nuGeldig": true,
                    "omschrijving": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                    "opschortingMogelijk": false,
                    "referentieproces": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_REFERENTIEPROCES",
                    "servicenorm": false,
                    "uuid": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID",
                    "verlengingMogelijk": false,
                    "versiedatum": "$DATE_2023_09_21",
                    "vertrouwelijkheidaanduiding": "openbaar",
                    "zaakafhandelparameters": {
                      "afrondenMail": "BESCHIKBAAR_UIT",
                      "automaticEmailConfirmation": {
                        "emailReply": "reply@info.nl",
                        "emailSender": "GEMEENTE",
                        "enabled": true,
                        "id": 1,
                        "templateName": "Ontvangstbevestiging"
                      },
                      "betrokkeneKoppelingen": {
                        "brpKoppelen": true,
                        "id": 1,
                        "kvkKoppelen": true
                      },
                      "brpDoelbindingen": {
                        "id": 1,
                        "raadpleegWaarde": "BRPACT-Totaal",
                        "zoekWaarde": "BRPACT-ZoekenAlgemeen"
                      },
                      "caseDefinition": {
                        "humanTaskDefinitions": [
                          {
                            "defaultFormulierDefinitie": "$FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE",
                            "id": "$FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE",
                            "naam": "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM",
                            "type": "$HUMAN_TASK_TYPE"
                          },
                          {
                            "defaultFormulierDefinitie": "GOEDKEUREN",
                            "id": "GOEDKEUREN",
                            "naam": "Goedkeuren",
                            "type": "$HUMAN_TASK_TYPE"
                          },
                          {
                            "defaultFormulierDefinitie": "EXTERN_ADVIES_VASTLEGGEN",
                            "id": "ADVIES_EXTERN",
                            "naam": "Advies extern",
                            "type": "$HUMAN_TASK_TYPE"
                          },
                          {
                            "defaultFormulierDefinitie": "DOCUMENT_VERZENDEN_POST",
                            "id": "DOCUMENT_VERZENDEN_POST",
                            "naam": "Document verzenden",
                            "type": "$HUMAN_TASK_TYPE"
                          },
                          {
                            "defaultFormulierDefinitie": "ADVIES",
                            "id": "ADVIES_INTERN",
                            "naam": "Advies intern",
                            "type": "$HUMAN_TASK_TYPE"
                          }
                        ],
                        "key": "generiek-zaakafhandelmodel",
                        "naam": "Generiek zaakafhandelmodel",
                        "userEventListenerDefinitions": [
                          {
                            "defaultFormulierDefinitie": "DEFAULT_TAAKFORMULIER",
                            "id": "$ACTIE_INTAKE_AFRONDEN",
                            "naam": "Intake afronden",
                            "type": "USER_EVENT_LISTENER"
                          },
                          {
                            "defaultFormulierDefinitie": "DEFAULT_TAAKFORMULIER",
                            "id": "$ACTIE_ZAAK_AFHANDELEN",
                            "naam": "Zaak afhandelen",
                            "type": "USER_EVENT_LISTENER"
                          }
                        ]
                      },
                      "defaultGroepId": "$TEST_GROUP_A_ID",
                      "humanTaskParameters": [
                        {
                          "actief": true,
                          "formulierDefinitieId": "$FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE",
                          "planItemDefinition": {
                            "defaultFormulierDefinitie": "$FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE",
                            "id": "$FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE",
                            "naam": "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM",
                            "type": "$HUMAN_TASK_TYPE"
                          },
                          "referentieTabellen": []
                        },
                        {
                          "actief": true,
                          "formulierDefinitieId": "GOEDKEUREN",
                          "planItemDefinition": {
                            "defaultFormulierDefinitie": "GOEDKEUREN",
                            "id": "GOEDKEUREN",
                            "naam": "Goedkeuren",
                            "type": "$HUMAN_TASK_TYPE"
                          },
                          "referentieTabellen": []
                        },
                        {
                          "actief": true,
                          "formulierDefinitieId": "EXTERN_ADVIES_VASTLEGGEN",
                          "planItemDefinition": {
                            "defaultFormulierDefinitie": "EXTERN_ADVIES_VASTLEGGEN",
                            "id": "ADVIES_EXTERN",
                            "naam": "Advies extern",
                            "type": "$HUMAN_TASK_TYPE"
                          },
                          "referentieTabellen": []
                        },
                        {
                          "actief": true,
                          "formulierDefinitieId": "DOCUMENT_VERZENDEN_POST",
                          "planItemDefinition": {
                            "defaultFormulierDefinitie": "DOCUMENT_VERZENDEN_POST",
                            "id": "DOCUMENT_VERZENDEN_POST",
                            "naam": "Document verzenden",
                            "type": "$HUMAN_TASK_TYPE"
                          },
                          "referentieTabellen": []
                        },
                        {
                          "actief": true,
                          "formulierDefinitieId": "ADVIES",
                          "planItemDefinition": {
                            "defaultFormulierDefinitie": "ADVIES",
                            "id": "ADVIES_INTERN",
                            "naam": "Advies intern",
                            "type": "$HUMAN_TASK_TYPE"
                          },
                          "referentieTabellen": [
                            {
                              "id": 1,
                              "tabel": {
                                "aantalWaarden": 5,
                                "code": "ADVIES",
                                "id": 1,
                                "naam": "Advies",
                                "systeem": true,
                                "waarden": []
                              },
                              "veld": "ADVIES"
                            }
                          ]
                        }
                      ],
                      "id": 1,
                      "intakeMail": "BESCHIKBAAR_UIT",
                      "mailtemplateKoppelingen": [],
                      "productaanvraagtype": "$PRODUCTAANVRAAG_TYPE_1",
                      "smartDocuments": {
                        "enabledForZaaktype": true,
                        "enabledGlobally": true
                      },
                      "userEventListenerParameters": [
                        {
                          "id": "$ACTIE_INTAKE_AFRONDEN",
                          "naam": "Intake afronden"
                        },
                        {
                          "id": "$ACTIE_ZAAK_AFHANDELEN",
                          "naam": "Zaak afhandelen"
                        }
                      ],
                      "valide": true,
                      "zaakAfzenders": [
                        {
                          "defaultMail": false,
                          "mail": "GEMEENTE",
                          "speciaal": true
                        },
                        {
                          "defaultMail": false,
                          "mail": "MEDEWERKER",
                          "speciaal": true
                        }
                      ],
                      "zaakNietOntvankelijkResultaattype": {
                        "archiefNominatie": "VERNIETIGEN",
                        "archiefTermijn": "5 jaren",
                        "besluitVerplicht": false,
                        "bronArchiefprocedure": {
                          "afleidingswijze": "afgehandeld",
                          "datumkenmerk": "",
                          "einddatumBekend": false,
                          "objecttype": "",
                          "registratie": ""
                        },
                        "datumKenmerkVerplicht": false,
                        "id": "$RESULTAAT_TYPE_GEWEIGERD_UUID",
                        "naam": "Geweigerd",
                        "naamGeneriek": "Geweigerd",
                        "toelichting": "Het door het orgaan behandelen van een aanvraag, melding of verzoek om toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen",
                        "vervaldatumBesluitVerplicht": false
                      },
                      "zaakbeeindigParameters": [],
                      "zaaktype": {
                        "beginGeldigheid": "$DATE_2023_09_21",
                        "doel": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                        "identificatie": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE",
                        "nuGeldig": true,
                        "omschrijving": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                        "servicenorm": false,
                        "uuid": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID",
                        "versiedatum": "$DATE_2023_09_21",
                        "vertrouwelijkheidaanduiding": "openbaar"
                      }
                    },
                    "zaaktypeRelaties": []
                  }
                ]
                """.trimIndent()
            }
        }
        When("the create zaak endpoint is called and the user has permissions for the zaaktype used") {
            val response = zacClient.createZaak(
                zaakTypeUUID = ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID,
                groupId = TEST_GROUP_A_ID,
                groupName = TEST_GROUP_A_DESCRIPTION,
                startDate = DATE_TIME_2020_01_01,
                communicatiekanaal = COMMUNICATIEKANAAL_TEST_1,
                vertrouwelijkheidaanduiding = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR,
                description = ZAAK_DESCRIPTION_2,
                toelichting = ZAAK_EXPLANATION_1
            )
            Then("the response should be a 200 HTTP response") {
                responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
            }
            And(
                """
                the response should contain the created zaak with the 'bekijkenZaakdata' and 'heropenen' permissions
                set to false since these actions are not allowed for the 'behandelaar' role
                """
            ) {
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "besluiten": [],
                      "bronorganisatie": "$BRON_ORGANISATIE",
                      "communicatiekanaal": "$COMMUNICATIEKANAAL_TEST_1",
                      "gerelateerdeZaken": [],
                      "groep": {
                        "id": "$TEST_GROUP_A_ID",
                        "naam": "$TEST_GROUP_A_DESCRIPTION"
                      },
                      "identificatie": "$ZAAK_MANUAL_2020_01_IDENTIFICATION",
                      "indicaties": ["ONTVANGSTBEVESTIGING_NIET_VERSTUURD"],
                      "isBesluittypeAanwezig": false,
                      "isDeelzaak": false,
                      "isHeropend": false,
                      "isHoofdzaak": false,
                      "isInIntakeFase": false,
                      "isOpen": true,
                      "isOpgeschort": false,
                      "isEerderOpgeschort": false,
                      "isProcesGestuurd": false,
                      "isVerlengd": false,
                      "kenmerken": [],
                      "omschrijving": "$ZAAK_DESCRIPTION_2",
                      "rechten": {
                        "afbreken": true,
                        "behandelen": true,
                        "bekijkenZaakdata": false,
                        "creerenDocument": true,
                        "heropenen": false,
                        "lezen": true,
                        "toekennen": true,
                        "toevoegenBagObject": true,
                        "toevoegenBetrokkeneBedrijf": true,
                        "toevoegenBetrokkenePersoon": true,
                        "toevoegenInitiatorBedrijf": true,
                        "toevoegenInitiatorPersoon": true,
                        "versturenEmail": true,
                        "versturenOntvangstbevestiging": true,
                        "verwijderenBetrokkene": true,
                        "verwijderenInitiator": true,
                        "wijzigen": true,
                        "wijzigenDoorlooptijd": true
                      },
                      "registratiedatum": "${LocalDate.now()}",
                      "startdatum": "$DATE_2020_01_01",
                      "toelichting": "$ZAAK_EXPLANATION_1",
                      "uiterlijkeEinddatumAfdoening": "$DATE_2020_01_15",
                      "verantwoordelijkeOrganisatie": "$VERANTWOORDELIJKE_ORGANISATIE",
                      "vertrouwelijkheidaanduiding": "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR",
                      "zaakdata": {
                        "zaakIdentificatie": "$ZAAK_MANUAL_2020_01_IDENTIFICATION",
                        "initiator": null,
                        "zaaktypeUUID": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID",
                        "zaaktypeOmschrijving": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                      },
                      "zaaktype": {
                        "beginGeldigheid": "$DATE_2023_09_21",
                        "doel": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                        "identificatie": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE",
                        "informatieobjecttypes": [
                          "$TEST_INFORMATIE_OBJECT_TYPE_1_UUID",
                          "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                        ],
                        "nuGeldig": true,
                        "omschrijving": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                        "opschortingMogelijk": false,
                        "referentieproces": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_REFERENTIEPROCES",
                        "servicenorm": false,
                        "uuid": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID",
                        "verlengingMogelijk": false,
                        "versiedatum": "$DATE_2023_09_21",
                        "vertrouwelijkheidaanduiding": "openbaar",
                        "zaakafhandelparameters": {
                          "afrondenMail": "BESCHIKBAAR_UIT",
                          "caseDefinition": {
                            "humanTaskDefinitions": [
                              {
                                "defaultFormulierDefinitie": "$FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE",
                                "id": "$FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE",
                                "naam": "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM",
                                "type": "$HUMAN_TASK_TYPE"
                              },
                              {
                                "defaultFormulierDefinitie": "GOEDKEUREN",
                                "id": "GOEDKEUREN",
                                "naam": "Goedkeuren",
                                "type": "$HUMAN_TASK_TYPE"
                              },
                              {
                                "defaultFormulierDefinitie": "ADVIES",
                                "id": "ADVIES_INTERN",
                                "naam": "Advies intern",
                                "type": "$HUMAN_TASK_TYPE"
                              },
                              {
                                "defaultFormulierDefinitie": "EXTERN_ADVIES_VASTLEGGEN",
                                "id": "ADVIES_EXTERN",
                                "naam": "Advies extern",
                                "type": "$HUMAN_TASK_TYPE"
                              },
                              {
                                "defaultFormulierDefinitie": "DOCUMENT_VERZENDEN_POST",
                                "id": "DOCUMENT_VERZENDEN_POST",
                                "naam": "Document verzenden",
                                "type": "$HUMAN_TASK_TYPE"
                              }
                            ],
                            "key": "generiek-zaakafhandelmodel",
                            "naam": "Generiek zaakafhandelmodel",
                            "userEventListenerDefinitions": [
                              {
                                "defaultFormulierDefinitie": "DEFAULT_TAAKFORMULIER",
                                "id": "$ACTIE_INTAKE_AFRONDEN",
                                "naam": "Intake afronden",
                                "type": "USER_EVENT_LISTENER"
                              },
                              {
                                "defaultFormulierDefinitie": "DEFAULT_TAAKFORMULIER",
                                "id": "$ACTIE_ZAAK_AFHANDELEN",
                                "naam": "Zaak afhandelen",
                                "type": "USER_EVENT_LISTENER"
                              }
                            ]
                          },
                          "defaultGroepId": "$TEST_GROUP_A_ID",
                          "humanTaskParameters": [
                            {
                              "actief": true,
                              "formulierDefinitieId": "$FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE",
                              "planItemDefinition": {
                                "defaultFormulierDefinitie": "$FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE",
                                "id": "$FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE",
                                "naam": "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM",
                                "type": "$HUMAN_TASK_TYPE"
                              },
                              "referentieTabellen": []
                            },
                            {
                              "actief": true,
                              "formulierDefinitieId": "GOEDKEUREN",
                              "planItemDefinition": {
                                "defaultFormulierDefinitie": "GOEDKEUREN",
                                "id": "GOEDKEUREN",
                                "naam": "Goedkeuren",
                                "type": "$HUMAN_TASK_TYPE"
                              },
                              "referentieTabellen": []
                            },
                            {
                              "actief": true,
                              "formulierDefinitieId": "ADVIES",
                              "planItemDefinition": {
                                "defaultFormulierDefinitie": "ADVIES",
                                "id": "ADVIES_INTERN",
                                "naam": "Advies intern",
                                "type": "$HUMAN_TASK_TYPE"
                              },
                              "referentieTabellen": [
                                {
                                  "id": 1,
                                  "tabel": {
                                    "aantalWaarden": 5,
                                    "code": "ADVIES",
                                    "id": 1,
                                    "naam": "Advies",
                                    "systeem": true,
                                    "waarden": []
                                  },
                                  "veld": "ADVIES"
                                }
                              ]
                            },
                            {
                              "actief": true,
                              "formulierDefinitieId": "EXTERN_ADVIES_VASTLEGGEN",
                              "planItemDefinition": {
                                "defaultFormulierDefinitie": "EXTERN_ADVIES_VASTLEGGEN",
                                "id": "ADVIES_EXTERN",
                                "naam": "Advies extern",
                                "type": "$HUMAN_TASK_TYPE"
                              },
                              "referentieTabellen": []
                            },
                            {
                              "actief": true,
                              "formulierDefinitieId": "DOCUMENT_VERZENDEN_POST",                       
                              "planItemDefinition": {
                                "defaultFormulierDefinitie": "DOCUMENT_VERZENDEN_POST",
                                "id": "DOCUMENT_VERZENDEN_POST",
                                "naam": "Document verzenden",
                                "type": "$HUMAN_TASK_TYPE"
                              },
                              "referentieTabellen": []
                            }
                          ],
                          "id": 1,
                          "intakeMail": "BESCHIKBAAR_UIT",
                          "mailtemplateKoppelingen": [],
                          "productaanvraagtype": "$PRODUCTAANVRAAG_TYPE_1",
                          "userEventListenerParameters": [
                            {
                              "id": "$ACTIE_INTAKE_AFRONDEN",
                              "naam": "Intake afronden"
                            },
                            {
                              "id": "$ACTIE_ZAAK_AFHANDELEN",
                              "naam": "Zaak afhandelen"
                            }
                          ],
                          "valide": true,
                          "zaakAfzenders": [
                            {
                              "defaultMail": false,
                              "mail": "GEMEENTE",
                              "speciaal": true
                            },
                            {
                              "defaultMail": false,
                              "mail": "MEDEWERKER",
                              "speciaal": true
                            }
                          ],
                          "zaakNietOntvankelijkResultaattype": {
                            "archiefNominatie": "VERNIETIGEN",
                            "archiefTermijn": "5 jaren",
                            "besluitVerplicht": false,
                            "id": "$RESULTAAT_TYPE_GEWEIGERD_UUID",
                            "naam": "Geweigerd",
                            "naamGeneriek": "Geweigerd",
                            "toelichting": "Het door het orgaan behandelen van een aanvraag, melding of verzoek om toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen",
                            "vervaldatumBesluitVerplicht": false
                          },
                          "zaakbeeindigParameters": [],
                          "zaaktype": {
                            "beginGeldigheid": "$DATE_2023_09_21",
                            "doel": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                            "identificatie": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE",
                            "nuGeldig": true,
                            "omschrijving": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                            "servicenorm": false,
                            "uuid": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID",
                            "versiedatum": "$DATE_2023_09_21",
                            "vertrouwelijkheidaanduiding": "openbaar"
                          }
                        },
                        "zaaktypeRelaties": []
                      }
                    }
                """.trimIndent()
                zaak2UUID = JSONObject(responseBody).getString("uuid").let(UUID::fromString)
            }
        }
    }

    Given("A zaak has been created and a logged-in behandelaar") {
        authenticate(username = TEST_BEHANDELAAR_1_USERNAME, password = TEST_BEHANDELAAR_1_PASSWORD)
        When("the get zaak endpoint is called") {
            val response = zacClient.retrieveZaak(zaak2UUID)
            Then("the response should be a 200 HTTP response and contain the created zaak") {
                with(response) {
                    code shouldBe HTTP_OK
                    val responseBody = response.body.string()
                    logger.info { "Response: $responseBody" }
                    with(JSONObject(responseBody)) {
                        getString("identificatie") shouldBe ZAAK_MANUAL_2020_01_IDENTIFICATION
                        getJSONObject("zaaktype").getString("identificatie") shouldBe ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
                    }
                }
            }
        }
        When("the add betrokkene to zaak endpoint is called with a natuurlijk persoon without a 'rol toelichting'") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/zaken/betrokkene",
                requestBodyAsString = """
                    {
                        "zaakUUID": "$zaak2UUID",
                        "roltypeUUID": "$ROLTYPE_UUID_BELANGHEBBENDE",
                        "betrokkeneIdentificatie": {
                            "bsnNummer": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                            "type": "$BETROKKENE_IDENTIFICATION_TYPE_BSN"
                        }
                    }
                """.trimIndent()
            )
            Then("the response should be a 200 OK HTTP response") {
                response.code shouldBe HTTP_OK
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                with(responseBody) {
                    shouldContainJsonKeyValue("uuid", zaak2UUID.toString())
                }
            }
        }
        When(
            """"
            the 'update zaak' endpoint is called where the communication channel and description are changed
            """
        ) {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaak2UUID",
                requestBodyAsString = """
                    { 
                        "zaak": {
                            "startdatum": "$startDateNew",
                            "uiterlijkeEinddatumAfdoening": "$fatalDateNew",
                            "communicatiekanaal": "$COMMUNICATIEKANAAL_TEST_2",
                            "omschrijving": "$ZAAK_DESCRIPTION_1"
                        },
                        "reden": "fakeReason"
                    }
                """.trimIndent()
            )
            Then("the response should be a 200 HTTP response with the changed zaak data") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("uuid", zaak2UUID.toString())
                    shouldContainJsonKeyValue("communicatiekanaal", COMMUNICATIEKANAAL_TEST_2)
                    shouldContainJsonKeyValue("omschrijving", ZAAK_DESCRIPTION_1)
                }
            }
        }
        When("the 'assign to zaak' endpoint is called with a group") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/toekennen",
                requestBodyAsString = """
                    {
                        "zaakUUID": "$zaak2UUID",
                        "groepId": "$TEST_GROUP_BEHANDELAARS_ID",
                        "reden": "fakeReason"
                    }
                """.trimIndent()
            )
            Then("the zaak should be assigned to the group") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true

                with(responseBody) {
                    shouldContainJsonKeyValue("uuid", zaak2UUID.toString())
                    shouldContainJsonKey("groep")
                    JSONObject(this).getJSONObject("groep").apply {
                        getString("id") shouldBe TEST_GROUP_BEHANDELAARS_ID
                        getString("naam") shouldBe TEST_GROUP_BEHANDELAARS_DESCRIPTION
                    }
                }
            }
        }
        When(
            """
            the add betrokkene to zaak endpoint is called with a role type and betrokkene identification 
            that are not already added to the zaak
            """
        ) {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/zaken/betrokkene",
                requestBodyAsString = """
                    {
                        "zaakUUID": "$zaak2UUID",
                        "roltypeUUID": "$ROLTYPE_UUID_MEDEAANVRAGER",
                        "roltoelichting": "fakeToelichting",
                        "betrokkeneIdentificatie": {
                            "bsnNummer": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                            "type": "$BETROKKENE_IDENTIFICATION_TYPE_BSN"
                        }
                    }
                """.trimIndent()
            )
            Then("the response should be a 200 OK HTTP response") {
                response.code shouldBe HTTP_OK
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                with(responseBody) {
                    shouldContainJsonKeyValue("uuid", zaak2UUID.toString())
                }
            }
        }
        When("the 'update Zaak Locatie' endpoint is called with a valid location") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/$zaak2UUID/zaaklocatie",
                requestBodyAsString = """
                        {
                            "geometrie": {
                                "point": {
                                    "longitude": $longitude,
                                    "latitude": $latitude
                                },
                                "type": "Point"
                            },
                            "reden": "fakeReason"
                        }
                """.trimIndent()
            )
            Then("the response should be a 200 HTTP response with the changed zaak data") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(responseBody) {
                    shouldContainJsonKey("zaakgeometrie")
                    val geometrie = JSONObject(responseBody)["zaakgeometrie"].toString()
                    with(geometrie) {
                        shouldContainJsonKeyValue("type", "Point")
                        shouldContainJsonKey("point")
                        with(JSONObject(geometrie)["point"].toString()) {
                            shouldContainJsonKeyValue("longitude", longitude)
                            shouldContainJsonKeyValue("latitude", latitude)
                        }
                    }
                }
            }
        }
        When("the update zaak endpoint is called with a changed zaak description") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaak2UUID",
                requestBodyAsString = """
                    { 
                        "zaak": {
                            "startdatum": "$startDateNew",
                            "uiterlijkeEinddatumAfdoening": "$fatalDateNew",
                            "omschrijving": "changedDescription"
                        },
                        "reden": "fakeReason"
                    }
                """.trimIndent()
            )
            Then(
                "the response should be a 200 HTTP response with only the changed zaak description and no other changes"
            ) {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "besluiten": [],
                      "bronorganisatie": "$BRON_ORGANISATIE",
                      "communicatiekanaal": "$COMMUNICATIEKANAAL_TEST_2",
                      "gerelateerdeZaken": [],
                      "groep": {
                        "id": "$TEST_GROUP_BEHANDELAARS_ID",
                        "naam": "$TEST_GROUP_BEHANDELAARS_DESCRIPTION"
                      },
                      "identificatie": "$ZAAK_MANUAL_2020_01_IDENTIFICATION",
                      "indicaties": ["ONTVANGSTBEVESTIGING_NIET_VERSTUURD"],
                      "isBesluittypeAanwezig": false,
                      "isDeelzaak": false,
                      "isHeropend": false,
                      "isHoofdzaak": false,
                      "isInIntakeFase": true,
                      "isOpen": true,
                      "isOpgeschort": false,
                      "isEerderOpgeschort": false,
                      "isProcesGestuurd": false,
                      "isVerlengd": false,
                      "kenmerken": [],
                      "omschrijving": "changedDescription",         
                      "registratiedatum": "${LocalDate.now()}",
                      "startdatum": "$startDateNew",
                      "toelichting": "$ZAAK_EXPLANATION_1",
                      "uiterlijkeEinddatumAfdoening": "$fatalDateNew",
                      "uuid" : "$zaak2UUID",
                      "verantwoordelijkeOrganisatie" : "$VERANTWOORDELIJKE_ORGANISATIE",
                      "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR",
                      "zaakgeometrie" : {
                        "point" : {
                            "latitude" : $latitude,
                            "longitude" : $longitude
                         },
                         "type" : "Point"
                      }
                   }
                """.trimIndent()
            }
        }
        When("the 'update Zaak Locatie' endpoint is called with a null value as location") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/$zaak2UUID/zaaklocatie",
                requestBodyAsString = """
                        {
                            "geometrie": null,
                            "reden": "fakeReason"
                        }
                """.trimIndent()
            )
            Then("the response should be a 200 HTTP response with the changed zaak data without zaakgeometrie") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(responseBody) {
                    shouldNotContainJsonKey("zaakgeometrie")
                }
            }
        }
        When("an initiator is added to the zaak with a vestigingsnummer") {
            val vestigingsnummer = "$TEST_KVK_VESTIGINGSNUMMER_1"
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/initiator",
                requestBodyAsString = """
                        {
                            "betrokkeneIdentificatie": {
                                "kvkNummer": "$TEST_KVK_NUMMER_1",
                                "type": "$BETROKKENE_IDENTIFACTION_TYPE_VESTIGING",
                                "vestigingsnummer": "$vestigingsnummer"
                            },
                            "zaakUUID": "$zaak2UUID"
                        }
                """.trimIndent()
            )
            Then("the response should be a 200 HTTP response and the initiator should be added") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("initiatorIdentificatieType", "VN")
                    shouldContainJsonKeyValue("initiatorIdentificatie", vestigingsnummer)
                }
            }
        }
    }

    Given("Betrokkenen have been added to a zaak and a logged-in behandelaar") {
        authenticate(username = TEST_BEHANDELAAR_1_USERNAME, password = TEST_BEHANDELAAR_1_PASSWORD)
        When("the get betrokkene endpoint is called for a zaak") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaak2UUID/betrokkene",
            )
            Then("the response should be a 200 HTTP response with a list consisting of the betrokkenen") {
                response.code shouldBe HTTP_OK
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                with(JSONArray(responseBody)) {
                    length() shouldBe 2
                    getJSONObject(0).apply {
                        getString("rolid") shouldNotBe null
                        getString("roltype") shouldBe ROLTYPE_NAME_MEDEAANVRAGER
                        getString("roltoelichting") shouldBe "fakeToelichting"
                        getString("type") shouldBe BETROKKENE_TYPE_NATUURLIJK_PERSOON
                        getString("identificatie") shouldBe TEST_PERSON_HENDRIKA_JANSE_BSN
                        getString("identificatieType") shouldBe "BSN"
                    }
                    getJSONObject(1).apply {
                        getString("rolid") shouldNotBe null
                        getString("roltype") shouldBe ROLTYPE_NAME_BELANGHEBBENDE
                        // if no toelichting was provided, the default value should be used
                        getString("roltoelichting") shouldBe BETROKKENE_ROL_TOEVOEGEN_REDEN
                        getString("type") shouldBe BETROKKENE_TYPE_NATUURLIJK_PERSOON
                        getString("identificatie") shouldBe TEST_PERSON_HENDRIKA_JANSE_BSN
                        getString("identificatieType") shouldBe "BSN"
                        zaakProductaanvraag1Betrokkene1Uuid = getString("rolid").let(UUID::fromString)
                    }
                }
            }
        }
    }

    Given(
        """
            Two zaken have been created and two websocket subscriptions have been created to listen for both a 'zaken verdelen' 
            screen event as well as for 'zaak rollen' screen events which will be sent by the asynchronous 'assign zaken from list' 
            job and a logged-in coordinator
        """
    ) {
        authenticate(username = TEST_COORDINATOR_1_USERNAME, password = TEST_COORDINATOR_1_PASSWORD)
        val uniqueResourceId = UUID.randomUUID()
        val zakenVerdelenWebsocketListener = WebSocketTestListener(
            textToBeSentOnOpen = """
            {
                "subscriptionType": "CREATE",
                "event": {
                    "opcode": "UPDATED",
                    "objectType": "$SCREEN_EVENT_TYPE_ZAKEN_VERDELEN",
                    "objectId": {
                        "resource": "$uniqueResourceId"
                    },
                "_key": "UPDATED;$SCREEN_EVENT_TYPE_ZAKEN_VERDELEN;$uniqueResourceId"
                 }
            }
            """.trimIndent()
        )
        itestHttpClient.connectNewWebSocket(
            url = ItestConfiguration.ZAC_WEBSOCKET_BASE_URI,
            webSocketListener = zakenVerdelenWebsocketListener
        )

        When(
            """the 'assign zaken from list' endpoint is called to start an asynchronous process 
                to assign the two zaken to a group and a user using the unique resource ID 
                that was used to create the websocket subscription"""
        ) {
            val lijstVerdelenResponse = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaken/lijst/verdelen",
                requestBodyAsString = "{\n" +
                    "\"uuids\":[\"$zaakProductaanvraag1Uuid\", \"$zaak2UUID\"],\n" +
                    "\"groepId\":\"$TEST_GROUP_A_ID\",\n" +
                    "\"behandelaarGebruikersnaam\":\"$TEST_USER_2_ID\",\n" +
                    "\"reden\":\"fakeLijstVerdelenReason\",\n" +
                    "\"screenEventResourceId\":\"$uniqueResourceId\"\n" +
                    "}"
            )
            Then(
                """the response should be a 204 HTTP response and eventually a screen event of type 'zaken verdelen'
                    should be received by the websocket listener and the two zaken should be assigned correctly"""
            ) {
                val lijstVerdelenResponseBody = lijstVerdelenResponse.body.string()
                logger.info { "Response: $lijstVerdelenResponseBody" }
                lijstVerdelenResponse.code shouldBe HTTP_NO_CONTENT
                // the backend process is asynchronous, so we need to wait a bit until the zaken are assigned
                eventually(10.seconds) {
                    zakenVerdelenWebsocketListener.messagesReceived.size shouldBe 1
                    with(JSONObject(zakenVerdelenWebsocketListener.messagesReceived[0])) {
                        getString("opcode") shouldBe "UPDATED"
                        getString("objectType") shouldBe "ZAKEN_VERDELEN"
                        getJSONObject("objectId").getString("resource") shouldBe uniqueResourceId.toString()
                    }
                    zacClient.retrieveZaak(zaakProductaanvraag1Uuid).use { response ->
                        response.code shouldBe HTTP_OK
                        with(JSONObject(response.body.string())) {
                            getJSONObject("groep").getString("id") shouldBe TEST_GROUP_A_ID
                            getJSONObject("behandelaar").getString("id") shouldBe TEST_USER_2_ID
                        }
                    }
                    zacClient.retrieveZaak(zaak2UUID).use { response ->
                        response.code shouldBe HTTP_OK
                        with(JSONObject(response.body.string())) {
                            getJSONObject("groep").getString("id") shouldBe TEST_GROUP_A_ID
                            getJSONObject("behandelaar").getString("id") shouldBe TEST_USER_2_ID
                        }
                    }
                }
            }
        }
    }

    Given("A zaak with domain exists and a websocket subscription has been created and a logged-in coordinator") {
        authenticate(username = TEST_COORDINATOR_1_USERNAME, password = TEST_COORDINATOR_1_PASSWORD)
        val response = zacClient.retrieveZaak(ZAAK_MANUAL_2024_01_IDENTIFICATION)
        response.code shouldBe HTTP_OK
        val responseBody = response.body.string()
        logger.info { "Response: $responseBody" }
        val zaakWithDomainUuid = JSONObject(responseBody).getString("uuid").let(UUID::fromString)

        val uniqueResourceId = UUID.randomUUID()
        val zakenVerdelenWebsocketListener = WebSocketTestListener(
            textToBeSentOnOpen = """
            {
                "subscriptionType": "CREATE",
                "event": {
                    "opcode": "UPDATED",
                    "objectType": "$SCREEN_EVENT_TYPE_ZAKEN_VERDELEN",
                    "objectId": {
                        "resource": "$uniqueResourceId"
                    },
                "_key": "UPDATED;$SCREEN_EVENT_TYPE_ZAKEN_VERDELEN;$uniqueResourceId"
                 }
            }
            """.trimIndent()
        )
        itestHttpClient.connectNewWebSocket(
            url = ItestConfiguration.ZAC_WEBSOCKET_BASE_URI,
            webSocketListener = zakenVerdelenWebsocketListener
        )

        When(
            """the 'assign zaken from list' endpoint is called to start an asynchronous process 
                to assign the two zaken to a group that is outside the zaak domain"""
        ) {
            val lijstVerdelenResponse = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaken/lijst/verdelen",
                requestBodyAsString = """{
                    "uuids":["$zaakWithDomainUuid"],
                    "groepId":"$TEST_GROUP_A_ID",
                    "reden":"fakeLijstVerdelenReason",
                    "screenEventResourceId":"$uniqueResourceId"
                }"""
            )
            Then(
                """a 204 HTTP response and a screen event of type 'zaken verdelen'
                    should be received by the websocket listener and the zaak should not be reassigned"""
            ) {
                val lijstVerdelenResponseBody = lijstVerdelenResponse.body.string()
                logger.info { "Response: $lijstVerdelenResponseBody" }
                lijstVerdelenResponse.code shouldBe HTTP_NO_CONTENT
                // the backend process is asynchronous, so we need to wait a bit until the zaken are assigned
                eventually(10.seconds) {
                    zakenVerdelenWebsocketListener.messagesReceived.size shouldBe 1
                    with(JSONObject(zakenVerdelenWebsocketListener.messagesReceived[0])) {
                        getString("opcode") shouldBe "UPDATED"
                        getString("objectType") shouldBe "ZAKEN_VERDELEN"
                        getJSONObject("objectId").getString("resource") shouldBe uniqueResourceId.toString()
                    }
                    zacClient.retrieveZaak(zaakWithDomainUuid).use { response ->
                        response.code shouldBe HTTP_OK
                        with(JSONObject(response.body.string())) {
                            getJSONObject("groep").getString("id") shouldBe TEST_GROUP_A_ID
                        }
                    }
                }
            }
        }
    }

    Given("A zaak has not been assigned to the currently logged in user and a logged-in behandelaar") {
        authenticate(username = TEST_BEHANDELAAR_1_USERNAME, password = TEST_BEHANDELAAR_1_PASSWORD)
        When("the 'assign to logged-in user from list' endpoint is called for the zaak") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaken/lijst/toekennen/mij",
                requestBodyAsString = """{
                    "zaakUUID":"$zaakProductaanvraag1Uuid",
                    "groepId":"$TEST_GROUP_BEHANDELAARS_ID",
                    "reden":"fakeAssignToMeFromListReason"
                }
                """.trimIndent()
            )
            Then(
                "the response should be a 200 HTTP response with zaak data and the zaak should be assigned to the user"
            ) {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("uuid", zaakProductaanvraag1Uuid.toString())
                    JSONObject(this).getJSONObject("behandelaar").apply {
                        getString("id") shouldBe TEST_BEHANDELAAR_1_USERNAME
                        getString("naam") shouldBe TEST_BEHANDELAAR_1_NAME
                    }
                }
                with(zacClient.retrieveZaak(zaakProductaanvraag1Uuid)) {
                    code shouldBe HTTP_OK
                    JSONObject(body.string()).apply {
                        getJSONObject("behandelaar").apply {
                            getString("id") shouldBe TEST_BEHANDELAAR_1_USERNAME
                            getString("naam") shouldBe TEST_BEHANDELAAR_1_NAME
                        }
                    }
                }
            }
        }
    }

    Given(
        """Zaken have been assigned and a websocket subscription has been created to listen
            for a 'zaken vrijgeven' screen event which will be sent by the asynchronous 'assign zaken from list' job
            and a logged-in coordinator"""
    ) {
        authenticate(username = TEST_COORDINATOR_1_USERNAME, password = TEST_COORDINATOR_1_PASSWORD)
        val uniqueResourceId = UUID.randomUUID()
        val websocketListener = WebSocketTestListener(
            textToBeSentOnOpen = """
                {
                    "subscriptionType":"CREATE",
                    "event":{
                        "opcode":"UPDATED",
                        "objectType":"$SCREEN_EVENT_TYPE_ZAKEN_VRIJGEVEN",
                        "objectId":{
                            "resource":"$uniqueResourceId"
                        },
                    "_key":"ANY;$SCREEN_EVENT_TYPE_ZAKEN_VRIJGEVEN;$uniqueResourceId"
                    }
                }
                """
        )
        itestHttpClient.connectNewWebSocket(
            url = ItestConfiguration.ZAC_WEBSOCKET_BASE_URI,
            webSocketListener = websocketListener
        )
        When("the 'lijst vrijgeven' endpoint is called for the zaken") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaken/lijst/vrijgeven",
                requestBodyAsString = """{
                    "uuids":["$zaakProductaanvraag1Uuid", "$zaak2UUID"],
                    "reden":"fakeLijstVrijgevenReason",
                    "screenEventResourceId":"$uniqueResourceId"
                }"""
            )
            Then(
                """the response should be a 204 HTTP response, eventually a screen event of type 'zaken vrijgeven'
                    should be received by the websocket listener and the zaak should be released from the user
                    but should still be assigned to the group """
            ) {
                val responseBody = response.body.string()
                logger.info { "### Response: $responseBody" }
                response.code shouldBe HTTP_NO_CONTENT
                // the backend process is asynchronous, so we need to wait a bit until the zaken are assigned
                eventually(20.seconds) {
                    websocketListener.messagesReceived.size shouldBe 1
                    with(zacClient.retrieveZaak(zaakProductaanvraag1Uuid)) {
                        code shouldBe HTTP_OK
                        JSONObject(body.string()).apply {
                            getJSONObject("groep").apply {
                                getString("id") shouldBe TEST_GROUP_BEHANDELAARS_ID
                                getString("naam") shouldBe TEST_GROUP_BEHANDELAARS_DESCRIPTION
                            }
                            has("behandelaar") shouldBe false
                        }
                    }
                    with(zacClient.retrieveZaak(zaak2UUID)) {
                        code shouldBe HTTP_OK
                        JSONObject(body.string()).apply {
                            getJSONObject("groep").apply {
                                getString("id") shouldBe TEST_GROUP_A_ID
                                getString("naam") shouldBe TEST_GROUP_A_DESCRIPTION
                            }
                            has("behandelaar") shouldBe false
                        }
                    }
                }
            }
        }
    }
})
