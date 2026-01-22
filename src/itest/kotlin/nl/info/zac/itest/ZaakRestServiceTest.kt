/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.json.shouldNotContainJsonKey
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_2
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.COORDINATOR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration
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
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2020_01_01
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.FEATURE_FLAG_PABC_INTEGRATION
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_NAME_BELANGHEBBENDE
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_NAME_MEDEAANVRAGER
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_UUID_BELANGHEBBENDE
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_UUID_MEDEAANVRAGER
import nl.info.zac.itest.config.ItestConfiguration.SCREEN_EVENT_TYPE_ZAKEN_VERDELEN
import nl.info.zac.itest.config.ItestConfiguration.SCREEN_EVENT_TYPE_ZAKEN_VRIJGEVEN
import nl.info.zac.itest.config.ItestConfiguration.TEST_INFORMATIE_OBJECT_TYPE_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_NUMMER_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGINGSNUMMER_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.info.zac.itest.config.ItestConfiguration.VERANTWOORDELIJKE_ORGANISATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_1_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_REFERENTIEPROCES
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_DESCRIPTION_1
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_DESCRIPTION_2
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_EXPLANATION_1
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_DOMAIN_TEST_1
import nl.info.zac.itest.util.WebSocketTestListener
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

const val ZAAK_OMSCHRIJVING_MAX_LENGTH = 80

