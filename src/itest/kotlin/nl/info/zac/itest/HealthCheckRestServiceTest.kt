/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_2023_09_21
import nl.info.zac.itest.config.ItestConfiguration.DATE_2023_10_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_2025_07_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_2026_07_23
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_4_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_4_DOEL
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_4_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK

class HealthCheckRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    given(
        """
            Default communicatiekanalen referentietabel data is provisioned on startup,
            and a logged-in beheerder
            """
    ) {
        `when`("the referentietabellen resource is read") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/health-check/referentietabellen",
                testUser = BEHEERDER_1
            )
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }

            then("the response is a 200 OK and reports that the e-formulier communicatiekanaal is present") {
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJson """
                    {
                      "isCommunicatiekanaalEformulierAanwezig": true
                    }
                """.trimIndent()
            }
        }
    }

    given("CMMN and BPMN zaaktypes are configured and a logged-in beheerder") {
        `when`("the inrichtingschecks are listed with the default query parameters") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/health-check/zaaktypes",
                testUser = BEHEERDER_1
            )
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }

            then("the response should be a 200 OK") {
                response.code shouldBe HTTP_OK
            }
            and("the page holds the checks of the CMMN zaaktypes only, in ascending order of omschrijving") {
                responseBody shouldEqualJson """
                    {
                      "foutmelding": "",
                      "totaal": 4,
                      "resultaten": [
                        {
                          "aantalBehandelaarroltypen": 1,
                          "aantalInitiatorroltypen": 1,
                          "isBesluittypeAanwezig": false,
                          "isBrpInstellingenCorrect": true,
                          "hasWaarschuwingen": false,
                          "isInformatieobjecttypeEmailAanwezig": true,
                          "isZaakspecifiekeAutorisatieEigenschapAanwezig": false,
                          "isZaakspecifiekeAutorisatieRoltypeAanwezig": false,
                          "isResultaattypeAanwezig": true,
                          "resultaattypesMetVerplichtBesluit": [],
                          "resultaattypesMetNietOndersteundeBrondatum": [],
                          "isRolOverigeAanwezig": true,
                          "isStatustypeAanvullendeInformatieVereist": true,
                          "isStatustypeAfgerondAanwezig": true,
                          "isStatustypeAfgerondLaatsteVolgnummer": true,
                          "isStatustypeHeropendAanwezig": true,
                          "isStatustypeInBehandelingAanwezig": true,
                          "isStatustypeIntakeAanwezig": true,
                          "isValide": true,
                          "isZaakafhandelParametersValide": true,
                          "zaaktype": {
                            "beginGeldigheid": "$DATE_2025_07_01",
                            "doel": "$ZAAKTYPE_CMMN_TEST_1_DESCRIPTION",
                            "omschrijving": "$ZAAKTYPE_CMMN_TEST_1_DESCRIPTION",
                            "uuid": "$ZAAKTYPE_CMMN_TEST_1_UUID"
                          }
                        },
                        {
                          "aantalBehandelaarroltypen": 1,
                          "aantalInitiatorroltypen": 1,
                          "isBesluittypeAanwezig": true,
                          "isBrpInstellingenCorrect": true,
                          "hasWaarschuwingen": false,
                          "isInformatieobjecttypeEmailAanwezig": true,
                          "isZaakspecifiekeAutorisatieEigenschapAanwezig": true,
                          "isZaakspecifiekeAutorisatieRoltypeAanwezig": true,
                          "isResultaattypeAanwezig": true,
                          "resultaattypesMetVerplichtBesluit": [],
                          "resultaattypesMetNietOndersteundeBrondatum": [],
                          "isRolOverigeAanwezig": true,
                          "isStatustypeAanvullendeInformatieVereist": true,
                          "isStatustypeAfgerondAanwezig": true,
                          "isStatustypeAfgerondLaatsteVolgnummer": true,
                          "isStatustypeHeropendAanwezig": true,
                          "isStatustypeInBehandelingAanwezig": true,
                          "isStatustypeIntakeAanwezig": true,
                          "isValide": true,
                          "isZaakafhandelParametersValide": true,
                          "zaaktype": {
                            "beginGeldigheid": "$DATE_2023_10_01",
                            "doel": "$ZAAKTYPE_CMMN_TEST_2_DESCRIPTION",
                            "omschrijving": "$ZAAKTYPE_CMMN_TEST_2_DESCRIPTION",
                            "uuid": "$ZAAKTYPE_CMMN_TEST_2_UUID"
                          }
                        },
                        {
                          "aantalBehandelaarroltypen": 1,
                          "aantalInitiatorroltypen": 1,
                          "isBrpInstellingenCorrect": true,
                          "isBesluittypeAanwezig": false,
                          "hasWaarschuwingen": false,
                          "isInformatieobjecttypeEmailAanwezig": true,
                          "isZaakspecifiekeAutorisatieEigenschapAanwezig": false,
                          "isZaakspecifiekeAutorisatieRoltypeAanwezig": false,
                          "isResultaattypeAanwezig": true,
                          "resultaattypesMetVerplichtBesluit": [],
                          "resultaattypesMetNietOndersteundeBrondatum": [],
                          "isRolOverigeAanwezig": true,
                          "isStatustypeAanvullendeInformatieVereist": true,
                          "isStatustypeAfgerondAanwezig": true,
                          "isStatustypeAfgerondLaatsteVolgnummer": true,
                          "isStatustypeHeropendAanwezig": true,
                          "isStatustypeInBehandelingAanwezig": true,
                          "isStatustypeIntakeAanwezig": true,
                          "isValide": true,
                          "isZaakafhandelParametersValide": true,
                          "zaaktype": {
                            "beginGeldigheid": "$DATE_2023_09_21",
                            "doel": "$ZAAKTYPE_CMMN_TEST_3_DESCRIPTION",
                            "omschrijving": "$ZAAKTYPE_CMMN_TEST_3_DESCRIPTION",
                            "uuid": "$ZAAKTYPE_CMMN_TEST_3_UUID"
                          }
                        },
                        {
                          "aantalBehandelaarroltypen": 1,
                          "aantalInitiatorroltypen": 1,
                          "isBrpInstellingenCorrect": true,
                          "isBesluittypeAanwezig": true,
                          "hasWaarschuwingen": false,
                          "isInformatieobjecttypeEmailAanwezig": true,
                          "isZaakspecifiekeAutorisatieEigenschapAanwezig": false,
                          "isZaakspecifiekeAutorisatieRoltypeAanwezig": false,
                          "isResultaattypeAanwezig": true,
                          "resultaattypesMetVerplichtBesluit": [
                            "Opgelegd - Verval besluit",
                            "Opgelegd - Ingang besluit"
                          ],
                          "resultaattypesMetNietOndersteundeBrondatum": [],
                          "isRolOverigeAanwezig": true,
                          "isStatustypeAanvullendeInformatieVereist": true,
                          "isStatustypeAfgerondAanwezig": true,
                          "isStatustypeAfgerondLaatsteVolgnummer": true,
                          "isStatustypeHeropendAanwezig": true,
                          "isStatustypeInBehandelingAanwezig": true,
                          "isStatustypeIntakeAanwezig": true,
                          "isValide": true,
                          "isZaakafhandelParametersValide": true,
                          "zaaktype": {
                            "beginGeldigheid": "$DATE_2026_07_23",
                            "doel": "$ZAAKTYPE_CMMN_TEST_4_DOEL",
                            "omschrijving": "$ZAAKTYPE_CMMN_TEST_4_DESCRIPTION",
                            "uuid": "$ZAAKTYPE_CMMN_TEST_4_UUID"
                          }
                        }
                      ]
                    }
                """.trimIndent()
            }
        }

        `when`("the inrichtingschecks are listed with a filter on part of the omschrijving") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/health-check/zaaktypes?omschrijving=ZAAKTYPE%204&rows=10",
                testUser = BEHEERDER_1
            )
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }

            then("the page holds only the zaaktype whose omschrijving contains the filter") {
                response.code shouldBe HTTP_OK
                responseBody shouldEqualSpecifiedJsonIgnoringOrder """
                    {
                      "totaal": 1,
                      "resultaten": [
                        {
                          "zaaktype": {
                            "omschrijving": "$ZAAKTYPE_CMMN_TEST_4_DESCRIPTION",
                            "uuid": "$ZAAKTYPE_CMMN_TEST_4_UUID"
                          }
                        }
                      ]
                    }
                """.trimIndent()
            }
        }

        listOf(
            "a page size above the maximum" to "rows=101",
            "a negative page" to "page=-1",
            "an unknown status" to "status=ONBEKEND",
            "an unknown sort field" to "sortField=IDENTIFICATIE"
        ).forEach { (description, queryParameter) ->
            `when`("the inrichtingschecks are listed with $description") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/health-check/zaaktypes?$queryParameter",
                    testUser = BEHEERDER_1
                )
                logger.info { "Response: ${response.bodyAsString}" }

                then("the response is a 400 Bad Request") {
                    response.code shouldBe HTTP_BAD_REQUEST
                }
            }
        }
    }
})
