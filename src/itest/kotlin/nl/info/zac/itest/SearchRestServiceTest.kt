/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.info.zac.configuratie.ConfiguratieService.Companion.STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_2024_01_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_2024_01_31
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2024_01_01
import nl.info.zac.itest.config.ItestConfiguration.HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_FACTUUR_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_INTERNAL_ENDPOINTS_API_KEY
import nl.info.zac.itest.config.RAADPLEGER_DOMAIN_TEST_1
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrder
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import okhttp3.Headers.Companion.toHeaders
import org.json.JSONArray
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
    val zaakHelper = ZaakHelper(zacClient)
    val taskHelper = TaskHelper(zacClient)
    val logger = KotlinLogging.logger {}

    Context("Listing search results") {
        Given(
            """
            A logged-in raadpleger and a zaak has been created and is indexed and an 'aanvullende informatie' 
            task has been added to the zaak           
            """".trimIndent()
            ) {
            // log in as a beheerder authorised in all domains to create the zaken, tasks and documents and index them
            authenticate(BEHEERDER_ELK_ZAAKTYPE)
            val zaak1Description = "fakeZaak1DescriptionForSearchRestServiceTest"
            val now = LocalDate.now()
            val aanvullendeInformatieTaskFatalDate = now.plusDays(1)
            val (zaak1Identification, zaak1Uuid) = zaakHelper.createAndIndexZaak(
                zaakDescription = zaak1Description
            )
            taskHelper.startAanvullendeInformatieTaskForZaak(
                zaakUuid = zaak1Uuid,
                zaakIdentificatie = zaak1Identification,
                fatalDate = aanvullendeInformatieTaskFatalDate,
                group = BEHANDELAARS_DOMAIN_TEST_1
            )
            // TODO: upload document to a zaak and index it (if needed)
            authenticate(RAADPLEGER_DOMAIN_TEST_1)

            When("the search endpoint is called to search open zaken on the unique description of the just created zaak") {
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
                            "page": 0,
                            "type": "ZAAK"
                        }
                    """.trimIndent()
                )

                Then(
                    """
                        the response is successful and the search results contain the newly created zaak and includes
                        data about the started 'aanvullende informatie' task
                    """.trimMargin()
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJsonIgnoringOrder """
                        {
                          "foutmelding" : "",
                          "resultaten" : [ {
                            "aantalOpenstaandeTaken" : 1,
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
                            "statustypeOmschrijving" : "Wacht op aanvullende informatie",
                            "toelichting" : "null",
                            "type" : "ZAAK",
                            "uiterlijkeEinddatumAfdoening" : "$aanvullendeInformatieTaskFatalDate",
                            "vertrouwelijkheidaanduiding" : "OPENBAAR",
                            "zaaktypeOmschrijving" : "$ZAAKTYPE_TEST_2_DESCRIPTION"
                          } ],
                          "totaal" : 1,
                          "filters" : {                            
                            "ZAAKTYPE" : [ 
                              {
                                "aantal" : 1,
                                "naam" : "$ZAAKTYPE_TEST_2_DESCRIPTION"
                               } 
                            ],
                            "BEHANDELAAR": [ 
                              {
                                "aantal": 1,
                                "naam": "-NULL-"
                               } 
                             ],         
                            "GROEP" : [ 
                              {
                                "aantal" : 1,
                                "naam" : "${BEHANDELAARS_DOMAIN_TEST_1.description}"
                              } 
                            ],                           
                            "ZAAK_STATUS" : [ 
                              {
                                "aantal" : 1,
                                "naam" : "$STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE"
                              } 
                            ],
                            "ZAAK_RESULTAAT": [
                              {
                                "aantal": 1,
                                "naam": "-NULL-"
                              }
                            ],
                            "ZAAK_INDICATIES": [
                              {
                                "aantal": 1,
                                "naam": "-NULL-"
                              }
                            ],
                            "ZAAK_COMMUNICATIEKANAAL" : [ 
                              {
                                "aantal" : 1,
                                "naam" : "$COMMUNICATIEKANAAL_TEST_1"
                              } 
                            ],
                            "ZAAK_VERTROUWELIJKHEIDAANDUIDING" : [ 
                              {
                                "aantal" : 1,
                                "naam" : "OPENBAAR"
                              } 
                            ],
                            "ZAAK_ARCHIEF_NOMINATIE": [
                              {
                                "aantal": 1,
                                "naam": "-NULL-"
                              }
                            ]                         
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
                   the response is successful and the search results include the indexed zaken
                   that may be linked according to the information object type UUID
                    """.trimMargin()
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    // TODO; delete this test?
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
                """the search endpoint is called to search for 'aanvullende informatie' tasks"""
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
                        "filters": { "TAAK_NAAM": { "values": [ "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM" ] } },
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
                    val aanvullendeInformatieTask = with(JSONObject(responseBody)) {
                        getInt("totaal") shouldBeGreaterThan 0
                        getJSONArray("resultaten")
                            .map { it as JSONObject }
                            .first { it.getString("zaakOmschrijving") == zaak1Description }
                    }
                    aanvullendeInformatieTask shouldNotBe null
                    aanvullendeInformatieTask.toString() shouldEqualJsonIgnoringOrderAndExtraneousFields """
                        {
                            "creatiedatum": "$now",
                            "fataledatum": "$aanvullendeInformatieTaskFatalDate",
                            "groepNaam": "${BEHANDELAARS_DOMAIN_TEST_1.description}",
                            "naam": "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM",
                            "rechten": {
                                "lezen": true,
                                "toekennen": false,
                                "toevoegenDocument": false,
                                "wijzigen": false
                            },
                            "status": "NIET_TOEGEKEND",
                            "type": "TAAK",
                            "zaakIdentificatie": "$zaak1Identification",
                            "zaakOmschrijving": "$zaak1Description",
                            "zaakToelichting": "null",
                            "zaakUuid": "$zaak1Uuid",
                            "zaaktypeOmschrijving": "$ZAAKTYPE_TEST_2_DESCRIPTION"
                        }
                    """.trimIndent()

                    //                             "id": "480",

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
