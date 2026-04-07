/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.config.ItestConfiguration.ZAC_CONTAINER_SERVICE_NAME
import nl.info.zac.itest.config.dockerComposeContainer
import org.json.JSONObject

const val NUMBER_OF_LOG_LINES = 10

class LoggingTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}

    Given("ZAC Docker container is running") {
        When("the ZAC container log output is retrieved") {
            val zacContainer = dockerComposeContainer
                .getContainerByServiceName(ZAC_CONTAINER_SERVICE_NAME)
                .get()
            val jsonLogLines = zacContainer.logs
                .lines()
                .filter { it.trimStart().startsWith("{") }
            logger.info { "Found ${jsonLogLines.size} JSON log lines in ZAC container output" }

            Then("log lines should be structured as valid JSON with required fields including service metadata") {
                jsonLogLines.isNotEmpty() shouldBe true

                jsonLogLines.take(NUMBER_OF_LOG_LINES).forEach { line ->
                    val logEntry = JSONObject(line)
                    logEntry.has("timestamp") shouldBe true
                    logEntry.has("level") shouldBe true
                    logEntry.has("message") shouldBe true
                    logEntry.has("loggerName") shouldBe true
                    logEntry.getString("service") shouldBe "zac"
                }
            }
        }
    }
})
