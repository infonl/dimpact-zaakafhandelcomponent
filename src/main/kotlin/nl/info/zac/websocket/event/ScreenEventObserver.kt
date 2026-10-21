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
import nl.info.zac.websocket.SessionRegistry
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.logging.Level
import java.util.logging.Logger

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
        private val LOG = Logger.getLogger(ScreenEventObserver::class.java.name)
        private val JSON_MAPPER = ObjectMapper()
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onFire(@ObservesAsync event: ScreenEvent) {
        try {
            LOG.fine { "Received screen event: $event" }
            sendToWebsocketSubscribers(event)
        } catch (throwable: Throwable) {
            LOG.log(Level.WARNING, "asynchronous guard", throwable)
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
            LOG.log(Level.WARNING, "Failed to convert the ScreenUpdateEvent to JSON.", jsonProcessingException)
        }
    }
}
