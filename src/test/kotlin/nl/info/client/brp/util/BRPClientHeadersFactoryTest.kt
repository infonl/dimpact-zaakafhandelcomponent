/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.UnsatisfiedResolutionException
import nl.info.zac.authentication.LoggedInUser
import org.jboss.resteasy.core.Headers

class BRPClientHeadersFactoryTest : BehaviorSpec({
    val apiKey = "apiKey"
    val originOin = "originOin"
    val purpose = "doelbinding"
    val process = "verwerking"
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()

    val brpClientHeadersFactory = BRPClientHeadersFactory(
        apiKey,
        originOin,
        purpose,
        process,
        loggedInUserInstance
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("BRP headers and a valid user") {
        every { loggedInUserInstance.get().id } returns "username"

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers<String>(), Headers<String>())

            Then("correct BRP protocollering headers are generated") {
                with(headers) {
                    shouldContain("X-API-KEY", listOf(apiKey))
                    shouldContain("X-ORIGIN-OIN", listOf(originOin))
                    shouldContain("X-DOELBINDING", listOf(purpose))
                    shouldContain("X-VERWERKING", listOf(process))
                    shouldContain("X-GEBRUIKER", listOf("username"))
                }
            }
        }
    }

    Given("BRP headers and a missing active user") {
        every { loggedInUserInstance.get().id } throws UnsatisfiedResolutionException()

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers<String>(), Headers<String>())

            Then("correct BRP protocollering headers are generated") {
                with(headers) {
                    shouldContain("X-API-KEY", listOf(apiKey))
                    shouldContain("X-ORIGIN-OIN", listOf(originOin))
                    shouldContain("X-DOELBINDING", listOf(purpose))
                    shouldContain("X-VERWERKING", listOf(process))
                    shouldContain("X-GEBRUIKER", listOf("BurgerZelf"))
                }
            }
        }
    }
})
