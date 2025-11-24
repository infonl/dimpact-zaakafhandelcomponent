/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_2024_01_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_2024_01_31
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2024_01_01
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_FACTUUR_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_INTERNAL_ENDPOINTS_API_KEY
import nl.info.zac.itest.config.RAADPLEGER_DOMAIN_TEST_1
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrder
import okhttp3.Headers.Companion.toHeaders
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * This test creates multiple zaken, tasks and documents, indexes them, and performs searches.
 * It is limited in scope because previously run tests may already have created zaken, tasks and documents.
 * In the future, this test could be improved by first deleting all existing zaken, tasks and documents.
 * This cannot be done currently because some of our integration tests are not yet isolated and rely
 * on existing zaken, tasks and documents created by previous tests.
 */
@Suppress("LargeClass")
class SearchRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val logger = KotlinLogging.logger {}

    Context("Listing search results") {
        Given("A logged-in raadpleger and a zaak, a task and a document have been created and are indexed") {
            // log in as a beheerder authorised in all domains to create the zaken, tasks and documents and index them
            authenticate(BEHEERDER_ELK_ZAAKTYPE)
            val zaak1Description = "fakeZaak1DescriptionForSearchRestServiceTest"
            val (zaak1Identification, zaak1Uuid) = createAndIndexZaak(
                zacClient = zacClient,
                itestHttpClient = itestHttpClient,
                logger = logger,
                zaakDescription = zaak1Description
            )
            // TODO: create and index a task and a document
            zacClient.startAanvullendeInformatieTaskForZaak(
                zaakUUID = zaak1Uuid,
                fatalDate = LocalDate.now().plusDays(1),
                group = BEHANDELAARS_DOMAIN_TEST_1
            )
            authenticate(RAADPLEGER_DOMAIN_TEST_1)

            When("the search endpoint is called to search all open zaken in the description of the just created zaak") {
                // keep on calling the search endpoint until we see the expected number of total objects
                // because the indexing will take some time to complete
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zoeken/list",
                    requestBodyAsString = """
                        {
                            "alleenMijnZaken": false,
                            "alleenOpenstaandeZaken": true,
                            "alleenAfgeslotenZaken": false,
                            "alleenMijnTaken": false,
                            "zoeken": { "ZAAK_OMSCHRIJVING": "$zaak1Description" },
                            "filters": {},
                            "datums": {},
                            "rows": 10,
                            "page": 0                  
                        }
                    """.trimIndent()
                )

                Then(
                    "the response is successful and the search results include the newly created zaken, tasks and documents"
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJsonIgnoringOrder """
                        {
                          "foutmelding" : "",
                          "resultaten" : [ {
                            "aantalOpenstaandeTaken" : 0,
                            "afgehandeld" : false,
                            "betrokkenen" : {
                              "Behandelaar" : [ "${BEHANDELAARS_DOMAIN_TEST_1.name}" ]
                            },
                            "communicatiekanaal" : "$COMMUNICATIEKANAAL_TEST_1",
                            "groepId" : "${BEHANDELAARS_DOMAIN_TEST_1.name}",
                            "groepNaam" : "${BEHANDELAARS_DOMAIN_TEST_1.description}",
                            "id" : "$zaak1Uuid",
                            "identificatie" : "$zaak1Identification",
                            "indicatieDeelzaak" : false,
                            "indicatieHeropend" : false,
                            "indicatieHoofdzaak" : false,
                            "indicatieOpschorting" : false,
                            "indicatieVerlenging" : false,
                            "indicaties" : [ ],
                            "omschrijving" : "$zaak1Description",
                            "rechten" : {
                              "afbreken" : false,
                              "behandelen" : false,
                              "bekijkenZaakdata" : false,
                              "creerenDocument" : false,
                              "heropenen" : false,
                              "lezen" : true,
                              "toekennen" : false,
                              "toevoegenBagObject" : false,
                              "toevoegenBetrokkeneBedrijf" : false,
                              "toevoegenBetrokkenePersoon" : false,
                              "toevoegenInitiatorBedrijf" : false,
                              "toevoegenInitiatorPersoon" : false,
                              "versturenEmail" : false,
                              "versturenOntvangstbevestiging" : false,
                              "verwijderenBetrokkene" : false,
                              "verwijderenInitiator" : false,
                              "wijzigen" : false,
                              "wijzigenDoorlooptijd" : false,
                              "wijzigenLocatie" : false
                            },
                            "registratiedatum" : "${LocalDate.now()}",
                            "startdatum" : "$DATE_2024_01_01",
                            "statusToelichting" : "Status gewijzigd",
                            "statustypeOmschrijving" : "Intake",
                            "toelichting" : "null",
                            "type" : "ZAAK",
                            "uiterlijkeEinddatumAfdoening" : "$DATE_2024_01_31",
                            "vertrouwelijkheidaanduiding" : "OPENBAAR",
                            "zaaktypeOmschrijving" : "$ZAAKTYPE_TEST_2_DESCRIPTION"
                          } ],
                          "totaal" : 1,
                          "filters" : {
                            "TYPE" : [ {
                              "aantal" : 1,
                              "naam" : "ZAAK"
                            } ],
                            "ZAAKTYPE" : [ {
                              "aantal" : 1,
                              "naam" : "$ZAAKTYPE_TEST_2_DESCRIPTION"
                            } ],
                            "BEHANDELAAR" : [ ],
                            "GROEP" : [ {
                              "aantal" : 1,
                              "naam" : "${BEHANDELAARS_DOMAIN_TEST_1.description}"
                            } ],
                            "TOEGEKEND" : [ {
                              "aantal" : 1,
                              "naam" : "false"
                            } ],
                            "ZAAK_STATUS" : [ {
                              "aantal" : 1,
                              "naam" : "Intake"
                            } ],
                            "ZAAK_RESULTAAT" : [ ],
                            "ZAAK_INDICATIES" : [ ],
                            "ZAAK_COMMUNICATIEKANAAL" : [ {
                              "aantal" : 1,
                              "naam" : "$COMMUNICATIEKANAAL_TEST_1"
                            } ],
                            "ZAAK_VERTROUWELIJKHEIDAANDUIDING" : [ {
                              "aantal" : 1,
                              "naam" : "OPENBAAR"
                            } ],
                            "ZAAK_ARCHIEF_NOMINATIE" : [ ],
                            "TAAK_NAAM" : [ ],
                            "TAAK_STATUS" : [ ],
                            "DOCUMENT_STATUS" : [ ],
                            "DOCUMENT_TYPE" : [ ],
                            "DOCUMENT_VERGRENDELD_DOOR" : [ ],
                            "DOCUMENT_INDICATIES" : [ ]
                          }
                        }
                    """.trimIndent()

//                    {
//                        "foutmelding": "",
//                        "totaal": ${TOTAL_COUNT_INDEXED_ZAKEN + TOTAL_COUNT_INDEXED_TASKS + TOTAL_COUNT_INDEXED_DOCUMENTS},
//                        "filters": {
//                            "TYPE": [
//                                {
//                                    "aantal": $TOTAL_COUNT_INDEXED_ZAKEN,
//                                    "naam": "ZAAK"
//                                },
//                                {
//                                    "aantal": $TOTAL_COUNT_INDEXED_TASKS,
//                                    "naam": "TAAK"
//                                },
//                                {
//                                    "aantal": $TOTAL_COUNT_INDEXED_DOCUMENTS,
//                                    "naam": "DOCUMENT"
//                                }
//                            ],
//                            "ZAAKTYPE": [
//                                {
//                                    "aantal": 7,
//                                    "naam": "$ZAAKTYPE_TEST_2_DESCRIPTION"
//                                },
//                                {
//                                    "aantal": 19,
//                                    "naam": "$ZAAKTYPE_TEST_3_DESCRIPTION"
//                                },
//                                {
//                                    "aantal": 6,
//                                    "naam": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION"
//                                 }
//                            ],
//                            "BEHANDELAAR": [
//                                {
//                                    "aantal": 1,
//                                    "naam": "${BEHANDELAAR_DOMAIN_TEST_1.displayName}"
//                                }
//                            ],
//                            "GROEP": [
//                                {
//                                    "aantal": 18,
//                                    "naam": "${BEHANDELAARS_DOMAIN_TEST_1.description}"
//                                }
//                            ],
//                            "TOEGEKEND": [
//                                {
//                                    "aantal": 17,
//                                    "naam": "false"
//                                },
//                                {
//                                    "aantal": 1,
//                                    "naam": "true"
//                                }
//                            ],
//                            "ZAAK_STATUS": [
//                                {
//                                    "aantal": 2,
//                                    "naam": "Wacht op aanvullende informatie"
//                                },
//                                {
//                                    "aantal": 7,
//                                    "naam": "Intake"
//                                },
//                                {
//                                    "aantal": 2,
//                                    "naam": "Afgerond"
//                                },
//                                {
//                                    "aantal": 1,
//                                    "naam": "In behandeling"
//                                }
//                            ],
//                           "ZAAK_RESULTAAT" : [
//                                { "aantal": 2, "naam": "Buiten behandeling" }
//                            ],
//                            "ZAAK_INDICATIES": [
//                                {
//                                    "aantal": 1,
//                                    "naam": "VERLENGD"
//                                }
//                            ],
//                            "ZAAK_COMMUNICATIEKANAAL": [
//                                {
//                                    "aantal": 8,
//                                    "naam": "$COMMUNICATIEKANAAL_TEST_1"
//                                },
//                                {
//                                    "aantal": 1,
//                                    "naam": "$COMMUNICATIEKANAAL_TEST_2"
//                                },
//                                {
//                                    "aantal": 5,
//                                    "naam": "E-formulier"
//                                }
//                            ],
//                            "ZAAK_VERTROUWELIJKHEIDAANDUIDING": [
//                                {
//                                    "aantal": 14,
//                                    "naam": "OPENBAAR"
//                                }
//                            ],
//                            "ZAAK_ARCHIEF_NOMINATIE": [
//                                {
//                                    "aantal": 2,
//                                    "naam": "VERNIETIGEN"
//                                }
//                            ],
//                            "TAAK_NAAM": [
//                                {
//                                    "aantal": 2,
//                                    "naam": "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM"
//                                },
//                                {
//                                    "aantal": 2,
//                                    "naam": "$BPMN_TEST_TASK_NAME"
//                                }
//                            ],
//                            "TAAK_STATUS": [
//                                {
//                                    "aantal": 3,
//                                    "naam": "NIET_TOEGEKEND"
//                                },
//                                {
//                                    "aantal": 1,
//                                    "naam": "TOEGEKEND"
//                                }
//                            ],
//                            "DOCUMENT_STATUS": [
//                                {
//                                    "aantal": 10,
//                                    "naam": "definitief"
//                                },
//                                {
//                                    "aantal": 4,
//                                    "naam": "in_bewerking"
//                                }
//                            ],
//                            "DOCUMENT_TYPE": [
//                                {
//                                    "aantal": 9,
//                                    "naam": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING"
//                                },
//                                {
//                                    "aantal": 4,
//                                    "naam": "$INFORMATIE_OBJECT_TYPE_EMAIL_OMSCHRIJVING"
//                                },
//                                {
//                                    "aantal": 1,
//                                    "naam": "$INFORMATIE_OBJECT_TYPE_FACTUUR_OMSCHRIJVING"
//                                }
//                            ],
//                            "DOCUMENT_VERGRENDELD_DOOR": [],
//                            "DOCUMENT_INDICATIES": [
//                                {
//                                    "aantal": 11,
//                                    "naam": "GEBRUIKSRECHT"
//                                },
//                                {
//                                    "aantal": 14,
//                                    "naam": "ONDERTEKEND"
//                                },
//                                {
//                                    "aantal": 4,
//                                    "naam": "VERZONDEN"
//                                }
//                            ]
//                        }
//                    }
//                        """.trimIndent()
                }
            }

            When(
                """
                the search endpoint is called to search for all objects of type 'ZAAK' filtered on a specific zaaktype
                and sorted on zaaktype
                """.trimMargin()
            ) {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zoeken/list",
                    requestBodyAsString = """
                   {
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": true,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": {},
                    "filters": {
                        "ZAAKTYPE": {
                             "values": [
                               "$ZAAKTYPE_TEST_2_DESCRIPTION"
                             ],
                             "inverse": "false"
                        }
                      },
                    "datums": {},
                    "rows": 10,
                    "page" :0,
                    "type": "ZAAK",
                    "sorteerRichting": "asc",
                    "sorteerVeld": "ZAAK_ZAAKTYPE"
                    }
                    """.trimIndent()
                )
                Then(
                    """
                   the response is successful and the search results include the indexed zaken for this zaaktype only
                """
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    // TODO
//                    responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
//                    {
//                      "foutmelding" : "",
//                      "totaal" : 4,
//                      "filters" : {
//                        "ZAAKTYPE" : [ {
//                          "aantal" : 6,
//                          "naam" : "$ZAAKTYPE_TEST_3_DESCRIPTION"
//                        }, {
//                          "aantal" : 4,
//                          "naam" : "$ZAAKTYPE_TEST_2_DESCRIPTION"
//                        }, {
//                          "aantal": 2,
//                          "naam": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION"
//                        }],
//                        "BEHANDELAAR": [
//                          {
//                            "aantal": 4,
//                            "naam": "-NULL-"
//                          }
//                        ],
//                        "GROEP" : [
//                            {
//                                "aantal": 4,
//                                "naam": "${BEHANDELAARS_DOMAIN_TEST_1.description}"
//                            }
//                        ],
//                        "ZAAK_STATUS" : [
//                           {
//                                "aantal": 2,
//                                "naam": "Intake"
//                           },
//                           {
//                                "aantal": 1,
//                                "naam": "In behandeling"
//                            },
//                            {
//                                "aantal" : 1,
//                                "naam" : "Wacht op aanvullende informatie"
//                            }
//                         ],
//                        "ZAAK_RESULTAAT" : [
//                          {
//                            "aantal" : 4,
//                            "naam" : "-NULL-"
//                          }
//                        ],
//                        "ZAAK_INDICATIES" : [
//                          {
//                            "aantal" : 4,
//                            "naam" : "-NULL-"
//                          }
//                        ],
//                        "ZAAK_COMMUNICATIEKANAAL" : [ {
//                          "aantal" : 4,
//                          "naam" : "$COMMUNICATIEKANAAL_TEST_1"
//                        } ],
//                        "ZAAK_VERTROUWELIJKHEIDAANDUIDING" : [ {
//                          "aantal" : 4,
//                          "naam" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR"
//                        } ],
//                        "ZAAK_ARCHIEF_NOMINATIE" : [ {
//                          "aantal" : 4,
//                          "naam" : "-NULL-"
//                        } ]
//                      }
//                    }
//                    """.trimIndent()
                }
            }

            When(
                """
                the search endpoint is called to search for all objects of type 'ZAAK' with a specific information 
                object UUID
                """.trimMargin()
            ) {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zoeken/zaken",
                    requestBodyAsString = """
                {
                    "rows": 5,
                    "page": 0,
                    "zaakIdentificator": "zaak",
                    "informationObjectTypeUuid": "$INFORMATIE_OBJECT_TYPE_FACTUUR_UUID"
                }
                    """.trimIndent()
                )
                Then(
                    """
                   the response is successful and the search results include the indexed zaken with compatibility info 
                   about the information object type UUID
                    """.trimMargin()
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    // TODO
//                    responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
//                {
//                  "foutmelding": "",
//                  "resultaten": [
//                    {
//                      "isKoppelbaar": true,
//                      "identificatie": "$ZAAK_MANUAL_2024_01_IDENTIFICATION",
//                      "type": "ZAAK"
//                    },
//                    {
//                      "isKoppelbaar": false,
//                      "identificatie": "$ZAAK_MANUAL_2020_01_IDENTIFICATION",
//                      "type": "ZAAK"
//                    },
//                    {
//                      "isKoppelbaar": true,
//                      "identificatie": "ZAAK-2000-0000000007",
//                      "type": "ZAAK"
//                    },
//                    {
//                      "isKoppelbaar": true,
//                      "identificatie": "ZAAK-2000-0000000006",
//                      "type": "ZAAK"
//                    },
//                    {
//                      "isKoppelbaar": true,
//                      "identificatie": "ZAAK-2000-0000000005",
//                      "type": "ZAAK"
//                    }
//                  ],
//                  "totaal": ${TOTAL_COUNT_INDEXED_ZAKEN},
//                  "filters": {}
//                }
//                    """.trimIndent()
                }
            }

            When(
                """
                the search endpoint is called to search for all objects of type 'ZAAK' with a non-existing information 
                object UUID
                """.trimMargin()
            ) {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zoeken/zaken",
                    requestBodyAsString = """
                {
                    "rows": 5,
                    "page": 0,
                    "zaakIdentificator": "zaak",
                    "informationObjectTypeUuid": "a47b0c7e-1d0d-4c33-918d-160677516f1c"
                }
                    """.trimIndent()
                )
                Then("the response is successful and search results contain no zaak that can be linked to") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    // TODO
