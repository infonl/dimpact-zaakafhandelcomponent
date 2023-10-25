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
import nl.info.zac.itest.config.ZACContainer
import org.json.JSONObject
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

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
                    statusCode shouldBe HttpStatus.SC_FORBIDDEN
                }
            }
        }
    }
    given("ZAC Docker container and all related Docker containers are running") {
        When("the notificaties endpoint is called with 'create zaak' payload with authentication header") {
            then(
                "the response should be 'no content', a zaak should be created in OpenZaak " +
                    "and a zaak productaanvraag proces of type 'Productaanvraag-Denhaag' should be started in ZAC"
            ) {
                khttp.post(
                    url = "${ProjectConfig.zacContainer.apiUrl}/notificaties",
                    headers = mapOf(
                        "Content-Type" to "application/json",
                        "Authorization" to ZACContainer.OPEN_NOTIFICATIONS_API_SECRET_KEY
                    ),
                    data = JSONObject(
                        mapOf(
                            // "kanaal" to "zaak",
                            "resource" to "object",
                            "actie" to "create",
                            "kenmerken" to mapOf("objectType" to "http://objecten-api/${UUID.randomUUID()}"),
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                        )
                    )
                ).apply {
                    logger.info { "response: $this" }
                    statusCode shouldBe HttpStatus.SC_NO_CONTENT
                }
            }
        }
    }
})
