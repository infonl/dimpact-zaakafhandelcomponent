/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_PROCESS_ID
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_GROUP_A
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_REFERENCE_TABLES_UPDATED
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields

@Order(TEST_SPEC_ORDER_AFTER_REFERENCE_TABLES_UPDATED)
class ZaaktypeBpmnConfigurationRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val testUrl = "$ZAC_API_URI/zaaktype-bpmn-configuration"

    val bpmnZaakType = """
        {
            "id": 1,
            "zaaktypeUuid": "$ZAAKTYPE_BPMN_TEST_UUID",
            "zaaktypeOmschrijving": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
            "bpmnProcessDefinitionKey": "$BPMN_TEST_PROCESS_ID",
            "productaanvraagtype": "$ZAAKTYPE_BPMN_PRODUCTAANVRAAG_TYPE",
            "groepNaam": "${OLD_IAM_TEST_GROUP_A.description}"
        }
    """.trimIndent()

    Given("No existing zaaktype - BPMN process definition mapping") {
        lateinit var responseBody: String

        When("a zaaktype configuration is created") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$testUrl/$BPMN_TEST_PROCESS_ID",
                requestBodyAsString = """{ 
                  "zaaktypeUuid": "$ZAAKTYPE_BPMN_TEST_UUID",
                  "zaaktypeOmschrijving": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                  "productaanvraagtype": "$ZAAKTYPE_BPMN_PRODUCTAANVRAAG_TYPE",
                  "groepNaam": "${OLD_IAM_TEST_GROUP_A.description}"
                }
                """.trimIndent()
            )

            Then("the response is successful") {
                responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
            }

            And("the response contains the created zaaktype configuration") {
                responseBody shouldEqualJsonIgnoringExtraneousFields bpmnZaakType
            }
        }
    }

    Given("Configured BPMN zaak type") {
        lateinit var responseBody: String

        When("the BPMN zaaktype configuration is retrieved") {
            val response = itestHttpClient.performGetRequest(
                "$testUrl/$BPMN_TEST_PROCESS_ID"
            )

            Then("the response is successful") {
                responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
            }

            And("the expected zaak type data is returned") {
                responseBody shouldEqualJsonIgnoringExtraneousFields bpmnZaakType
            }
        }

        When("list of all BPMN zaaktype configurations is retrieved") {
            lateinit var responseBody: String

            val response = itestHttpClient.performGetRequest(testUrl)

            Then("the response is successful") {
                responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
            }

            And("the expected zaak type data list is returned") {
                responseBody shouldEqualJsonIgnoringExtraneousFields "[$bpmnZaakType]"
            }
        }
    }
})
