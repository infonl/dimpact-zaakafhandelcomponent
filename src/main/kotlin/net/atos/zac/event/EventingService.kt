/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.event

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Event
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import net.atos.zac.signalering.event.SignaleringEvent
import net.atos.zac.signalering.event.SignaleringEventUtil
import net.atos.zac.util.event.JobEvent
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@ApplicationScoped
@AllOpen
@NoArgConstructor
class EventingService @Inject constructor(
    private val screenUpdateEvent: Event<ScreenEvent>,
    private val signaleringEvent: Event<SignaleringEvent<*>>,
    private val signaleringJobEvent: Event<JobEvent>,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    /**
     * Send [ScreenEvent]s to Observer(s), which pass them on to the subscribed websocket clients.
     *
     * The event is stamped with the user that caused it before it is fired, while the user session is
     * still in scope: the observer runs on another thread, where it no longer is.
     *
     * Prefer using the factory methods on [ScreenEventType] to create these events.
     */
    fun send(event: ScreenEvent) {
        event.actorUserId = loggedInUserInstance.get().id
        screenUpdateEvent.fireAsync(event)
    }

    /**
     * Send [SignaleringEvent]s to Observer(s), which use them to create and/or send signaleringen.
     *
     * Prefer using the factory methods on [SignaleringEventUtil] to create these events.
     */
    fun send(event: SignaleringEvent<*>) {
        signaleringEvent.fireAsync(event)
    }

    /**
     * Send [JobEvent]s to Observer(s), which use them to start a background job of the correct type.
     */
    fun send(event: JobEvent) {
        signaleringJobEvent.fireAsync(event)
    }
}
