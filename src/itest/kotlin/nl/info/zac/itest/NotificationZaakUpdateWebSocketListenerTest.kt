/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.inspectors.forAtLeastOne
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.SCREEN_EVENT_TYPE_ZAAK_ROLLEN
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_DOMAIN_TEST_1
import nl.info.zac.itest.util.WebSocketTestListener
import okhttp3.Headers
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * This test tests the handling of zaak-update notifications, which should result in messages being sent to a
 * websocket listener.
 */
@Suppress("LargeClass")
class NotificationZaakUpdateWebSocketListenerTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    lateinit var zaakProductaanvraag1Uuid: UUID
    lateinit var zaakProductaanvraag1Betrokkene1Uuid: UUID

    Given("""A websocket subscription is created to listen to all changes made to a specific zaak""") {
        val websocketListener = WebSocketTestListener(
            textToBeSentOnOpen = "{" +
                "\"subscriptionType\":\"CREATE\"," +
                "\"event\":{" +
                "  \"opcode\":\"ANY\"," +
                "  \"objectType\":\"ZAAK\"," +
                "  \"objectId\":{" +
                "    \"resource\":\"$zaakProductaanvraag1Uuid\"" +
                "  }," +
                "\"_key\":\"ANY;ZAAK;$zaakProductaanvraag1Uuid\"" +
                "}" +
                "}"
        )
        itestHttpClient.connectNewWebSocket(
            url = ItestConfiguration.ZAC_WEBSOCKET_BASE_URI,
            webSocketListener = websocketListener,
            testUser = RAADPLEGER_DOMAIN_TEST_1
        )
        When("""a notification is sent to ZAC that the zaak in question has been updated""") {
            // wait a bit because it takes some time before the new websocket has been successfully created in ZAC
            eventually(30.seconds) {
                val response = itestHttpClient.performJSONPostRequest(
                    url = "$ZAC_API_URI/notificaties",
                    headers = Headers.headersOf(
                        "Content-Type",
                        "application/json",
                        "Authorization",
                        OPEN_NOTIFICATIONS_API_SECRET_KEY
                    ),
                    requestBodyAsString = JSONObject(
                        mapOf(
                            "kanaal" to "zaken",
                            "resource" to "zaak",
                            "resourceUrl" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakProductaanvraag1Uuid",
                            "hoofdObject" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakProductaanvraag1Uuid",
                            "actie" to "partial_update",
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                            "kenmerken" to mapOf(
                                "zaaktype" to "$OPEN_ZAAK_BASE_URI/catalogi/api/v1/zaaktypen/$ZAAKTYPE_TEST_3_UUID",
                                "bronorganisatie" to "123443210",
                                "vertrouwelijkheidaanduiding" to "openbaar"
                            )
                        )
                    ).toString()
                )
                response.code shouldBe HTTP_NO_CONTENT
                // because of the retries using eventually, we can end up with duplicate messages. that's ok.
                websocketListener.messagesReceived.size shouldBeGreaterThan 0
            }
        }
        Then(
            """the response should be 'no content' and an event that the zaak has been updated should be sent to the websocket"""
        ) {
            websocketListener.messagesReceived.forAtLeastOne {
                with(JSONObject(it)) {
                    getString("opcode") shouldBe "UPDATED"
                    getString("objectType") shouldBe "ZAAK"
                    getJSONObject("objectId").getString("resource") shouldBe zaakProductaanvraag1Uuid.toString()
                }
            }
        }
    }

    Given("""A websocket subscription is created to listen to all changes made to zaak-rollen""") {
        val websocketListener = WebSocketTestListener(
            textToBeSentOnOpen = """
                {
                    "subscriptionType": "CREATE",
                    "event": {
                        "opcode": "ANY",
                        "objectType": "$SCREEN_EVENT_TYPE_ZAAK_ROLLEN",
                         "objectId": {
                            "resource": "$zaakProductaanvraag1Uuid"
                         },
                        "_key": "ANY;$SCREEN_EVENT_TYPE_ZAAK_ROLLEN;$zaakProductaanvraag1Uuid"
                    }
                }
            """.trimIndent()
        )
        itestHttpClient.connectNewWebSocket(
            url = ItestConfiguration.ZAC_WEBSOCKET_BASE_URI,
            webSocketListener = websocketListener,
            testUser = RAADPLEGER_DOMAIN_TEST_1
        )
        When("""a notification is sent to ZAC that a zaak-rol has been created""") {
            // we need eventually here because it takes some time before the new websocket has been
            // successfully created in ZAC
            eventually(30.seconds) {
                val response = itestHttpClient.performJSONPostRequest(
                    url = "$ZAC_API_URI/notificaties",
                    headers = Headers.headersOf(
                        "Content-Type",
                        "application/json",
                        "Authorization",
                        OPEN_NOTIFICATIONS_API_SECRET_KEY
                    ),
                    requestBodyAsString = JSONObject(
                        mapOf(
                            "kanaal" to "zaken",
                            "resource" to "rol",
                            "resourceUrl" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/rollen/$zaakProductaanvraag1Betrokkene1Uuid",
                            "hoofdObject" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakProductaanvraag1Uuid",
                            "actie" to "create",
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                            "kenmerken" to mapOf(
                                "zaaktype" to "$OPEN_ZAAK_BASE_URI/catalogi/api/v1/zaaktypen/$ZAAKTYPE_TEST_3_UUID",
                                "bronorganisatie" to "123443210",
                                "vertrouwelijkheidaanduiding" to "openbaar"
                            )
                        )
                    ).toString()
                )
                response.code shouldBe HTTP_NO_CONTENT
                // because of the retries using eventually, we can end up with duplicate messages. that's ok.
                websocketListener.messagesReceived.size shouldBeGreaterThan 0
            }
        }
        Then(
            """the response should be 'no content' and an event that the zaak-rol
                has been updated should be sent to the websocket"""
        ) {
            websocketListener.messagesReceived.forAtLeastOne {
                with(JSONObject(it)) {
                    getString("opcode") shouldBe "UPDATED"
                    getString("objectType") shouldBe "ZAAK_ROLLEN"
                }
            }
        }
    }
})
