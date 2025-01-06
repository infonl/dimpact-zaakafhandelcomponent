/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_MANAGEMENT_URI

class AppContainerTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    Given("ZAC Docker container and all related Docker containers are running") {
        When("the health endpoint is called") {
            Then("the response should be ok and the status should be UP") {
                itestHttpClient.performGetRequest(
                    url = "$ZAC_MANAGEMENT_URI/health"
                ).use { response ->
                    response.isSuccessful shouldBe true
                    with(response.body!!.string()) {
                        shouldContainJsonKeyValue("status", "UP")
                    }
                }
            }
        }
        When("the metrics endpoint is called") {
            Then("the response should be ok and the the uptime var should be present") {
                itestHttpClient.performGetRequest(
                    url = "$ZAC_MANAGEMENT_URI/metrics"
                ).use { response ->
                    response.isSuccessful shouldBe true
                    with(response.body!!.string()) {
                        contains("base_jvm_uptime_seconds").shouldBe(true)
                    }
                }
            }
        }
    }
})
