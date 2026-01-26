/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFACTION_TYPE_VESTIGING
import nl.info.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFICATION_TYPE_BSN
import nl.info.zac.itest.config.ItestConfiguration.BRP_WIREMOCK_API
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_COUNT
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_ADRES_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_EERSTE_HANDELSNAAM_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_NAAM_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_NUMMER_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_PLAATS_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_RSIN_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_TYPE_RECHTSPERSOON
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_HOOFDACTIVITEIT
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_NEVENACTIVITEIT1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_NEVENACTIVITEIT2
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_TOTAAL_WERKZAME_PERSONEN
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_VOLTIJD_WERKZAME_PERSONEN
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGINGSNUMMER_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGINGSTYPE_HOOFDVESTIGING
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BIRTHDATE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_FULLNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_GENDER
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_PLACE_OF_RESIDENCE
import nl.info.zac.itest.config.ItestConfiguration.TEST_VESTIGING_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.TEST_VESTIGING_TELEPHONE_NUMBER
import nl.info.zac.itest.config.ItestConfiguration.VESTIGINGTYPE_NEVENVESTIGING
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_BETROKKENE_BELANGHEBBENDE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_BETROKKENE_BEWINDVOERDER
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_BETROKKENE_CONTACTPERSOON
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_BETROKKENE_GEMACHTIGDE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_BETROKKENE_MEDEAANVRAGER
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_BETROKKENE_PLAATSVERVANGER
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_DOMAIN_TEST_1
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID

private const val HEADER_ZAAK_ID = "X-ZAAKTYPE-UUID"

