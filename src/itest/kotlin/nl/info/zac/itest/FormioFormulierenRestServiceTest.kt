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
import nl.info.zac.itest.config.ItestConfiguration.BPMN_SUMMARY_FORM_NAME
import nl.info.zac.itest.config.ItestConfiguration.BPMN_SUMMARY_FORM_RESOURCE_PATH
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_FORM_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import java.io.File
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_OK

class FormioFormulierenRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given(
        """BPMN form.io process task forms have been created in ZAC in the integration test setup phase
            and a beheerder is logged in
            """
    ) {
        authenticate(BEHEERDER_ELK_ZAAKTYPE)

        When("the summary test form is uploaded") {
            val formIoFileContent = Thread.currentThread().contextClassLoader.getResource(
                BPMN_SUMMARY_FORM_RESOURCE_PATH
            )?.let {
                File(it.path)
            }!!.readText(Charsets.UTF_8).replace("\"", "\\\"").replace("\n", "\\n")
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/formio-formulieren",
                requestBodyAsString = """
                {
                    "filename": "$BPMN_SUMMARY_FORM_RESOURCE_PATH",
                    "content": "$formIoFileContent"
                }
                """.trimIndent()
            )
            Then("the response is successful") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_CREATED
            }
        }

        When("the Form.io forms are retrieved") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/formio-formulieren"
            )
            Then("the response contains the form.io forms that were just created") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                [
                    {
                        "id": 2,
                        "name": "summaryForm",
                        "title": "$BPMN_SUMMARY_FORM_NAME"
                    },
                    {
                        "id": 1,
                        "name": "testForm",
                        "title": "$BPMN_TEST_FORM_NAME"
                    }
                ]
                """.trimIndent()
            }
        }
    }
})
