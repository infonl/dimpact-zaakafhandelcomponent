/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket

import jakarta.websocket.Session
import net.atos.zac.websocket.event.ScreenEvent

/**
 * Indicates whether a client subscribes to an event, or whether a client no longer wishes to receive an event (or all events)
 * Maps to subscription-type.ts
 */
enum class SubscriptionType {
    /**
     * Subscribe to an event
     */
    CREATE {
        override fun register(registry: SessionRegistry, session: Session, event: ScreenEvent?) {
            registry.create(event ?: missingEvent(), session)
        }
    },

    /**
     * Cancel subscription to an event
     */
    DELETE {
        override fun register(registry: SessionRegistry, session: Session, event: ScreenEvent?) {
            registry.delete(event ?: missingEvent(), session)
        }
    },

    /**
     * Cancel subscriptions to all events
     */
    DELETE_ALL {
        // 1 instance is enough for this immutable object.
        private val deleteAllMessage = SubscriptionMessage(this, null)

        override fun register(registry: SessionRegistry, session: Session, event: ScreenEvent?) {
            registry.deleteAll(session)
        }

        override fun message(event: ScreenEvent?): SubscriptionMessage {
            require(event == null) { "Unexpected event argument" }
            return message()
        }

        override fun message() = deleteAllMessage
    };

    protected abstract fun register(registry: SessionRegistry, session: Session, event: ScreenEvent?)

    /**
     * Factory method for SubscriptionMessages ([CREATE] and [DELETE] types).
     *
     * @param event the event subscribed to.
     * @return message
     */
    open fun message(event: ScreenEvent?): SubscriptionMessage =
        event?.let { SubscriptionMessage(this, it) } ?: message()

    /**
     * Factory method for SubscriptionMessages (type [DELETE_ALL]).
     *
     * @return message
     */
    open fun message(): SubscriptionMessage = missingEvent()

    protected fun missingEvent(): Nothing = throw IllegalArgumentException("Missing event argument")

    /**
     * Message which, depending on the subscriptionType, indicates to which event the client wishes to subscribe, or which event the client
     * no longer wishes to receive.
     *
     * No public constructor, use the factory methods!
     */
    class SubscriptionMessage internal constructor(
        val subscriptionType: SubscriptionType,
        val event: ScreenEvent?
    ) {
        /**
         * Process this message at one of the sessions in a session registry.
         *
         * @param registry the session registry in which the message must be processed
         * @param session  the session in which the message is to be processed
         */
        fun register(registry: SessionRegistry, session: Session) {
            subscriptionType.register(registry, session, event)
        }
    }
}
