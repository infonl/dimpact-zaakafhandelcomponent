/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid

/**
 * Integration test to test the functionality of extending a zaak.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
@Suppress("MagicNumber")
class ZaakRestServiceExtensionTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}

    Given(
        """A zaak exists which has not been extended yet"""
    ) {

        When("the zaak is extended") {
            val daysExtended = 3
            val reason = "dummyReason"
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakProductaanvraag1Uuid/verlenging",
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
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("redenVerlenging", reason)
                    shouldContainJsonKeyValue("duurVerlenging", "$daysExtended dagen")
                }
            }
        }
    }
})
