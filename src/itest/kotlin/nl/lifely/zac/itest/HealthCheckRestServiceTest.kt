/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI

@Suppress("MagicNumber")
class HealthCheckRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("Default communicatiekanalen referentietabel data is provisioned on startup") {
        When("the check on the existence of the e-formulier communicatiekanaal is performed") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/health-check/bestaat-communicatiekanaal-eformulier"
            )
            Then(
                """the response should be a 200 OK with a response body 'true'"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody shouldBe "true"
            }
        }
    }
})
