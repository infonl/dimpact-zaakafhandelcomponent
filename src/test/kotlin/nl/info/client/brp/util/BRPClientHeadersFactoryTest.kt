/*
 * SPDX-FileCopyrightText: 2024 Lifely
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
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Protocolering disabled") {
        val brpClientHeadersFactory = BRPClientHeadersFactory(
            Optional.of(apiKey),
            Optional.of(false),
            Optional.of(originOin),
            Optional.of("customPurpose"),
            Optional.of("customProcess"),
            loggedInUserInstance
        )
        val existingHeaders = Headers<String>().apply {
            add("header", "value")
        }

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers<String>(), existingHeaders)

            Then("no protocolering headers are changed or set") {
                headers shouldContainExactly mapOf("header" to listOf("value"))
            }
        }
    }

    Given("BRP headers not set, no custom doelbinding and verwerking, protocolering enabled and a valid user") {
        every { loggedInUserInstance.get().id } returns "username"

        val brpClientHeadersFactory = BRPClientHeadersFactory(
            Optional.of(apiKey),
            Optional.of(true),
            Optional.of(originOin),
            Optional.empty(),
            Optional.empty(),
            loggedInUserInstance
        )

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers<String>(), Headers<String>())

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

    Given("BRP headers not set, no custom doelbinding or verwerking, protocolering enabled and a missing active user") {
        every { loggedInUserInstance.get().id } throws UnsatisfiedResolutionException()

        val brpClientHeadersFactory = BRPClientHeadersFactory(
            Optional.of(apiKey),
            Optional.of(true),
            Optional.of(originOin),
            Optional.empty(),
            Optional.empty(),
            loggedInUserInstance
        )

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers<String>(), Headers<String>())

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

    Given("No BRP headers, custom doelbinding and verwerking, protocolering enabled an active user") {
        every { loggedInUserInstance.get().id } returns "username"

        val brpClientHeadersFactory = BRPClientHeadersFactory(
            Optional.of(apiKey),
            Optional.of(true),
            Optional.of(originOin),
            Optional.of("customPurpose"),
            Optional.of("customProcess"),
            loggedInUserInstance
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

    Given("Previously set BRP headers, no custom doelbinding and verwerking, protocolering enabled  and a valid user") {
        val outgoingHeaders = Headers<String>().apply {
            add("X-API-KEY", apiKey)
            add("X-DOELBINDING", "test")
        }
        every { loggedInUserInstance.get().id } returns "username"

        val brpClientHeadersFactory = BRPClientHeadersFactory(
            Optional.of(apiKey),
            Optional.of(true),
            Optional.of(originOin),
            Optional.empty(),
            Optional.empty(),
            loggedInUserInstance
        )

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers<String>(), outgoingHeaders)

            Then("correct BRP protocollering headers are generated") {
                with(headers) {
                    shouldContain("X-API-KEY", listOf(apiKey))
                    shouldContain("X-ORIGIN-OIN", listOf(originOin))
                    shouldContain("X-DOELBINDING", listOf("test"))
                    shouldNotHaveKey("X-VERWERKING")
                    shouldContain("X-GEBRUIKER", listOf("username"))
                }
            }
        }
    }
})
