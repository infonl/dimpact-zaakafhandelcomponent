/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.websocket.Session
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Periodically sends a WebSocket ping frame to every open connection, so that a proxy or load balancer between
 * the browser and this server keeps seeing traffic on an otherwise idle connection and does not close it because
 * of its own idle timeout. Browsers reply to a ping frame with a pong frame automatically, without any
 * involvement of the client-side application code.
 */
@ApplicationScoped
@AllOpen
@NoArgConstructor
class WebSocketHeartbeatScheduler @Inject constructor(
    private val sessionRegistry: SessionRegistry
) {
    companion object {
        private val LOG = Logger.getLogger(WebSocketHeartbeatScheduler::class.java.name)
        private const val HEARTBEAT_INTERVAL_SECONDS = 30L
    }

    private lateinit var executor: ScheduledExecutorService

    // Nothing else in the application injects this bean, so without observing this event CDI would never have
    // a reason to create it and @PostConstruct would never run.
    fun onStartup(@Observes @Initialized(ApplicationScoped::class) @Suppress("UNUSED_PARAMETER") event: Any) {
        LOG.info { "WebSocket heartbeat scheduler starting" }
    }

    @PostConstruct
    fun start() {
        executor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "websocket-heartbeat").apply { isDaemon = true }
        }
        executor.scheduleAtFixedRate(
            ::sendHeartbeats,
            HEARTBEAT_INTERVAL_SECONDS,
            HEARTBEAT_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        )
    }

    @PreDestroy
    fun stop() {
        if (this::executor.isInitialized) {
            executor.shutdownNow()
        }
    }

    internal fun sendHeartbeats() {
        sessionRegistry.listAllSessions().forEach(::sendPing)
    }

    private fun sendPing(session: Session) {
        try {
            if (session.isOpen) {
                session.basicRemote.sendPing(ByteBuffer.allocate(0))
            }
        } catch (ioException: IOException) {
            LOG.log(Level.FINE, "Failed to send WebSocket heartbeat to session ${session.id}", ioException)
        }
    }
}
