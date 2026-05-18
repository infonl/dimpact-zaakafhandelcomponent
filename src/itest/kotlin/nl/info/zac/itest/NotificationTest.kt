/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.dockerComposeContainer
import okhttp3.Headers
import org.json.JSONObject
import org.testcontainers.containers.wait.strategy.Wait
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * This test tests the productaanvraag flow in ZAC which starts with a received productaanvraag notification.
 */
class NotificationTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    Given("A fake notifications payload without authentication header") {
        When("the notificaties endpoint is called") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/notificaties",
                headers = Headers.headersOf("Content-Type", "application/json"),
                requestBodyAsString = JSONObject(
                    mapOf(
                        "fake" to "fake"
                    )
                ).toString()
            )
            Then("the response should be forbidden") {
                response.code shouldBe HTTP_FORBIDDEN
            }
        }
    }

    Given(
        "An invalid notifications payload"
    ) {
        When(
            """the notificaties endpoint is called with a 'create zaaktype' payload with a 
                    fake resourceUrl that does not start with the 'ZGW_API_CLIENT_MP_REST_URL' environment variable"""
        ) {
            val response = itestHttpClient.performJSONPostRequest(
                url = "${ZAC_API_URI}/notificaties",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json",
                    // this test simulates that Open Notificaties sends the request to ZAC
                    // using the secret API key that is configured in ZAC
                    "Authorization",
                    OPEN_NOTIFICATIONS_API_SECRET_KEY
                ),
                requestBodyAsString = JSONObject(
                    mapOf(
                        "kanaal" to "zaaktypen",
                        "resource" to "zaaktype",
                        "resourceUrl" to "https://example.com/fakeResourceUrl",
                        "hoofdObject" to "https://example.com/fakeResourceUrl",
                        "actie" to "create",
                        "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                    )
                ).toString()
            )

            Then(
                """the response should be 'no content' and a corresponding error message should be logged in ZAC"""
            ) {
                response.code shouldBe HTTP_NO_CONTENT

                // we expect ZAC to log an error message indicating that the resourceURL is invalid
                dockerComposeContainer.waitingFor(
                    "zac",
                    Wait.forLogMessage(
                        ".* Failed to handle notification 'null ZAAKTYPE CREATE' .*" +
                            "java.lang.RuntimeException: URI 'http://example.com/fakeResourceUrl' does not " +
                            "start with value for environment variable 'ZGW_API_CLIENT_MP_REST_URL': '$OPEN_ZAAK_BASE_URI/' .*",
                        1
                    ).withStartupTimeout(30.seconds.toJavaDuration())
                )
            }
        }
    }
})
