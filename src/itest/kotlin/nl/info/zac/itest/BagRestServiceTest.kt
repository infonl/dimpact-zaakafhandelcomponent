/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.BAG_MOCK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.BAG_TEST_ADRES_1_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_NO_CONTENT
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import org.json.JSONObject
import java.util.UUID

@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class BagRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    Given("A zaak exists and address data is present in the BAG API mock") {
        When("the list addresses endpoint is called for a search query for which we have mock data") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/bag/adres",
                requestBodyAsString = """
                        { "trefwoorden": "dummy search text"}
                """.trimIndent()
            )
            Then(
                "the response should be a 200 HTTP response with the expected addresses that match the search criteria"
            ) {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    {
                      "foutmelding" : "",
                      "resultaten" : [ {
                        "geconstateerd" : false,
                        "identificatie" : "$BAG_TEST_ADRES_1_IDENTIFICATION",
                        "url" : "$BAG_MOCK_BASE_URI/lvbag/individuelebevragingen/v2/adressen/$BAG_TEST_ADRES_1_IDENTIFICATION",
                        "bagObjectType" : "ADRES",
                        "huisnummer" : 1,
                        "huisnummerWeergave" : "1",
                        "nummeraanduiding" : {
                          "geconstateerd" : false,
                          "identificatie" : "$BAG_TEST_ADRES_1_IDENTIFICATION",
                          "url" : "$BAG_MOCK_BASE_URI/lvbag/individuelebevragingen/v2/nummeraanduidingen/$BAG_TEST_ADRES_1_IDENTIFICATION",
                          "bagObjectType" : "NUMMERAANDUIDING",
                          "huisnummer" : 1,
                          "huisnummerWeergave" : "1",
                          "omschrijving" : "1 1012JS",
                          "postcode" : "1012JS",
                          "status" : "Naamgeving uitgegeven",
                          "typeAdresseerbaarObject" : "Verblijfsobject"
                        },
                        "omschrijving" : "Dam 1, 1012JS Amsterdam",
                        "openbareRuimte" : {
                          "geconstateerd" : false,
                          "identificatie" : "0363300000003186",
                          "url" : "$BAG_MOCK_BASE_URI/lvbag/individuelebevragingen/v2/openbareruimten/0363300000003186",
                          "bagObjectType" : "OPENBARE_RUIMTE",
                          "naam" : "Dam",
                          "omschrijving" : "Dam",
                          "status" : "Naamgeving uitgegeven",
                          "type" : "Weg",
                          "typeWeergave" : "Weg",
                          "woonplaatsNaam" : "Amsterdam"
                        },
                        "openbareRuimteNaam" : "Dam",
                        "panden" : [ {
                          "geconstateerd" : false,
                          "identificatie" : "0363100012168052",
                          "bagObjectType" : "PAND",
                          "omschrijving" : "0363100012168052",
                          "oorspronkelijkBouwjaar" : "1914",
                          "status" : "Pand in gebruik",
                          "statusWeergave" : "Pand in gebruik"
                        } ],
                        "postcode" : "1012JS",
                        "woonplaats" : {
                          "geconstateerd" : false,
                          "identificatie" : "3594",
                          "url" : "$BAG_MOCK_BASE_URI/lvbag/individuelebevragingen/v2/woonplaatsen/3594",
                          "bagObjectType" : "WOONPLAATS",
                          "naam" : "Amsterdam",
                          "omschrijving" : "Amsterdam",
                          "status" : "Woonplaats aangewezen"
                        },
                        "woonplaatsNaam" : "Amsterdam"
                      }, {
                        "geconstateerd" : false,
                        "identificatie" : "0363200012113669",
                        "url" : "$BAG_MOCK_BASE_URI/lvbag/individuelebevragingen/v2/adressen/0363200012113669",
                        "bagObjectType" : "ADRES",                                     
                        "huisnummer" : 1,
                        "huisnummerWeergave" : "1",
                        "nummeraanduiding" : {
                          "geconstateerd" : false,
                          "identificatie" : "0363200012113669",
                          "url" : "$BAG_MOCK_BASE_URI/lvbag/individuelebevragingen/v2/nummeraanduidingen/0363200012113669",
                          "bagObjectType" : "NUMMERAANDUIDING",
                          "huisnummer" : 1,
                          "huisnummerWeergave" : "1",
                          "omschrijving" : "1 1012LG",
                          "postcode" : "1012LG",
                          "status" : "Naamgeving uitgegeven",
                          "typeAdresseerbaarObject" : "Verblijfsobject"
                        },
                        "omschrijving" : "Damrak 1, 1012LG Amsterdam",
                        "openbareRuimte" : {
                          "geconstateerd" : false,
                          "identificatie" : "0363300000003187",
                          "url" : "$BAG_MOCK_BASE_URI/lvbag/individuelebevragingen/v2/openbareruimten/0363300000003187",
                          "bagObjectType" : "OPENBARE_RUIMTE",
                          "naam" : "Damrak",
                          "omschrijving" : "Damrak",
                          "status" : "Naamgeving uitgegeven",
                          "type" : "Weg",
                          "typeWeergave" : "Weg",
                          "woonplaatsNaam" : "Amsterdam"
                        },
                        "openbareRuimteNaam" : "Damrak",
                        "panden" : [ {
                          "geconstateerd" : false,
                          "identificatie" : "0363100012185508",
                          "bagObjectType" : "PAND",
                          "omschrijving" : "0363100012185508",
                          "oorspronkelijkBouwjaar" : "1890",
                          "status" : "Pand in gebruik",
                          "statusWeergave" : "Pand in gebruik"
                        } ],
                        "postcode" : "1012LG",
                        "woonplaats" : {
                          "geconstateerd" : false,
                          "identificatie" : "3594",
                          "url" : "$BAG_MOCK_BASE_URI/lvbag/individuelebevragingen/v2/woonplaatsen/3594",
                          "bagObjectType" : "WOONPLAATS",
                          "naam" : "Amsterdam",
                          "omschrijving" : "Amsterdam",
                          "status" : "Woonplaats aangewezen"
                        },
                        "woonplaatsNaam" : "Amsterdam"
                      } ],
                      "totaal" : 2.0
                    }
                """.trimIndent()
            }
        }
    }
    Given("An existing zaak") {
        lateinit var zaakUUID: UUID
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            startDate = DATE_TIME_2000_01_01
        ).run {
            val responseBody = body!!.string()
            logger.info { "Response: $responseBody" }
            JSONObject(responseBody).run {
                zaakUUID = getString("uuid").run(UUID::fromString)
            }
        }
        When("a BAG object is added") {
            // note that the zaakobject fields 'woonplaatsNaam' and 'openbareRuimteNaam'
            // are mandatory for a zaakobject of type 'ADRES' by OpenZaak
            // (not, by the way, by the ZGW ZRC API, nor by the ZAC backend API)
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/bag",
                requestBodyAsString = """
                    {
                        "zaakUuid": "$zaakUUID",
                        "zaakobject": {                        
                            "identificatie": "$BAG_TEST_ADRES_1_IDENTIFICATION",
                             "url": "$BAG_MOCK_BASE_URI/lvbag/individuelebevragingen/v2/adressen/$BAG_TEST_ADRES_1_IDENTIFICATION",
                             "bagObjectType": "ADRES",
                             "woonplaatsNaam": "Amsterdam",
                             "openbareRuimteNaam": "Dam"
                        }                 
                    }
                """.trimIndent()
            )
            Then("it is successfully added to the zaak") {
                response.code shouldBe HTTP_STATUS_NO_CONTENT

                // retrieve the BAG objects for the zaak
                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/bag/zaak/$zaakUUID"
                ).run {
                    val responseBody = body!!.string()
                    logger.info { "Response: $responseBody" }
                    responseBody shouldEqualJsonIgnoringExtraneousFields """
                        [
                            {                             
                                "zaakUuid": "$zaakUUID",
                                "zaakobject": {
                                    "geconstateerd": false,
                                    "identificatie": "$BAG_TEST_ADRES_1_IDENTIFICATION",
                                    "url": "http://bag-wiremock.local:8080/lvbag/individuelebevragingen/v2/adressen/$BAG_TEST_ADRES_1_IDENTIFICATION",
                                    "bagObjectType": "ADRES",
                                    "huisnummer": 0,
                                    "huisnummerWeergave": "0",
                                    "omschrijving": "Dam 0,  Amsterdam",
                                    "openbareRuimteNaam": "Dam",
                                    "panden": [],
                                    "postcode": "",
                                    "woonplaatsNaam": "Amsterdam"
                                },
                                "bagObject": {
                                    "geconstateerd": false,
                                    "identificatie": "$BAG_TEST_ADRES_1_IDENTIFICATION",
                                    "url": "http://bag-wiremock.local:8080/lvbag/individuelebevragingen/v2/adressen/$BAG_TEST_ADRES_1_IDENTIFICATION",
                                    "bagObjectType": "ADRES",
                                    "huisnummer": 0,
                                    "huisnummerWeergave": "0",
                                    "omschrijving": "Dam 0,  Amsterdam",
                                    "openbareRuimteNaam": "Dam",
                                    "panden": [],
                                    "postcode": "",
                                    "woonplaatsNaam": "Amsterdam"
                                }
                            }
                        ]
                    """.trimIndent()
                }
            }
        }
    }
})
