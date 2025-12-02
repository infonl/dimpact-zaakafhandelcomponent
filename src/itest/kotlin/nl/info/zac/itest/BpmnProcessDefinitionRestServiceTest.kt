/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import java.net.HttpURLConnection.HTTP_OK

class BpmnProcessDefinitionRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given(
        """A BPMN process definition has been created in ZAC in the integration test setup phase
            and a beheerder is logged in"""
    ) {
        authenticate(BEHEERDER_ELK_ZAAKTYPE)
        When("the process definitions are retrieved") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/bpmn-process-definitions"
            )
            Then("the response contains the BPMN process definition that was just created") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
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
