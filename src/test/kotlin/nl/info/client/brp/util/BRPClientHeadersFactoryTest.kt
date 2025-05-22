/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldNotHaveKey
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
    val purpose = "purpose"
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("originOin is empty") {
        val brpClientHeadersFactory = BRPClientHeadersFactory(
            Optional.of(apiKey),
            Optional.empty(),
            loggedInUserInstance
        )
        val existingHeaders = Headers<String>().apply {
            add("header", "value")
        }

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), existingHeaders)

            Then("no protocolering headers are changed or set") {
                headers shouldContainExactly mapOf("header" to listOf("value"))
            }
        }
    }

    Given("originOIN is present and a valid user exists") {
        every { loggedInUserInstance.get().id } returns "username"

        val brpClientHeadersFactory = BRPClientHeadersFactory(
            Optional.of(apiKey),
            Optional.of(originOin),
            loggedInUserInstance
        )

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), Headers())

            Then("correct BRP protocollering headers are generated") {
                with(headers) {
                    shouldContain("X-API-KEY", listOf(apiKey))
                    shouldContain("X-ORIGIN-OIN", listOf(originOin))
                    shouldNotHaveKey("X-DOELBINDING")
                    shouldNotHaveKey("X-VERWERKING")
                    shouldContain("X-GEBRUIKER", listOf("username"))
                }
            }
        }
    }

    Given("originOIN is present, no custom doelbinding or verwerking and a missing active user") {
        every { loggedInUserInstance.get().id } throws UnsatisfiedResolutionException()

        val brpClientHeadersFactory = BRPClientHeadersFactory(
            Optional.of(apiKey),
            Optional.of(originOin),
            loggedInUserInstance
        )

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), Headers())

            Then("correct BRP protocollering headers are generated") {
                with(headers) {
                    shouldContain("X-API-KEY", listOf(apiKey))
                    shouldContain("X-ORIGIN-OIN", listOf(originOin))
                    shouldNotHaveKey("X-DOELBINDING")
                    shouldNotHaveKey("X-VERWERKING")
                    shouldContain("X-GEBRUIKER", listOf("BurgerZelf"))
                }
            }
        }
    }

    Given("Previously set BRP headers, no custom doelbinding and verwerking, protocolering enabled and a valid user") {
        val outgoingHeaders = Headers<String>().apply {
            add("X-API-KEY", apiKey)
            add("X-DOELBINDING", purpose)
        }
        every { loggedInUserInstance.get().id } returns "username"

        val brpClientHeadersFactory = BRPClientHeadersFactory(
            Optional.of(apiKey),
            Optional.of(originOin),
            loggedInUserInstance
        )

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), outgoingHeaders)

            Then("correct BRP protocollering headers are generated") {
                with(headers) {
                    shouldContain("X-API-KEY", listOf(apiKey))
                    shouldContain("X-ORIGIN-OIN", listOf(originOin))
                    shouldContain("X-DOELBINDING", listOf(purpose))
                    shouldNotHaveKey("X-VERWERKING")
                    shouldContain("X-GEBRUIKER", listOf("username"))
                }
            }
        }
    }
})