@Suppress("LongParameterList", "LargeClass")
class KlantRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val logger = KotlinLogging.logger {}

    val personId: UUID = zacClient.getPersonId(TEST_PERSON_HENDRIKA_JANSE_BSN, BEHANDELAAR_DOMAIN_TEST_1)

    var zaakUuid: UUID? = null

    Context("Create zaak with initiator") {
        Given("A behandelaar is logged in") {
            authenticate(BEHANDELAAR_DOMAIN_TEST_1)

            When("zaak is created") {
                val response = zacClient.createZaak(
                    zaakTypeUUID = ZAAKTYPE_TEST_2_UUID,
                    groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
                    groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
                    behandelaarId = BEHANDELAAR_DOMAIN_TEST_1.username,
                    startDate = DATE_TIME_2000_01_01,
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )

                Then("response is ok") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HttpURLConnection.HTTP_OK
                    JSONObject(responseBody).run {
                        getJSONObject("zaakdata").run {
                            zaakUuid = getString("zaakUUID").run(UUID::fromString)
                        }
                    }
                }
            }

            When("initiator is added") {
                val response = itestHttpClient.performPatchRequest(
                    url = "${ZAC_API_URI}/zaken/initiator",
                    requestBodyAsString = """
                    {
                        "betrokkeneIdentificatie": {
                            "personId": "$personId",
                            "type": "$BETROKKENE_IDENTIFICATION_TYPE_BSN"
                        },
                        "zaakUUID": "$zaakUuid"
                    }
                    """.trimIndent(),
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )
                Then("the requested initiator should be added") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    val identificatie = JSONObject(responseBody).getJSONObject("initiatorIdentificatie")
                    with(identificatie.toString()) {
                        shouldContainJsonKeyValue("type", BETROKKENE_IDENTIFICATION_TYPE_BSN)
                        shouldContainJsonKeyValue("personId", personId.toString())
                    }
                }
            }

            When("zaak is set-up") {
                Then("delete BRP Wiremock requests journal") {
                    itestHttpClient.performDeleteRequest(url = "$BRP_WIREMOCK_API/requests").code shouldBe HTTP_OK
                }
            }
        }
    }

    Context("Listing role types") {
        Given(
            """
            Persons and company data is present in the BRP Personen Mock and the Klanten API database,
            and a logged-in raadpleger
            """
        ) {
            When("the list role types endpoint is called") {
                // this endpoint requires no explicit authorisation, however to pass the basic authorisation filter in ZAC
                // a user with at least one ZAC role must be logged in
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/klanten/roltype",
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )
                Then("the response should be a 200 HTTP response with the correct amount of roltypen") {
                    response.code shouldBe HTTP_OK
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    with(responseBody) {
                        shouldBeJsonArray()
                        JSONArray(responseBody).length() shouldBe ROLTYPE_COUNT
                        with(JSONArray(responseBody)[0].toString()) {
                            shouldContainJsonKeyValue("naam", "Behandelaar")
                            shouldContainJsonKeyValue("omschrijvingGeneriekEnum", "behandelaar")
                        }
                    }
                }
            }
        }
    }

    Context("Retrieving a person") {
        Given(
            """
                A person which is present in the BRP and for which contact data exists in the Klanten API,
                and a logged-in raadpleger
                """
        ) {
            val expectedResponse = """
                    {
                      "bsn": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                      "personId": "$personId",
                      "emailadres": "$TEST_PERSON_HENDRIKA_JANSE_EMAIL",
                      "geboortedatum": "$TEST_PERSON_HENDRIKA_JANSE_BIRTHDATE",
                      "geslacht": "$TEST_PERSON_HENDRIKA_JANSE_GENDER",
                      "identificatie": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                      "identificatieType": "BSN",
                      "indicaties": [ "EMIGRATIE", "OPSCHORTING_BIJHOUDING" ],
                      "naam": "$TEST_PERSON_HENDRIKA_JANSE_FULLNAME",
                      "telefoonnummer": "$TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER",
                      "verblijfplaats": "$TEST_PERSON_HENDRIKA_JANSE_PLACE_OF_RESIDENCE"
                    }
            """.trimIndent()

            When("zaaktype uuid is provided in the request headers and the person is retrieved") {
                val headers = Headers.Builder()
                    .add(HEADER_ZAAK_ID, "$ZAAKTYPE_TEST_3_UUID")
                    .build()
                // this endpoint requires no explicit authorisation, however to pass the basic authorisation filter in ZAC
                // a user with at least one ZAC role must be logged in
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/klanten/person/$personId",
                    headers = headers,
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )
                Then(
                    "the response should be a 200 HTTP response with personal data from both the BRP and Klanten databases"
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJson expectedResponse
                }
                And("BRP search request with zaaktype settings should be made") {
                    val response = itestHttpClient.performJSONPostRequest(
                        url = "$BRP_WIREMOCK_API/requests/count",
                        requestBodyAsString = """
                          {
                            "method": "POST",
                            "url": "/haalcentraal/api/brp/personen",
                            "headers": {
                              "X-DOELBINDING": {
                                "matches": "BRPACT-AlgemeneTaken"
                              },
                              "X-VERWERKING": {
                                "matches": "Algemeen@$ZAAKTYPE_TEST_3_DESCRIPTION"
                              },
                              "X-GEBRUIKER": {
                                "matches": ".+"
                              }
                            }
                          }
                        """.trimIndent(),
                        testUser = RAADPLEGER_DOMAIN_TEST_1
                    )
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    response.bodyAsString shouldEqualJsonIgnoringExtraneousFields """{ "count": 1 }"""
                }
            }

            When("no zaaktype uuid is provided") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/klanten/person/$personId",
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )

                Then(
                    "the response should be a 200 HTTP response with personal data from both the BRP and Klanten databases"
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJson expectedResponse
                }

                And("BRP request with defaults should be made") {
                    val response = itestHttpClient.performJSONPostRequest(
                        url = "$BRP_WIREMOCK_API/requests/count",
                        requestBodyAsString = """
                          {
                            "method": "POST",
                            "url": "/haalcentraal/api/brp/personen",
                            "headers": {
                              "X-DOELBINDING": {
                                "matches": "BRPACT-Totaal"
                              },
                              "X-VERWERKING": {
                                "matches": "Algemeen"
                              },
                              "X-GEBRUIKER": {
                                "matches": ".+"
                              }
                            }
                          }
                        """.trimIndent(),
                        testUser = RAADPLEGER_DOMAIN_TEST_1
                    )
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    response.bodyAsString shouldEqualJsonIgnoringExtraneousFields """{ "count": 1 }"""
                }
            }
        }

        Given("an existing person and a logged-in raadpleger") {
            val expectedResponse = """{ 
                "foutmelding": "",
                "resultaten": [{
                   "bsn": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                   "personId": "$personId",
                   "geboortedatum": "$TEST_PERSON_HENDRIKA_JANSE_BIRTHDATE",
                   "geslacht": "$TEST_PERSON_HENDRIKA_JANSE_GENDER",
                   "identificatie": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                   "identificatieType": "$BETROKKENE_IDENTIFICATION_TYPE_BSN",
                   "indicaties": [ "EMIGRATIE", "OPSCHORTING_BIJHOUDING"],
                   "naam": "$TEST_PERSON_HENDRIKA_JANSE_FULLNAME",
                   "verblijfplaats": "$TEST_PERSON_HENDRIKA_JANSE_PLACE_OF_RESIDENCE"
               }],
               "totaal":1.0
            }
            """.trimIndent()

            When("zaaktype uuid is provided in the request headers") {
                val headers = Headers.Builder()
                    .add(HEADER_ZAAK_ID, "$ZAAKTYPE_TEST_3_UUID")
                    .build()
                // this endpoint requires no explicit authorisation, however to pass the basic authorisation filter in ZAC
                // a user with at least one ZAC role must be logged in
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/klanten/personen",
                    headers = headers,
                    requestBodyAsString = """{ "bsn": "$TEST_PERSON_HENDRIKA_JANSE_BSN" }""",
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )

                Then(
                    "the response should be a 200 HTTP response with personal data from both the BRP and Klanten databases"
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJson expectedResponse
                }

                And("BRP search request with zaaktype settings should be made") {
                    val response = itestHttpClient.performJSONPostRequest(
                        url = "$BRP_WIREMOCK_API/requests/count",
                        requestBodyAsString = """
                          {
                            "method": "POST",
                            "url": "/haalcentraal/api/brp/personen",
                            "headers": {
                              "X-DOELBINDING": {
                                "matches": "BRPACT-AlgemeneTaken"
                              },
                              "X-VERWERKING": {
                                "matches": "Algemeen@$ZAAKTYPE_TEST_3_DESCRIPTION"
                              },
                              "X-GEBRUIKER": {
                                "matches": ".+"
                              }
                            }
                          }
                        """.trimIndent(),
                        testUser = RAADPLEGER_DOMAIN_TEST_1
                    )
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    response.bodyAsString shouldEqualJsonIgnoringExtraneousFields """{ "count": 2 }"""
                }
            }

            When("zaaktype uuid is missing in the request") {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/klanten/personen",
                    requestBodyAsString = """{ "bsn": "$TEST_PERSON_HENDRIKA_JANSE_BSN" }""",
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )

                Then(
                    "the response should be a 200 HTTP response with personal data from both the BRP and Klanten databases"
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJson expectedResponse
                }

                And("BRP request with defaults should be made") {
                    val response = itestHttpClient.performJSONPostRequest(
                        url = "$BRP_WIREMOCK_API/requests/count",
                        requestBodyAsString = """
                          {
                            "method": "POST",
                            "url": "/haalcentraal/api/brp/personen",
                            "headers": {
                              "X-DOELBINDING": {
                                "matches": "BRPACT-Totaal"
                              },
                              "X-VERWERKING": {
                                "matches": "Algemeen"
                              },
                              "X-GEBRUIKER": {
                                "matches": ".+"
                              }
                            }
                          }
                        """.trimIndent(),
                        testUser = RAADPLEGER_DOMAIN_TEST_1
                    )
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    response.bodyAsString shouldEqualJsonIgnoringExtraneousFields """{ "count": 2 }"""
                }
            }
        }
    }

    Context("Retrieving a vestiging") {
        Given("An existing vestiging and a logged-in raadpleger") {
            When(
                """
                A vestiging is requested by vestigingsnummer and KVK number which is present in the KVK test environment
                and for which contact details are present in Open Klant
            """
            ) {
                // this endpoint requires no explicit authorisation, however to pass the basic authorisation filter in ZAC
                // a user with at least one ZAC role must be logged in
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/klanten/vestiging/$TEST_KVK_VESTIGINGSNUMMER_1/$TEST_KVK_NUMMER_1",
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )

                Then("the vestiging is returned with the expected data including contact details") {
                    response.code shouldBe HTTP_OK
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    // since there is customer contact data linked to this vestiging in our Open Klant container
                    // the response should contain an email address and telephone number
                    responseBody shouldEqualJson """
                    {
                      "adres": "$TEST_KVK_ADRES_1, $TEST_KVK_PLAATS_1",
                      "emailadres": "$TEST_VESTIGING_EMAIL",
                      "identificatie": "$TEST_KVK_VESTIGINGSNUMMER_1",
                      "identificatieType": "$BETROKKENE_IDENTIFACTION_TYPE_VESTIGING",
                      "kvkNummer": "$TEST_KVK_NUMMER_1",
                      "naam": "$TEST_KVK_NAAM_1",
                      "type": "$VESTIGINGTYPE_NEVENVESTIGING",
                      "telefoonnummer": "$TEST_VESTIGING_TELEPHONE_NUMBER",
                      "vestigingsnummer": "$TEST_KVK_VESTIGINGSNUMMER_1"
                    }
                    """.trimIndent()
                }
            }

            When(
                """
                A vestiging is requested by vestigingsnummer alone which is present in the KVK test environment     
            """
            ) {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/klanten/vestiging/$TEST_KVK_VESTIGINGSNUMMER_1",
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )

                Then("the vestiging is returned with the expected data including contact details") {
                    response.code shouldBe HTTP_OK
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    // even though there is customer contact data linked to this vestiging in our Open Klant container
                    // we do not support retrieving contact details when only the vestigingsnummer is provided,
                    // so the response should not contain an email address and telephone number
                    responseBody shouldEqualJson """
                    {
                      "adres": "$TEST_KVK_ADRES_1, $TEST_KVK_PLAATS_1",
                      "identificatie": "$TEST_KVK_VESTIGINGSNUMMER_1",
                      "identificatieType": "$BETROKKENE_IDENTIFACTION_TYPE_VESTIGING",
                      "naam": "$TEST_KVK_NAAM_1",
                      "type": "$VESTIGINGTYPE_NEVENVESTIGING",
                      "vestigingsnummer": "$TEST_KVK_VESTIGINGSNUMMER_1"
                    }
                    """.trimIndent()
                }
            }

            When("a vestigingsprofiel is requested which is present in the KVK test environment") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/klanten/vestigingsprofiel/$TEST_KVK_VESTIGINGSNUMMER_1",
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )
                Then("the vestigingsprofiel is returned with the expected data") {
                    response.code shouldBe HTTP_OK
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    with(responseBody) {
                        shouldContainJsonKey("adressen")
                        val adressen = JSONObject(responseBody).getJSONArray("adressen")
                        adressen.length() shouldBe 1
                        with(JSONArray(adressen).get(0).toString()) {
                            shouldContainJsonKeyValue("type", "bezoekadres")
                            shouldContainJsonKeyValue("afgeschermd", false)
                            shouldContainJsonKeyValue("volledigAdres", "$TEST_KVK_ADRES_1, $TEST_KVK_PLAATS_1")
                        }
                        shouldContainJsonKeyValue("commercieleVestiging", true)
                        shouldContainJsonKeyValue("deeltijdWerkzamePersonen", 1)
                        shouldContainJsonKeyValue("eersteHandelsnaam", TEST_KVK_EERSTE_HANDELSNAAM_1)
                        shouldContainJsonKeyValue("kvkNummer", TEST_KVK_NUMMER_1)
                        shouldContainJsonKey("sbiActiviteiten")
                        val sbiActiviteiten = JSONObject(responseBody).getJSONArray("sbiActiviteiten")
                        sbiActiviteiten.length() shouldBe 2
                        with(JSONArray(sbiActiviteiten).get(0).toString()) {
                            shouldContain(TEST_KVK_VESTIGING1_NEVENACTIVITEIT1)
                        }
                        with(JSONArray(sbiActiviteiten).get(1).toString()) {
                            shouldContain(TEST_KVK_VESTIGING1_NEVENACTIVITEIT2)
                        }
                        shouldContainJsonKeyValue("sbiHoofdActiviteit", TEST_KVK_VESTIGING1_HOOFDACTIVITEIT)
                        shouldContainJsonKeyValue(
                            "totaalWerkzamePersonen",
                            TEST_KVK_VESTIGING1_TOTAAL_WERKZAME_PERSONEN
                        )
                        shouldContainJsonKeyValue("type", TEST_KVK_VESTIGINGSTYPE_HOOFDVESTIGING)
                        shouldContainJsonKeyValue("vestigingsnummer", TEST_KVK_VESTIGINGSNUMMER_1)
                        shouldContainJsonKeyValue(
                            "voltijdWerkzamePersonen",
                            TEST_KVK_VESTIGING1_VOLTIJD_WERKZAME_PERSONEN
                        )
                    }
                }
            }

            When("a search on companies by name is performed") {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/klanten/bedrijven",
                    requestBodyAsString = JSONObject(
                        mapOf("naam" to TEST_KVK_NAAM_1)
                    ).toString(),
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )

                Then("the expected companies as defined in the KVK mock are returned") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJson """
                    { 
                        "foutmelding" : "",
                        "resultaten" : [ {
                            "adres" : "$TEST_KVK_ADRES_1, $TEST_KVK_PLAATS_1",
                            "identificatie" : "$TEST_KVK_VESTIGINGSNUMMER_1",
                            "identificatieType" : "$BETROKKENE_IDENTIFACTION_TYPE_VESTIGING",
                            "kvkNummer" : "$TEST_KVK_NUMMER_1",
                            "naam" : "$TEST_KVK_NAAM_1",
                            "type" : "$VESTIGINGTYPE_NEVENVESTIGING",
                            "vestigingsnummer" : "$TEST_KVK_VESTIGINGSNUMMER_1"
                        } ],
                       "totaal" : 1.0
                    }
                    """.trimIndent()
                }
            }

            When("a search on companies by vestigingsnummer is performed") {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/klanten/bedrijven",
                    requestBodyAsString = JSONObject(
                        mapOf("vestigingsnummer" to TEST_KVK_VESTIGINGSNUMMER_1)
                    ).toString(),
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )

                Then("the expected companies as defined in the KVK mock are returned") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJson """
                    { 
                        "foutmelding" : "",
                        "resultaten" : [ {
                            "adres" : "$TEST_KVK_ADRES_1, $TEST_KVK_PLAATS_1",
                            "identificatie" : "$TEST_KVK_VESTIGINGSNUMMER_1",
                            "identificatieType" : "$BETROKKENE_IDENTIFACTION_TYPE_VESTIGING",
                            "kvkNummer" : "$TEST_KVK_NUMMER_1",
                            "naam" : "$TEST_KVK_NAAM_1",
                            "type" : "$VESTIGINGTYPE_NEVENVESTIGING",
                            "vestigingsnummer" : "$TEST_KVK_VESTIGINGSNUMMER_1"
                        } ],
                       "totaal" : 1.0
                    }
                    """.trimIndent()
                }
            }
        }
    }

    Context("Retrieving contactmomenten for a person") {
        Given("Existing contactmomenten and a logged-in raadpleger") {
            When("the list contactmomenten endpoint is called with the BSN of this test customer") {
                // this endpoint requires no explicit authorisation, however to pass the basic authorisation filter in ZAC
                // a user with at least one ZAC role must be logged in
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/klanten/contactmomenten",
                    requestBodyAsString = """
                    {
                        "bsn": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                        "page": 0
                    }
                    """.trimIndent(),
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )

                Then("the response should be a 200 HTTP response with the customer contactmomenten") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJson """
                    {
                      "foutmelding": "",
                      "resultaten": [
                        {
                          "initiatiefnemer": "First of Last",
                          "kanaal": "email",
                          "medewerker": "Actor Name",
                          "registratiedatum": "2000-01-01T12:00:00Z",
                          "tekst": "email contact"
                        },
                        {
                          "initiatiefnemer": "Name in Family",
                          "kanaal": "telefoon",
                          "registratiedatum": "2010-01-01T12:00:00Z",
                          "tekst": "phone contact"
                        }
                      ],
                      "totaal": 2
                    }
                    """.trimIndent()
                }
            }
        }
    }

    Context("Retrieving a rechtspersoon") {
        Given("An existing rechtspersoon and a logged-in raadpleger") {
            When(
                """
                the read rechtspersoon endpoint is called with the RSIN of a test company available in the KVK mock
                """
            ) {
                // this endpoint requires no explicit authorisation, however to pass the basic authorisation filter in ZAC
                // a user with at least one ZAC role must be logged in
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/klanten/rechtspersoon/rsin/$TEST_KVK_RSIN_1",
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )

                Then("the response should be ok and the test company should be returned without contact data") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJson """
                    {
                      "adres" : "$TEST_KVK_ADRES_1, $TEST_KVK_PLAATS_1",
                      "identificatie" : "$TEST_KVK_RSIN_1",
                      "identificatieType" : "RSIN",
                      "naam" : "$TEST_KVK_NAAM_1",
                      "rsin" : "$TEST_KVK_RSIN_1",
                      "type" : "$TEST_KVK_TYPE_RECHTSPERSOON"
                    }
                    """.trimIndent()
                }
            }

            When(
                """
                the read rechtspersoon endpoint is called with the KVK nummer of a test company available in the KVK mock
                """
            ) {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/klanten/rechtspersoon/kvknummer/$TEST_KVK_NUMMER_1",
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )

                Then("the response should be ok and the test company should be returned without contact data") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJson """
                   {
                      "adres" : "$TEST_KVK_ADRES_1, $TEST_KVK_PLAATS_1",
                      "identificatie" : "$TEST_KVK_RSIN_1",
                      "identificatieType" : "RSIN",
                      "kvkNummer" : "$TEST_KVK_NUMMER_1",
                      "naam" : "$TEST_KVK_NAAM_1",
                      "rsin" : "$TEST_KVK_RSIN_1",
                      "type" : "$TEST_KVK_TYPE_RECHTSPERSOON"
                    }
                    """.trimIndent()
                }
            }
        }
    }

    Context("Retrieving personen parameters") {
        Given("Existing personen parameters and a logged-in raadpleger") {
            When("the personen parameters endpoint is called") {
                // this endpoint requires no explicit authorisation, however to pass the basic authorisation filter in ZAC
                // a user with at least one ZAC role must be logged in
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/klanten/personen/parameters",
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )

                Then("the response should be ok and the test company should be returned without contact data") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJson """
                  [ 
                    {
                      "bsn" : "REQ",
                      "geboortedatum" : "NON",
                      "gemeenteVanInschrijving" : "NON",
                      "geslachtsnaam" : "NON",
                      "huisnummer" : "NON",
                      "postcode" : "NON",
                      "straat" : "NON",
                      "voornamen" : "NON",
                      "voorvoegsel" : "NON"
                    }, 
                    {
                      "bsn" : "NON",
                      "geboortedatum" : "REQ",
                      "gemeenteVanInschrijving" : "NON",
                      "geslachtsnaam" : "REQ",
                      "huisnummer" : "NON",
                      "postcode" : "NON",
                      "straat" : "NON",
                      "voornamen" : "OPT",
                      "voorvoegsel" : "OPT"
                    }, 
                    {
                      "bsn" : "NON",
                      "geboortedatum" : "NON",
                      "gemeenteVanInschrijving" : "REQ",
                      "geslachtsnaam" : "REQ",
                      "huisnummer" : "NON",
                      "postcode" : "NON",
                      "straat" : "NON",
                      "voornamen" : "REQ",
                      "voorvoegsel" : "OPT"
                    }, 
                    {
                      "bsn" : "NON",
                      "geboortedatum" : "NON",
                      "gemeenteVanInschrijving" : "NON",
                      "geslachtsnaam" : "NON",
                      "huisnummer" : "REQ",
                      "postcode" : "REQ",
                      "straat" : "NON",
                      "voornamen" : "NON",
                      "voorvoegsel" : "NON"
                    }, 
                    {
                      "bsn" : "NON",
                      "geboortedatum" : "NON",
                      "gemeenteVanInschrijving" : "REQ",
                      "geslachtsnaam" : "NON",
                      "huisnummer" : "REQ",
                      "postcode" : "NON",
                      "straat" : "REQ",
                      "voornamen" : "NON",
                      "voorvoegsel" : "NON"
                    } 
                  ]
                    """.trimIndent()
                }
            }
        }
    }

    Context("Retrieving betrokkenen for a zaaktype") {
        Given("Existing betrokkenen and a logged-in raadpleger") {
            When("the betrokkenen are retrieved for the zaaktype 'Test zaaktype 2'") {
                // this endpoint requires no explicit authorisation, however to pass the basic authorisation filter in ZAC
                // a user with at least one ZAC role must be logged in
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/klanten/roltype/$ZAAKTYPE_TEST_2_UUID/betrokkene",
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )

                Then("the response should be ok and the test company should be returned without contact data") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJson """
                    [
                      {
                        "naam": "Belanghebbende",
                        "omschrijvingGeneriekEnum": "belanghebbende",
                        "uuid": "$ZAAKTYPE_TEST_2_BETROKKENE_BELANGHEBBENDE"
                      },
                      {
                        "naam": "Bewindvoerder",
                        "omschrijvingGeneriekEnum": "belanghebbende",
                        "uuid": "$ZAAKTYPE_TEST_2_BETROKKENE_BEWINDVOERDER"
                      },
                      {
                        "naam": "Contactpersoon",
                        "omschrijvingGeneriekEnum": "belanghebbende",
                        "uuid": "$ZAAKTYPE_TEST_2_BETROKKENE_CONTACTPERSOON"
                      },
                      {
                        "naam": "Gemachtigde",
                        "omschrijvingGeneriekEnum": "belanghebbende",
                        "uuid": "$ZAAKTYPE_TEST_2_BETROKKENE_GEMACHTIGDE"
                      },
                      {
                        "naam": "Medeaanvrager",
                        "omschrijvingGeneriekEnum": "mede_initiator",
                        "uuid": "$ZAAKTYPE_TEST_2_BETROKKENE_MEDEAANVRAGER"
                      },
                      {
                        "naam": "Plaatsvervanger",
                        "omschrijvingGeneriekEnum": "belanghebbende",
                        "uuid": "$ZAAKTYPE_TEST_2_BETROKKENE_PLAATSVERVANGER"
                      }
                    ]
                    """.trimIndent()
                }
            }
        }
    }
})
