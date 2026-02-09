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
import nl.info.zac.itest.client.DocumentHelper
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_2024_01_01
import nl.info.zac.itest.config.ItestConfiguration.HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_FACTUUR_UUID
import nl.info.zac.itest.config.ItestConfiguration.STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_DOMAIN_TEST_1
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrder
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate

/**
 * This test creates multiple zaken, tasks and documents, indexes them, and performs searches.
 * It is limited in scope because previously run tests may already have created zaken, tasks and documents.
 * In the future, this test could be improved by first deleting all existing zaken, tasks and documents.
 */
@Suppress("LargeClass")
class SearchRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val taskHelper = TaskHelper(zacClient)
    val documentHelper = DocumentHelper(zacClient)
    val logger = KotlinLogging.logger {}
    val now = System.currentTimeMillis()

    Context("Listing search results") {
        Given(
            """
            A logged-in raadpleger and a zaak has been created and is indexed, 
            an 'aanvullende informatie' task has been added to the zaak and is indexed,
            and a document has been uploaded to the zaak and is indexed           
            """.trimIndent()
        ) {
            // First log in as a beheerder authorised in all domains
            // and create the zaken, tasks and documents and index them.
            // Make sure the zaak description and document title are unique for this test run,
            // because we use it later on to search on this zaak.
            val zaakDescription = "${SearchRestServiceTest::class.simpleName}-listingsearchresults-$now"
            val documentTitle = "${SearchRestServiceTest::class.simpleName}-listingsearchresults-$now"
            val documentAuthorName = "fakeAuthorNameForSearchRestServiceTest"
            val today = LocalDate.now()
            val aanvullendeInformatieTaskFatalDate = today.plusDays(1)
            val (zaakIdentification, zaakUuid) = zaakHelper.createZaak(
                zaakDescription = zaakDescription,
                zaaktypeUuid = ZAAKTYPE_TEST_2_UUID,
                indexZaak = true,
                testUser = BEHEERDER_ELK_ZAAKTYPE
            )
            taskHelper.startAanvullendeInformatieTaskForZaak(
                zaakUuid = zaakUuid,
                zaakIdentificatie = zaakIdentification,
                fatalDate = aanvullendeInformatieTaskFatalDate,
                group = BEHANDELAARS_DOMAIN_TEST_1,
                waitForTaskToBeIndexed = true,
                testUser = BEHEERDER_ELK_ZAAKTYPE
            )
            documentHelper.uploadDocumentToZaak(
                zaakUuid = zaakUuid,
                documentTitle = documentTitle,
                authorName = documentAuthorName,
                fileName = TEST_PDF_FILE_NAME,
                indexDocument = true,
                testUser = BEHEERDER_ELK_ZAAKTYPE
            )

            When(
                """the search endpoint is called to search open zaken on the unique description
                    of the just created zaak"""
            ) {
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
                    """.trimIndent(),
                    testUser = RAADPLEGER_DOMAIN_TEST_1
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
                            "statustypeOmschrijving" : "$STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE",
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
                    """.trimIndent(),
                    testUser = RAADPLEGER_DOMAIN_TEST_1,
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
                    """.trimIndent(),
                    testUser = RAADPLEGER_DOMAIN_TEST_1,
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
                            "indicatieOndertekend" : false,
                            "indicatieVergrendeld" : false,
                            "indicaties" : [ ],
                            "rechten" : {
                              "converteren": false,
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
            """Two zaken have been created, each with a different zaaktype, and have been indexed, 
                one of which can be linked to an information object of a specific type,
                and a logged-in raadpleger authorised for the domain of these zaaktypes"""
        ) {
            // make sure the zaak descriptions are unique for this test run,
            // because we use it later on to search on these zaken
            val zaak1Description = "${SearchRestServiceTest::class.simpleName}-listzakenforinformationobjecttype1-$now"
            val zaak2Description = "${SearchRestServiceTest::class.simpleName}-listzakenforinformationobjecttype2-$now"
            val (zaak1Identification, zaak1Uuid) = zaakHelper.createZaak(
                zaakDescription = zaak1Description,
                zaaktypeUuid = ZAAKTYPE_TEST_2_UUID,
                indexZaak = true,
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )
            val (zaak2Identification, zaak2Uuid) = zaakHelper.createZaak(
                zaakDescription = zaak2Description,
                zaaktypeUuid = ZAAKTYPE_TEST_3_UUID,
                indexZaak = true,
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )
            When(
                """
                the search endpoint is called to search for the first created zaak
                with a specific information object UUID
                """.trimMargin()
            ) {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zoeken/zaken",
                    requestBodyAsString = """
                        {
                            "rows": 5,
                            "page": 0,
                            "zaakIdentificator": "$zaak1Identification",
                            "informationObjectTypeUuid": "$INFORMATIE_OBJECT_TYPE_FACTUUR_UUID"
                        }
                    """.trimIndent(),
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )
                Then(
                    """
                   the response is successful and the search results consists of the indexed zaak
                   with a 'linkable' flag set to true
                    """.trimMargin()
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "foutmelding" : "",
                      "resultaten" : [ 
                      {
                        "id" : "$zaak1Uuid",
                        "identificatie" : "$zaak1Identification",
                        "isKoppelbaar" : true,
                        "omschrijving" : "$zaak1Description",
                        "statustypeOmschrijving" : "Intake",
                        "toelichting" : "null",
                        "type" : "ZAAK",
                        "zaaktypeOmschrijving" : "$ZAAKTYPE_TEST_2_DESCRIPTION"
                      } 
                      ],
                      "totaal" : 1,
                      "filters" : { }
                    }
                    """.trimIndent()
                }
            }

            When(
                """
                the search endpoint is called to search for the second created zaak
                with a specific information object UUID
                """.trimMargin()
            ) {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zoeken/zaken",
                    requestBodyAsString = """
                        {
                            "rows": 5,
                            "page": 0,
                            "zaakIdentificator": "$zaak2Identification",
                            "informationObjectTypeUuid": "$INFORMATIE_OBJECT_TYPE_FACTUUR_UUID"
                        }
                    """.trimIndent(),
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )
                Then(
                    """
                      the response is successful and the search results consists of the indexed zaak
                      with a 'linkable' flag set to false
                    """.trimMargin()
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                        {
                          "foutmelding" : "",
                          "resultaten" : [ 
                          {
                            "id" : "$zaak2Uuid",
                            "identificatie" : "$zaak2Identification",
                            "isKoppelbaar" : false,
                            "omschrijving" : "$zaak2Description",
                            "statustypeOmschrijving" : "Intake",
                            "toelichting" : "null",
                            "type" : "ZAAK",
                            "zaaktypeOmschrijving" : "$ZAAKTYPE_TEST_3_DESCRIPTION"
                          } 
                          ],
                          "totaal" : 1,
                          "filters" : { }
                        }
                    """.trimIndent()
                }
            }

            When(
                """
                the search endpoint is called to search for the first created zaak 
                but with a non-existing information object UUID
                """.trimMargin()
            ) {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zoeken/zaken",
                    requestBodyAsString = """
                        {
                            "rows": 5,
                            "page": 0,
                            "zaakIdentificator": "$zaak1Identification",
                            "informationObjectTypeUuid": "a47b0c7e-1d0d-4c33-918d-160677516f1c"
                        }
                    """.trimIndent(),
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )
                Then(
                    """
                    the response is successful and the search results consists of the indexed zaak
                    with a 'linkable' flag set to false
                    """
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                        {
                            "foutmelding" : "",
                            "resultaten" : [
                            {
                                "id" : "$zaak1Uuid",
                                "identificatie" : "$zaak1Identification",
                                "isKoppelbaar" : false,
                                "omschrijving" : "$zaak1Description",
                                "statustypeOmschrijving" : "Intake",
                                "toelichting" : "null",
                                "type" : "ZAAK",
                                "zaaktypeOmschrijving" : "$ZAAKTYPE_TEST_2_DESCRIPTION"
                            }
                            ],
                            "totaal" : 1,
                            "filters" : { }
                        }
                    """.trimIndent()
                }
            }

            When(
                """
                the search endpoint is called with a null zaakIdentificator
                """.trimMargin()
            ) {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zoeken/zaken",
                    requestBodyAsString = """
                        {
                            "rows": 5,
                            "page": 0,
                            "zaakIdentificator": null,
                            "informationObjectTypeUuid": "$INFORMATIE_OBJECT_TYPE_FACTUUR_UUID"
                        }
                    """.trimIndent(),
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                )
                Then(
                    """
                    the response returns a BAD_REQUEST with the error code for missing required parameter
                    """
                ) {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_BAD_REQUEST
                    responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                        {
                            "message": "msg.error.search.required.parameter.missing"
                        }
                    """.trimIndent()
                }
            }
        }
    }
})