@Suppress("LargeClass")
class ZaakRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val logger = KotlinLogging.logger {}
    val longitude = Random.nextFloat()
    val latitude = Random.nextFloat()
    val startDateNew = LocalDate.now()
    val fatalDateNew = startDateNew.plusDays(1)
    lateinit var zaak1UUID: UUID
    lateinit var zaak2UUID: UUID

    Context("Listing zaaktypes for creating zaken") {
        Given(
            """
            Zaakafhandelparameters is created and a user with access to all zaaktypes in all domains is logged-in
            """.trimIndent()
        ) {
            When("zaak types are listed") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaaktypes-for-creation",
                    testUser = BEHEERDER_ELK_ZAAKTYPE
                )
                lateinit var responseBody: String

                Then("the response should be a 200 HTTP response") {
                    responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                }

                And("the response body should contain the zaaktypes in all domains") {
                    responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    [
                      {
                        "doel": "$ZAAKTYPE_TEST_1_DESCRIPTION",
                        "identificatie": "$ZAAKTYPE_TEST_1_IDENTIFICATIE",
                        "omschrijving": "$ZAAKTYPE_TEST_1_DESCRIPTION"
                      },
                      {
                        "doel": "$ZAAKTYPE_BPMN_TEST_1_DESCRIPTION",
                        "identificatie": "$ZAAKTYPE_BPMN_TEST_1_IDENTIFICATIE",
                        "omschrijving": "$ZAAKTYPE_BPMN_TEST_1_DESCRIPTION"
                      },
                      {
                        "doel": "$ZAAKTYPE_TEST_2_DESCRIPTION",
                        "identificatie": "$ZAAKTYPE_TEST_2_IDENTIFICATIE",
                        "omschrijving": "$ZAAKTYPE_TEST_2_DESCRIPTION"
                      },
                      {
                        "doel": "$ZAAKTYPE_TEST_3_DESCRIPTION",
                        "identificatie": "$ZAAKTYPE_TEST_3_IDENTIFICATIE",
                        "omschrijving": "$ZAAKTYPE_TEST_3_DESCRIPTION"
                      },
                      {
                        "doel": "$ZAAKTYPE_BPMN_TEST_2_DESCRIPTION",
                        "identificatie": "$ZAAKTYPE_BPMN_TEST_2_IDENTIFICATIE",
                        "omschrijving": "$ZAAKTYPE_BPMN_TEST_2_DESCRIPTION"
                      }
                    ]
                    """.trimIndent()
                }
            }
        }

        Given(
            """
            ZAC Docker container is running and zaaktypeCmmnConfiguration have been created
            and a behandelaar user that is authorised for zaaktypes in domain test 2 only is logged-in
            """.trimIndent()
        ) {
            lateinit var responseBody: String

            When("zaak types for creation are listed") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaaktypes-for-creation",
                    testUser = BEHANDELAAR_DOMAIN_TEST_2
                )
                Then("the response should be a 200 HTTP response") {
                    responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                }
                And("the response body should contain only the zaaktypes for which the user is authorized") {
                    // In the old IAM architecture we always return BPMN zaaktypes in the 'zaaktypes for creation' list
                    val nonPABCPayload = if (FEATURE_FLAG_PABC_INTEGRATION) {
                        ""
                    } else {
                        """
                         , {
                             "doel": "$ZAAKTYPE_BPMN_TEST_1_DESCRIPTION",
                             "identificatie": "$ZAAKTYPE_BPMN_TEST_1_IDENTIFICATIE",
                             "omschrijving": "$ZAAKTYPE_BPMN_TEST_1_DESCRIPTION"
                           },
                           {
                             "doel": "$ZAAKTYPE_BPMN_TEST_2_DESCRIPTION",
                             "identificatie": "$ZAAKTYPE_BPMN_TEST_2_IDENTIFICATIE",
                             "omschrijving": "$ZAAKTYPE_BPMN_TEST_2_DESCRIPTION"
                           }
                        """.trimIndent()
                    }

                    responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    [
                      {
                        "doel": "$ZAAKTYPE_TEST_1_DESCRIPTION",
                        "identificatie": "$ZAAKTYPE_TEST_1_IDENTIFICATIE",
                        "omschrijving": "$ZAAKTYPE_TEST_1_DESCRIPTION"
                      }$nonPABCPayload
                    ]
                    """.trimIndent()
                }
            }
        }
    }

    Context("Creating and retrieving zaken") {
        Given(
            """
            zaaktype CMMN configuration has been created for zaaktype test 3 
            and a behandelaar authorised for this zaaktype is logged in
            """
        ) {
            lateinit var responseBody: String

            When("the create zaak endpoint is called and the user has permissions for the zaaktype used") {
                val response = zacClient.createZaak(
                    zaakTypeUUID = ZAAKTYPE_TEST_3_UUID,
                    groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
                    groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
                    startDate = DATE_TIME_2020_01_01,
                    communicatiekanaal = COMMUNICATIEKANAAL_TEST_1,
                    vertrouwelijkheidaanduiding = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR,
                    description = ZAAK_DESCRIPTION_2,
                    toelichting = ZAAK_EXPLANATION_1,
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )

                Then("the response should be a 200 HTTP response") {
                    responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                }

                And(
                    """
                the response should contain the created zaak with the 'bekijkenZaakdata' and 'heropenen' permissions
                set to false since these actions are not allowed for the 'behandelaar' role
                """
                ) {
                    // Note that we do not check the contents of the `zaakafhandelparameters` field below, in order to make
                    // this test more manageable.
                    // Also, the `zaakafhandelparameters` are already tested in other integration tests.
                    responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "besluiten": [],
                      "bronorganisatie": "$BRON_ORGANISATIE",
                      "communicatiekanaal": "$COMMUNICATIEKANAAL_TEST_1",
                      "eerdereOpschorting": false,
                      "gerelateerdeZaken": [],
                      "groep": {
                        "id": "${BEHANDELAARS_DOMAIN_TEST_1.name}",
                        "naam": "${BEHANDELAARS_DOMAIN_TEST_1.description}"
                      },
                      "heeftOntvangstbevestigingVerstuurd": false,
                      "indicaties": ["ONTVANGSTBEVESTIGING_NIET_VERSTUURD"],
                      "isBesluittypeAanwezig": false,
                      "isDeelzaak": false,
                      "isHeropend": false,
                      "isHoofdzaak": false,
                      "isInIntakeFase": false,
                      "isOpen": true,
                      "isOpgeschort": false,
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
                        "wijzigenDoorlooptijd": true,
                        "wijzigenLocatie": true
                      },
                      "registratiedatum": "${LocalDate.now()}",
                      "startdatum": "$DATE_2020_01_01",
                      "toelichting": "$ZAAK_EXPLANATION_1",
                      "uiterlijkeEinddatumAfdoening": "$DATE_2020_01_15",
                      "verantwoordelijkeOrganisatie": "$VERANTWOORDELIJKE_ORGANISATIE",
                      "vertrouwelijkheidaanduiding": "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR",
                      "zaakdata": {
                        "initiator": null,
                        "zaaktypeUUID": "$ZAAKTYPE_TEST_3_UUID",
                        "zaaktypeOmschrijving": "$ZAAKTYPE_TEST_3_DESCRIPTION"
                      },
                      "zaaktype": {
                        "beginGeldigheid": "$DATE_2023_09_21",
                        "doel": "$ZAAKTYPE_TEST_3_DESCRIPTION",
                        "identificatie": "$ZAAKTYPE_TEST_3_IDENTIFICATIE",
                        "informatieobjecttypes": [
                          "$TEST_INFORMATIE_OBJECT_TYPE_1_UUID",
                          "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                        ],
                        "nuGeldig": true,
                        "omschrijving": "$ZAAKTYPE_TEST_3_DESCRIPTION",
                        "opschortingMogelijk": false,
                        "referentieproces": "$ZAAKTYPE_TEST_3_REFERENTIEPROCES",
                        "servicenorm": false,
                        "uuid": "$ZAAKTYPE_TEST_3_UUID",
                        "verlengingMogelijk": false,
                        "versiedatum": "$DATE_2023_09_21",
                        "vertrouwelijkheidaanduiding": "openbaar",                    
                        "zaaktypeRelaties": []
                      }
                    }
                    """.trimIndent()
                    zaak2UUID = JSONObject(responseBody).getString("uuid").let(UUID::fromString)
                }
            }

            When("the get zaak endpoint is called as a behandelaar") {
                val response = zacClient.retrieveZaak(
                    zaakUUID = zaak2UUID,
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )

                Then(
                    """the response should be a 200 HTTP response and contain the created zaak,
                     and the permissions should be those of a behandelaar (e.g. 'bekijkenZaakdata' should be false)"""
                ) {
                    with(response) {
                        code shouldBe HTTP_OK
                        val responseBody = response.bodyAsString
                        logger.info { "Response: $responseBody" }
                        with(JSONObject(responseBody)) {
                            getString("identificatie") shouldNotBe null
                            getJSONObject("zaaktype").getString("identificatie") shouldBe ZAAKTYPE_TEST_3_IDENTIFICATIE
                            getJSONObject("rechten").toString() shouldEqualJsonIgnoringOrderAndExtraneousFields """
                             {
                              "versturenOntvangstbevestiging" : true,
                              "wijzigenDoorlooptijd" : true,
                              "heropenen" : false,
                              "toevoegenBetrokkeneBedrijf" : true,
                              "verwijderenInitiator" : true,
                              "lezen" : true,
                              "wijzigen" : true,
                              "toevoegenInitiatorPersoon" : true,
                              "versturenEmail" : true,
                              "verwijderenBetrokkene" : true,
                              "toevoegenBetrokkenePersoon" : true,
                              "creerenDocument" : true,
                              "toevoegenBagObject" : true,
                              "bekijkenZaakdata" : false,
                              "wijzigenLocatie" : true,
                              "toevoegenInitiatorBedrijf" : true,
                              "afbreken" : true,
                              "behandelen" : true,
                              "toekennen" : true
                            }
                            """.trimIndent()
                        }
                    }
                }
            }

            When("the get zaak endpoint is called as a beheerder") {
                val response = zacClient.retrieveZaak(
                    zaakUUID = zaak2UUID,
                    testUser = BEHEERDER_ELK_ZAAKTYPE)

                Then(
                    """the response should be a 200 HTTP response and contain the created zaak,
                     and the permissions should be those of a beheerder (e.g. 'bekijkenZaakdata' should be true)"""
                ) {
                    with(response) {
                        code shouldBe HTTP_OK
                        val responseBody = response.bodyAsString
                        logger.info { "Response: $responseBody" }
                        with(JSONObject(responseBody)) {
                            getJSONObject("zaaktype").getString("identificatie") shouldBe ZAAKTYPE_TEST_3_IDENTIFICATIE
                            getJSONObject("rechten").toString() shouldEqualJsonIgnoringOrderAndExtraneousFields """
                            {
                              "versturenOntvangstbevestiging" : true,
                              "wijzigenDoorlooptijd" : true,
                              "heropenen" : true,
                              "toevoegenBetrokkeneBedrijf" : true,
                              "verwijderenInitiator" : true,
                              "lezen" : true,
                              "wijzigen" : true,
                              "toevoegenInitiatorPersoon" : true,
                              "versturenEmail" : true,
                              "verwijderenBetrokkene" : true,
                              "toevoegenBetrokkenePersoon" : true,
                              "creerenDocument" : true,
                              "toevoegenBagObject" : true,
                              "bekijkenZaakdata" : true,
                              "wijzigenLocatie" : true,
                              "toevoegenInitiatorBedrijf" : true,
                              "afbreken" : true,
                              "behandelen" : true,
                              "toekennen" : true
                            }
                            """.trimIndent()
                        }
                    }
                }
            }
        }
    }

    Context("Updating zaken and adding betrokkenen") {
        Given("A zaak has been created and a behandelaar authorised for this zaaktype is logged in") {
            When(
                "the add betrokkene to zaak endpoint is called with a natuurlijk persoon without a 'rol toelichting'"
            ) {
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
                    """.trimIndent(),
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )
                Then("the response should be a 200 OK HTTP response") {
                    response.code shouldBe HTTP_OK
                    val responseBody = response.bodyAsString
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
                    """.trimIndent(),
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )

                Then("the response should be a 200 HTTP response with the changed zaak data") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    with(responseBody) {
                        shouldContainJsonKeyValue("uuid", zaak2UUID.toString())
                        shouldContainJsonKeyValue("communicatiekanaal", COMMUNICATIEKANAAL_TEST_2)
                        shouldContainJsonKeyValue("omschrijving", ZAAK_DESCRIPTION_1)
                    }
                }
            }

            When(
                """
            the 'update zaak' endpoint is called with a description field that is longer than allowed
            """
            ) {
                val descriptionThatIsTooLong = "x".repeat(ZAAK_OMSCHRIJVING_MAX_LENGTH + 1)
                val response = itestHttpClient.performPatchRequest(
                    url = "$ZAC_API_URI/zaken/zaak/$zaak2UUID",
                    requestBodyAsString = """
                    { 
                        "zaak": {
                            "communicatiekanaal": "$COMMUNICATIEKANAAL_TEST_2",
                            "omschrijving": "$descriptionThatIsTooLong"
                        },
                        "reden": "fakeReason"
                    }
                    """.trimIndent(),
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )

                Then("the response should be a 400 HTTP response with the validation error message") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_BAD_REQUEST
                    response.bodyAsString shouldEqualJson """
                   {
                      "classViolations" : [ ],
                      "parameterViolations" : [ {
                        "constraintType" : "PARAMETER",
                        "message" : "size must be between 0 and $ZAAK_OMSCHRIJVING_MAX_LENGTH",
                        "path" : "updateZaak.arg1.zaak.omschrijving",
                        "value" : "$descriptionThatIsTooLong"
                      } ],
                      "propertyViolations" : [ ],
                      "returnValueViolations" : [ ]
                    }
                    """.trimIndent()
                }
            }

            When("the 'assign to zaak' endpoint is called with a group") {
                val response = itestHttpClient.performPatchRequest(
                    url = "$ZAC_API_URI/zaken/toekennen",
                    requestBodyAsString = """
                    {
                        "zaakUUID": "$zaak2UUID",
                        "groepId": "${BEHANDELAARS_DOMAIN_TEST_1.name}",
                        "reden": "fakeReason"
                    }
                    """.trimIndent(),
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )

                Then("the zaak should be assigned to the group") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK

                    with(responseBody) {
                        shouldContainJsonKeyValue("uuid", zaak2UUID.toString())
                        shouldContainJsonKey("groep")
                        JSONObject(this).getJSONObject("groep").apply {
                            getString("id") shouldBe BEHANDELAARS_DOMAIN_TEST_1.name
                            getString("naam") shouldBe BEHANDELAARS_DOMAIN_TEST_1.description
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
                    """.trimIndent(),
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )

                Then("the response should be a 200 OK HTTP response") {
                    response.code shouldBe HTTP_OK
                    val responseBody = response.bodyAsString
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
                    """.trimIndent(),
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )

                Then("the response should be a 200 HTTP response with the changed zaak data") {
                    val responseBody = response.bodyAsString
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
                    """.trimIndent(),
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )

                Then(
                    "the response should be a 200 HTTP response with only the changed zaak description and no other changes"
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "besluiten": [],
                      "bronorganisatie": "$BRON_ORGANISATIE",
                      "communicatiekanaal": "$COMMUNICATIEKANAAL_TEST_2",
                      "gerelateerdeZaken": [],
                      "groep": {
                        "id": "${BEHANDELAARS_DOMAIN_TEST_1.name}",
                        "naam": "${BEHANDELAARS_DOMAIN_TEST_1.description}"
                      },
                      "indicaties": ["ONTVANGSTBEVESTIGING_NIET_VERSTUURD"],
                      "isBesluittypeAanwezig": false,
                      "isDeelzaak": false,
                      "isHeropend": false,
                      "isHoofdzaak": false,
                      "isInIntakeFase": true,
                      "isOpen": true,
                      "isOpgeschort": false,
                      "eerdereOpschorting": false,
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
                    """.trimIndent(),
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )
                Then("the response should be a 200 HTTP response with the changed zaak data without zaakgeometrie") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    with(responseBody) {
                        shouldNotContainJsonKey("zaakgeometrie")
                    }
                }
            }

            When("an initiator is added to the zaak with a vestigingsnummer") {
                val vestigingsnummer = TEST_KVK_VESTIGINGSNUMMER_1
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
                    """.trimIndent(),
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )
                Then("the response should be a 200 HTTP response and the initiator should be added") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    with(JSONObject(responseBody).getJSONObject("initiatorIdentificatie").toString()) {
                        shouldContainJsonKeyValue("type", "VN")
                        shouldContainJsonKeyValue("vestigingsnummer", vestigingsnummer)
                    }
                }
            }

            When("the get betrokkene endpoint is called for a zaak") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaak/$zaak2UUID/betrokkene",
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )
                Then("the response should be a 200 HTTP response with a list consisting of the betrokkenen") {
                    response.code shouldBe HTTP_OK
                    val responseBody = response.bodyAsString
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
                        }
                    }
                }
            }
        }
    }

    Context("Assigning zaken from the list") {
        Given(
            """
            Two zaken have been created and a websocket subscriptions has been created to listen for 'zaken verdelen' 
            screen events which will be sent by the asynchronous 'assign zaken from list',
            and a coordinator authorized for the zaaktypes of these zaken is logged in
        """
        ) {
            zaak1UUID = zaakHelper.createZaak(
                zaaktypeUuid = ZAAKTYPE_TEST_3_UUID,
                testUser = COORDINATOR_DOMAIN_TEST_1
            ).second
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
            val websocket = itestHttpClient.connectNewWebSocket(
                url = ItestConfiguration.ZAC_WEBSOCKET_BASE_URI,
                webSocketListener = zakenVerdelenWebsocketListener
            )
            logger.info { "WebSocket created: '$websocket'" }

            When(
                """the 'assign zaken from list' endpoint is called to start an asynchronous process 
                to assign the two zaken to a group and a user using the unique resource ID 
                that was used to create the websocket subscription"""
            ) {
                val lijstVerdelenResponse = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zaken/lijst/verdelen",
                    requestBodyAsString = """
                    {
                        "uuids": [ "$zaak1UUID", "$zaak2UUID" ],
                        "groepId": "${BEHANDELAARS_DOMAIN_TEST_1.name}",
                        "behandelaarGebruikersnaam": "${BEHANDELAAR_DOMAIN_TEST_1.username}",
                        "reden": "fakeLijstVerdelenReason",
                        "screenEventResourceId": "$uniqueResourceId"
                    }
                    """.trimIndent(),
                    testUser = COORDINATOR_DOMAIN_TEST_1
                )

                Then(
                    """the response should be a 204 HTTP response and eventually a screen event of type 'zaken verdelen'
                    should be received by the websocket listener and the two zaken should be assigned correctly"""
                ) {
                    lijstVerdelenResponse.code shouldBe HTTP_NO_CONTENT
                    // the backend process is asynchronous, so we need to wait a bit until the zaken are assigned
                    eventually(10.seconds) {
                        zakenVerdelenWebsocketListener.messagesReceived.size shouldBe 1
                        with(JSONObject(zakenVerdelenWebsocketListener.messagesReceived[0])) {
                            getString("opcode") shouldBe "UPDATED"
                            getString("objectType") shouldBe "ZAKEN_VERDELEN"
                            getJSONObject("objectId").getString("resource") shouldBe uniqueResourceId.toString()
                        }
                        zacClient.retrieveZaak(
                            zaakUUID = zaak1UUID,
                            testUser = RAADPLEGER_DOMAIN_TEST_1
                        ).let { response ->
                            response.code shouldBe HTTP_OK
                            with(JSONObject(response.bodyAsString)) {
                                getJSONObject("groep").getString("id") shouldBe BEHANDELAARS_DOMAIN_TEST_1.name
                                getJSONObject("behandelaar").getString("id") shouldBe BEHANDELAAR_DOMAIN_TEST_1.username
                            }
                        }
                        zacClient.retrieveZaak(
                            zaakUUID = zaak2UUID,
                            testUser = RAADPLEGER_DOMAIN_TEST_1
                        ).let { response ->
                            response.code shouldBe HTTP_OK
                            with(JSONObject(response.bodyAsString)) {
                                getJSONObject("groep").getString("id") shouldBe BEHANDELAARS_DOMAIN_TEST_1.name
                                getJSONObject("behandelaar").getString("id") shouldBe BEHANDELAAR_DOMAIN_TEST_1.username
                            }
                        }
                    }
                }
            }
        }

        Given("A zaak exists and a websocket subscription has been created and a logged-in coordinator") {
            val (_, zaakUuid) = zaakHelper.createZaak(
                zaaktypeUuid = ZAAKTYPE_TEST_2_UUID,
                testUser = COORDINATOR_DOMAIN_TEST_1
            )
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
                to assign the zaak to a group which is not authorised for the zaaktype of the zaak"""
            ) {
                val lijstVerdelenResponse = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zaken/lijst/verdelen",
                    requestBodyAsString = """{
                        "uuids": [ "$zaakUuid" ],
                        "groepId": "${BEHANDELAARS_DOMAIN_TEST_1.name}",
                        "reden": "fakeLijstVerdelenReason",
                        "screenEventResourceId": "$uniqueResourceId"                 
                    }""".trimIndent(),
                    testUser = COORDINATOR_DOMAIN_TEST_1
                )
                Then(
                    """a 204 HTTP response and a screen event of type 'zaken verdelen'
                    should be received by the websocket listener and the zaak should not be reassigned"""
                ) {
                    val lijstVerdelenResponseBody = lijstVerdelenResponse.bodyAsString
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
                        zacClient.retrieveZaak(
                            zaakUUID = zaakUuid,
                            testUser = RAADPLEGER_DOMAIN_TEST_1
                        ).let { response ->
                            response.code shouldBe HTTP_OK
                            with(JSONObject(response.bodyAsString)) {
                                getJSONObject("groep").getString("id") shouldBe BEHANDELAARS_DOMAIN_TEST_1.name
                            }
                        }
                    }
                }
            }
        }
    }

    Context("Assigning a zaak to the logged-in user from the list") {
        Given(
            """
        A zaak which has not been assigned to the currently logged in user, 
        and a behandelaar authorised for this zaaktype is logged in
        """
        ) {
            val (_, zaakUuid) = zaakHelper.createZaak(
                zaaktypeUuid = ZAAKTYPE_TEST_2_UUID,
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            When("the 'assign to logged-in user from list' endpoint is called for the zaak") {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zaken/lijst/toekennen/mij",
                    requestBodyAsString = """{
                        "zaakUUID": "$zaakUuid",
                        "groepId": "${BEHANDELAARS_DOMAIN_TEST_1.name}",
                        "reden": "fakeAssignToMeFromListReason"
                    }""".trimIndent(),
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )
                Then(
                    "the response should be a 200 HTTP response with the expected zaak data"
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    with(responseBody) {
                        shouldContainJsonKeyValue("uuid", zaakUuid.toString())
                        JSONObject(this).getJSONObject("behandelaar").apply {
                            getString("id") shouldBe BEHANDELAAR_DOMAIN_TEST_1.username
                            getString("naam") shouldBe BEHANDELAAR_DOMAIN_TEST_1.displayName
                        }
                    }
                }
                And("the zaak should be assigned to the user") {
                    with(zacClient.retrieveZaak(
                        zaakUUID = zaakUuid,
                        testUser = RAADPLEGER_DOMAIN_TEST_1
                    )) {
                        code shouldBe HTTP_OK
                        JSONObject(bodyAsString).apply {
                            getJSONObject("behandelaar").apply {
                                getString("id") shouldBe BEHANDELAAR_DOMAIN_TEST_1.username
                                getString("naam") shouldBe BEHANDELAAR_DOMAIN_TEST_1.displayName
                            }
                        }
                    }
                }
            }
        }
    }

    Context("Releasing zaken from the list") {
        Given(
            """Two zaken that have been assigned and a websocket subscription has been created to listen
            for a 'zaken vrijgeven' screen event which will be sent by the asynchronous 'assign zaken from list' job
            and a coordinator authorized for the zaaktypes of these zaken is logged in"""
        ) {
            val uniqueResourceId = UUID.randomUUID()
            val websocketListener = WebSocketTestListener(
                textToBeSentOnOpen = """
                {
                    "subscriptionType": "CREATE",
                    "event": {
                        "opcode": "UPDATED",
                        "objectType": "$SCREEN_EVENT_TYPE_ZAKEN_VRIJGEVEN",
                        "objectId": {
                            "resource": "$uniqueResourceId"
                        },
                    "_key": "ANY;$SCREEN_EVENT_TYPE_ZAKEN_VRIJGEVEN;$uniqueResourceId"
                    }
                }
                """
            )
            itestHttpClient.connectNewWebSocket(
                url = ItestConfiguration.ZAC_WEBSOCKET_BASE_URI,
                webSocketListener = websocketListener,
                testUser = COORDINATOR_DOMAIN_TEST_1
            )

            When("the 'lijst vrijgeven' endpoint is called for the zaken") {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zaken/lijst/vrijgeven",
                    requestBodyAsString = """{
                        "uuids": [ "$zaak1UUID", "$zaak2UUID"],
                        "reden": "fakeLijstVrijgevenReason",
                        "screenEventResourceId": "$uniqueResourceId"
                    }""",
                    testUser = COORDINATOR_DOMAIN_TEST_1
                )
                Then(
                    """the response should be a 204 HTTP response, eventually a screen event of type 'zaken vrijgeven'
                    should be received by the websocket listener and the zaken should be released from the user
                    but should still be assigned to the previously assigned groups"""
                ) {
                    response.code shouldBe HTTP_NO_CONTENT
                    // the backend process is asynchronous, so we need to wait a bit until the zaken are assigned
                    eventually(20.seconds) {
                        websocketListener.messagesReceived.size shouldBe 1
                        with(zacClient.retrieveZaak(
                            zaakUUID = zaak1UUID,
                            testUser = RAADPLEGER_DOMAIN_TEST_1
                        )) {
                            code shouldBe HTTP_OK
                            JSONObject(bodyAsString).apply {
                                getJSONObject("groep").apply {
                                    getString("id") shouldBe BEHANDELAARS_DOMAIN_TEST_1.name
                                    getString("naam") shouldBe BEHANDELAARS_DOMAIN_TEST_1.description
                                }
                                has("behandelaar") shouldBe false
                            }
                        }
                        with(zacClient.retrieveZaak(
                            zaakUUID = zaak2UUID,
                            testUser = RAADPLEGER_DOMAIN_TEST_1
                        )) {
                            code shouldBe HTTP_OK
                            JSONObject(bodyAsString).apply {
                                getJSONObject("groep").apply {
                                    getString("id") shouldBe BEHANDELAARS_DOMAIN_TEST_1.name
                                    getString("naam") shouldBe BEHANDELAARS_DOMAIN_TEST_1.description
                                }
                                has("behandelaar") shouldBe false
                            }
                        }
                    }
                }
            }
        }
    }
})
