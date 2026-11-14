/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.util

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

private val logger = KotlinLogging.logger {}

class WebSocketTestListener(private val textToBeSentOnOpen: String?) : WebSocketListener() {
    companion object {
        // as defined by the official websocket specification
        private const val NORMAL_CLOSURE_STATUS = 1000
    }
    var messagesReceived = mutableListOf<String>()

    override fun onOpen(webSocket: WebSocket, response: Response) {
        textToBeSentOnOpen?.let {
            webSocket.send(it)
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        logger.info { "Receiving websocket text message: '$text'" }
        messagesReceived.add(text)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        logger.info { "Receiving websocket bytestring message: '${bytes.hex()}'" }
        messagesReceived.add(bytes.hex())
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        logger.info { "Closing websocket with code: '$code' and reason: '$reason'" }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        logger.info { "Failure on websocket: $t ${response?.let { ", response: '$response'" }}" }
    }
}
