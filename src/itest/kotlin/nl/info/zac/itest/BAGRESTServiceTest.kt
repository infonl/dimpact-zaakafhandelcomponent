package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields

@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class BAGRESTServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}

    Given("A zaak exists and address data is present in the BAG API mock") {
        When("the list addresses endpoint is called for the search query 'de dam 1 amsterdam'") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/bag/adres",
                requestBodyAsString = """
                        { "trefwoorden": "de dam 1 amsterdam"}
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
                        "identificatie" : "0363200003761447",
                        "url" : "https://api.bag.kadaster.nl/lvbag/individuelebevragingen/v2/adressen/0363200003761447",
                        "bagObjectType" : "ADRES",
                        "huisnummer" : 1,
                        "huisnummerWeergave" : "1",
                        "nummeraanduiding" : {
                          "geconstateerd" : false,
                          "identificatie" : "0363200003761447",
                          "url" : "https://api.bag.kadaster.nl/lvbag/individuelebevragingen/v2/nummeraanduidingen/0363200003761447",
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
                          "url" : "https://api.bag.kadaster.nl/lvbag/individuelebevragingen/v2/openbareruimten/0363300000003186",
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
                          "url" : "https://api.bag.kadaster.nl/lvbag/individuelebevragingen/v2/woonplaatsen/3594",
                          "bagObjectType" : "WOONPLAATS",
                          "naam" : "Amsterdam",
                          "omschrijving" : "Amsterdam",
                          "status" : "Woonplaats aangewezen"
                        },
                        "woonplaatsNaam" : "Amsterdam"
                      }, {
                        "geconstateerd" : false,
                        "identificatie" : "0363200012113669",
                        "url" : "https://api.bag.kadaster.nl/lvbag/individuelebevragingen/v2/adressen/0363200012113669",
                        "bagObjectType" : "ADRES",                                     
                        "huisnummer" : 1,
                        "huisnummerWeergave" : "1",
                        "nummeraanduiding" : {
                          "geconstateerd" : false,
                          "identificatie" : "0363200012113669",
                          "url" : "https://api.bag.kadaster.nl/lvbag/individuelebevragingen/v2/nummeraanduidingen/0363200012113669",
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
                          "url" : "https://api.bag.kadaster.nl/lvbag/individuelebevragingen/v2/openbareruimten/0363300000003187",
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
                          "url" : "https://api.bag.kadaster.nl/lvbag/individuelebevragingen/v2/woonplaatsen/3594",
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
})
