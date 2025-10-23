/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.ItestConfiguration.TEST_BEHANDELAAR_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_BEHANDELAAR_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_WITHOUT_ANY_ROLE_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_WITHOUT_ANY_ROLE_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_MANAGEMENT_URI
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_MOVED_TEMP
import java.net.HttpURLConnection.HTTP_OK

class AppContainerTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    afterSpec {
        // re-authenticate using testuser1 since currently all subsequent integration tests rely on this user being logged in
        authenticate(username = TEST_USER_1_USERNAME, password = TEST_USER_1_PASSWORD)
    }

    Given("ZAC Docker container and all related Docker containers are running") {
        When("the health endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_MANAGEMENT_URI/health"
            )
            Then("the response should be ok and the status should be UP") {
                response.isSuccessful shouldBe true
                with(response.body.string()) {
                    shouldContainJsonKeyValue("status", "UP")
                }
            }
        }
        When("the metrics endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_MANAGEMENT_URI/metrics"
            )
            Then("the response should be ok and the the uptime var should be present") {
                response.isSuccessful shouldBe true
                with(response.body.string()) {
                    contains("base_jvm_uptime_seconds").shouldBe(true)
                }
            }
        }
        When("/admin is requested for a user who has the 'beheerder' role") {
            authenticate(username = TEST_USER_1_USERNAME, password = TEST_USER_1_USERNAME)
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_BASE_URI/admin"
            )
            Then("the response should be ok") {
                response.code shouldBe HTTP_OK
            }
        }
        When("/admin is requested for a user who does not have the 'beheerder' role") {
            authenticate(username = TEST_BEHANDELAAR_1_USERNAME, password = TEST_BEHANDELAAR_1_PASSWORD)
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_BASE_URI/admin"
            )
            Then("the response should be forbidden") {
                response.code shouldBe HTTP_FORBIDDEN
            }
        }
    }

    Given("A logged-in user who does not have any of the ZAC application roles") {
        authenticate(username = TEST_USER_WITHOUT_ANY_ROLE_USERNAME, password = TEST_USER_WITHOUT_ANY_ROLE_PASSWORD)

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
            Then(
                """
                the response should be a redirect and the 'Location' header 
                 should point to the Keycloak logout endpoint
                    """
            ) {
                response.code shouldBe HTTP_MOVED_TEMP
                val locationHeader = response.headers["Location"] ?: ""
                locationHeader.contains("/realms/") && locationHeader.contains("/protocol/openid-connect/logout")
            }
        }
    }
})
