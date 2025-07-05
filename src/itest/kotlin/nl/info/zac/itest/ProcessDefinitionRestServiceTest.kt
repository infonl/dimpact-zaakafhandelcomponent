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
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_PROCESS_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_INITIAL
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import java.io.File

@Order(TEST_SPEC_ORDER_INITIAL)
class ProcessDefinitionRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("No existing BPMN process definitions") {
        When("the integration test BPMN process definition is created from our BPMN test process file") {
            val bpmnTestProcessFileContent = Thread.currentThread().contextClassLoader.getResource(
                BPMN_TEST_PROCESS_RESOURCE_PATH
            )?.let {
                File(it.path)
            }!!.readText(Charsets.UTF_8).replace("\"", "\\\"").replace("\n", "\\n")
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/process-definitions",
                requestBodyAsString = """
                {
                    "filename": "$BPMN_TEST_PROCESS_RESOURCE_PATH",
                    "content": "$bpmnTestProcessFileContent"
                }
                """.trimIndent()
            )
            Then("the response is successful") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
            }
        }
        When("the process definitions are retrieved") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/process-definitions"
            )
            Then("the response contains the BPMN process definition that was just created") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                 [  
                  {
                    "key": "itProcessDefinition",
                    "name": "Integration Tests BPMN Process Definition",
                    "version": 1
                  }
                ]  
                """.trimIndent()
            }
        }
    }
})
