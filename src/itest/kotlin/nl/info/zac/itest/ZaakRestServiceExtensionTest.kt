/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK

/**
 * Tests extending a zaak.
 */
@Suppress("MagicNumber")
class ZaakRestServiceExtensionTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val logger = KotlinLogging.logger {}

    Given(
        """A zaak exists which has not been extended yet and a behandelaar is logged in"""
    ) {
        authenticate(BEHANDELAAR_DOMAIN_TEST_1)
        lateinit var zaakUuid: String
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_TEST_2_UUID,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2000_01_01
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).run {
                zaakUuid = getString("uuid")
            }
        }
        When("the zaak is extended") {
            val daysExtended = 3
            val reason = "fakeReason"
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/verlenging",
                requestBodyAsString = """
                    {
                        "redenVerlenging": "$reason",
                        "duurDagen": $daysExtended,
                        "takenVerlengen": false
                    }
                """.trimIndent()
            )
            Then(
                """the response should be OK and the returned zaak should be extended"""
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("redenVerlenging", reason)
                    shouldContainJsonKeyValue("duurVerlenging", "$daysExtended dagen")
                }
            }
        }
    }
})
