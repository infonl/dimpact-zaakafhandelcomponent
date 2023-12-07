/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.KeycloakClient
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject

private val logger = KotlinLogging.logger {}

class ZakenRESTServiceTest : BehaviorSpec({
    given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the create zaak endpoint is called with a group name that is longer than 24 characters") {
            then("the response should be a 400 invalid request with a clear error message") {
                khttp.post(
                    url = "${ZAC_API_URI}/zaken/zaak",
                    headers = mapOf("Authorization" to "Bearer ${KeycloakClient.requestAccessToken()}"),
                    data = JSONObject(
                        mapOf(
                            "zaakUUID" to zaakUUID
                        )
                    )
                ).apply {
                    logger.info { "Create zaak with group name that is too long response: $text" }
                    this.statusCode shouldBe 400
                }
            }
        }
    }
})
