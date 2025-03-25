/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_2
import nl.info.zac.itest.config.ItestConfiguration.DATE_2024_01_01
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_DEFINITIEF
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_NOT_FOUND
import nl.info.zac.itest.config.ItestConfiguration.HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_EMAIL_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_FACTUUR_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_1_BRON_KENMERK
import nl.info.zac.itest.config.ItestConfiguration.OPEN_FORMULIEREN_FORMULIER_BRON_NAAM
import nl.info.zac.itest.config.ItestConfiguration.TAAK_1_FATAL_DATE
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_REINDEXING
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TOTAL_COUNT_DOCUMENTS
import nl.info.zac.itest.config.ItestConfiguration.TOTAL_COUNT_TASKS
import nl.info.zac.itest.config.ItestConfiguration.TOTAL_COUNT_ZAKEN
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_DESCRIPTION_1
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_EXPLANATION_1
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_MANUAL_1_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_TOELICHTING
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_CREATION_DATE
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_TITEL
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.enkelvoudigInformatieObjectUUID
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import org.json.JSONObject

// Run this test after reindexing so that all the required data is available in the Solr index
@Order(TEST_SPEC_ORDER_AFTER_REINDEXING)
@Suppress("LargeClass")
class SearchRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}

    Given("""Multiple zaken, tasks and documents have been created and are indexed""") {
        When(
            """the search endpoint is called to search for all objects of all types"""
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
                    "zoeken":{"ALLE":""},
                    "filters": {},
                    "datums": {},
                    "rows": 10,
                    "page":0                  
                    }
                """.trimIndent()
            )
            Then(
                """
                   the response is successful and the search results include the indexed zaken, tasks and documents
                """
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                // we only test on the total number of results and the filters, not on the actual results,
                // in order to keep the test maintainable
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                        "foutmelding": "",                      
                        "totaal": ${TOTAL_COUNT_ZAKEN + TOTAL_COUNT_TASKS + TOTAL_COUNT_DOCUMENTS},
                        "filters": {
                            "TYPE": [
                                {
                                    "aantal": $TOTAL_COUNT_ZAKEN,
                                    "naam": "ZAAK"
                                },                           
                                {
                                    "aantal": $TOTAL_COUNT_TASKS,
                                    "naam": "TAAK"
                                },
                                {
                                    "aantal": $TOTAL_COUNT_DOCUMENTS,
                                    "naam": "DOCUMENT"
                                }
                            ],
                            "ZAAKTYPE": [
                                {
                                    "aantal": 7,
                                    "naam": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION"
                                },
                                {
                                    "aantal": 12,
                                    "naam": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                                }                                
                            ],
                            "BEHANDELAAR": [
                                {
                                    "aantal": 2,
                                    "naam": "$TEST_USER_1_NAME"
                                }
                            ],
                            "GROEP": [
                                {
                                    "aantal": 12,
                                    "naam": "$TEST_GROUP_A_DESCRIPTION"
                                }
                            ],
                            "TOEGEKEND": [
                                {
                                    "aantal": 10,
                                    "naam": "false"
                                },
                                {
                                    "aantal": 2,
                                    "naam": "true"
                                }
                            ],
                            "ZAAK_STATUS": [
                                {
                                    "aantal": 2,
                                    "naam": "Wacht op aanvullende informatie"
                                },
                                {
                                    "aantal": 5,
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
                            "ZAAK_RESULTAAT": [
                              {
                                "aantal": 2,
                                "naam": "Buiten behandeling"
                              },
                              {
                                "aantal": 1,
                                "naam": "Toegekend"
                              }
                            ],
                            "ZAAK_INDICATIES": [                        
                                {
                                    "aantal": 1,
                                    "naam": "VERLENGD"
                                }
                            ],
                            "ZAAK_COMMUNICATIEKANAAL": [
                                {
                                    "aantal": 7,
                                    "naam": "$COMMUNICATIEKANAAL_TEST_1"
                                },
                                {
                                    "aantal": 1,
                                    "naam": "$COMMUNICATIEKANAAL_TEST_2"
                                },
                                {
                                    "aantal": 2,
                                    "naam": "E-formulier"
                                }                               
                            ],
                            "ZAAK_VERTROUWELIJKHEIDAANDUIDING": [
                                {
                                    "aantal": 10,
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
                                }
                            ],
                            "TAAK_STATUS": [
                                {
                                    "aantal": 1,
                                    "naam": "NIET_TOEGEKEND"
                                },
                                {
                                    "aantal": 1,
                                    "naam": "TOEGEKEND"
                                }
                            ],
                            "DOCUMENT_STATUS": [
                                {
                                    "aantal": 5,
                                    "naam": "definitief"
                                },
                                {
                                    "aantal": 2,
                                    "naam": "in_bewerking"
                                }
                            ],
                            "DOCUMENT_TYPE": [
                                {
                                    "aantal": 5,
                                    "naam": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING"
                                },
                                {
                                    "aantal": 1,
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
                                    "aantal": 6,
                                    "naam": "GEBRUIKSRECHT"
                                },
                                {
                                    "aantal": 7,
                                    "naam": "ONDERTEKEND"
                                },
                                {
                                    "aantal": 1,
                                    "naam": "VERZONDEN"
                                }
                            ]
                        }
                    }
                """.trimIndent()
            }
        }
    }

    Given("""Multiple zaken have been created and are indexed""") {
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
                               "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION"
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
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "foutmelding" : "",                     
                      "totaal" : 4,
                      "filters" : {
                        "ZAAKTYPE" : [ {
                          "aantal" : 4,
                          "naam" : "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                        }, {
                          "aantal" : 4,
                          "naam" : "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION"
                        } ],          
                        "BEHANDELAAR": [
                          {
                            "aantal": 4,
                            "naam": "-NULL-"
                          }
                        ],
                        "GROEP" : [ {
                          "aantal" : 4,
                          "naam" : "$TEST_GROUP_A_DESCRIPTION"
                        } ],
                        "ZAAK_STATUS" : [
                           {
                                "aantal": 1,
                                "naam": "In behandeling"
                            },
                            {
                                "aantal": 2,
                                "naam": "Intake"
                            },
                            {
                                "aantal" : 1,
                                "naam" : "Wacht op aanvullende informatie"
                            } 
                         ],
                        "ZAAK_RESULTAAT" : [
                         {
                            "aantal": 1,
                            "naam": "Toegekend"
                          },                       
                          {
                             "aantal" : 3,
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
    }

    Given("""Multiple zaken have been created and are indexed""") {
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
                    "page":0,
                    "zaakIdentificator": "zaak",
                    "documentUUID": "$enkelvoudigInformatieObjectUUID"
                }
                """.trimIndent()
            )
            Then(
                """
                   the response is successful and the search results include the indexed zaken with compatibility info 
                   about the information object type UUID
                """
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                {
                  "foutmelding": "",
                  "resultaten": [
                    {
                      "documentKoppelen": true,
                      "identificatie": "ZAAK-2024-0000000001",
                      "omschrijving": "$ZAAK_DESCRIPTION_1",
                      "toelichting": "null",
                      "type": "ZAAK"
                    },
                    {
                      "documentKoppelen": false,
                      "identificatie": "$ZAAK_MANUAL_1_IDENTIFICATION",
                      "omschrijving": "changedDescription",
                      "toelichting": "$ZAAK_EXPLANATION_1",
                      "type": "ZAAK"
                    },
                    {
                      "documentKoppelen": true,
                      "identificatie": "ZAAK-2000-0000000006",
                      "omschrijving": "dummyOmschrijving",
                      "toelichting": "null",
                      "type": "ZAAK"
                    },
                    {
                      "documentKoppelen": true,
                      "identificatie": "ZAAK-2000-0000000005",
                      "omschrijving": "dummyOmschrijving",
                      "toelichting": "null",
                      "type": "ZAAK"
                    },
                    {
                      "documentKoppelen": true,
                      "identificatie": "ZAAK-2000-0000000004",
                      "omschrijving": "dummyOmschrijving",
                      "toelichting": "null",
                      "type": "ZAAK"
                    }
                  ],
                  "totaal": 10,
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
                    "page":0,
                    "zaakIdentificator": "zaak",
                    "documentUUID": "a47b0c7e-1d0d-4c33-918d-160677516f1c"
                }
                """.trimIndent()
            )
            Then("the response is successful and search results contain no zaak that can be linked to") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_NOT_FOUND
            }
        }
    }

    Given("""Multiple taken have been created and are indexed""") {
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
                """the response is successful and the search results include the indexed taken"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """                                          
                    {
                        "foutmelding": "",
                        "resultaten": [
                            {
                                "type": "TAAK",
                                "behandelaarGebruikersnaam": "$TEST_USER_1_USERNAME",
                                "behandelaarNaam": "$TEST_USER_1_NAME",
                                "fataledatum": "$DATE_2024_01_01",
                                "groepNaam": "$TEST_GROUP_A_DESCRIPTION",
                                "naam": "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM",
                                "rechten": {
                                    "lezen": true,
                                    "toekennen": true,
                                    "toevoegenDocument": false,
                                    "wijzigen": true
                                },
                                "status": "TOEGEKEND",
                                "zaakOmschrijving": "$ZAAK_DESCRIPTION_1",
                                "zaakToelichting": "null",
                                "zaaktypeOmschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION"
                            },
                            {
                                "type": "TAAK",
                                "fataledatum": "$TAAK_1_FATAL_DATE",
                                "groepNaam": "$TEST_GROUP_A_DESCRIPTION",
                                "naam": "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM",
                                "rechten": {
                                    "lezen": true,
                                    "toekennen": true,
                                    "toevoegenDocument": false,
                                    "wijzigen": true
                                },
                                "status": "NIET_TOEGEKEND",
                                "zaakIdentificatie": "$ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION",
                                "zaakOmschrijving": "$ZAAK_PRODUCTAANVRAAG_1_OMSCHRIJVING",
                                "zaakToelichting": "Aangemaakt vanuit $OPEN_FORMULIEREN_FORMULIER_BRON_NAAM met kenmerk '$OBJECT_PRODUCTAANVRAAG_1_BRON_KENMERK'. $ZAAK_PRODUCTAANVRAAG_1_TOELICHTING",
                                "zaaktypeOmschrijving": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                            }
                        ],
                        "totaal": 2.0,
                        "filters": {
                            "ZAAKTYPE": [
                                {
                                    "aantal": 1,
                                    "naam": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION"
                                },
                                {
                                    "aantal": 1,
                                    "naam": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                                }
                            ],
                            "BEHANDELAAR": [
                                {
                                    "aantal": 1,
                                    "naam": "$TEST_USER_1_NAME"
                                },
                                {
                                    "aantal": 1,
                                    "naam": "-NULL-"
                                }
                            ],
                            "GROEP": [
                                {
                                    "aantal": 2,
                                    "naam": "$TEST_GROUP_A_DESCRIPTION"
                                }
                            ],
                            "TAAK_NAAM": [
                                {
                                    "aantal": 2,
                                    "naam": "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM"
                                }
                            ],
                            "TAAK_STATUS": [
                                {
                                    "aantal": 1,
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
    }
    Given("""Documents have been created and are indexed""") {
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
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                JSONObject(responseBody).getInt("totaal") shouldBe TOTAL_COUNT_DOCUMENTS
                JSONObject(responseBody).getJSONObject("filters").toString() shouldEqualJsonIgnoringOrderAndExtraneousFields """                   
                      {
                        "ZAAKTYPE" : [ {
                          "aantal" : $TOTAL_COUNT_DOCUMENTS,
                          "naam" : "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                        } ],
                        "DOCUMENT_STATUS" : [ {
                          "aantal" : 5,
                          "naam" : "$DOCUMENT_STATUS_DEFINITIEF"
                        }, {
                          "aantal" : 2,
                          "naam" : "$DOCUMENT_STATUS_IN_BEWERKING"
                        } ],
                        "DOCUMENT_TYPE" : [ {
                          "aantal" : 5,
                          "naam" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING"
                        }, {
                          "aantal" : 1,
                          "naam" : "$INFORMATIE_OBJECT_TYPE_EMAIL_OMSCHRIJVING"
                        }, {
                          "aantal" : 1,
                          "naam" : "$INFORMATIE_OBJECT_TYPE_FACTUUR_OMSCHRIJVING"
                        } ],
                        "DOCUMENT_VERGRENDELD_DOOR" : [ {
                          "aantal" : $TOTAL_COUNT_DOCUMENTS,
                          "naam" : "-NULL-"
                        } ],
                        "DOCUMENT_INDICATIES" : [ {
                          "aantal" : $TOTAL_COUNT_DOCUMENTS,
                          "naam" : "ONDERTEKEND"
                        }, {
                          "aantal" : 6,
                          "naam" : "GEBRUIKSRECHT"
                        }, {
                          "aantal" : 1,
                          "naam" : "VERZONDEN"
                        } ]
                      }                                                                  
                """
            }
        }
    }
    Given("""Documents have been created and are indexed""") {
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
                    "zoeken": { "DOCUMENT_TITEL": "Dummy test document" },
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
                """.trimMargin()
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
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
                          "ontgrendelen" : true,
                          "toevoegenNieuweVersie" : true,
                          "vergrendelen" : false,
                          "verwijderen" : true,
                          "wijzigen" : true
                        },
                        "registratiedatum" : "$ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_CREATION_DATE",
                        "status" : "definitief",
                        "titel" : "$ZAAK_PRODUCTAANVRAAG_2_DOCUMENT_TITEL",
                        "versie" : 1,
                        "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK",
                        "zaakIdentificatie" : "$ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION",
                        "zaakRelatie" : "Hoort bij, omgekeerd: kent",
                        "zaaktypeIdentificatie" : "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE",
                        "zaaktypeOmschrijving" : "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                        "zaaktypeUuid" : "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID"
                      } ],
                      "totaal" : 1.0,
                      "filters" : {
                        "ZAAKTYPE" : [ {
                          "aantal" : 1,
                          "naam" : "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                        } ],
                        "DOCUMENT_STATUS" : [ {
                          "aantal" : 1,
                          "naam" : "$DOCUMENT_STATUS_DEFINITIEF"
                        } ],
                        "DOCUMENT_TYPE" : [ {
                          "aantal" : 1,
                          "naam" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING"
                        } ],
                        "DOCUMENT_VERGRENDELD_DOOR" : [ {
                          "aantal" : 1,
                          "naam" : "-NULL-"
                        } ],
                        "DOCUMENT_INDICATIES" : [ {
                          "aantal" : 1,
                          "naam" : "ONDERTEKEND"
                        } ]
                      }                   
                    }                                              
                """
            }
        }
    }
})
