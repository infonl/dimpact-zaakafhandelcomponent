/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.provided.ProjectConfig

private val logger = KotlinLogging.logger {}

class AppContainerTest : BehaviorSpec({
    given("ZAC Docker container and all related Docker containers are running") {
        When("the health endpoint is called") {
            then("the response should be ok and the status should be UP") {
                khttp.get(
                    url = "${ProjectConfig.zacContainer.managementUrl}/health/ready"
                ).apply {
                    logger.info { "response: $this" }
                    // TODO: http status is 503 and status is DOWN at the moment
                    statusCode shouldBe HttpStatus.SC_OK
                    this.jsonObject.getString("status") shouldBe "UP"
                }
            }
        }
    }
})
