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
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHEERDER_1
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

    given("ZAC Docker container and all related Docker containers are running") {
        `when`("the liveness endpoint is called") {
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
            then("it should return 200 OK with UP status") {
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

        `when`("the readiness endpoint is called") {
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
            then("the response should be ok with UP status") { response.code shouldBe HTTP_OK }
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

            And("it should include PABC readiness check") {
                val checks = healthResponse.getJSONArray("checks")

                var foundOpenZaakCheck = false
                for (i in 0 until checks.length()) {
                    val check = checks.getJSONObject(i)
                    if (check.getString("name") == "nl.info.zac.health.PabcReadinessHealthCheck") {
                        foundOpenZaakCheck = true
                        check.getString("status") shouldBe "UP"
                        break
                    }
                }
                foundOpenZaakCheck shouldBe true
            }
        }

        `when`("the generic health endpoint is called") {
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
            then("the response should be ok with UP status") { response.code shouldBe HTTP_OK }

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

        `when`("the metrics endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_MANAGEMENT_URI/metrics"
            )
            then("the response should be ok and the the uptime var should be present") {
                response.code shouldBe HTTP_OK
                with(response.bodyAsString) {
                    contains("base_jvm_uptime_seconds").shouldBe(true)
                }
            }
        }
        `when`("/admin is requested for a user who has the 'beheerder' role") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_BASE_URI/admin",
                testUser = BEHEERDER_1
            )
            then("the response should be ok") {
                response.code shouldBe HTTP_OK
            }
        }
        `when`("/admin is requested for a user who does not have the 'beheerder' role") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_BASE_URI/admin",
                testUser = BEHANDELAAR_1
            )
            then("the response should be forbidden") {
                response.code shouldBe HTTP_FORBIDDEN
            }
        }
    }

    given("A logged-in user who does not have any of the ZAC application roles") {
        `when`("The ZAC base URI is requested") {
            val response = itestHttpClient.performGetRequest(
                url = ZAC_BASE_URI,
                testUser = USER_WITHOUT_ANY_ROLE
            )
            then("the response should be forbidden") {
                response.code shouldBe HTTP_FORBIDDEN
            }
        }

        `when`("The ZAC logout URI is requested") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_BASE_URI/sign-out",
                testUser = USER_WITHOUT_ANY_ROLE
            )
            then("the response should redirect to the ZAC root") {
                response.code shouldBe HTTP_MOVED_TEMP
                response.headers["Location"] shouldBe "$ZAC_BASE_URI/"
            }
        }
    }

    given("An authenticated user with ZAC application roles") {
        `when`("the ZAC logout URI is requested") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_BASE_URI/sign-out",
                testUser = BEHANDELAAR_1
            )
            then("the response should redirect to the ZAC root and not to the Keycloak logout page") {
                response.code shouldBe HTTP_MOVED_TEMP
                response.headers["Location"] shouldBe "$ZAC_BASE_URI/"
            }
        }
    }
})
