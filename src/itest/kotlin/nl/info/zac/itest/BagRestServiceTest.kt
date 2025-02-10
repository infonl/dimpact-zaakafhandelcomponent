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
@Suppress("LargeClass")
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
                val responseBody = response.body!!.string()
                response.code shouldBe HTTP_STATUS_OK
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
    Given("A BAG object that is present in the BAG API mock") {
        When("the BAG object is requested") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/bag/ADRES/$BAG_TEST_ADRES_1_IDENTIFICATION"
            )
            Then("the BAG object is successfully returned") {
                val responseBody = response.body!!.string()
                response.code shouldBe HTTP_STATUS_OK
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    {
                      "geconstateerd" : false,
                      "identificatie" : "$BAG_TEST_ADRES_1_IDENTIFICATION",
                      "url" : "http://bag-wiremock.local:8080/lvbag/individuelebevragingen/v2/adressen/$BAG_TEST_ADRES_1_IDENTIFICATION?expand=panden%2C+adresseerbaarObject%2C+nummeraanduiding%2C+openbareRuimte%2C+woonplaats",
                      "adresseerbaarObject" : {
                        "geconstateerd" : false,
                        "identificatie" : "0363010003761571",
                        "bagObjectType" : "ADRESSEERBAAR_OBJECT",
                        "geometry" : {
                          "point" : {
                            "latitude" : 487383.0,
                            "longitude" : 121394.0
                          },
                          "type" : "Point"
                        },
                        "omschrijving" : "Verblijfsobject 0363010003761571",
                        "status" : "Verblijfsobject in gebruik",
                        "typeAdresseerbaarObject" : "Verblijfsobject",
                        "vboDoel" : "winkelfunctie",
                        "vboOppervlakte" : 23820
                      },
                      "bagObjectType" : "ADRES",
                      "geometry" : {
                        "geometrycollection" : [ {
                          "point" : {
                            "latitude" : 487383.0,
                            "longitude" : 121394.0
                          },
                          "type" : "Point"
                        }, {
                          "polygon" : [ [ {
                            "latitude" : 487444.779,
                            "longitude" : 121488.34
                          }, {
                            "latitude" : 487447.647,
                            "longitude" : 121484.902
                          }, {
                            "latitude" : 487453.339,
                            "longitude" : 121478.079
                          }, {
                            "latitude" : 487453.426,
                            "longitude" : 121478.151
                          }, {
                            "latitude" : 487453.786,
                            "longitude" : 121477.719
                          }, {
                            "latitude" : 487453.92,
                            "longitude" : 121477.83
                          }, {
                            "latitude" : 487469.879,
                            "longitude" : 121458.644
                          }, {
                            "latitude" : 487469.999,
                            "longitude" : 121458.499
                          }, {
                            "latitude" : 487454.792,
                            "longitude" : 121445.692
                          }, {
                            "latitude" : 487449.675,
                            "longitude" : 121441.383
                          }, {
                            "latitude" : 487448.7,
                            "longitude" : 121442.53
                          }, {
                            "latitude" : 487437.534,
                            "longitude" : 121433.035
                          }, {
                            "latitude" : 487437.64,
                            "longitude" : 121432.91
                          }, {
                            "latitude" : 487433.305,
                            "longitude" : 121429.235
                          }, {
                            "latitude" : 487433.538,
                            "longitude" : 121428.961
                          }, {
                            "latitude" : 487432.663,
                            "longitude" : 121428.22
                          }, {
                            "latitude" : 487432.672,
                            "longitude" : 121428.208
                          }, {
                            "latitude" : 487432.68,
                            "longitude" : 121428.196
                          }, {
                            "latitude" : 487432.688,
                            "longitude" : 121428.183
                          }, {
                            "latitude" : 487432.694,
                            "longitude" : 121428.169
                          }, {
                            "latitude" : 487432.698,
                            "longitude" : 121428.155
                          }, {
                            "latitude" : 487432.702,
                            "longitude" : 121428.14
                          }, {
                            "latitude" : 487432.705,
                            "longitude" : 121428.126
                          }, {
                            "latitude" : 487432.706,
                            "longitude" : 121428.111
                          }, {
                            "latitude" : 487432.706,
                            "longitude" : 121428.096
                          }, {
                            "latitude" : 487432.705,
                            "longitude" : 121428.08
                          }, {
                            "latitude" : 487432.702,
                            "longitude" : 121428.063
                          }, {
                            "latitude" : 487432.697,
                            "longitude" : 121428.047
                          }, {
                            "latitude" : 487432.692,
                            "longitude" : 121428.032
                          }, {
                            "latitude" : 487432.684,
                            "longitude" : 121428.017
                          }, {
                            "latitude" : 487432.676,
                            "longitude" : 121428.003
                          }, {
                            "latitude" : 487432.666,
                            "longitude" : 121427.99
                          }, {
                            "latitude" : 487432.655,
                            "longitude" : 121427.977
                          }, {
                            "latitude" : 487432.643,
                            "longitude" : 121427.966
                          }, {
                            "latitude" : 487429.469,
                            "longitude" : 121425.276
                          }, {
                            "latitude" : 487429.457,
                            "longitude" : 121425.267
                          }, {
                            "latitude" : 487429.445,
                            "longitude" : 121425.259
                          }, {
                            "latitude" : 487429.432,
                            "longitude" : 121425.251
                          }, {
                            "latitude" : 487429.418,
                            "longitude" : 121425.245
                          }, {
                            "latitude" : 487429.404,
                            "longitude" : 121425.24
                          }, {
                            "latitude" : 487429.39,
                            "longitude" : 121425.237
                          }, {
                            "latitude" : 487429.375,
                            "longitude" : 121425.234
                          }, {
                            "latitude" : 487429.36,
                            "longitude" : 121425.233
                          }, {
                            "latitude" : 487429.345,
                            "longitude" : 121425.233
                          }, {
                            "latitude" : 487429.33,
                            "longitude" : 121425.234
                          }, {
                            "latitude" : 487429.315,
                            "longitude" : 121425.237
                          }, {
                            "latitude" : 487429.301,
                            "longitude" : 121425.24
                          }, {
                            "latitude" : 487429.287,
                            "longitude" : 121425.245
                          }, {
                            "latitude" : 487429.273,
                            "longitude" : 121425.251
                          }, {
                            "latitude" : 487429.26,
                            "longitude" : 121425.258
                          }, {
                            "latitude" : 487429.248,
                            "longitude" : 121425.267
                          }, {
                            "latitude" : 487429.236,
                            "longitude" : 121425.276
                          }, {
                            "latitude" : 487429.225,
                            "longitude" : 121425.286
                          }, {
                            "latitude" : 487429.215,
                            "longitude" : 121425.297
                          }, {
                            "latitude" : 487428.49,
                            "longitude" : 121424.682
                          }, {
                            "latitude" : 487428.499,
                            "longitude" : 121424.67
                          }, {
                            "latitude" : 487428.507,
                            "longitude" : 121424.658
                          }, {
                            "latitude" : 487428.515,
                            "longitude" : 121424.645
                          }, {
                            "latitude" : 487428.521,
                            "longitude" : 121424.631
                          }, {
                            "latitude" : 487428.526,
                            "longitude" : 121424.617
                          }, {
                            "latitude" : 487428.529,
                            "longitude" : 121424.603
                          }, {
                            "latitude" : 487428.532,
                            "longitude" : 121424.588
                          }, {
                            "latitude" : 487428.533,
                            "longitude" : 121424.573
                          }, {
                            "latitude" : 487428.533,
                            "longitude" : 121424.558
                          }, {
                            "latitude" : 487428.532,
                            "longitude" : 121424.543
                          }, {
                            "latitude" : 487428.529,
                            "longitude" : 121424.528
                          }, {
                            "latitude" : 487428.526,
                            "longitude" : 121424.514
                          }, {
                            "latitude" : 487428.521,
                            "longitude" : 121424.5
                          }, {
                            "latitude" : 487428.515,
                            "longitude" : 121424.486
                          }, {
                            "latitude" : 487428.508,
                            "longitude" : 121424.473
                          }, {
                            "latitude" : 487428.499,
                            "longitude" : 121424.461
                          }, {
                            "latitude" : 487428.49,
                            "longitude" : 121424.449
                          }, {
                            "latitude" : 487428.48,
                            "longitude" : 121424.438
                          }, {
                            "latitude" : 487428.469,
                            "longitude" : 121424.428
                          }, {
                            "latitude" : 487425.296,
                            "longitude" : 121421.739
                          }, {
                            "latitude" : 487425.284,
                            "longitude" : 121421.73
                          }, {
                            "latitude" : 487425.272,
                            "longitude" : 121421.722
                          }, {
                            "latitude" : 487425.259,
                            "longitude" : 121421.714
                          }, {
                            "latitude" : 487425.245,
                            "longitude" : 121421.708
                          }, {
                            "latitude" : 487425.231,
                            "longitude" : 121421.703
                          }, {
                            "latitude" : 487425.217,
                            "longitude" : 121421.7
                          }, {
                            "latitude" : 487425.202,
                            "longitude" : 121421.697
                          }, {
                            "latitude" : 487425.187,
                            "longitude" : 121421.696
                          }, {
                            "latitude" : 487425.172,
                            "longitude" : 121421.696
                          }, {
                            "latitude" : 487425.157,
                            "longitude" : 121421.697
                          }, {
                            "latitude" : 487425.142,
                            "longitude" : 121421.7
                          }, {
                            "latitude" : 487425.128,
                            "longitude" : 121421.703
                          }, {
                            "latitude" : 487425.114,
                            "longitude" : 121421.708
                          }, {
                            "latitude" : 487425.1,
                            "longitude" : 121421.714
                          }, {
                            "latitude" : 487425.087,
                            "longitude" : 121421.721
                          }, {
                            "latitude" : 487425.075,
                            "longitude" : 121421.73
                          }, {
                            "latitude" : 487425.063,
                            "longitude" : 121421.739
                          }, {
                            "latitude" : 487425.052,
                            "longitude" : 121421.749
                          }, {
                            "latitude" : 487425.042,
                            "longitude" : 121421.76
                          }, {
                            "latitude" : 487424.318,
                            "longitude" : 121421.146
                          }, {
                            "latitude" : 487424.327,
                            "longitude" : 121421.134
                          }, {
                            "latitude" : 487424.335,
                            "longitude" : 121421.122
                          }, {
                            "latitude" : 487424.343,
                            "longitude" : 121421.109
                          }, {
                            "latitude" : 487424.349,
                            "longitude" : 121421.095
                          }, {
                            "latitude" : 487424.353,
                            "longitude" : 121421.081
                          }, {
                            "latitude" : 487424.357,
                            "longitude" : 121421.066
                          }, {
                            "latitude" : 487424.36,
                            "longitude" : 121421.052
                          }, {
                            "latitude" : 487424.361,
                            "longitude" : 121421.037
                          }, {
                            "latitude" : 487424.361,
                            "longitude" : 121421.022
                          }, {
                            "latitude" : 487424.36,
                            "longitude" : 121421.006
                          }, {
                            "latitude" : 487424.357,
                            "longitude" : 121420.989
                          }, {
                            "latitude" : 487424.352,
                            "longitude" : 121420.973
                          }, {
                            "latitude" : 487424.347,
                            "longitude" : 121420.958
                          }, {
                            "latitude" : 487424.339,
                            "longitude" : 121420.943
                          }, {
                            "latitude" : 487424.331,
                            "longitude" : 121420.929
                          }, {
                            "latitude" : 487424.321,
                            "longitude" : 121420.916
                          }, {
                            "latitude" : 487424.31,
                            "longitude" : 121420.903
                          }, {
                            "latitude" : 487424.298,
                            "longitude" : 121420.892
                          }, {
                            "latitude" : 487421.125,
                            "longitude" : 121418.203
                          }, {
                            "latitude" : 487421.113,
                            "longitude" : 121418.194
                          }, {
                            "latitude" : 487421.101,
                            "longitude" : 121418.186
                          }, {
                            "latitude" : 487421.088,
                            "longitude" : 121418.178
                          }, {
                            "latitude" : 487421.074,
                            "longitude" : 121418.172
                          }, {
                            "latitude" : 487421.06,
                            "longitude" : 121418.167
                          }, {
                            "latitude" : 487421.046,
                            "longitude" : 121418.164
                          }, {
                            "latitude" : 487421.031,
                            "longitude" : 121418.161
                          }, {
                            "latitude" : 487421.016,
                            "longitude" : 121418.16
                          }, {
                            "latitude" : 487421.001,
                            "longitude" : 121418.16
                          }, {
                            "latitude" : 487420.986,
                            "longitude" : 121418.161
                          }, {
                            "latitude" : 487420.971,
                            "longitude" : 121418.164
                          }, {
                            "latitude" : 487420.957,
                            "longitude" : 121418.167
                          }, {
                            "latitude" : 487420.943,
                            "longitude" : 121418.172
                          }, {
                            "latitude" : 487420.929,
                            "longitude" : 121418.178
                          }, {
                            "latitude" : 487420.916,
                            "longitude" : 121418.185
                          }, {
                            "latitude" : 487420.904,
                            "longitude" : 121418.194
                          }, {
                            "latitude" : 487420.892,
                            "longitude" : 121418.203
                          }, {
                            "latitude" : 487420.881,
                            "longitude" : 121418.213
                          }, {
                            "latitude" : 487420.871,
                            "longitude" : 121418.224
                          }, {
                            "latitude" : 487419.994,
                            "longitude" : 121417.48
                          }, {
                            "latitude" : 487419.761,
                            "longitude" : 121417.754
                          }, {
                            "latitude" : 487415.421,
                            "longitude" : 121414.075
                          }, {
                            "latitude" : 487415.324,
                            "longitude" : 121414.19
                          }, {
                            "latitude" : 487404.069,
                            "longitude" : 121404.775
                          }, {
                            "latitude" : 487405.06,
                            "longitude" : 121403.59
                          }, {
                            "latitude" : 487384.63,
                            "longitude" : 121386.49
                          }, {
                            "latitude" : 487367.471,
                            "longitude" : 121407.025
                          }, {
                            "latitude" : 487367.737,
                            "longitude" : 121407.23
                          }, {
                            "latitude" : 487377.357,
                            "longitude" : 121415.216
                          }, {
                            "latitude" : 487373.653,
                            "longitude" : 121419.678
                          }, {
                            "latitude" : 487369.432,
                            "longitude" : 121424.763
                          }, {
                            "latitude" : 487407.169,
                            "longitude" : 121456.069
                          }, {
                            "latitude" : 487408.549,
                            "longitude" : 121454.359
                          }, {
                            "latitude" : 487417.119,
                            "longitude" : 121461.859
                          }, {
                            "latitude" : 487444.779,
                            "longitude" : 121488.34
                          } ] ],
                          "type" : "Polygon"
                        } ],
                        "type" : "GeometryCollection"
                      },
                      "huisnummer" : 1,
                      "huisnummerWeergave" : "1",
                      "nummeraanduiding" : {
                        "geconstateerd" : false,
                        "identificatie" : "$BAG_TEST_ADRES_1_IDENTIFICATION",
                        "url" : "http://bag-wiremock.local:8080/lvbag/individuelebevragingen/v2/nummeraanduidingen/$BAG_TEST_ADRES_1_IDENTIFICATION",
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
                        "url" : "http://bag-wiremock.local:8080/lvbag/individuelebevragingen/v2/openbareruimten/0363300000003186",
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
                        "geometry" : {
                          "polygon" : [ [ {
                            "latitude" : 487444.779,
                            "longitude" : 121488.34
                          }, {
                            "latitude" : 487447.647,
                            "longitude" : 121484.902
                          }, {
                            "latitude" : 487453.339,
                            "longitude" : 121478.079
                          }, {
                            "latitude" : 487453.426,
                            "longitude" : 121478.151
                          }, {
                            "latitude" : 487453.786,
                            "longitude" : 121477.719
                          }, {
                            "latitude" : 487453.92,
                            "longitude" : 121477.83
                          }, {
                            "latitude" : 487469.879,
                            "longitude" : 121458.644
                          }, {
                            "latitude" : 487469.999,
                            "longitude" : 121458.499
                          }, {
                            "latitude" : 487454.792,
                            "longitude" : 121445.692
                          }, {
                            "latitude" : 487449.675,
                            "longitude" : 121441.383
                          }, {
                            "latitude" : 487448.7,
                            "longitude" : 121442.53
                          }, {
                            "latitude" : 487437.534,
                            "longitude" : 121433.035
                          }, {
                            "latitude" : 487437.64,
                            "longitude" : 121432.91
                          }, {
                            "latitude" : 487433.305,
                            "longitude" : 121429.235
                          }, {
                            "latitude" : 487433.538,
                            "longitude" : 121428.961
                          }, {
                            "latitude" : 487432.663,
                            "longitude" : 121428.22
                          }, {
                            "latitude" : 487432.672,
                            "longitude" : 121428.208
                          }, {
                            "latitude" : 487432.68,
                            "longitude" : 121428.196
                          }, {
                            "latitude" : 487432.688,
                            "longitude" : 121428.183
                          }, {
                            "latitude" : 487432.694,
                            "longitude" : 121428.169
                          }, {
                            "latitude" : 487432.698,
                            "longitude" : 121428.155
                          }, {
                            "latitude" : 487432.702,
                            "longitude" : 121428.14
                          }, {
                            "latitude" : 487432.705,
                            "longitude" : 121428.126
                          }, {
                            "latitude" : 487432.706,
                            "longitude" : 121428.111
                          }, {
                            "latitude" : 487432.706,
                            "longitude" : 121428.096
                          }, {
                            "latitude" : 487432.705,
                            "longitude" : 121428.08
                          }, {
                            "latitude" : 487432.702,
                            "longitude" : 121428.063
                          }, {
                            "latitude" : 487432.697,
                            "longitude" : 121428.047
                          }, {
                            "latitude" : 487432.692,
                            "longitude" : 121428.032
                          }, {
                            "latitude" : 487432.684,
                            "longitude" : 121428.017
                          }, {
                            "latitude" : 487432.676,
                            "longitude" : 121428.003
                          }, {
                            "latitude" : 487432.666,
                            "longitude" : 121427.99
                          }, {
                            "latitude" : 487432.655,
                            "longitude" : 121427.977
                          }, {
                            "latitude" : 487432.643,
                            "longitude" : 121427.966
                          }, {
                            "latitude" : 487429.469,
                            "longitude" : 121425.276
                          }, {
                            "latitude" : 487429.457,
                            "longitude" : 121425.267
                          }, {
                            "latitude" : 487429.445,
                            "longitude" : 121425.259
                          }, {
                            "latitude" : 487429.432,
                            "longitude" : 121425.251
                          }, {
                            "latitude" : 487429.418,
                            "longitude" : 121425.245
                          }, {
                            "latitude" : 487429.404,
                            "longitude" : 121425.24
                          }, {
                            "latitude" : 487429.39,
                            "longitude" : 121425.237
                          }, {
                            "latitude" : 487429.375,
                            "longitude" : 121425.234
                          }, {
                            "latitude" : 487429.36,
                            "longitude" : 121425.233
                          }, {
                            "latitude" : 487429.345,
                            "longitude" : 121425.233
                          }, {
                            "latitude" : 487429.33,
                            "longitude" : 121425.234
                          }, {
                            "latitude" : 487429.315,
                            "longitude" : 121425.237
                          }, {
                            "latitude" : 487429.301,
                            "longitude" : 121425.24
                          }, {
                            "latitude" : 487429.287,
                            "longitude" : 121425.245
                          }, {
                            "latitude" : 487429.273,
                            "longitude" : 121425.251
                          }, {
                            "latitude" : 487429.26,
                            "longitude" : 121425.258
                          }, {
                            "latitude" : 487429.248,
                            "longitude" : 121425.267
                          }, {
                            "latitude" : 487429.236,
                            "longitude" : 121425.276
                          }, {
                            "latitude" : 487429.225,
                            "longitude" : 121425.286
                          }, {
                            "latitude" : 487429.215,
                            "longitude" : 121425.297
                          }, {
                            "latitude" : 487428.49,
                            "longitude" : 121424.682
                          }, {
                            "latitude" : 487428.499,
                            "longitude" : 121424.67
                          }, {
                            "latitude" : 487428.507,
                            "longitude" : 121424.658
                          }, {
                            "latitude" : 487428.515,
                            "longitude" : 121424.645
                          }, {
                            "latitude" : 487428.521,
                            "longitude" : 121424.631
                          }, {
                            "latitude" : 487428.526,
                            "longitude" : 121424.617
                          }, {
                            "latitude" : 487428.529,
                            "longitude" : 121424.603
                          }, {
                            "latitude" : 487428.532,
                            "longitude" : 121424.588
                          }, {
                            "latitude" : 487428.533,
                            "longitude" : 121424.573
                          }, {
                            "latitude" : 487428.533,
                            "longitude" : 121424.558
                          }, {
                            "latitude" : 487428.532,
                            "longitude" : 121424.543
                          }, {
                            "latitude" : 487428.529,
                            "longitude" : 121424.528
                          }, {
                            "latitude" : 487428.526,
                            "longitude" : 121424.514
                          }, {
                            "latitude" : 487428.521,
                            "longitude" : 121424.5
                          }, {
                            "latitude" : 487428.515,
                            "longitude" : 121424.486
                          }, {
                            "latitude" : 487428.508,
                            "longitude" : 121424.473
                          }, {
                            "latitude" : 487428.499,
                            "longitude" : 121424.461
                          }, {
                            "latitude" : 487428.49,
                            "longitude" : 121424.449
                          }, {
                            "latitude" : 487428.48,
                            "longitude" : 121424.438
                          }, {
                            "latitude" : 487428.469,
                            "longitude" : 121424.428
                          }, {
                            "latitude" : 487425.296,
                            "longitude" : 121421.739
                          }, {
                            "latitude" : 487425.284,
                            "longitude" : 121421.73
                          }, {
                            "latitude" : 487425.272,
                            "longitude" : 121421.722
                          }, {
                            "latitude" : 487425.259,
                            "longitude" : 121421.714
                          }, {
                            "latitude" : 487425.245,
                            "longitude" : 121421.708
                          }, {
                            "latitude" : 487425.231,
                            "longitude" : 121421.703
                          }, {
                            "latitude" : 487425.217,
                            "longitude" : 121421.7
                          }, {
                            "latitude" : 487425.202,
                            "longitude" : 121421.697
                          }, {
                            "latitude" : 487425.187,
                            "longitude" : 121421.696
                          }, {
                            "latitude" : 487425.172,
                            "longitude" : 121421.696
                          }, {
                            "latitude" : 487425.157,
                            "longitude" : 121421.697
                          }, {
                            "latitude" : 487425.142,
                            "longitude" : 121421.7
                          }, {
                            "latitude" : 487425.128,
                            "longitude" : 121421.703
                          }, {
                            "latitude" : 487425.114,
                            "longitude" : 121421.708
                          }, {
                            "latitude" : 487425.1,
                            "longitude" : 121421.714
                          }, {
                            "latitude" : 487425.087,
                            "longitude" : 121421.721
                          }, {
                            "latitude" : 487425.075,
                            "longitude" : 121421.73
                          }, {
                            "latitude" : 487425.063,
                            "longitude" : 121421.739
                          }, {
                            "latitude" : 487425.052,
                            "longitude" : 121421.749
                          }, {
                            "latitude" : 487425.042,
                            "longitude" : 121421.76
                          }, {
                            "latitude" : 487424.318,
                            "longitude" : 121421.146
                          }, {
                            "latitude" : 487424.327,
                            "longitude" : 121421.134
                          }, {
                            "latitude" : 487424.335,
                            "longitude" : 121421.122
                          }, {
                            "latitude" : 487424.343,
                            "longitude" : 121421.109
                          }, {
                            "latitude" : 487424.349,
                            "longitude" : 121421.095
                          }, {
                            "latitude" : 487424.353,
                            "longitude" : 121421.081
                          }, {
                            "latitude" : 487424.357,
                            "longitude" : 121421.066
                          }, {
                            "latitude" : 487424.36,
                            "longitude" : 121421.052
                          }, {
                            "latitude" : 487424.361,
                            "longitude" : 121421.037
                          }, {
                            "latitude" : 487424.361,
                            "longitude" : 121421.022
                          }, {
                            "latitude" : 487424.36,
                            "longitude" : 121421.006
                          }, {
                            "latitude" : 487424.357,
                            "longitude" : 121420.989
                          }, {
                            "latitude" : 487424.352,
                            "longitude" : 121420.973
                          }, {
                            "latitude" : 487424.347,
                            "longitude" : 121420.958
                          }, {
                            "latitude" : 487424.339,
                            "longitude" : 121420.943
                          }, {
                            "latitude" : 487424.331,
                            "longitude" : 121420.929
                          }, {
                            "latitude" : 487424.321,
                            "longitude" : 121420.916
                          }, {
                            "latitude" : 487424.31,
                            "longitude" : 121420.903
                          }, {
                            "latitude" : 487424.298,
                            "longitude" : 121420.892
                          }, {
                            "latitude" : 487421.125,
                            "longitude" : 121418.203
                          }, {
                            "latitude" : 487421.113,
                            "longitude" : 121418.194
                          }, {
                            "latitude" : 487421.101,
                            "longitude" : 121418.186
                          }, {
                            "latitude" : 487421.088,
                            "longitude" : 121418.178
                          }, {
                            "latitude" : 487421.074,
                            "longitude" : 121418.172
                          }, {
                            "latitude" : 487421.06,
                            "longitude" : 121418.167
                          }, {
                            "latitude" : 487421.046,
                            "longitude" : 121418.164
                          }, {
                            "latitude" : 487421.031,
                            "longitude" : 121418.161
                          }, {
                            "latitude" : 487421.016,
                            "longitude" : 121418.16
                          }, {
                            "latitude" : 487421.001,
                            "longitude" : 121418.16
                          }, {
                            "latitude" : 487420.986,
                            "longitude" : 121418.161
                          }, {
                            "latitude" : 487420.971,
                            "longitude" : 121418.164
                          }, {
                            "latitude" : 487420.957,
                            "longitude" : 121418.167
                          }, {
                            "latitude" : 487420.943,
                            "longitude" : 121418.172
                          }, {
                            "latitude" : 487420.929,
                            "longitude" : 121418.178
                          }, {
                            "latitude" : 487420.916,
                            "longitude" : 121418.185
                          }, {
                            "latitude" : 487420.904,
                            "longitude" : 121418.194
                          }, {
                            "latitude" : 487420.892,
                            "longitude" : 121418.203
                          }, {
                            "latitude" : 487420.881,
                            "longitude" : 121418.213
                          }, {
                            "latitude" : 487420.871,
                            "longitude" : 121418.224
                          }, {
                            "latitude" : 487419.994,
                            "longitude" : 121417.48
                          }, {
                            "latitude" : 487419.761,
                            "longitude" : 121417.754
                          }, {
                            "latitude" : 487415.421,
                            "longitude" : 121414.075
                          }, {
                            "latitude" : 487415.324,
                            "longitude" : 121414.19
                          }, {
                            "latitude" : 487404.069,
                            "longitude" : 121404.775
                          }, {
                            "latitude" : 487405.06,
                            "longitude" : 121403.59
                          }, {
                            "latitude" : 487384.63,
                            "longitude" : 121386.49
                          }, {
                            "latitude" : 487367.471,
                            "longitude" : 121407.025
                          }, {
                            "latitude" : 487367.737,
                            "longitude" : 121407.23
                          }, {
                            "latitude" : 487377.357,
                            "longitude" : 121415.216
                          }, {
                            "latitude" : 487373.653,
                            "longitude" : 121419.678
                          }, {
                            "latitude" : 487369.432,
                            "longitude" : 121424.763
                          }, {
                            "latitude" : 487407.169,
                            "longitude" : 121456.069
                          }, {
                            "latitude" : 487408.549,
                            "longitude" : 121454.359
                          }, {
                            "latitude" : 487417.119,
                            "longitude" : 121461.859
                          }, {
                            "latitude" : 487444.779,
                            "longitude" : 121488.34
                          } ] ],
                          "type" : "Polygon"
                        },
                        "omschrijving" : "0363100012168052",
                        "oorspronkelijkBouwjaar" : "1914",
                        "status" : "Pand in gebruik",
                        "statusWeergave" : "Pand in gebruik"
                      } ],
                      "postcode" : "1012JS",
                      "woonplaats" : {
                        "geconstateerd" : false,
                        "identificatie" : "3594",
                        "url" : "http://bag-wiremock.local:8080/lvbag/individuelebevragingen/v2/woonplaatsen/3594",
                        "bagObjectType" : "WOONPLAATS",
                        "naam" : "Amsterdam",
                        "omschrijving" : "Amsterdam",
                        "status" : "Woonplaats aangewezen"
                      },
                      "woonplaatsNaam" : "Amsterdam"
                    }
                """.trimIndent()
            }
        }
    }
})
