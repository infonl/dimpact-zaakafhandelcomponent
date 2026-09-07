/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimaps
import com.google.common.collect.SetMultimap
import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.Session
import net.atos.zac.event.Opcode
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventId
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * This Registry is used to maintain a list of active sessions.
 * EventSessions contains all (from the browser) registered client sessions
 */
@ApplicationScoped
@AllOpen
@NoArgConstructor
class SessionRegistry {
    companion object {
        private val QUOTED: Pattern = Pattern.compile("^\"(.*)\"$")
    }

    private val eventSessions: SetMultimap<ScreenEvent, Session> =
        Multimaps.synchronizedSetMultimap(HashMultimap.create())

    // Tracks every open session regardless of its event subscriptions, so the heartbeat can reach all of them,
    // including a session that has not (yet) subscribed to any event.
    private val allSessions: MutableSet<Session> = Collections.newSetFromMap(ConcurrentHashMap())

    /**
     * Return a set of all active sessions for a particular event.
     */
    fun listSessions(event: ScreenEvent): Set<Session> = Collections.unmodifiableSet(eventSessions.get(fix(event)))

    /**
     * Add a session for a specific event.
     */
    fun create(wildcarded: ScreenEvent, session: Session?) {
        if (session != null) {
            glob(fix(wildcarded)).forEach { event -> eventSessions.put(event, session) }
        }
    }

    /**
     * Delete a session for a specific event.
     */
    fun delete(wildcarded: ScreenEvent, session: Session?) {
        if (session != null) {
            glob(fix(wildcarded)).forEach { event -> eventSessions.get(event).remove(session) }
        }
    }

    /**
     * Delete a session for all events.
     */
    fun deleteAll(session: Session?) {
        if (session != null) {
            eventSessions.values().removeAll(setOf(session))
        }
    }

    /**
     * Registers a newly opened session so that it can receive heartbeats, regardless of which events
     * it ends up subscribing to.
     */
    fun addSession(session: Session) {
        allSessions.add(session)
    }

    /**
     * Removes a session so that it no longer receives heartbeats.
     */
    fun removeSession(session: Session) {
        allSessions.remove(session)
    }

    /**
     * Return the set of all currently open sessions, regardless of their event subscriptions.
     */
    fun listAllSessions(): Set<Session> = Collections.unmodifiableSet(allSessions)

    private fun glob(event: ScreenEvent): List<ScreenEvent> {
        if (event.opcode == Opcode.ANY) {
            val anyOpcode = Opcode.any().toMutableSet()
            // There will not be any websocket subscriptions with this opcode, so skip it in globbing.
            anyOpcode.remove(Opcode.CREATED)
            if (event.objectType == ScreenEventType.ANY) {
                return anyOpcode.flatMap { operation ->
                    ScreenEventType.any().map { objectType -> ScreenEvent(operation, objectType, event.objectId) }
                }
            }
            return anyOpcode.map { operation -> ScreenEvent(operation, event.objectType, event.objectId) }
        }
        if (event.objectType == ScreenEventType.ANY) {
            return ScreenEventType.any().map { objectType -> ScreenEvent(event.opcode, objectType, event.objectId) }
        }
        return listOf(event)
    }

    /**
     * This method is applied to all event arguments to make sure that the objectId being quoted (by Angular?) doesn't cause any problems.
     * Events that are otherwise equal except for the quoted/unquoted objectIds should in all cases be regarded as the same event.
     */
    fun fix(event: ScreenEvent): ScreenEvent {
        val resource = fix(event.objectId.resource())
        val detail = fix(event.objectId.detail())
        return if (resource != null || detail != null) {
            ScreenEvent(event.opcode, event.objectType, ScreenEventId(resource, detail))
        } else {
            event
        }
    }

    private fun fix(id: String?): String? {
        if (id == null) {
            return null
        }
        val matcher = QUOTED.matcher(id)
        return if (matcher.matches()) fix(matcher.replaceAll("$1")) else id
    }
}
