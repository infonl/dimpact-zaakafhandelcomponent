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
import java.util.Optional

class BRPClientHeadersFactoryTest : BehaviorSpec({
    val apiKey = "apiKey"
    val originOin = "originOin"
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("No previously set BRP headers, no custom doelbinding and verwerking and a valid user") {
        every { loggedInUserInstance.get().id } returns "username"

        val brpClientHeadersFactory = BRPClientHeadersFactory(
            Optional.of(apiKey),
            Optional.of(originOin),
            loggedInUserInstance
        )

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers<String>(), Headers<String>())

            Then("correct BRP protocollering headers are generated") {
                with(headers) {
                    shouldContain("X-API-KEY", listOf(apiKey))
                    shouldContain("X-ORIGIN-OIN", listOf(originOin))
                    shouldContain("X-DOELBINDING", listOf("BRPACT-Totaal"))
                    shouldContain("X-VERWERKING", listOf("zaakafhandelcomponent"))
                    shouldContain("X-GEBRUIKER", listOf("username"))
                }
            }
        }
    }

    Given("No previously set BRP headers, no custom doelbinding and verwerking and a missing active user") {
        every { loggedInUserInstance.get().id } throws UnsatisfiedResolutionException()

        val brpClientHeadersFactory = BRPClientHeadersFactory(
            Optional.of(apiKey),
            Optional.of(originOin),
            loggedInUserInstance
        )

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers<String>(), Headers<String>())

            Then("correct BRP protocollering headers are generated") {
                with(headers) {
                    shouldContain("X-API-KEY", listOf(apiKey))
                    shouldContain("X-ORIGIN-OIN", listOf(originOin))
                    shouldContain("X-DOELBINDING", listOf("BRPACT-Totaal"))
                    shouldContain("X-VERWERKING", listOf("zaakafhandelcomponent"))
                    shouldContain("X-GEBRUIKER", listOf("BurgerZelf"))
                }
            }
        }
    }

    Given("No previously set BRP headers, custom doelbinding and verwerking, an active user") {
        every { loggedInUserInstance.get().id } returns "username"

        val brpClientHeadersFactory = BRPClientHeadersFactory(
            Optional.of(apiKey),
            Optional.of(originOin),
            loggedInUserInstance,
            Optional.of("customPurpose"),
            Optional.of("customProcess")
        )

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers<String>(), Headers<String>())

            Then("correct BRP protocollering headers are generated") {
                with(headers) {
                    shouldContain("X-API-KEY", listOf(apiKey))
                    shouldContain("X-ORIGIN-OIN", listOf(originOin))
                    shouldContain("X-DOELBINDING", listOf("customPurpose"))
                    shouldContain("X-VERWERKING", listOf("customProcess"))
                    shouldContain("X-GEBRUIKER", listOf("username"))
                }
            }
        }
    }

    Given("Previously set BRP headers, no custom doelbinding and verwerking and a valid user") {
        val outgoingHeaders = Headers<String>().apply {
            add("X-API-KEY", apiKey)
            add("X-DOELBINDING", "test")
        }
        every { loggedInUserInstance.get().id } returns "username"

        val brpClientHeadersFactory = BRPClientHeadersFactory(
            Optional.of(apiKey),
            Optional.of(originOin),
            loggedInUserInstance
        )

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers<String>(), outgoingHeaders)

            Then("correct BRP protocollering headers are generated") {
                with(headers) {
                    shouldContain("X-API-KEY", listOf(apiKey))
                    shouldContain("X-ORIGIN-OIN", listOf(originOin))
                    shouldContain("X-DOELBINDING", listOf("test"))
                    shouldContain("X-VERWERKING", listOf("zaakafhandelcomponent"))
                    shouldContain("X-GEBRUIKER", listOf("username"))
                }
            }
        }
    }
})