//                    responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
//                {
//                  "foutmelding": "",
//                  "resultaten": [
//                    {
//                      "isKoppelbaar": false,
//                      "identificatie": "$ZAAK_MANUAL_2024_01_IDENTIFICATION",
//                      "type": "ZAAK"
//                    },
//                    {
//                      "isKoppelbaar": false,
//                      "identificatie": "$ZAAK_MANUAL_2020_01_IDENTIFICATION",
//                      "type": "ZAAK"
//                    },
//                    {
//                      "isKoppelbaar": false,
//                      "identificatie": "ZAAK-2000-0000000007",
//                      "type": "ZAAK"
//                    },
//                    {
//                      "isKoppelbaar": false,
//                      "identificatie": "ZAAK-2000-0000000006",
//                      "type": "ZAAK"
//                    },
//                    {
//                      "isKoppelbaar": false,
//                      "identificatie": "ZAAK-2000-0000000005",
//                      "type": "ZAAK"
//                    }
//                  ],
//                  "totaal": $TOTAL_COUNT_INDEXED_ZAKEN,
//                  "filters": {}
//                }
//                    """.trimIndent()
                }
            }

            When(
                """the search endpoint is called to search for all objects of type 'TAAK'"""
            ) {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zoeken/list",
                    requestBodyAsString = """
                   {
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": false,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": {},
                    "filters": {},
                    "datums": {},
                    "rows": 10,
                    "page": 0,
                    "type": "TAAK"
                    }
                    """.trimIndent()
                )
                Then(
                    """
                    the response is successful and the search results include the filters corresponding to
                     the indexed tasks, and the returned permissions are those for the raadpleger role
                    """.trimMargin()
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    // we do not test on the actual results ('resultaten' attribute) to keep the test somewhat maintainable
                    // TODO
//                    /responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
//                    {
//                        "foutmelding": "",
//                        "totaal": 4,
//                        "filters": {
//                            "ZAAKTYPE": [
//                                {
//                                    "aantal": 2,
//                                    "naam": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION"
//                                },
//                                {
//                                    "aantal": 1,
//                                    "naam": "$ZAAKTYPE_TEST_2_DESCRIPTION"
//                                },
//                                {
//                                    "aantal": 1,
//                                    "naam": "$ZAAKTYPE_TEST_3_DESCRIPTION"
//                                }
//                            ],
//                            "BEHANDELAAR": [
//                                {
//                                    "aantal": 1,
//                                    "naam": "${BEHANDELAAR_DOMAIN_TEST_1.displayName}"
//                                },
//                                {
//                                    "aantal": 3,
//                                    "naam": "-NULL-"
//                                }
//                            ],
//                            "GROEP": [
//                                {
//                                    "aantal": 4,
//                                    "naam": "${BEHANDELAARS_DOMAIN_TEST_1.description}"
//                                }
//                            ],
//                            "TAAK_NAAM": [
//                                {
//                                    "aantal": 2,
//                                    "naam": "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM"
//                                },
//                                {
//                                    "aantal": 2,
//                                    "naam": "$BPMN_TEST_TASK_NAME"
//                                }
//                            ],
//                            "TAAK_STATUS": [
//                                {
//                                    "aantal": 3,
//                                    "naam": "NIET_TOEGEKEND"
//                                },
//                                {
//                                    "aantal": 1,
//                                    "naam": "TOEGEKEND"
//                                }
//                            ]
//                        }
//                    }
//                    """.trimIndent()
                }
            }

            When(
                """the search endpoint is called to search for all objects of type 'DOCUMENT'"""
            ) {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zoeken/list",
                    requestBodyAsString = """
                   {
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": false,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": {},
                    "filters": {},
                    "datums": {},
                    "rows": 10,
                    "page": 0,
                    "type": "DOCUMENT"
                    }
                    """.trimIndent()
                )
                Then(
                    """
                    the response is successful and the search results should include the expected number of indexed documents
                    and the expected search filters
                    """.trimMargin()
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    // TODO
//
//                    JSONObject(responseBody).getInt("totaal") shouldBe TOTAL_COUNT_INDEXED_DOCUMENTS
//                    JSONObject(responseBody).getJSONObject("filters")
//                        .toString() shouldEqualJsonIgnoringOrderAndExtraneousFields """
//                      {
//                        "ZAAKTYPE" : [ {
//                          "aantal" : 12,
//                          "naam" : "$ZAAKTYPE_TEST_3_DESCRIPTION"
//                        }, {
//                          "aantal": 2,
//                          "naam": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION"
//                        } ],
//                        "DOCUMENT_STATUS" : [ {
//                          "aantal" : 10,
//                          "naam" : "$DOCUMENT_STATUS_DEFINITIEF"
//                        }, {
//                          "aantal" : 4,
//                          "naam" : "$DOCUMENT_STATUS_IN_BEWERKING"
//                        } ],
//                        "DOCUMENT_TYPE" : [ {
//                          "aantal" : 9,
//                          "naam" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING"
//                        }, {
//                          "aantal" : 4,
//                          "naam" : "$INFORMATIE_OBJECT_TYPE_EMAIL_OMSCHRIJVING"
//                        }, {
//                          "aantal" : 1,
//                          "naam" : "$INFORMATIE_OBJECT_TYPE_FACTUUR_OMSCHRIJVING"
//                        } ],
//                        "DOCUMENT_VERGRENDELD_DOOR" : [ {
//                          "aantal" : $TOTAL_COUNT_INDEXED_DOCUMENTS,
//                          "naam" : "-NULL-"
//                        } ],
//                        "DOCUMENT_INDICATIES" : [ {
//                          "aantal" : $TOTAL_COUNT_INDEXED_DOCUMENTS,
//                          "naam" : "ONDERTEKEND"
//                        }, {
//                          "aantal" : 11,
//                          "naam" : "GEBRUIKSRECHT"
//                        }, {
//                          "aantal" : 4,
//                          "naam" : "VERZONDEN"
//                        } ]
//                      }
//                """
                }
            }

            When(
                """the search endpoint is called to search for a specific document"""
            ) {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zoeken/list",
                    requestBodyAsString = """
                   {
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": false,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": { "DOCUMENT_TITEL": "Fake test document" },
                    "filters": {},
                    "datums": {},
                    "rows": 10,
                    "page": 0,
                    "type": "DOCUMENT"
                    }
                    """.trimIndent()
                )
                Then(
                    """
                    the response is successful and the search results should include the expected document
                    and the returned permissions are those for the raadpleger role
                    """.trimMargin()
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    // TODO
//                    responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
//                    {
//                      "foutmelding" : "",
//                      "resultaten" : [ {
//                        "type" : "DOCUMENT",
//                        "auteur" : "Aanvrager",
//                        "beschrijving" : "Ingezonden formulier",
//                        "bestandsnaam" : "$ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_FILE_NAME",
//                        "bestandsomvang" : 1234,
//                        "creatiedatum" : "$ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_CREATION_DATE",
//                        "documentType" : "bijlage",
//                        "formaat" : "application/pdf",
//                        "indicatieGebruiksrecht" : false,
//                        "indicatieOndertekend" : true,
//                        "indicatieVergrendeld" : false,
//                        "indicaties" : [ "ONDERTEKEND" ],
//                        "rechten" : {
//                          "lezen" : true,
//                          "ondertekenen" : false,
//                          "ontgrendelen" : false,
//                          "toevoegenNieuweVersie" : false,
//                          "vergrendelen" : false,
//                          "verwijderen" : false,
//                          "wijzigen" : false
//                        },
//                        "registratiedatum" : "$ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_CREATION_DATE",
//                        "status" : "definitief",
//                        "titel" : "$ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_TITEL",
//                        "versie" : 1,
//                        "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK",
//                        "zaakIdentificatie" : "$ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION",
//                        "zaakRelatie" : "Hoort bij, omgekeerd: kent",
//                        "zaaktypeIdentificatie" : "$ZAAKTYPE_TEST_3_IDENTIFICATIE",
//                        "zaaktypeOmschrijving" : "$ZAAKTYPE_TEST_3_DESCRIPTION",
//                        "zaaktypeUuid" : "$ZAAKTYPE_TEST_3_UUID"
//                      }, {
//                        "type" : "DOCUMENT"
//                      }, {
//                        "type" : "DOCUMENT"
//                      } ],
//                      "totaal" : 3.0,
//                      "filters" : {
//                        "ZAAKTYPE" : [ {
//                          "aantal" : 3,
//                          "naam" : "$ZAAKTYPE_TEST_3_DESCRIPTION"
//                        } ],
//                        "DOCUMENT_STATUS" : [ {
//                          "aantal" : 3,
//                          "naam" : "$DOCUMENT_STATUS_DEFINITIEF"
//                        } ],
//                        "DOCUMENT_TYPE" : [ {
//                          "aantal" : 3,
//                          "naam" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING"
//                        } ],
//                        "DOCUMENT_VERGRENDELD_DOOR" : [ {
//                          "aantal" : 3,
//                          "naam" : "-NULL-"
//                        } ],
//                        "DOCUMENT_INDICATIES" : [ {
//                          "aantal" : 3,
//                          "naam" : "ONDERTEKEND"
//                        } ]
//                      }
//                    }
//                """
                }
            }
        }
    }
})

