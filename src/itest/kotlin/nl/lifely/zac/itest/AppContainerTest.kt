/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_MANAGEMENT_URI

private val zacClient: ZacClient = ZacClient()

class AppContainerTest : BehaviorSpec({
    given("ZAC Docker container and all related Docker containers are running") {
        When("the health endpoint is called") {
            then("the response should be ok and the status should be UP") {
                zacClient.performGetRequest(
                    url = "${ZAC_MANAGEMENT_URI}/health"
                ).use { response ->
                    response.isSuccessful shouldBe true
                    with(response.body!!.string()) {
                        shouldContainJsonKeyValue("status", "UP")
                    }
                }
            }
        }
    }
    given("ZAC Docker container and all related Docker containers are running") {
        When("the metrics endpoint is called") {
            then("the response should be ok and the the uptime var should be present") {
                zacClient.performGetRequest(
                    url = "${ZAC_MANAGEMENT_URI}/metrics"
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
