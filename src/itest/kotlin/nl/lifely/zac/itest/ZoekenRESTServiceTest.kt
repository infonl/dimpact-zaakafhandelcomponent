/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.lifely.zac.itest.config.ItestConfiguration.DATE_2024_01_01
import nl.lifely.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.lifely.zac.itest.config.ItestConfiguration.HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM
import nl.lifely.zac.itest.config.ItestConfiguration.TAAK_1_FATAL_DATE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_LAST
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import nl.lifely.zac.itest.config.ItestConfiguration.TOTAL_COUNT_DOCUMENTS
import nl.lifely.zac.itest.config.ItestConfiguration.TOTAL_COUNT_TASKS
import nl.lifely.zac.itest.config.ItestConfiguration.TOTAL_COUNT_ZAKEN
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_DESCRIPTION_1
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_OMSCHRIJVING
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_TOELICHTING
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.zaakManual2Identification
import nl.lifely.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields

// Run this test last so that all the required data is available in the Solr index
@Order(TEST_SPEC_ORDER_LAST)
@Suppress("LargeClass")
class ZoekenRESTServiceTest : BehaviorSpec({
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
                        "totaal": ${TOTAL_COUNT_ZAKEN + TOTAL_COUNT_TASKS + TOTAL_COUNT_DOCUMENTS}.0,
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
                                    "aantal": 5,
                                    "naam": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION"
                                },
                                {
                                    "aantal": 10,
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
                                    "aantal": 9,
                                    "naam": "$TEST_GROUP_A_DESCRIPTION"
                                }
                            ],
                            "TOEGEKEND": [
                                {
                                    "aantal": 7,
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
                                    "naam": "Aanvullende informatie vereist"
                                },
                                {
                                    "aantal": 2,
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
                            "ZAAK_INDICATIES": [],
                            "ZAAK_COMMUNICATIEKANAAL": [
                                {
                                    "aantal": 5,
                                    "naam": "$COMMUNICATIEKANAAL_TEST_1"
                                },
                                {
                                    "aantal": 2,
                                    "naam": "E-formulier"
                                }                               
                            ],
                            "ZAAK_VERTROUWELIJKHEIDAANDUIDING": [
                                {
                                    "aantal": 7,
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
                                    "aantal": 4,
                                    "naam": "definitief"
                                },
                                {
                                    "aantal": 2,
                                    "naam": "in_bewerking"
                                }
                            ],
                            "DOCUMENT_TYPE": [
                                {
                                    "aantal": 6,
                                    "naam": "bijlage"
                                }
                            ],
                            "DOCUMENT_VERGRENDELD_DOOR": [],
                            "DOCUMENT_INDICATIES": [
                                {
                                    "aantal": 5,
                                    "naam": "GEBRUIKSRECHT"
                                },
                                {
                                    "aantal": 6,
                                    "naam": "ONDERTEKEND"
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
                      "resultaten" : [ 
                      {
                        "identificatie" : "$zaakManual2Identification",
                        "type" : "ZAAK",
                        "aantalOpenstaandeTaken" : 1,
                        "afgehandeld" : false,
                        "betrokkenen" : {
                          "Behandelaar" : [ "$TEST_GROUP_A_ID" ]
                        },
                        "communicatiekanaal" : "$COMMUNICATIEKANAAL_TEST_1",
                        "groepId" : "$TEST_GROUP_A_ID",
                        "groepNaam" : "$TEST_GROUP_A_DESCRIPTION",
                        "indicatieDeelzaak" : false,
                        "indicatieHeropend" : false,
                        "indicatieHoofdzaak" : false,
                        "indicatieOpschorting" : false,
                        "indicatieVerlenging" : false,
                        "indicaties" : [ ],
                        "omschrijving" : "$ZAAK_DESCRIPTION_1",                     
                        "statusToelichting" : "Status gewijzigd",
                        "statustypeOmschrijving" : "Aanvullende informatie vereist",
                        "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR",
                        "zaaktypeOmschrijving" : "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION"
                      },
                      {
                         "identificatie": "ZAAK-2000-0000000001",
                         "type": "ZAAK",
                         "aantalOpenstaandeTaken": 0,
                         "afgehandeld": false,
                         "betrokkenen": {
                           "Behandelaar": [
                             "test-group-a"
                           ]
                         },
                         "communicatiekanaal": "dummyCommunicatiekanaal1",
                         "groepId": "test-group-a",
                         "groepNaam": "Test group A",
                         "indicatieDeelzaak": false,
                         "indicatieHeropend": false,
                         "indicatieHoofdzaak": false,
                         "indicatieOpschorting": false,
                         "indicatieVerlenging": false,
                         "indicaties": [],
                         "omschrijving": "dummyOmschrijving",               
                         "resultaattypeOmschrijving": "Toegekend",
                         "statusToelichting": "Status gewijzigd",
                         "statustypeOmschrijving": "In behandeling",
                         "toelichting": "null",
                         "vertrouwelijkheidaanduiding": "OPENBAAR",
                         "zaaktypeOmschrijving": "Indienen aansprakelijkstelling door derden behandelen"
                        }
                       ],
                      "totaal" : 2.0,
                      "filters" : {
                        "ZAAKTYPE" : [ {
                          "aantal" : 3,
                          "naam" : "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                        }, {
                          "aantal" : 2,
                          "naam" : "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION"
                        } ],          
                        "BEHANDELAAR": [
                          {
                            "aantal": 2,
                            "naam": "-NULL-"
                          }
                        ],
                        "GROEP" : [ {
                          "aantal" : 2,
                          "naam" : "$TEST_GROUP_A_DESCRIPTION"
                        } ],
                        "ZAAK_STATUS" : [
                           {
                                "aantal": 1,
                                "naam": "In behandeling"
                            },
                            {
                                "aantal" : 1,
                                "naam" : "Aanvullende informatie vereist"
                            } 
                         ],
                        "ZAAK_RESULTAAT" : [
                         {
                            "aantal": 1,
                            "naam": "Toegekend"
                          },                       
                          {
                             "aantal" : 1,
                             "naam" : "-NULL-"
                           } 
                        ],
                        "ZAAK_INDICATIES" : [ {
                          "aantal" : 2,
                          "naam" : "-NULL-"
                        } ],
                        "ZAAK_COMMUNICATIEKANAAL" : [ {                    
                          "aantal" : 2,
                          "naam" : "$COMMUNICATIEKANAAL_TEST_1"
                        } ],
                        "ZAAK_VERTROUWELIJKHEIDAANDUIDING" : [ {
                          "aantal" : 2,
                          "naam" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR"
                        } ],
                        "ZAAK_ARCHIEF_NOMINATIE" : [ {
                          "aantal" : 2,
                          "naam" : "-NULL-"
                        } ]
                      }
                    }
                """.trimIndent()
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
                                "zaakToelichting": "$ZAAK_PRODUCTAANVRAAG_1_TOELICHTING",
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
})