private suspend fun createAndIndexZaak(
    zacClient: ZacClient,
    itestHttpClient: ItestHttpClient,
    logger: KLogger,
    zaakDescription: String,
): Pair<String, UUID> {
    var zaak1Identification1: String
    var zaak1Uuid1: UUID
    zacClient.createZaak(
        description = zaakDescription,
        groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
        groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
        startDate = DATE_TIME_2024_01_01,
        zaakTypeUUID = ZAAKTYPE_TEST_2_UUID
    ).run {
        logger.info { "Response: $bodyAsString" }
        code shouldBe HTTP_OK
        JSONObject(bodyAsString).run {
            zaak1Identification1 = getString("identificatie")
            zaak1Uuid1 = getString("uuid").run(UUID::fromString)
        }
    }
    // (re)index all zaken
    itestHttpClient.performGetRequest(
        url = "$ZAC_API_URI/internal/indexeren/herindexeren/ZAAK",
        headers = mapOf(
            "Content-Type" to "application/json",
            "X-API-KEY" to ZAC_INTERNAL_ENDPOINTS_API_KEY
        ).toHeaders(),
        addAuthorizationHeader = false
    ).run {
        code shouldBe HTTP_NO_CONTENT
    }
    // wait for the indexing to complete by searching for the newly created zaak until we get the expected result
    eventually(10.seconds) {
        val response = itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/zoeken/list",
            requestBodyAsString = """
                {
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": true,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": { "ZAAK_OMSCHRIJVING": "$zaakDescription" },
                    "filters": {},
                    "datums": {},
                    "rows": 1,
                    "page": 0,
                    "type": "ZAAK"
                }
            """.trimIndent()
        )
        JSONObject(response.bodyAsString).getInt("totaal") shouldBe 1
    }
    return Pair(zaak1Identification1, zaak1Uuid1)
}
