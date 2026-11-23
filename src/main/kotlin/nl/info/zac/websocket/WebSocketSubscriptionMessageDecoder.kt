/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.websocket

import jakarta.json.Json
import jakarta.json.JsonException
import jakarta.websocket.DecodeException
import jakarta.websocket.Decoder
import jakarta.websocket.EndpointConfig
import nl.info.zac.event.Opcode
import nl.info.zac.log.GenericEvent
import nl.info.zac.log.ZacLogger
import nl.info.zac.websocket.SubscriptionType.DELETE_ALL
import nl.info.zac.websocket.SubscriptionType.SubscriptionMessage
import nl.info.zac.websocket.event.ScreenEvent
import nl.info.zac.websocket.event.ScreenEventId
import nl.info.zac.websocket.event.ScreenEventType
import java.io.StringReader

/**
 * Converts websocket messages to SubscriptionType.SubscriptionMessage objects
 */
class WebSocketSubscriptionMessageDecoder : Decoder.Text<SubscriptionMessage> {
    companion object {
        private val LOG = ZacLogger.getLogger(WebSocketSubscriptionMessageDecoder::class)

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
            LOG.warning(GenericEvent(type = GenericEvent.Type.SUBSCRIPTION_MESSAGE_UNDECODABLE), jsonException)
            false
        }
}
