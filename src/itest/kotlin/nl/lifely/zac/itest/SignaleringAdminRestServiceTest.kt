/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers

/**
 * This test assumes a human task plan item (=task) has been started for a zaak in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_TASK_CREATED)
class SignaleringAdminRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("A task that is about to expire") {

        When("the admin endpoint to send signaleringen is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/admin/signaleringen/send-signaleringen",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json"
                ),
                // the endpoint is a system / admin endpoint currently not requiring any authentication
                addAuthorizationHeader = false
            )

            Then("the responses should be 'ok'") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody shouldBe "Started sending signaleringen using job: 'Signaleringen verzenden'"
            }
        }
    }
})
