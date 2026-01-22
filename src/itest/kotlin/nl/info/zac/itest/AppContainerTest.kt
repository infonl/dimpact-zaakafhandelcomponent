/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAC_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_MANAGEMENT_URI
import nl.info.zac.itest.config.USER_WITHOUT_ANY_ROLE
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_MOVED_TEMP
import java.net.HttpURLConnection.HTTP_OK

class AppContainerTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("ZAC Docker container and all related Docker containers are running") {
        When("the liveness endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_MANAGEMENT_URI/health/live"
            )
            logger.info {
                """
                |GET $ZAC_MANAGEMENT_URI/health/live
                |Response:
                | Code: ${response.code}
                | Body: ${response.bodyAsString}
                """.trimMargin()
            }
            Then("it should return 200 OK with UP status") {
                response.code shouldBe HTTP_OK

                val healthResponse = JSONObject(response.bodyAsString)
                healthResponse.getString("status") shouldBe "UP"

                // Should contain our LivenessHealthCheck
                val checks = healthResponse.getJSONArray("checks")
                checks.length() shouldBeGreaterThan 0
                val livenessCheck = checks.getJSONObject(0)
                livenessCheck.getString("name") shouldBe "nl.info.zac.health.LivenessHealthCheck"
                livenessCheck.getString("status") shouldBe "UP"
            }
        }

        When("the readiness endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_MANAGEMENT_URI/health/ready"
            )
            logger.info {
                """
                |GET $ZAC_MANAGEMENT_URI/health/ready
                |Response:
                | Code: ${response.code}
                | Body: ${response.bodyAsString}
                """.trimMargin()
            }
            Then("the response should be ok with UP status") { response.code shouldBe HTTP_OK }
            val healthResponse = JSONObject(response.bodyAsString)
            And("the response should report status UP") { healthResponse.getString("status") shouldBe "UP" }

            And("the response should report all checks status UP") {
                val checks = healthResponse.getJSONArray("checks")

                checks.length() shouldBeGreaterThan 0
                // Check that all readiness checks are UP
                for (i in 0 until checks.length()) {
                    val check = checks.getJSONObject(i)
                    check.getString("status") shouldBe "UP"
                }
            }

            And("it should include OpenZaak readiness check") {
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

        When("the generic health endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_MANAGEMENT_URI/health"
            )
            logger.info {
                """
                |GET $ZAC_MANAGEMENT_URI/health
                |Response:
                | Code: ${response.code}
                | Body: ${response.bodyAsString}
                """.trimMargin()
            }
            Then("the response should be ok with UP status") { response.code shouldBe HTTP_OK }

            val healthResponse = JSONObject(response.bodyAsString)
            And("the response should report status UP") { healthResponse.getString("status") shouldBe "UP" }

            And("the response should contain all health checks information") {
                val checks = healthResponse.getJSONArray("checks")

                checks.length() shouldBeGreaterThan 0
                // Verify all checks are UP
                for (i in 0 until checks.length()) {
                    val check = checks.getJSONObject(i)
                    check.getString("status") shouldBe "UP"
                }
            }
            And("it should include all expected health check classes") {
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

        When("the metrics endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_MANAGEMENT_URI/metrics"
            )
            Then("the response should be ok and the the uptime var should be present") {
                response.code shouldBe HTTP_OK
                with(response.bodyAsString) {
                    contains("base_jvm_uptime_seconds").shouldBe(true)
                }
            }
        }
        When("/admin is requested for a user who has the 'beheerder' role") {
            authenticate(BEHEERDER_ELK_ZAAKTYPE)
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_BASE_URI/admin"
            )
            Then("the response should be ok") {
                response.code shouldBe HTTP_OK
            }
        }
        When("/admin is requested for a user who does not have the 'beheerder' role") {
            authenticate(BEHANDELAAR_DOMAIN_TEST_1)
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_BASE_URI/admin"
            )
            Then("the response should be forbidden") {
                response.code shouldBe HTTP_FORBIDDEN
            }
        }
    }

    Given("A logged-in user who does not have any of the ZAC application roles") {
        authenticate(USER_WITHOUT_ANY_ROLE)

        When("The ZAC base URI is requested") {
            val response = itestHttpClient.performGetRequest(
                url = ZAC_BASE_URI
            )
            Then("the response should be forbidden") {
                response.code shouldBe HTTP_FORBIDDEN
            }
        }

        When("The ZAC logout URI is requested") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_BASE_URI/logout"
            )
            Then("the response should be a redirect") {
                response.code shouldBe HTTP_MOVED_TEMP
            }
        }
    }
})
