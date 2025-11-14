/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_2
import nl.info.zac.itest.config.ItestConfiguration.DATE_2024_01_01
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_DEFINITIEF
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
import nl.info.zac.itest.config.ItestConfiguration.HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_EMAIL_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_FACTUUR_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_FACTUUR_UUID
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_1_BRON_KENMERK
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_BPMN_BRON_KENMERK
import nl.info.zac.itest.config.ItestConfiguration.OPEN_FORMULIEREN_FORMULIER_BRON_NAAM
import nl.info.zac.itest.config.ItestConfiguration.TAAK_1_FATAL_DATE
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_REINDEXING
import nl.info.zac.itest.config.ItestConfiguration.TOTAL_COUNT_INDEXED_DOCUMENTS
import nl.info.zac.itest.config.ItestConfiguration.TOTAL_COUNT_INDEXED_TASKS
import nl.info.zac.itest.config.ItestConfiguration.TOTAL_COUNT_INDEXED_ZAKEN
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_BPMN_TEST_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_DESCRIPTION_1
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_MANUAL_2020_01_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_MANUAL_2024_01_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_TOELICHTING
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_CREATION_DATE
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_TITEL
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_BPMN_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.OLD_IAM_TEST_GROUP_A
import nl.info.zac.itest.config.RAADPLEGER_DOMAIN_TEST_1
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK

/**
 * Run this test after reindexing so that all the required data is available in the Solr index.
 * Note that this test is currently heavily dependent on data created by previously run integration tests.
 * It would be good to make this test much more isolated because it is hard to maintain in its current form.
 */
