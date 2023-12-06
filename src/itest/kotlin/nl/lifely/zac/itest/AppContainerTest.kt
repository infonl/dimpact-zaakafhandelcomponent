/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_MANAGEMENT_URI

class AppContainerTest : BehaviorSpec({
    given("ZAC Docker container and all related Docker containers are running") {
        When("the health endpoint is called") {
            then("the response should be ok and the status should be UP") {
                khttp.get(
                    url = "${ZAC_MANAGEMENT_URI}/health/ready"
                ).apply {
                    statusCode shouldBe HttpStatus.SC_OK
                    jsonObject.getString("status") shouldBe "UP"
                }
            }
        }
    }
    given("ZAC Docker container and all related Docker containers are running") {
        When("the metrics endpoint is called") {
            then("the response should be ok and the the uptime var should be present") {
                khttp.get(
                    url = "${ZAC_MANAGEMENT_URI}/metrics"
                ).apply {
                    statusCode shouldBe HttpStatus.SC_OK
                    text.contains("base_jvm_uptime_seconds").shouldNotBeNull()
                }
            }
        }
    }
})
