/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldNotContainJsonKey
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration
import nl.info.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFICATION_TYPE_BSN
import nl.info.zac.itest.config.ItestConfiguration.BETROKKENE_ROL_TOEVOEGEN_REDEN
import nl.info.zac.itest.config.ItestConfiguration.BETROKKENE_TYPE_NATUURLIJK_PERSOON
import nl.info.zac.itest.config.ItestConfiguration.BRON_ORGANISATIE
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_2
import nl.info.zac.itest.config.ItestConfiguration.DATE_2020_01_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_2020_01_15
import nl.info.zac.itest.config.ItestConfiguration.DATE_2023_09_21
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2020_01_01
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_NO_CONTENT
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_NAME_BELANGHEBBENDE
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_NAME_MEDEAANVRAGER
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_UUID_BELANGHEBBENDE
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_UUID_MEDEAANVRAGER
import nl.info.zac.itest.config.ItestConfiguration.SCREEN_EVENT_TYPE_ZAKEN_VERDELEN
import nl.info.zac.itest.config.ItestConfiguration.SCREEN_EVENT_TYPE_ZAKEN_VRIJGEVEN
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_INFORMATIE_OBJECT_TYPE_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_2_ID
import nl.info.zac.itest.config.ItestConfiguration.VERANTWOORDELIJKE_ORGANISATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_REFERENTIEPROCES
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_DESCRIPTION_2
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_EXPLANATION_1
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_MANUAL_1_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Betrokkene1Uuid
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import nl.info.zac.itest.util.WebSocketTestListener
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import org.json.JSONArray
import org.json.JSONObject
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

    lateinit var zaak2UUID: UUID

    Given("ZAC Docker container is running and zaakafhandelparameters have been created") {
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
            Then("the response should be a 200 HTTP response with the created zaak") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
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
                      "identificatie": "$ZAAK_MANUAL_1_IDENTIFICATION",
                      "indicaties": [],
                      "isBesluittypeAanwezig": false,
                      "isDeelzaak": false,
                      "isHeropend": false,
                      "isHoofdzaak": false,
                      "isInIntakeFase": false,
                      "isOntvangstbevestigingVerstuurd": false,
                      "isOpen": true,
                      "isOpgeschort": false,
                      "isProcesGestuurd": false,
                      "isVerlengd": false,
                      "kenmerken": [],
                      "omschrijving": "$ZAAK_DESCRIPTION_2",
                      "rechten": {
                        "afbreken": true,
                        "behandelen": true,
                        "bekijkenZaakdata": true,
                        "creeerenDocument": true,
                        "heropenen": true,
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
                        "zaakIdentificatie": "$ZAAK_MANUAL_1_IDENTIFICATION",
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
                                "defaultFormulierDefinitie": "AANVULLENDE_INFORMATIE",
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
                                "defaultFormulierDefinitie": "ADVIES",
                                "id": "ADVIES_INTERN",
                                "naam": "Advies intern",
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
                          "defaultGroepId": "$TEST_GROUP_A_ID",
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
                              "formulierDefinitieId": "ADVIES",
                              "planItemDefinition": {
                                "defaultFormulierDefinitie": "ADVIES",
                                "id": "ADVIES_INTERN",
                                "naam": "Advies intern",
                                "type": "HUMAN_TASK"
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
                            }
                          ],
                          "id": 1,
                          "intakeMail": "BESCHIKBAAR_UIT",
                          "mailtemplateKoppelingen": [],
                          "productaanvraagtype": "productaanvraag-type-1",
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
                            "id": "dd2bcd87-ed7e-4b23-a8e3-ea7fe7ef00c6",
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
    Given("A zaak has been created") {
        When("the get zaak endpoint is called") {
            val response = zacClient.retrieveZaak(zaak2UUID)
            Then("the response should be a 200 HTTP response and contain the created zaak") {
                with(response) {
                    code shouldBe HTTP_STATUS_OK
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    with(JSONObject(responseBody)) {
                        getString("identificatie") shouldBe ZAAK_MANUAL_1_IDENTIFICATION
                        getJSONObject("zaaktype").getString("identificatie") shouldBe ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
                    }
                }
            }
        }
        When("the add betrokkene to zaak endpoint is called without a 'rol toelichting'") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/zaken/betrokkene",
                requestBodyAsString = """
                    {
                        "zaakUUID": "$zaak2UUID",
                        "roltypeUUID": "$ROLTYPE_UUID_BELANGHEBBENDE",
                        "betrokkeneIdentificatieType": "$BETROKKENE_IDENTIFICATION_TYPE_BSN",
                        "betrokkeneIdentificatie": "$TEST_PERSON_HENDRIKA_JANSE_BSN"
                    }
                """.trimIndent()
            )
            Then("the response should be a 200 OK HTTP response") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                with(responseBody) {
                    shouldContainJsonKeyValue("uuid", zaak2UUID.toString())
                }
            }
        }
        When(
            """"
            the 'update zaak' endpoint is called where the start and fatal dates are changed,
            and also the communication channel is changed
            """
        ) {
            val startDateNew = LocalDate.now()
            val fatalDateNew = startDateNew.plusDays(1)
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaak2UUID",
                requestBodyAsString = """
                    { 
                        "zaak": {
                            "startdatum": "$startDateNew",
                            "uiterlijkeEinddatumAfdoening": "$fatalDateNew",
                            "communicatiekanaal": "$COMMUNICATIEKANAAL_TEST_2"
                        },
                        "reden": "dummyReason"
                    }
                """.trimIndent()
            )
            Then("the response should be a 200 HTTP response with the changed zaak data") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("uuid", zaak2UUID.toString())
                    shouldContainJsonKeyValue("startdatum", startDateNew.toString())
                    shouldContainJsonKeyValue("uiterlijkeEinddatumAfdoening", fatalDateNew.toString())
                    shouldContainJsonKeyValue("communicatiekanaal", COMMUNICATIEKANAAL_TEST_2)
                }
            }
        }
        When("the 'assign to zaak' endpoint is called with a group") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/toekennen",
                requestBodyAsString = """
                    {
                        "zaakUUID": "$zaakProductaanvraag1Uuid",
                        "groepId": "$TEST_GROUP_A_ID",
                        "reden": "dummyReason"
                    }
                """.trimIndent()
            )
            Then("the group should be assigned to the zaak") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true

                with(responseBody) {
                    shouldContainJsonKeyValue("uuid", zaakProductaanvraag1Uuid.toString())
                    shouldContainJsonKey("groep")
                    JSONObject(this).getJSONObject("groep").apply {
                        getString("id") shouldBe TEST_GROUP_A_ID
                        getString("naam") shouldBe TEST_GROUP_A_DESCRIPTION
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
                        "roltoelichting": "dummyToelichting",
                        "betrokkeneIdentificatieType": "$BETROKKENE_IDENTIFICATION_TYPE_BSN",
                        "betrokkeneIdentificatie": "$TEST_PERSON_HENDRIKA_JANSE_BSN"
                    }
                """.trimIndent()
            )
            Then("the response should be a 200 OK HTTP response") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                with(responseBody) {
                    shouldContainJsonKeyValue("uuid", zaak2UUID.toString())
                }
            }
        }
        When("the 'update Zaak Locatie' endpoint is called with a valid location") {
            val longitude = Random.nextFloat()
            val latitude = Random.nextFloat()
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
                            "reden": "dummyReason"
                        }
                """.trimIndent()
            )
            Then("the response should be a 200 HTTP response with the changed zaak data") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
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

        When("the 'update Zaak Locatie' endpoint is called with a null value as location") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/$zaak2UUID/zaaklocatie",
                requestBodyAsString = """
                        {
                            "geometrie": null,
                            "reden": "dummyReason"
                        }
                """.trimIndent()
            )
            Then("the response should be a 200 HTTP response with the changed zaak data without zaakgeometrie") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                with(responseBody) {
                    shouldNotContainJsonKey("zaakgeometrie")
                }
            }
        }
    }
    Given("Betrokkenen have been added to a zaak") {
        When("the get betrokkene endpoint is called for a zaak") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaak2UUID/betrokkene",
            )
            Then("the response should be a 200 HTTP response with a list consisting of the betrokkenen") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                with(JSONArray(responseBody)) {
                    length() shouldBe 2
                    getJSONObject(0).apply {
                        getString("rolid") shouldNotBe null
                        getString("roltype") shouldBe ROLTYPE_NAME_MEDEAANVRAGER
                        getString("roltoelichting") shouldBe "dummyToelichting"
                        getString("type") shouldBe BETROKKENE_TYPE_NATUURLIJK_PERSOON
                        getString("identificatie") shouldBe TEST_PERSON_HENDRIKA_JANSE_BSN
                    }
                    getJSONObject(1).apply {
                        getString("rolid") shouldNotBe null
                        getString("roltype") shouldBe ROLTYPE_NAME_BELANGHEBBENDE
                        // if no toelichting was provided, the default value should be used
                        getString("roltoelichting") shouldBe BETROKKENE_ROL_TOEVOEGEN_REDEN
                        getString("type") shouldBe BETROKKENE_TYPE_NATUURLIJK_PERSOON
                        getString("identificatie") shouldBe TEST_PERSON_HENDRIKA_JANSE_BSN
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
            job
        """
    ) {
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
            """the 'assign zaken from list' endpoint is called to start an asynchronous process to assign the two zaken 
                     to a group and a user using the unique resource ID that was used to create the websocket subscription"""
        ) {
            val lijstVerdelenResponse = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaken/lijst/verdelen",
                requestBodyAsString = "{\n" +
                    "\"uuids\":[\"$zaakProductaanvraag1Uuid\", \"$zaak2UUID\"],\n" +
                    "\"groepId\":\"$TEST_GROUP_A_ID\",\n" +
                    "\"behandelaarGebruikersnaam\":\"$TEST_USER_2_ID\",\n" +
                    "\"reden\":\"dummyLijstVerdelenReason\",\n" +
                    "\"screenEventResourceId\":\"$uniqueResourceId\"\n" +
                    "}"
            )
            Then(
                """the response should be a 204 HTTP response and eventually a screen event of type 'zaken verdelen'
                    should be received by the websocket listener and the two zaken should be assigned correctly"""
            ) {
                val lijstVerdelenResponseBody = lijstVerdelenResponse.body!!.string()
                logger.info { "Response: $lijstVerdelenResponseBody" }
                lijstVerdelenResponse.code shouldBe HTTP_STATUS_NO_CONTENT
                // the backend process is asynchronous, so we need to wait a bit until the zaken are assigned
                eventually(10.seconds) {
                    zakenVerdelenWebsocketListener.messagesReceived.size shouldBe 1
                    with(JSONObject(zakenVerdelenWebsocketListener.messagesReceived[0])) {
                        getString("opcode") shouldBe "UPDATED"
                        getString("objectType") shouldBe "ZAKEN_VERDELEN"
                        getJSONObject("objectId").getString("resource") shouldBe uniqueResourceId.toString()
                    }
                    zacClient.retrieveZaak(zaakProductaanvraag1Uuid).use { response ->
                        response.code shouldBe HTTP_STATUS_OK
                        with(JSONObject(response.body!!.string())) {
                            getJSONObject("groep").getString("id") shouldBe TEST_GROUP_A_ID
                            getJSONObject("behandelaar").getString("id") shouldBe TEST_USER_2_ID
                        }
                    }
                    zacClient.retrieveZaak(zaak2UUID).use { response ->
                        response.code shouldBe HTTP_STATUS_OK
                        with(JSONObject(response.body!!.string())) {
                            getJSONObject("groep").getString("id") shouldBe TEST_GROUP_A_ID
                            getJSONObject("behandelaar").getString("id") shouldBe TEST_USER_2_ID
                        }
                    }
                }
            }
        }
    }
    Given("A zaak has not been assigned to the currently logged in user") {
        When("the 'assign to logged-in user from list' endpoint is called for the zaak") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaken/lijst/toekennen/mij",
                requestBodyAsString = "{\n" +
                    "\"zaakUUID\":\"$zaakProductaanvraag1Uuid\",\n" +
                    "\"behandelaarGebruikersnaam\":\"$TEST_USER_1_USERNAME\",\n" +
                    "\"reden\":\"dummyAssignToMeFromListReason\"\n" +
                    "}"
            )
            Then(
                "the response should be a 200 HTTP response with zaak data and the zaak should be assigned to the user"
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("uuid", zaakProductaanvraag1Uuid.toString())
                    JSONObject(this).getJSONObject("behandelaar").apply {
                        getString("id") shouldBe TEST_USER_1_USERNAME
                        getString("naam") shouldBe TEST_USER_1_NAME
                    }
                }
                with(zacClient.retrieveZaak(zaakProductaanvraag1Uuid)) {
                    code shouldBe HTTP_STATUS_OK
                    JSONObject(body!!.string()).apply {
                        getJSONObject("behandelaar").apply {
                            getString("id") shouldBe TEST_USER_1_USERNAME
                            getString("naam") shouldBe TEST_USER_1_NAME
                        }
                    }
                }
            }
        }
    }
    Given(
        """Zaken have been assigned and a websocket subscription has been created to listen
                 for a 'zaken vrijgeven' screen event which will be sent by the asynchronous 'assign zaken from list' job"""
    ) {
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
                requestBodyAsString = """
                    {
                        "uuids":["$zaakProductaanvraag1Uuid", "$zaak2UUID"],
                        "reden":"dummyLijstVrijgevenReason",
                        "screenEventResourceId":"$uniqueResourceId"
                    }
                    """
            )
            Then(
                """the response should be a 204 HTTP response and eventually a screen event of type 'zaken vrijgeven'
                                      should be received by the websocker listener and the zaak should be released from the user
                    but should still be assigned to the group """
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_NO_CONTENT
                // the backend process is asynchronous, so we need to wait a bit until the zaken are assigned
                eventually(10.seconds) {
                    websocketListener.messagesReceived.size shouldBe 1
                    with(zacClient.retrieveZaak(zaakProductaanvraag1Uuid)) {
                        code shouldBe HTTP_STATUS_OK
                        JSONObject(body!!.string()).apply {
                            getJSONObject("groep").apply {
                                getString("id") shouldBe TEST_GROUP_A_ID
                                getString("naam") shouldBe TEST_GROUP_A_DESCRIPTION
                            }
                            has("behandelaar") shouldBe false
                        }
                    }
                    with(zacClient.retrieveZaak(zaak2UUID)) {
                        code shouldBe HTTP_STATUS_OK
                        JSONObject(body!!.string()).apply {
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