@Order(TEST_SPEC_ORDER_AFTER_REINDEXING)
@Suppress("LargeClass")
class SearchRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}

    beforeSpec {
        authenticate(RAADPLEGER_DOMAIN_TEST_1)
    }

    afterSpec {
        // re-authenticate as beheerder since currently subsequent integration tests rely on this user being logged in
        authenticate(BEHEERDER_ELK_ZAAKTYPE)
    }

    Given("A logged-in raadpleger and multiple zaken, tasks and documents have been created and are indexed") {
        When("the search endpoint is called to search for all objects of all types") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zoeken/list",
                requestBodyAsString = """
                   {
                    "filtersType": "ZoekParameters",
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": false,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": { "ALLE": "" },
                    "filters": {},
                    "datums": {},
                    "rows": 10,
                    "page": 0                  
                    }
                """.trimIndent()
            )
            Then("the response is successful and the search results include the indexed zaken, tasks and documents") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK

                // we only test on the total number of results and the filters, not on the actual results,
                // to keep the test more or less maintainable
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                        "foutmelding": "",
                        "totaal": ${TOTAL_COUNT_INDEXED_ZAKEN + TOTAL_COUNT_INDEXED_TASKS + TOTAL_COUNT_INDEXED_DOCUMENTS},
                        "filters": {
                            "TYPE": [
                                {
                                    "aantal": $TOTAL_COUNT_INDEXED_ZAKEN,
                                    "naam": "ZAAK"
                                },                           
                                {
                                    "aantal": $TOTAL_COUNT_INDEXED_TASKS,
                                    "naam": "TAAK"
                                },
                                {
                                    "aantal": $TOTAL_COUNT_INDEXED_DOCUMENTS,
                                    "naam": "DOCUMENT"
                                }
                            ],
                            "ZAAKTYPE": [
                                {
                                    "aantal": 7,
                                    "naam": "$ZAAKTYPE_TEST_2_DESCRIPTION"
                                },
                                {
                                    "aantal": 19,
                                    "naam": "$ZAAKTYPE_TEST_3_DESCRIPTION"
                                },
                                {
                                    "aantal": 6,
                                    "naam": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION"
                                 }
                            ],
                            "BEHANDELAAR": [
                                {
                                    "aantal": 1,
                                    "naam": "${BEHANDELAAR_DOMAIN_TEST_1.displayName}"
                                }
                            ],            
                            "GROEP": [
                                {
                                    "aantal": 11,
                                    "naam": "${OLD_IAM_TEST_GROUP_A.description}"
                                },
                                {
                                    "aantal": 7,
                                    "naam": "${BEHANDELAARS_DOMAIN_TEST_1.description}"
                                }
                            ],         
                            "TOEGEKEND": [
                                {
                                    "aantal": 17,
                                    "naam": "false"
                                },
                                {
                                    "aantal": 1,
                                    "naam": "true"
                                }
                            ],
                            "ZAAK_STATUS": [
                                {
                                    "aantal": 2,
                                    "naam": "Wacht op aanvullende informatie"
                                },
                                {
                                    "aantal": 7,
                                    "naam": "Intake"
                                },
                                {
                                    "aantal": 2,
                                    "naam": "Afgerond"
                                },
                                {
                                    "aantal": 1,
                                    "naam": "In behandeling"
                                }
                            ],
                           "ZAAK_RESULTAAT" : [
                                { "aantal": 2, "naam": "Buiten behandeling" }
                            ],
                            "ZAAK_INDICATIES": [                        
                                {
                                    "aantal": 1,
                                    "naam": "VERLENGD"
                                }
                            ],
                            "ZAAK_COMMUNICATIEKANAAL": [
                                {
                                    "aantal": 8,
                                    "naam": "$COMMUNICATIEKANAAL_TEST_1"
                                },
                                {
                                    "aantal": 1,
                                    "naam": "$COMMUNICATIEKANAAL_TEST_2"
                                },
                                {
                                    "aantal": 5,
                                    "naam": "E-formulier"
                                }                               
                            ],
                            "ZAAK_VERTROUWELIJKHEIDAANDUIDING": [
                                {
                                    "aantal": 14,
                                    "naam": "OPENBAAR"
                                }
                            ],
                            "ZAAK_ARCHIEF_NOMINATIE": [
                                {
                                    "aantal": 2,
                                    "naam": "VERNIETIGEN"
                                }
                            ],
                            "TAAK_NAAM": [
                                {
                                    "aantal": 2,
                                    "naam": "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM"
                                },
                                {
                                    "aantal": 2,
                                    "naam": "$BPMN_TEST_TASK_NAME"
                                }
                            ],
                            "TAAK_STATUS": [
                                {
                                    "aantal": 3,
                                    "naam": "NIET_TOEGEKEND"
                                },
                                {
                                    "aantal": 1,
                                    "naam": "TOEGEKEND"
                                }
                            ],
                            "DOCUMENT_STATUS": [
                                {
                                    "aantal": 10,
                                    "naam": "definitief"
                                },
                                {
                                    "aantal": 4,
                                    "naam": "in_bewerking"
                                }
                            ],
                            "DOCUMENT_TYPE": [
                                {
                                    "aantal": 9,
                                    "naam": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING"
                                },
                                {
                                    "aantal": 4,
                                    "naam": "$INFORMATIE_OBJECT_TYPE_EMAIL_OMSCHRIJVING"
                                },
                                {
                                    "aantal": 1,
                                    "naam": "$INFORMATIE_OBJECT_TYPE_FACTUUR_OMSCHRIJVING"
                                }
                            ],
                            "DOCUMENT_VERGRENDELD_DOOR": [],
                            "DOCUMENT_INDICATIES": [
                                {
                                    "aantal": 11,
                                    "naam": "GEBRUIKSRECHT"
                                },
                                {
                                    "aantal": 14,
                                    "naam": "ONDERTEKEND"
                                },
                                {
                                    "aantal": 4,
                                    "naam": "VERZONDEN"
                                }
                            ]
                        }
                    }
                """.trimIndent()
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
                    "filtersType": "ZoekParameters",
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
                    "page":0,
                    "type":"ZAAK",
                    "sorteerRichting":"asc",
                    "sorteerVeld":"ZAAK_ZAAKTYPE"
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
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "foutmelding" : "",                     
                      "totaal" : 4,
                      "filters" : {
                        "ZAAKTYPE" : [ {
                          "aantal" : 6,
                          "naam" : "$ZAAKTYPE_TEST_3_DESCRIPTION"
                        }, {
                          "aantal" : 4,
                          "naam" : "$ZAAKTYPE_TEST_2_DESCRIPTION"
                        }, {
                          "aantal": 2,
                          "naam": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION"
                        }],          
                        "BEHANDELAAR": [
                          {
                            "aantal": 4,
                            "naam": "-NULL-"
                          }
                        ],
                        "GROEP" : [ 
                            {
                              "aantal" : 3,
                              "naam" : "${OLD_IAM_TEST_GROUP_A.description}"
                            },
                            {
                                "aantal": 1,
                                "naam": "${BEHANDELAARS_DOMAIN_TEST_1.description}"
                            }
                        ],
                        "ZAAK_STATUS" : [
                           {
                                "aantal": 2,
                                "naam": "Intake"
                           },
                           {
                                "aantal": 1,
                                "naam": "In behandeling"
                            },
                            {
                                "aantal" : 1,
                                "naam" : "Wacht op aanvullende informatie"
                            } 
                         ],
                        "ZAAK_RESULTAAT" : [
                          {
                            "aantal" : 4,
                            "naam" : "-NULL-"
                          }
                        ],
                        "ZAAK_INDICATIES" : [                    
                          {                       
                            "aantal" : 4,
                            "naam" : "-NULL-"
                          } 
                        ],
                        "ZAAK_COMMUNICATIEKANAAL" : [ {                    
                          "aantal" : 4,
                          "naam" : "$COMMUNICATIEKANAAL_TEST_1"
                        } ],
                        "ZAAK_VERTROUWELIJKHEIDAANDUIDING" : [ {
                          "aantal" : 4,
                          "naam" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR"
                        } ],
                        "ZAAK_ARCHIEF_NOMINATIE" : [ {
                          "aantal" : 4,
                          "naam" : "-NULL-"
                        } ]
                      }
                    }
                """.trimIndent()
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
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                {
                  "foutmelding": "",
                  "resultaten": [
                    {
                      "isKoppelbaar": true,
                      "identificatie": "$ZAAK_MANUAL_2024_01_IDENTIFICATION",
                      "type": "ZAAK"
                    },
                    {
                      "isKoppelbaar": false,
                      "identificatie": "$ZAAK_MANUAL_2020_01_IDENTIFICATION",
                      "type": "ZAAK"
                    },
                    {
                      "isKoppelbaar": true,
                      "identificatie": "ZAAK-2000-0000000007",
                      "type": "ZAAK"
                    },
                    {
                      "isKoppelbaar": true,
                      "identificatie": "ZAAK-2000-0000000006",
                      "type": "ZAAK"
                    },
                    {
                      "isKoppelbaar": true,
                      "identificatie": "ZAAK-2000-0000000005",
                      "type": "ZAAK"
                    }
                  ],
                  "totaal": ${TOTAL_COUNT_INDEXED_ZAKEN},
                  "filters": {}
                }
                """.trimIndent()
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
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                {
                  "foutmelding": "",
                  "resultaten": [
                    {
                      "isKoppelbaar": false,
                      "identificatie": "$ZAAK_MANUAL_2024_01_IDENTIFICATION",
                      "type": "ZAAK"
                    },
                    {
                      "isKoppelbaar": false,
                      "identificatie": "$ZAAK_MANUAL_2020_01_IDENTIFICATION",
                      "type": "ZAAK"
                    },
                    {
                      "isKoppelbaar": false,
                      "identificatie": "ZAAK-2000-0000000007",
                      "type": "ZAAK"
                    },
                    {
                      "isKoppelbaar": false,
                      "identificatie": "ZAAK-2000-0000000006",
                      "type": "ZAAK"
                    },
                    {
                      "isKoppelbaar": false,
                      "identificatie": "ZAAK-2000-0000000005",
                      "type": "ZAAK"
                    }
                  ],
                  "totaal": $TOTAL_COUNT_INDEXED_ZAKEN,
                  "filters": {}
                }
                """.trimIndent()
            }
        }

        When(
            """the search endpoint is called to search for all objects of type 'TAAK'"""
        ) {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zoeken/list",
                requestBodyAsString = """
                   {
                    "filtersType": "ZoekParameters",
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": false,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": {},
                    "filters": {},
                    "datums": {},
                    "rows": 10,
                    "page":0,
                    "type":"TAAK"
                    }
                """.trimIndent()
            )
            Then(
                """
                    the response is successful and the search results include the indexed taken 
                    and the returned permissions are those for the raadpleger role
                """.trimMargin()
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """                                          
                    {
                        "foutmelding": "",
                        "resultaten": [
                            {
                              "groepNaam": "${BEHANDELAARS_DOMAIN_TEST_1.description}",
                              "naam": "$BPMN_TEST_TASK_NAME",
                              "rechten": {
                                "lezen": true,
                                "toekennen": false,
                                "toevoegenDocument": false,
                                "wijzigen": false
                              },
                              "status": "NIET_TOEGEKEND",
                              "type": "TAAK",
                              "zaakIdentificatie": "$ZAAK_BPMN_TEST_IDENTIFICATION",
                              "zaakOmschrijving": "$ZAAK_OMSCHRIJVING",
                              "zaakToelichting": "null",
                              "zaaktypeOmschrijving": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION"
                            },
                            {
                                "type": "TAAK",                       
                                "fataledatum": "$DATE_2024_01_01",
                                "groepNaam": "${OLD_IAM_TEST_GROUP_A.description}",
                                "naam": "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM",
                                "rechten": {
                                    "lezen": true,
                                    "toekennen": false,
                                    "toevoegenDocument": false,
                                    "wijzigen": false
                                },
                                "status": "TOEGEKEND",
                                "zaakOmschrijving": "$ZAAK_DESCRIPTION_1",
                                "zaakToelichting": "null",
                                "zaaktypeOmschrijving": "$ZAAKTYPE_TEST_2_DESCRIPTION"
                            },
                            {
                                "type": "TAAK",
                                "fataledatum": "$TAAK_1_FATAL_DATE",
                                "groepNaam": "${OLD_IAM_TEST_GROUP_A.description}",
                                "naam": "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM",
                                "rechten": {
                                    "lezen": true,
                                    "toekennen": false,
                                    "toevoegenDocument": false,
                                    "wijzigen": false
                                },
                                "status": "NIET_TOEGEKEND",
                                "zaakIdentificatie": "$ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION",
                                "zaakOmschrijving": "$ZAAK_PRODUCTAANVRAAG_1_OMSCHRIJVING",
                                "zaakToelichting": "Aangemaakt vanuit $OPEN_FORMULIEREN_FORMULIER_BRON_NAAM met kenmerk '$OBJECT_PRODUCTAANVRAAG_1_BRON_KENMERK'. $ZAAK_PRODUCTAANVRAAG_1_TOELICHTING",
                                "zaaktypeOmschrijving": "$ZAAKTYPE_TEST_3_DESCRIPTION"
                            },
                            {
                              "type": "TAAK",
                              "groepNaam": "${OLD_IAM_TEST_GROUP_A.description}",
                              "rechten": {
                                "lezen": true,
                                "toekennen": false,
                                "toevoegenDocument": false,
                                "wijzigen": false
                              },
                              "status": "NIET_TOEGEKEND",
                              "zaakIdentificatie": "$ZAAK_PRODUCTAANVRAAG_BPMN_IDENTIFICATION",
                              "zaakToelichting": "Aangemaakt vanuit $OPEN_FORMULIEREN_FORMULIER_BRON_NAAM met kenmerk '$OBJECT_PRODUCTAANVRAAG_BPMN_BRON_KENMERK'.",
                              "zaaktypeOmschrijving": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION"
                            }
                        ],
                        "totaal": 4,
                        "filters": {
                            "ZAAKTYPE": [
                                {
                                    "aantal": 2,
                                    "naam": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION"
                                },
                                {
                                    "aantal": 1,
                                    "naam": "$ZAAKTYPE_TEST_2_DESCRIPTION"
                                },
                                {
                                    "aantal": 1,
                                    "naam": "$ZAAKTYPE_TEST_3_DESCRIPTION"
                                }
                            ],
                            "BEHANDELAAR": [
                                {
                                    "aantal": 1                       
                                },
                                {
                                    "aantal": 3,
                                    "naam": "-NULL-"
                                }
                            ],
                            "GROEP": [
                                {
                                    "aantal": 3,
                                    "naam": "${OLD_IAM_TEST_GROUP_A.description}"
                                },
                                {
                                    "aantal": 1,
                                    "naam": "${BEHANDELAARS_DOMAIN_TEST_1.description}"
                                }
                            ],
                            "TAAK_NAAM": [
                                {
                                    "aantal": 2,
                                    "naam": "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM"
                                },
                                {
                                    "aantal": 2,
                                    "naam": "$BPMN_TEST_TASK_NAME"
                                }
                            ],
                            "TAAK_STATUS": [
                                {
                                    "aantal": 3,
                                    "naam": "NIET_TOEGEKEND"
                                },
                                {
                                    "aantal": 1,
                                    "naam": "TOEGEKEND"
                                }
                            ]
                        }
                    }
                """.trimIndent()
            }
        }

        When(
            """the search endpoint is called to search for all objects of type 'DOCUMENT'"""
        ) {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zoeken/list",
                requestBodyAsString = """
                   {
                    "filtersType": "ZoekParameters",
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": false,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": {},
                    "filters": {},
                    "datums": {},
                    "rows": 10,
                    "page":0,
                    "type":"DOCUMENT"
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
                JSONObject(responseBody).getInt("totaal") shouldBe TOTAL_COUNT_INDEXED_DOCUMENTS
                JSONObject(responseBody).getJSONObject("filters").toString() shouldEqualJsonIgnoringOrderAndExtraneousFields """                   
                      {
                        "ZAAKTYPE" : [ {
                          "aantal" : 12,
                          "naam" : "$ZAAKTYPE_TEST_3_DESCRIPTION"
                        }, {
                          "aantal": 2,
                          "naam": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION"
                        } ],
                        "DOCUMENT_STATUS" : [ {
                          "aantal" : 10,
                          "naam" : "$DOCUMENT_STATUS_DEFINITIEF"
                        }, {
                          "aantal" : 4,
                          "naam" : "$DOCUMENT_STATUS_IN_BEWERKING"
                        } ],
                        "DOCUMENT_TYPE" : [ {
                          "aantal" : 9,
                          "naam" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING"
                        }, {
                          "aantal" : 4,
                          "naam" : "$INFORMATIE_OBJECT_TYPE_EMAIL_OMSCHRIJVING"
                        }, {
                          "aantal" : 1,
                          "naam" : "$INFORMATIE_OBJECT_TYPE_FACTUUR_OMSCHRIJVING"
                        } ],
                        "DOCUMENT_VERGRENDELD_DOOR" : [ {
                          "aantal" : $TOTAL_COUNT_INDEXED_DOCUMENTS,
                          "naam" : "-NULL-"
                        } ],
                        "DOCUMENT_INDICATIES" : [ {
                          "aantal" : $TOTAL_COUNT_INDEXED_DOCUMENTS,
                          "naam" : "ONDERTEKEND"
                        }, {
                          "aantal" : 11,
                          "naam" : "GEBRUIKSRECHT"
                        }, {
                          "aantal" : 4,
                          "naam" : "VERZONDEN"
                        } ]
                      }                                                                  
                """
            }
        }

        When(
            """the search endpoint is called to search for a specific document"""
        ) {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zoeken/list",
                requestBodyAsString = """
                   {
                    "filtersType": "ZoekParameters",
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": false,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": { "DOCUMENT_TITEL": "Fake test document" },
                    "filters": {},
                    "datums": {},
                    "rows": 10,
                    "page":0,
                    "type":"DOCUMENT"
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
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "foutmelding" : "",
                      "resultaten" : [ {
                        "type" : "DOCUMENT",
                        "auteur" : "Aanvrager",
                        "beschrijving" : "Ingezonden formulier",
                        "bestandsnaam" : "$ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_FILE_NAME",
                        "bestandsomvang" : 1234,
                        "creatiedatum" : "$ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_CREATION_DATE",
                        "documentType" : "bijlage",
                        "formaat" : "application/pdf",
                        "indicatieGebruiksrecht" : false,
                        "indicatieOndertekend" : true,
                        "indicatieVergrendeld" : false,
                        "indicaties" : [ "ONDERTEKEND" ],
                        "rechten" : {
                          "lezen" : true,
                          "ondertekenen" : false,
                          "ontgrendelen" : false,
                          "toevoegenNieuweVersie" : false,
                          "vergrendelen" : false,
                          "verwijderen" : false,
                          "wijzigen" : false
                        },
                        "registratiedatum" : "$ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_CREATION_DATE",
                        "status" : "definitief",
                        "titel" : "$ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_TITEL",
                        "versie" : 1,
                        "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK",
                        "zaakIdentificatie" : "$ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION",
                        "zaakRelatie" : "Hoort bij, omgekeerd: kent",
                        "zaaktypeIdentificatie" : "$ZAAKTYPE_TEST_3_IDENTIFICATIE",
                        "zaaktypeOmschrijving" : "$ZAAKTYPE_TEST_3_DESCRIPTION",
                        "zaaktypeUuid" : "$ZAAKTYPE_TEST_3_UUID"
                      }, {
                        "type" : "DOCUMENT" 
                      }, {
                        "type" : "DOCUMENT"
                      } ],
                      "totaal" : 3.0,
                      "filters" : {
                        "ZAAKTYPE" : [ {
                          "aantal" : 3,
                          "naam" : "$ZAAKTYPE_TEST_3_DESCRIPTION"
                        } ],
                        "DOCUMENT_STATUS" : [ {
                          "aantal" : 3,
                          "naam" : "$DOCUMENT_STATUS_DEFINITIEF"
                        } ],
                        "DOCUMENT_TYPE" : [ {
                          "aantal" : 3,
                          "naam" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING"
                        } ],
                        "DOCUMENT_VERGRENDELD_DOOR" : [ {
                          "aantal" : 3,
                          "naam" : "-NULL-"
                        } ],
                        "DOCUMENT_INDICATIES" : [ {
                          "aantal" : 3,
                          "naam" : "ONDERTEKEND"
                        } ]
                      }                   
                    }                                              
                """
            }
        }
    }
})
