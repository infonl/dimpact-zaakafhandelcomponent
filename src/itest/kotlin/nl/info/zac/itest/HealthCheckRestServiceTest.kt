/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.DATE_2023_09_21
import nl.info.zac.itest.config.ItestConfiguration.DATE_2023_10_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_2025_01_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_2025_07_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_1_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import java.net.HttpURLConnection.HTTP_OK

class HealthCheckRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("Default communicatiekanalen referentietabel data is provisioned on startup") {
        When("the check on the existence of the e-formulier communicatiekanaal is performed") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/health-check/bestaat-communicatiekanaal-eformulier"
            )
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }

            Then("the response should be a 200 OK with a response body 'true'") {
                response.code shouldBe HTTP_OK
                responseBody shouldBe "true"
            }
        }
    }

    Given("Zaak types are configured correctly") {
        When("the check for zaak types validity is performed") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/health-check/zaaktypes"
            )
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }

            Then("the response should be a 200 OK") {
                response.code shouldBe HTTP_OK
            }
            responseBody shouldEqualJson """
                    [
                      {
                        "aantalBehandelaarroltypen": 1,
                        "aantalInitiatorroltypen": 1,
                        "besluittypeAanwezig": false,
                        "brpInstellingenCorrect": true,
                        "informatieobjecttypeEmailAanwezig": true,
                        "resultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [],
                        "rolOverigeAanwezig": true,
                        "statustypeAanvullendeInformatieVereist": true,
                        "statustypeAfgerondAanwezig": true,
                        "statustypeAfgerondLaatsteVolgnummer": true,
                        "statustypeHeropendAanwezig": true,
                        "statustypeInBehandelingAanwezig": true,
                        "statustypeIntakeAanwezig": true,
                        "valide": true,
                        "zaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2025_07_01",
                          "doel": "$ZAAKTYPE_TEST_1_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_TEST_1_IDENTIFICATIE",
                          "nuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_TEST_1_DESCRIPTION",
                          "servicenorm": false,
                          "uuid": "$ZAAKTYPE_TEST_1_UUID",
                          "versiedatum": "$DATE_2025_07_01",
                          "vertrouwelijkheidaanduiding": "openbaar"
                        }
                      },
                      {                     
                        "aantalBehandelaarroltypen": 1,
                        "aantalInitiatorroltypen": 1,
                        "besluittypeAanwezig": false,
                        "brpInstellingenCorrect": true,
                        "informatieobjecttypeEmailAanwezig": true,
                        "resultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [],
                        "rolOverigeAanwezig": true,
                        "statustypeAanvullendeInformatieVereist": true,
                        "statustypeAfgerondAanwezig": true,
                        "statustypeAfgerondLaatsteVolgnummer": true,
                        "statustypeHeropendAanwezig": true,
                        "statustypeInBehandelingAanwezig": true,
                        "statustypeIntakeAanwezig": true,
                        "valide": true,
                        "zaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2025_01_01",
                          "doel": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_BPMN_TEST_IDENTIFICATIE",
                          "nuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                          "servicenorm": false,
                          "uuid": "$ZAAKTYPE_BPMN_TEST_UUID",
                          "versiedatum": "$DATE_2025_01_01",
                          "vertrouwelijkheidaanduiding": "openbaar"
                        }
                      },  
                      {
                        "aantalBehandelaarroltypen": 1,
                        "aantalInitiatorroltypen": 1,
                        "besluittypeAanwezig": true,
                        "brpInstellingenCorrect": true,
                        "informatieobjecttypeEmailAanwezig": true,
                        "resultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [],
                        "rolOverigeAanwezig": true,
                        "statustypeAanvullendeInformatieVereist": true,
                        "statustypeAfgerondAanwezig": true,
                        "statustypeAfgerondLaatsteVolgnummer": true,
                        "statustypeHeropendAanwezig": true,
                        "statustypeInBehandelingAanwezig": true,
                        "statustypeIntakeAanwezig": true,
                        "valide": true,
                        "zaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2023_10_01",
                          "doel": "$ZAAKTYPE_TEST_2_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_TEST_2_IDENTIFICATIE",
                          "nuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_TEST_2_DESCRIPTION",
                          "servicenorm": false,
                          "uuid": "$ZAAKTYPE_TEST_2_UUID",
                          "versiedatum": "$DATE_2023_10_01",
                          "vertrouwelijkheidaanduiding": "openbaar"
                        }
                      },
                      {
                        "aantalBehandelaarroltypen": 1,
                        "aantalInitiatorroltypen": 1,
                        "brpInstellingenCorrect": true,
                        "besluittypeAanwezig": false,
                        "informatieobjecttypeEmailAanwezig": true,
                        "resultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [],
                        "rolOverigeAanwezig": true,
                        "statustypeAanvullendeInformatieVereist": true,
                        "statustypeAfgerondAanwezig": true,
                        "statustypeAfgerondLaatsteVolgnummer": true,
                        "statustypeHeropendAanwezig": true,
                        "statustypeInBehandelingAanwezig": true,
                        "statustypeIntakeAanwezig": true,
                        "valide": true,
                        "zaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "$DATE_2023_09_21",
                          "doel": "$ZAAKTYPE_TEST_3_DESCRIPTION",
                          "identificatie": "$ZAAKTYPE_TEST_3_IDENTIFICATIE",
                          "nuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_TEST_3_DESCRIPTION",
                          "servicenorm": false,
                          "uuid": "$ZAAKTYPE_TEST_3_UUID",
                          "versiedatum": "$DATE_2023_09_21",
                          "vertrouwelijkheidaanduiding": "openbaar"
                        }
                      }
                    ]    
            """.trimIndent()

            And("the body contains all the performed checks") {
            }
        }
    }
})
