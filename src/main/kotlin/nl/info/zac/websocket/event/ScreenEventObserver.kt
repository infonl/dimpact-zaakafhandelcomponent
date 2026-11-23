/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.websocket.event

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.ObservesAsync
import jakarta.inject.Inject
import jakarta.inject.Named
import nl.info.zac.event.AbstractEventObserver
import nl.info.zac.log.GenericEvent
import nl.info.zac.log.ZacLogger
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.websocket.SessionRegistry
import nl.info.zac.websocket.log.ScreenEventReceived

/**
 * This bean listens for [ScreenEvent], converts them to a Websockets event and then forwards it to the browsers that have subscribed
 * to it.
 */
@Named
@ApplicationScoped
@NoArgConstructor
@AllOpen
class ScreenEventObserver @Inject constructor(
    private val sessionRegistry: SessionRegistry
) : AbstractEventObserver<ScreenEvent>() {
    companion object {
        private val LOG = ZacLogger.getLogger(ScreenEventObserver::class)
        private val JSON_MAPPER = ObjectMapper()
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onFire(@ObservesAsync event: ScreenEvent) {
        try {
            // Not the object ID: for some screen events, such as SIGNALERINGEN, it is a username
            LOG.fine { ScreenEventReceived(opcode = event.opcode.toString(), screenEventType = event.objectType.toString()) }
            sendToWebsocketSubscribers(event)
        } catch (throwable: Throwable) {
            LOG.warning(GenericEvent(type = GenericEvent.Type.SCREEN_EVENT_HANDLING_FAILED), throwable)
        }
    }

    private fun sendToWebsocketSubscribers(event: ScreenEvent) {
        try {
            val subscribers = sessionRegistry.listSessions(event)
            if (subscribers.isNotEmpty()) {
                val json = JSON_MAPPER.writeValueAsString(event)
                subscribers.forEach { it.asyncRemote.sendText(json) }
            }
        } catch (jsonProcessingException: JsonProcessingException) {
            LOG.warning(GenericEvent(type = GenericEvent.Type.SCREEN_EVENT_JSON_CONVERSION_FAILED), jsonProcessingException)
        }
    }
}
