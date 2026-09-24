/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.event

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.enterprise.event.Event
import jakarta.enterprise.inject.Instance
import net.atos.zac.signalering.event.SignaleringEvent
import net.atos.zac.signalering.event.SignaleringEventId
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.util.event.JobEvent
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.LoggedInUserProvider.Companion.FUNCTIONEEL_GEBRUIKER
import nl.info.zac.authentication.createLoggedInUser
import java.net.URI
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class EventingServiceTest : BehaviorSpec({
    afterEach { checkUnnecessaryStub() }

    val screenUpdateEvent = mockk<Event<ScreenEvent>>()
    val signaleringEvent = mockk<Event<SignaleringEvent<*>>>()
    val signaleringJobEvent = mockk<Event<JobEvent>>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val eventingService = EventingService(
        screenUpdateEvent = screenUpdateEvent,
        signaleringEvent = signaleringEvent,
        signaleringJobEvent = signaleringJobEvent,
        loggedInUserInstance = loggedInUserInstance
    )

    context("Sending a screen event") {
        given("a logged-in user") {
            val loggedInUser = createLoggedInUser(id = "fakeUserId")
            val screenEvent = ScreenEventType.ZAAK.updated(UUID.randomUUID())
            val sentScreenEvent = slot<ScreenEvent>()

            every { loggedInUserInstance.get() } returns loggedInUser
            every { screenUpdateEvent.fireAsync(capture(sentScreenEvent)) } returns mockk()

            `when`("the screen event is sent") {
                eventingService.send(screenEvent)

                then("the event identifies the user that caused it, so that a screen can recognise its own change") {
                    sentScreenEvent.captured.actorUserId shouldBe "fakeUserId"
                }
            }
        }

        given("a change made outside a user session, such as a notification or a cron job") {
            val screenEvent = ScreenEventType.ZAAK.updated(UUID.randomUUID())
            val sentScreenEvent = slot<ScreenEvent>()

            every { loggedInUserInstance.get() } returns FUNCTIONEEL_GEBRUIKER
            every { screenUpdateEvent.fireAsync(capture(sentScreenEvent)) } returns mockk()

            `when`("the screen event is sent") {
                eventingService.send(screenEvent)

                then("the event identifies the functionele gebruiker, which no screen mistakes for its own user") {
                    sentScreenEvent.captured.actorUserId shouldBe FUNCTIONEEL_GEBRUIKER.id
                }
            }
        }
    }

    context("Sending a signalering event") {
        given("an observer that fails to handle the event") {
            val event = SignaleringEvent(
                SignaleringType.Type.ZAAK_OP_NAAM,
                SignaleringEventId(URI("https://example.com/fakeRol"), null),
                null
            )
            every {
                signaleringEvent.fireAsync(event)
            } returns CompletableFuture.failedFuture(IllegalStateException("fakeFailure"))

            `when`("the signalering event is sent") {
                val logRecords = captureLogRecords { eventingService.send(event) }

                then("the failure is logged, because nothing else observes it") {
                    logRecords.single { it.level == Level.SEVERE }.thrown.message shouldBe "fakeFailure"
                }
            }
        }
    }
})

private fun captureLogRecords(block: () -> Unit): List<LogRecord> {
    val logger = Logger.getLogger(EventingService::class.java.name)
    val records = mutableListOf<LogRecord>()
    val handler = object : Handler() {
        override fun publish(record: LogRecord) {
            records.add(record)
        }
        override fun flush() = Unit
        override fun close() = Unit
    }
    logger.addHandler(handler)
    try {
        block()
    } finally {
        logger.removeHandler(handler)
    }
    return records
}
