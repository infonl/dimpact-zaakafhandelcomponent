/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid

private const val ROWS_DEFAULT = 10
private const val PAGE_DEFAULT = 0

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
@Suppress("LargeClass")
class ZaakKoppelenRestServiceTest : BehaviorSpec({
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    Given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the searching for linkable cases happy path") {
            val response = zacClient.findLinkableCases(
                zaakProductaanvraag1Uuid,
                "ZAAC_2000345",
                "HOOFDZAAK",
                PAGE_DEFAULT,
                ROWS_DEFAULT
            )
            Then("the response should be a 200 HTTP response") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
            }
        }
    }
})
