/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.event

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import nl.info.zac.signalering.ZaakTaskDueDateEmailNotificationService

class JobEventObserverTest : BehaviorSpec({
    val zaakTaskDueDateEmailNotificationService = mockk<ZaakTaskDueDateEmailNotificationService>()
    val jobEventObserver = JobEventObserver(zaakTaskDueDateEmailNotificationService)

    afterEach {
        checkUnnecessaryStub()
    }

    given("a job event for the signaleringen job") {
        val jobEvent = JobEvent(JobId.SIGNALERINGEN_JOB)
        every { zaakTaskDueDateEmailNotificationService.sendDueDateEmailNotifications() } just Runs

        `when`("the event is observed") {
            jobEventObserver.onFire(jobEvent)

            then("the due date email notifications are sent") {
                verify(exactly = 1) { zaakTaskDueDateEmailNotificationService.sendDueDateEmailNotifications() }
            }
        }
    }

    given("a job event for the signaleringen job and sending the notifications fails") {
        val jobEvent = JobEvent(JobId.SIGNALERINGEN_JOB)
        every {
            zaakTaskDueDateEmailNotificationService.sendDueDateEmailNotifications()
        } throws IllegalStateException("fake failure")

        `when`("the event is observed") {
            then("the exception is caught, so that it does not escape the asynchronous observer") {
                shouldNotThrowAny { jobEventObserver.onFire(jobEvent) }
            }
        }
    }
})
