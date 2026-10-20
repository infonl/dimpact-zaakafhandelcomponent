/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.inspectors.forAtLeastOne
import io.kotest.inspectors.forNone
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.SCREEN_EVENT_TYPE_ZAAK_ROLLEN
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_WEBSOCKET_BASE_URI
import nl.info.zac.itest.config.RAADPLEGER_1
import nl.info.zac.itest.util.WebSocketTestListener
import okhttp3.Headers
import okhttp3.WebSocket
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private const val SCREEN_EVENT_TYPE_ZAAK = "ZAAK"

class WebSocketServerEndPointTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val testStartEpochSecond = Instant.now().epochSecond
    lateinit var zaakUuid: UUID

    fun subscriptionMessage(subscriptionType: String, objectType: String, resource: UUID) = """
        {
            "subscriptionType": "$subscriptionType",
            "event": {
                "opcode": "ANY",
                "objectType": "$objectType",
                "objectId": { "resource": "$resource" },
                "_key": "ANY;$objectType;$resource"
            }
        }
    """.trimIndent()

    fun connectWebSocket(webSocketTestListener: WebSocketTestListener): WebSocket =
        itestHttpClient.connectNewWebSocket(
            url = ZAC_WEBSOCKET_BASE_URI,
            webSocketListener = webSocketTestListener,
            testUser = RAADPLEGER_1
        )

    fun sendNotification(resource: String, resourceUrl: String) {
        itestHttpClient.performJSONPostRequest(
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
                    "resource" to resource,
                    "resourceUrl" to resourceUrl,
                    "hoofdObject" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakUuid",
                    "actie" to "partial_update",
                    "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                    "kenmerken" to mapOf(
                        "zaaktype" to "$OPEN_ZAAK_BASE_URI/catalogi/api/v1/zaaktypen/$ZAAKTYPE_CMMN_TEST_3_UUID",
                        "bronorganisatie" to "123443210",
                        "vertrouwelijkheidaanduiding" to "openbaar"
                    )
                )
            ).toString()
        ).code shouldBe HTTP_NO_CONTENT
    }

    fun sendZaakNotification() = sendNotification("zaak", "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakUuid")

    fun sendRolNotification() =
        sendNotification("rol", "$OPEN_ZAAK_BASE_URI/zaken/api/v1/rollen/${UUID.randomUUID()}")

    fun List<String>.containsEventFor(objectType: String) =
        any { JSONObject(it).getString("objectType") == objectType }

    suspend fun awaitZaakEvent(webSocketTestListener: WebSocketTestListener) =
        eventually(30.seconds) {
            sendZaakNotification()
            webSocketTestListener.messagesReceived.containsEventFor(SCREEN_EVENT_TYPE_ZAAK) shouldBe true
        }

    suspend fun awaitRolEvent(webSocketTestListener: WebSocketTestListener) =
        eventually(30.seconds) {
            sendRolNotification()
            webSocketTestListener.messagesReceived.containsEventFor(SCREEN_EVENT_TYPE_ZAAK_ROLLEN) shouldBe true
        }

    beforeSpec {
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_CMMN_TEST_3_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_1
        ).run {
            code shouldBe HTTP_OK
            zaakUuid = JSONObject(bodyAsString).getString("uuid").let(UUID::fromString)
        }
    }

    context("Receiving a screen event on a websocket") {
        given("a websocket that subscribes to all changes of a zaak") {
            val webSocketTestListener = WebSocketTestListener(
                textToBeSentOnOpen = subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ZAAK, zaakUuid)
            )
            connectWebSocket(webSocketTestListener)

            `when`("a notification is sent to ZAC that the zaak has been updated") {
                eventually(30.seconds) {
                    sendZaakNotification()
                    webSocketTestListener.messagesReceived.containsEventFor(SCREEN_EVENT_TYPE_ZAAK) shouldBe true
                }

                then("the websocket receives the complete screen event, with the id of the zaak and without a detail") {
                    webSocketTestListener.messagesReceived.forAtLeastOne {
                        with(JSONObject(it)) {
                            keySet() shouldBe setOf("timestamp", "opcode", "objectType", "objectId", "actorUserId")
                            getLong("timestamp") shouldBeGreaterThanOrEqual testStartEpochSecond
                            getString("opcode") shouldBe "UPDATED"
                            getString("objectType") shouldBe SCREEN_EVENT_TYPE_ZAAK
                            getJSONObject("objectId").getString("resource") shouldBe zaakUuid.toString()
                            getJSONObject("objectId").isNull("detail") shouldBe true
                            getString("actorUserId").shouldNotBeBlank()
                        }
                    }
                }
            }
        }
    }

    context("Cancelling a websocket subscription") {
        given("a websocket that subscribes to all changes of a zaak and then cancels that subscription") {
            val webSocketTestListener = WebSocketTestListener(
                textToBeSentOnOpen = subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ZAAK, zaakUuid)
            )
            val webSocket = connectWebSocket(webSocketTestListener)
            awaitZaakEvent(webSocketTestListener)
            webSocket.send(subscriptionMessage("DELETE", SCREEN_EVENT_TYPE_ZAAK, zaakUuid))
            webSocket.send(subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ZAAK_ROLLEN, zaakUuid))
            awaitRolEvent(webSocketTestListener)
            webSocketTestListener.messagesReceived.clear()

            `when`("notifications are sent to ZAC that the zaak and one of its rollen have been updated") {
                eventually(30.seconds) {
                    sendZaakNotification()
                    sendRolNotification()
                    webSocketTestListener.messagesReceived.containsEventFor(SCREEN_EVENT_TYPE_ZAAK_ROLLEN) shouldBe true
                }

                then("the websocket only receives events for the rollen and never for the zaak") {
                    continually(5.seconds) {
                        webSocketTestListener.messagesReceived.forNone {
                            JSONObject(it).getString("objectType") shouldBe SCREEN_EVENT_TYPE_ZAAK
                        }
                    }
                }
            }
        }

        given(
            "a websocket that subscribes to all changes of a zaak and then cancels all its subscriptions, " +
                "and a second websocket that subscribes to all changes of the same zaak"
        ) {
            val webSocketTestListener = WebSocketTestListener(
                textToBeSentOnOpen = subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ZAAK, zaakUuid)
            )
            val webSocket = connectWebSocket(webSocketTestListener)
            awaitZaakEvent(webSocketTestListener)
            webSocket.send("""{ "subscriptionType": "DELETE_ALL" }""")
            webSocket.send(subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ZAAK_ROLLEN, zaakUuid))
            awaitRolEvent(webSocketTestListener)
            webSocketTestListener.messagesReceived.clear()
            val otherWebSocketTestListener = WebSocketTestListener(
                textToBeSentOnOpen = subscriptionMessage("CREATE", SCREEN_EVENT_TYPE_ZAAK, zaakUuid)
            )
            connectWebSocket(otherWebSocketTestListener)

            `when`("a notification is sent to ZAC that the zaak has been updated") {
                eventually(30.seconds) {
                    sendZaakNotification()
                    otherWebSocketTestListener.messagesReceived.containsEventFor(SCREEN_EVENT_TYPE_ZAAK) shouldBe true
                }

                then("only the second websocket receives the event for the zaak") {
                    continually(5.seconds) {
                        webSocketTestListener.messagesReceived.forNone {
                            JSONObject(it).getString("objectType") shouldBe SCREEN_EVENT_TYPE_ZAAK
                        }
                    }
                }
            }
        }
    }
})
