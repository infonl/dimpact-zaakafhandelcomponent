/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * This test tests the productaanvraag flow in ZAC which starts with a received productaanvraag notification.
 */
class NotificationTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    given("A fake notifications payload without authentication header") {
        `when`("the notificaties endpoint is called") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/notificaties",
                headers = Headers.headersOf("Content-Type", "application/json"),
                requestBodyAsString = JSONObject(
                    mapOf(
                        "fake" to "fake"
                    )
                ).toString()
            )
            then("the response should be forbidden") {
                response.code shouldBe HTTP_FORBIDDEN
            }
        }
    }

    given(
        "An invalid notifications payload"
    ) {
        `when`(
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

            then("the response should be 'no content'") {
                response.code shouldBe HTTP_NO_CONTENT
            }
        }
    }
})
