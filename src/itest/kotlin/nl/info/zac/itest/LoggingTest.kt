/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BIRTHDATE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_CONTAINER_SERVICE_NAME
import nl.info.zac.itest.config.RAADPLEGER_1
import nl.info.zac.itest.config.dockerComposeContainer
import okhttp3.Headers
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK

const val NUMBER_OF_LOG_LINES = 10

class LoggingTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    given("ZAC Docker container is running") {
        `when`("the ZAC container log output is retrieved") {
            val zacContainer = dockerComposeContainer
                .getContainerByServiceName(ZAC_CONTAINER_SERVICE_NAME)
                .get()
            val jsonLogLines = zacContainer.logs
                .lines()
                .filter { it.trimStart().startsWith("{") }
            logger.info { "Found ${jsonLogLines.size} JSON log lines in ZAC container output" }

            then("log lines should be structured as valid JSON with required fields including service metadata") {
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

    given("ZAC Docker container is running") {
        `when`("a person is looked up in the BRP by BSN and the ZAC container log output is retrieved") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/klanten/personen",
                headers = Headers.Builder().add("X-ZAAKTYPE-UUID", "$ZAAKTYPE_CMMN_TEST_3_UUID").build(),
                requestBodyAsString = """{ "bsn": "$TEST_PERSON_HENDRIKA_JANSE_BSN" }""",
                testUser = RAADPLEGER_1
            )
            val zacContainerLogs = dockerComposeContainer
                .getContainerByServiceName(ZAC_CONTAINER_SERVICE_NAME)
                .get()
                .logs

            then("neither the full BSN nor the date of birth of the person is logged") {
                response.code shouldBe HTTP_OK
                zacContainerLogs shouldNotContain TEST_PERSON_HENDRIKA_JANSE_BSN
                zacContainerLogs shouldNotContain TEST_PERSON_HENDRIKA_JANSE_BIRTHDATE
            }
        }
    }
})
