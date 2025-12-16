/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.ZAC_MANAGEMENT_URI
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK


/**
 * Integration tests for MicroProfile Health endpoints that verify the health and readiness
 * of the ZAC application and its dependencies.
 */
class AppHealthCheckTest : BehaviorSpec({
    Given("ZAC application is running") {
        When("the liveness endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_MANAGEMENT_URI/health/live",
                addAuthorizationHeader = false
            )
            val responseBody = response.bodyAsString
            logger.info { "Liveness response: $responseBody" }

            Then("it should return 200 OK with UP status") {
                response.code shouldBe HTTP_OK

                val healthResponse = JSONObject(responseBody)
                healthResponse.getString("status") shouldBe "UP"

                // Should contain our LivenessHealthCheck
                val checks = healthResponse.getJSONArray("checks")
                checks.length() shouldBe LIVELINESS_CHECKS_COUNT
                val livenessCheck = checks.getJSONObject(0)
                livenessCheck.getString("name") shouldBe "nl.info.zac.health.LivenessHealthCheck"
                livenessCheck.getString("status") shouldBe "UP"
            }
        }
    }

    Given("ZAC application and its dependencies are running") {
        When("the readiness endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_MANAGEMENT_URI/health/ready",
                addAuthorizationHeader = false
            )
            val responseBody = response.bodyAsString
            logger.info { "Readiness response: $responseBody" }

            Then("it should return 200 OK with UP status") {
                response.code shouldBe HTTP_OK

                val healthResponse = JSONObject(responseBody)
                healthResponse.getString("status") shouldBe "UP"

                val checks = healthResponse.getJSONArray("checks")

                // Should have at least 2 readiness checks: OpenZaak and Solr
                checks.length() shouldBe READINESS_CHECKS_COUNT

                // Check that all readiness checks are UP
                for (i in 0 until checks.length()) {
                    val check = checks.getJSONObject(i)
                    check.getString("status") shouldBe "UP"
                }
            }

            And("it should include OpenZaak readiness check") {
                val healthResponse = JSONObject(responseBody)
                val checks = healthResponse.getJSONArray("checks")

                var foundOpenZaakCheck = false
                for (i in 0 until checks.length()) {
                    val check = checks.getJSONObject(i)
                    if (check.getString("name") == "nl.info.zac.health.OpenZaakReadinessHealthCheck") {
                        foundOpenZaakCheck = true
                        check.getString("status") shouldBe "UP"
                        break
                    }
                }
                foundOpenZaakCheck shouldBe true
            }

            And("it should include Solr readiness check") {
                val healthResponse = JSONObject(responseBody)
                val checks = healthResponse.getJSONArray("checks")

                var foundSolrCheck = false
                for (i in 0 until checks.length()) {
                    val check = checks.getJSONObject(i)
                    if (check.getString("name") == "nl.info.zac.health.SolrReadinessHealthCheck") {
                        foundSolrCheck = true
                        check.getString("status") shouldBe "UP"

                        // Check that Solr-specific data is included
                        val data = check.optJSONObject("data")
                        if (data != null) {
                            data.getString("core") shouldBe "zac"
                            data.getInt("status") shouldBe 0
                        }
                        break
                    }
                }
                foundSolrCheck shouldBe true
            }
        }
    }

    Given("ZAC application health endpoints") {
        When("the generic health endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_MANAGEMENT_URI/health",
                addAuthorizationHeader = false
            )
            val responseBody = response.bodyAsString
            logger.info { "Health response: $responseBody" }

            Then("it should return 200 OK with comprehensive health information") {
                response.code shouldBe HTTP_OK

                val healthResponse = JSONObject(responseBody)
                healthResponse.getString("status") shouldBe "UP"

                val checks = healthResponse.getJSONArray("checks")

                // Should have all health checks (liveness + readiness)
                checks.length() shouldBe LIVELINESS_CHECKS_COUNT + READINESS_CHECKS_COUNT

                // Verify all checks are UP
                for (i in 0 until checks.length()) {
                    val check = checks.getJSONObject(i)
                    check.getString("status") shouldBe "UP"
                }
            }

            And("it should include all expected health check classes") {
                val healthResponse = JSONObject(responseBody)
                val checks = healthResponse.getJSONArray("checks")

                val checkNames = mutableSetOf<String>()
                for (i in 0 until checks.length()) {
                    checkNames.add(checks.getJSONObject(i).getString("name"))
                }

                checkNames shouldContain "nl.info.zac.health.LivenessHealthCheck"
                checkNames shouldContain "nl.info.zac.health.OpenZaakReadinessHealthCheck"
                checkNames shouldContain "nl.info.zac.health.SolrReadinessHealthCheck"
            }
        }
    }
}) {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val itestHttpClient = ItestHttpClient()

        private const val LIVELINESS_CHECKS_COUNT = 1
        private const val READINESS_CHECKS_COUNT = 2
    }
}
