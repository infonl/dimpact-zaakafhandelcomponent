/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import java.net.HttpURLConnection

class FormioFormulierenRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("BPMN process definition and Formio forms have been created in ZAC in the integration test setup phase") {
        authenticate(BEHEERDER_ELK_ZAAKTYPE)

        // Upload of forms is already handled by ZacItestProjectConfig

        When("the Form.io forms are listed") {
            val response = itestHttpClient.performGetRequest(
                "${ZAC_API_URI}/formio-formulieren"
            )
            Then("the response contains the form.io forms that were created") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HttpURLConnection.HTTP_OK
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                [
                    {
                        "name": "testForm",
                        "title": "Test form"
                    },
                    {
                        "name": "summaryForm",
                        "title": "Summary form"
                    },
                    {
                        "name": "zaakDefaults",
                        "title": "Zaak defaults"
                    },
                    {
                        "name": "hardCoded",
                        "title": "Hard-coded"
                    }
                ]
                """.trimIndent()
            }
        }
    }
})
