/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.servlet.http.HttpSession
import net.atos.zac.event.EventingService
import net.atos.zac.util.event.JobEvent
import net.atos.zac.util.event.JobId
import nl.info.zac.app.admin.SignaleringAdminRestService
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.signalering.SignaleringService

class SignaleringAdminRestServiceTest : BehaviorSpec({
    val signaleringService = mockk<SignaleringService>()
    val eventingService = mockk<EventingService>()
    val httpSession = mockk<HttpSession>()
    val httpSessionInstance = mockk<Instance<HttpSession>>()
    val deleteOlderThanDays = 123L
    val signaleringAdminRestService = SignaleringAdminRestService(
        signaleringService = signaleringService,
        eventingService = eventingService,
        httpSession = httpSessionInstance,
        deleteOlderThanDays = deleteOlderThanDays
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A valid HTTP session") {
        every { httpSessionInstance.get() } returns httpSession
        every { httpSession.setAttribute(any(), any()) } just Runs
        every { eventingService.send(any<JobEvent>()) } just Runs
        val loggedInUserSlot = slot<LoggedInUser>()

        When("sendSignaleringen is called") {
            val result = signaleringAdminRestService.sendSignaleringen()

            Then("it should set the functioneel gebruiker in the HTTP session") {
                verify { httpSession.setAttribute("logged-in-user", capture(loggedInUserSlot)) }
                with(loggedInUserSlot.captured) {
                    id shouldBe "FG"
                    firstName shouldBe ""
                    lastName shouldBe "Functionele gebruiker"
                    roles shouldBe setOf("functionele_gebruiker")
                }
            }

            And("it should send the signaleringen job event") {
                verify { eventingService.send(JobEvent(JobId.SIGNALERINGEN_JOB)) }
            }

            And("it should return a success message with the job name") {
                result shouldBe "Started sending signaleringen using job: 'Signaleringen verzenden'"
            }
        }
    }

    Given("An invalid HTTP session") {
        every { httpSessionInstance.get() } throws IllegalStateException("No active session")

        When("sendSignaleringen is called") {
            val exception = shouldThrow<IllegalStateException> {
                signaleringAdminRestService.sendSignaleringen()
            }

            Then("it should throw an IllegalStateException") {
                exception.message shouldBe "No active session"
            }
        }
    }
})
