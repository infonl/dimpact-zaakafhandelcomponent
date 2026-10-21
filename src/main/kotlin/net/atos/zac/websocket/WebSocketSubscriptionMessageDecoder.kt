/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket

import jakarta.json.Json
import jakarta.json.JsonException
import jakarta.websocket.DecodeException
import jakarta.websocket.Decoder
import jakarta.websocket.EndpointConfig
import net.atos.zac.event.Opcode
import net.atos.zac.websocket.SubscriptionType.DELETE_ALL
import net.atos.zac.websocket.SubscriptionType.SubscriptionMessage
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventId
import net.atos.zac.websocket.event.ScreenEventType
import java.io.StringReader
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Converts websocket messages to SubscriptionType.SubscriptionMessage objects
 */
class WebSocketSubscriptionMessageDecoder : Decoder.Text<SubscriptionMessage> {
    companion object {
        private val LOG = Logger.getLogger(WebSocketSubscriptionMessageDecoder::class.java.name)

        private const val SUBSCRIPTION_TYPE = "subscriptionType"
        private const val EVENT = "event"
        private const val EVENT_OPCODE = "opcode"
        private const val EVENT_TYPE = "objectType"
        private const val EVENT_OBJECT_ID = "objectId"
        private const val EVENT_RESOURCE = "resource"
    }

    @Throws(DecodeException::class)
    override fun decode(jsonMessage: String): SubscriptionMessage =
        Json.createReader(StringReader(jsonMessage)).use { jsonReader ->
            val jsonObject = jsonReader.readObject()
            val subscriptionType = SubscriptionType.valueOf(jsonObject.getString(SUBSCRIPTION_TYPE))
            if (subscriptionType == DELETE_ALL) {
                subscriptionType.message()
            } else {
                val jsonEvent = jsonObject.getJsonObject(EVENT)
                val opcode = Opcode.valueOf(jsonEvent.getString(EVENT_OPCODE))
                val screenEventType = ScreenEventType.valueOf(jsonEvent.getString(EVENT_TYPE))
                val resource = jsonEvent.getJsonObject(EVENT_OBJECT_ID).getString(EVENT_RESOURCE)
                subscriptionType.message(ScreenEvent(opcode, screenEventType, ScreenEventId(resource, null)))
            }
        }

    override fun init(endpointConfig: EndpointConfig) {
        // NOOP
    }

    override fun destroy() {
        // NOOP
    }

    override fun willDecode(jsonMessage: String): Boolean =
        try {
            Json.createReader(StringReader(jsonMessage)).use { it.readObject() }
            true
        } catch (jsonException: JsonException) {
            LOG.log(Level.WARNING, "SubscriptionMessage cannot be decrypted: $jsonMessage", jsonException)
            false
        }
}
