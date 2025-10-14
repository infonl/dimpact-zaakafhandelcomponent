/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldStartWith
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.UnsatisfiedResolutionException
import nl.info.client.brp.util.BrpClientHeadersFactory.Companion.MAX_HEADER_SIZE
import nl.info.client.brp.util.BrpClientHeadersFactory.Companion.MAX_USER_HEADER_SIZE
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.SecurityUtil.Companion.FUNCTIONEEL_GEBRUIKER
import org.jboss.resteasy.core.Headers
import java.util.Optional

class BrpClientHeadersFactoryTest : BehaviorSpec({
    val apiKey = "apiKey"
    val originOin = "originOin"
    val purpose = "purpose"
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("originOin is empty") {
        val brpConfiguration = createBrpConfiguration(originOin = Optional.empty())
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance)
        val existingHeaders = Headers<String>().apply {
            add("header", "value")
        }

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), existingHeaders)

            Then("no protocollering headers are changed or set") {
                headers shouldContainExactly mapOf("header" to listOf("value"))
            }
        }
    }

    Given("originOIN is present and a valid user exists") {
        every { loggedInUserInstance.get().id } returns "username"

        val brpConfiguration = createBrpConfiguration()
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), Headers())

            Then("correct BRP protocollering headers are generated") {
                headers shouldContainExactly mapOf(
                    "X-API-KEY" to listOf(apiKey),
                    "X-ORIGIN-OIN" to listOf(originOin),
                    "X-GEBRUIKER" to listOf("username")
                )
            }
        }
    }

    Given("FUNCTIONEEL_GEBRUIKER is the current active user") {
        every { loggedInUserInstance.get().id } returns FUNCTIONEEL_GEBRUIKER.id

        val brpConfiguration = createBrpConfiguration()
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), Headers())

            Then("Systeem user is sent as the active user") {
                headers shouldContainExactly mapOf(
                    "X-API-KEY" to listOf(apiKey),
                    "X-ORIGIN-OIN" to listOf(originOin),
                    "X-GEBRUIKER" to listOf("Systeem")
                )
            }
        }
    }

    Given("originOIN is present, no custom doelbinding or verwerking and a missing active user") {
        every { loggedInUserInstance.get().id } throws UnsatisfiedResolutionException()

        val brpConfiguration = createBrpConfiguration(
            queryPersonenDefaultPurpose = Optional.empty(),
            retrievePersoonDefaultPurpose = Optional.empty(),
            processingRegisterDefault = Optional.empty()
        )
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), Headers())

            Then("correct BRP protocollering headers are generated") {
                headers shouldContainExactly mapOf(
                    "X-API-KEY" to listOf(apiKey),
                    "X-ORIGIN-OIN" to listOf(originOin),
                    "X-GEBRUIKER" to listOf("Systeem")
                )
            }
        }
    }

    Given("Previously set BRP headers, no custom doelbinding and verwerking, protocollering enabled and a valid user") {
        val outgoingHeaders = Headers<String>().apply {
            add("X-API-KEY", apiKey)
            add("X-DOELBINDING", purpose)
        }
        every { loggedInUserInstance.get().id } returns "username"

        val brpConfiguration = createBrpConfiguration()
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), outgoingHeaders)

            Then("correct BRP protocollering headers are generated") {
                headers shouldContainExactly mapOf(
                    "X-API-KEY" to listOf(apiKey),
                    "X-ORIGIN-OIN" to listOf(originOin),
                    "X-DOELBINDING" to listOf(purpose),
                    "X-GEBRUIKER" to listOf("username")
                )
            }
        }
    }

    Given("User longer than $MAX_USER_HEADER_SIZE characters") {
        val longUserName = "a".repeat(MAX_USER_HEADER_SIZE + 1)
        val outgoingHeaders = Headers<String>().apply {
            add("X-GEBRUIKER", longUserName)
        }

        val brpConfiguration = createBrpConfiguration(originOin = Optional.empty())
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), outgoingHeaders)

            Then("header is truncated to $MAX_USER_HEADER_SIZE characters") {
                headers shouldHaveSize 1
                headers shouldContainKey "X-GEBRUIKER"
                with(headers["X-GEBRUIKER"]?.first()) {
                    this shouldStartWith "aaa"
                    this shouldHaveLength MAX_USER_HEADER_SIZE
                }
            }
        }
    }

    Given("Header longer than $MAX_HEADER_SIZE characters") {
        val longZaakDescription = "a".repeat(MAX_HEADER_SIZE + 1)
        val outgoingHeaders = Headers<String>().apply {
            add("X-VERWERKING", "General@$longZaakDescription")
        }

        val brpConfiguration = createBrpConfiguration(originOin = Optional.empty())
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), outgoingHeaders)

            Then("header is truncated to $MAX_HEADER_SIZE characters") {
                headers shouldHaveSize 1
                headers shouldContainKey "X-VERWERKING"
                with(headers["X-VERWERKING"]?.first()) {
                    this shouldStartWith "General@aaa"
                    this shouldHaveLength MAX_HEADER_SIZE
                }
            }
        }
    }

    Given("Invalid audit log provider") {
        val brpConfiguration = createBrpConfiguration(auditLogProvider = Optional.of("invalid"))
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance)

        every { loggedInUserInstance.get().id } returns "username"

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), Headers())

            Then("default iConnect headers are generated") {
                headers shouldContainExactly mapOf(
                    "X-API-KEY" to listOf(apiKey),
                    "X-ORIGIN-OIN" to listOf(originOin),
                    "X-GEBRUIKER" to listOf("username")
                )
            }
        }
    }
})
