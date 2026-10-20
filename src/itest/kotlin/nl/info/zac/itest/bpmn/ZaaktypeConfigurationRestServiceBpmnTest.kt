/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

class ZaaktypeConfigurationRestServiceBpmnTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val bpmnZaaktype1ConfigurationUrl = "$ZAC_API_URI/zaakafhandelparameters/$ZAAKTYPE_BPMN_TEST_1_UUID"

    given("a BPMN zaaktype configuration that was created in the overall test setup") {
        `when`("a beheerder reads it") {
            val response = itestHttpClient.performGetRequest(url = bpmnZaaktype1ConfigurationUrl, testUser = BEHEERDER_1)
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }

            then("the response names the BPMN process definition that the zaaktype is bound to") {
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    {
                      "zaaktype": {
                        "uuid": "$ZAAKTYPE_BPMN_TEST_1_UUID",
                        "omschrijving": "$ZAAKTYPE_BPMN_TEST_1_DESCRIPTION"
                      },
                      "processDefinition": {
                        "processEngine": "BPMN",
                        "key": "$BPMN_TEST_PROCESS_DEFINITION_KEY",
                        "humanTaskDefinitions": [],
                        "userEventListenerDefinitions": []
                      },
                      "productaanvraagtype": "$ZAAKTYPE_BPMN_TEST_1_PRODUCTAANVRAAG_TYPE",
                      "defaultGroepId": "${GROUP_BEHANDELAARS_TEST_1.name}",
                      "defaultBehandelaarId": "${BEHANDELAAR_1.username}",
                      "smartDocuments": { "isEnabledGlobally": true, "isEnabledForZaaktype": false }
                    }
                """.trimIndent()
            }

            and("it has no settings of plan items, as a BPMN process definition has no plan items") {
                with(JSONObject(responseBody)) {
                    getJSONArray("humanTaskParameters").isEmpty shouldBe true
                    getJSONArray("userEventListenerParameters").isEmpty shouldBe true
                    has("intakeMail") shouldBe false
                    has("afrondenMail") shouldBe false
                }
            }
        }

        `when`("a user who may start a zaak but who is no beheerder reads it") {
            val response = itestHttpClient.performGetRequest(
                url = bpmnZaaktype1ConfigurationUrl,
                testUser = BEHANDELAAR_1
            )

            then("the request is refused") {
                response.code shouldBe HTTP_FORBIDDEN
            }
        }
    }

    given("the BPMN zaaktype configuration resource that served BPMN configurations before") {
        `when`("a beheerder reads the list of BPMN configurations from it") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaaktype-bpmn-configuration",
                testUser = BEHEERDER_1
            )

            then("it does not exist") {
                response.code shouldBe HTTP_NOT_FOUND
            }
        }
    }
})
