/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_WEBSOCKET_BASE_URI
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

private val itestHttpClient = ItestHttpClient()

private val logger = KotlinLogging.logger {}

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class WebSocketServerTest : BehaviorSpec({
    Given("A zaak has been created") {
        When("TODO..") {
            itestHttpClient.connectNewWebSocket(
                url = ZAC_WEBSOCKET_BASE_URI,
                webSocketListener = EchoWebSocketListener()
            )
            // shutdown client to close websocket
            itestHttpClient.shutdownClient()
            Then("TODO..") {
                // TODO: check if listener works
                // force backend to sent a websocket event to our listener somehow
                // by updating a zaak maybe?
            }
        }
    }
})

class EchoWebSocketListener : WebSocketListener() {
    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        val websocketCreateSubscriptionForZaakAny = "{" +
            "\"subscriptionType\":\"CREATE\"," +
            "\"event\":{" +
            "  \"opcode\":\"ANY\"," +
            "  \"objectType\":\"ZAAK\"," +
            "  \"objectId\":{" +
            "    \"resource\":\"$zaak1UUID\"" +
            "  }," +
            "\"_key\":\"ANY;ZAAK;zaak1UUID\"" +
            "}" +
            "}"
        webSocket.send(websocketCreateSubscriptionForZaakAny)
        webSocket.close(NORMAL_CLOSURE_STATUS, "dummyClosingReason")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        logger.info { "Receiving: '$text'" }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        logger.info { "Receiving: '${bytes.hex()}'" }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        logger.info { "Closing websocket with code: '$code' and reason: '$reason'" }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        logger.info { "Failure on websocket: $t ${response?.let { ", response: '$response'" }}" }
    }
}
