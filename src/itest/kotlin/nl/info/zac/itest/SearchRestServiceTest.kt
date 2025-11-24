/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.info.zac.configuratie.ConfiguratieService.Companion.STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE
import nl.info.zac.itest.client.DocumentHelper
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_2024_01_01
import nl.info.zac.itest.config.ItestConfiguration.HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_FACTUUR_UUID
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_DOMAIN_TEST_1
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrder
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate

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
    val documentHelper = DocumentHelper(zacClient)
    val logger = KotlinLogging.logger {}

    Context("Listing search results") {
        Given(
            """
            A logged-in raadpleger and a zaak has been created and is indexed and an 'aanvullende informatie' 
            task has been added to the zaak           
            "
            """.trimIndent()
        ) {
            // log in as a beheerder authorised in all domains
            // and create the zaken, tasks and documents and index them
            authenticate(BEHEERDER_ELK_ZAAKTYPE)
            val zaakDescription = "fakeZaakDescriptionForSearchRestServiceTest"
            val documentTitle = "fakeDocumentTitleForSearchRestServiceTest"
            val documentAuthorName = "fakeAuthorNameForSearchRestServiceTest"
            val today = LocalDate.now()
            val aanvullendeInformatieTaskFatalDate = today.plusDays(1)
            val (zaakIdentification, zaakUuid) = zaakHelper.createAndIndexZaak(
                zaakDescription = zaakDescription,
                zaaktypeUuid = ZAAKTYPE_TEST_2_UUID
            )
            taskHelper.startAanvullendeInformatieTaskForZaak(
                zaakUuid = zaakUuid,
                zaakIdentificatie = zaakIdentification,
                fatalDate = aanvullendeInformatieTaskFatalDate,
                group = BEHANDELAARS_DOMAIN_TEST_1
            )
            documentHelper.uploadDocumentToZaakAndIndexDocument(
                zaakUuid = zaakUuid,
                documentTitle = documentTitle,
                authorName = documentAuthorName,
                fileName = TEST_PDF_FILE_NAME
            )
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
                            "zoeken": { "ZAAK_OMSCHRIJVING": "$zaakDescription" },
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
                            "id" : "$zaakUuid",
                            "identificatie" : "$zaakIdentification",
                            "indicatieDeelzaak" : false,
                            "indicatieHeropend" : false,
                            "indicatieHoofdzaak" : false,
                            "indicatieOpschorting" : false,
                            "indicatieVerlenging" : false,
                            "indicaties" : [ ],
                            "omschrijving" : "$zaakDescription",
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
                            .first { it.getString("zaakOmschrijving") == zaakDescription }
                    }
                    aanvullendeInformatieTask shouldNotBe null
                    aanvullendeInformatieTask.toString() shouldEqualJsonIgnoringOrderAndExtraneousFields """
                        {
                            "creatiedatum": "$today",
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
                            "zaakIdentificatie": "$zaakIdentification",
                            "zaakOmschrijving": "$zaakDescription",
                            "zaakToelichting": "null",
                            "zaakUuid": "$zaakUuid",
                            "zaaktypeOmschrijving": "$ZAAKTYPE_TEST_2_DESCRIPTION"
                        }
                    """.trimIndent()
                }
            }

            When(
                """the search endpoint is called to search for the uploaded document"""
            ) {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zoeken/list",
                    requestBodyAsString = """
                    {
                        "alleenMijnZaken": false,
                        "alleenOpenstaandeZaken": false,
                        "alleenAfgeslotenZaken": false,
                        "alleenMijnTaken": false,
                        "zoeken": { "DOCUMENT_TITEL": "$documentTitle" },
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
                    responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                        {
                          "foutmelding" : "",
                          "resultaten" : [ {
                            "auteur" : "$documentAuthorName",
                            "bestandsnaam" : "$TEST_PDF_FILE_NAME",
                            "creatiedatum" : "$today",
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
                            "registratiedatum" : "$today",
                            "status" : "in_bewerking",
                            "titel" : "$documentTitle",
                            "type" : "DOCUMENT",
                            "versie" : 1,
                            "vertrouwelijkheidaanduiding" : "ZAAKVERTROUWELIJK",
                            "zaakIdentificatie" : "$zaakIdentification",
                            "zaakRelatie" : "Hoort bij, omgekeerd: kent",
                            "zaakUuid" : "$zaakUuid",
                            "zaaktypeIdentificatie" : "$ZAAKTYPE_TEST_2_IDENTIFICATIE",
                            "zaaktypeOmschrijving" : "$ZAAKTYPE_TEST_2_DESCRIPTION",
                            "zaaktypeUuid" : "$ZAAKTYPE_TEST_2_UUID"
                          } ],
                          "totaal" : 1,
                          "filters" : {
                            "ZAAKTYPE" : [ {
                              "aantal" : 1,
                              "naam" : "Test zaaktype 2"
                            } ],
                            "DOCUMENT_STATUS" : [ {
                              "aantal" : 1,
                              "naam" : "in_bewerking"
                            } ],
                            "DOCUMENT_TYPE" : [ {
                              "aantal" : 1,
                              "naam" : "bijlage"
                            } ],
                            "DOCUMENT_VERGRENDELD_DOOR" : [ {
                              "aantal" : 1,
                              "naam" : "-NULL-"
                            } ],
                            "DOCUMENT_INDICATIES" : [ {
                              "aantal" : 1,
                              "naam" : "GEBRUIKSRECHT"
                            }, {
                              "aantal" : 1,
                              "naam" : "ONDERTEKEND"
                            } ]
                          }
                        }
                    """.trimIndent()
                }
            }
        }
    }

    Context("Listing zaken for information object type") {
        Given(
            """Two zaken have been created and indexed, 
                one of which can be linked to an information object of a specific type"""
        ) {
            When(
                """
                the search endpoint is called to search for all objects of type 'ZAAK' 
                with a specific information object UUID
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
                the search endpoint is called to search for all objects of type 'ZAAK' 
                with a non-existing information object UUID
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
        }
    }
})
