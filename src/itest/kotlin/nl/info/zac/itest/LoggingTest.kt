/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BIRTHDATE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestKindStack
import nl.info.zac.itest.config.RAADPLEGER_1
import nl.info.zac.itest.util.newLogsSince
import nl.info.zac.itest.util.zacContainerLogs
import okhttp3.Headers
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK

const val NUMBER_OF_LOG_LINES = 10
private const val PERSONAL_DATA_MASKING_FILTER = "personalDataMaskingFilter"
private const val MASKED_TEST_PERSON_HENDRIKA_JANSE_BSN = "******896"
private const val BRP_PERSONEN_PATH = "/haalcentraal/api/brp/personen"
private const val ZAC_LOGGER = "nl.info"

@Isolate
class LoggingTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    given("ZAC Docker container is running") {
        `when`("the ZAC container log output is retrieved") {
            val jsonLogLines = zacContainerLogs()
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

    given("ZAC Docker container is running in the WildFly server of the ZAC Docker image") {
        `when`("the configuration of the console log handler and its filter is read") {
            val filterSpec = ItestKindStack.runZacCliCommand(
                "/subsystem=logging/console-handler=CONSOLE:read-attribute(name=filter-spec)"
            )
            val filter = ItestKindStack.runZacCliCommand(
                "/subsystem=logging/filter=$PERSONAL_DATA_MASKING_FILTER:read-resource"
            )

            then("the console log handler masks the personal data with the filter of the ZAC logging filter module") {
                filterSpec.opt("result") shouldBe PERSONAL_DATA_MASKING_FILTER
                filter.getJSONObject("result").run {
                    getString("class") shouldBe "nl.info.zac.logfilter.PersonalDataMaskingLogFilter"
                    getString("module") shouldBe "nl.info.zac.logfilter"
                }
            }
        }
    }

    given(
        "ZAC Docker container is running, a person with a BSN and a date of birth exists in the BRP, " +
            "and all logging of ZAC is at level FINE"
    ) {
        `when`("the person is looked up in the BRP by BSN") {
            val logsBeforeLookup = zacContainerLogs()
            val consoleLogLevel = ItestKindStack.runZacCliCommand(
                "/subsystem=logging/console-handler=CONSOLE:read-attribute(name=level)"
            ).getString("result")
            val response = try {
                ItestKindStack.runZacCliCommand("/subsystem=logging/logger=$ZAC_LOGGER:add(level=FINE)")
                ItestKindStack.runZacCliCommand(
                    "/subsystem=logging/console-handler=CONSOLE:write-attribute(name=level,value=FINE)"
                )
                itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/klanten/personen",
                    headers = Headers.Builder().add("X-ZAAKTYPE-UUID", "$ZAAKTYPE_CMMN_TEST_3_UUID").build(),
                    requestBodyAsString = """{ "bsn": "$TEST_PERSON_HENDRIKA_JANSE_BSN" }""",
                    testUser = RAADPLEGER_1
                )
            } finally {
                ItestKindStack.runZacCliCommand(
                    "/subsystem=logging/console-handler=CONSOLE:write-attribute(name=level,value=$consoleLogLevel)"
                )
                ItestKindStack.runZacCliCommand("/subsystem=logging/logger=$ZAC_LOGGER:remove")
            }
            val logsOfLookup = zacContainerLogs().newLogsSince(logsBeforeLookup)

            then(
                "the logs contain neither the BSN, plain or masked, nor the date of birth nor the path of the BRP call"
            ) {
                response.code shouldBe HTTP_OK
                logsOfLookup shouldNotContain TEST_PERSON_HENDRIKA_JANSE_BSN
                logsOfLookup shouldNotContain MASKED_TEST_PERSON_HENDRIKA_JANSE_BSN
                logsOfLookup shouldNotContain TEST_PERSON_HENDRIKA_JANSE_BIRTHDATE
                logsOfLookup shouldNotContain BRP_PERSONEN_PATH
            }
        }
    }
})
