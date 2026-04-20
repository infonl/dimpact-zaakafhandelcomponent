/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldNotContainJsonKey
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK

/**
 * Tests suspending and resuming a zaak.
 */
@Suppress("MagicNumber")
class ZaakSuspendRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val logger = KotlinLogging.logger {}

    Given("A zaak exists and a behandelaar is logged in") {
        lateinit var zaakUuid: String
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_TEST_2_UUID,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).run {
                zaakUuid = getString("uuid")
            }
        }

        When("the zaak is suspended") {
            val suspensionDays = 5
            val suspensionReason = "fakeSuspensionReason"
            val suspendResponse = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/suspend",
                requestBodyAsString = """
                    {
                        "reason": "$suspensionReason",
                        "numberOfDays": $suspensionDays
                    }
                """.trimIndent(),
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )
            Then("the response should be OK and the returned zaak should indicate suspension") {
                val responseBody = suspendResponse.bodyAsString
                logger.info { "Response: $responseBody" }
                suspendResponse.code shouldBe HTTP_OK
                responseBody.shouldContainJsonKeyValue("redenOpschorting", suspensionReason)
                responseBody.shouldContainJsonKeyValue("isOpgeschort", true)
            }

            When("the suspension details of the zaak are read") {
                val readResponse = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/opschorting",
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )
                Then("the response should be OK and return the suspension details") {
                    val responseBody = readResponse.bodyAsString
                    logger.info { "Response: $responseBody" }
                    readResponse.code shouldBe HTTP_OK
                    responseBody.shouldContainJsonKey("vanafDatumTijd")
                    responseBody.shouldContainJsonKeyValue("duurDagen", suspensionDays)
                }
            }

            When("the zaak is resumed") {
                val resumeReason = "fakeResumeReason"
                val resumeResponse = itestHttpClient.performPatchRequest(
                    url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/resume",
                    requestBodyAsString = """
                        {
                            "reason": "$resumeReason"
                        }
                    """.trimIndent(),
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )
                Then("the response should be OK and the returned zaak should not indicate suspension") {
                    val responseBody = resumeResponse.bodyAsString
                    logger.info { "Response: $responseBody" }
                    resumeResponse.code shouldBe HTTP_OK
                    responseBody.shouldContainJsonKeyValue("isOpgeschort", false)
                    responseBody.shouldNotContainJsonKey("redenOpschorting")
                }

                When("the suspension details of the resumed zaak are read") {
                    val readAfterResumeResponse = itestHttpClient.performGetRequest(
                        url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/opschorting",
                        testUser = BEHANDELAAR_DOMAIN_TEST_1
                    )
                    Then("the response should be OK and return no suspension date and zero days") {
                        val responseBody = readAfterResumeResponse.bodyAsString
                        logger.info { "Response: $responseBody" }
                        readAfterResumeResponse.code shouldBe HTTP_OK
                        responseBody.shouldNotContainJsonKey("vanafDatumTijd")
                        responseBody.shouldContainJsonKeyValue("duurDagen", 0)
                    }
                }
            }
        }
    }
})
