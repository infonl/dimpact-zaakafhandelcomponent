/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.provided.ProjectConfig
import org.json.JSONObject

private val logger = KotlinLogging.logger {}

class NotificationsTest : BehaviorSpec({
    given("ZAC Docker container and all related Docker containers are running") {
        When("the notificaties endpoint is called with dummy payload without authentication header") {
            then("the response should be forbidden") {
                khttp.post(
                    url = "${ProjectConfig.zacContainer.apiUrl}/notificaties",
                    headers = mapOf("Content-Type" to "application/json"),
                    data = JSONObject(
                        mapOf(
                            "dummy" to "dummy"
                        )
                    )
                ).apply {
                    logger.info { "response: $this" }
                    statusCode shouldBe HttpStatus.SC_FORBIDDEN
                }
            }
        }
    }
})
