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
import nl.info.zac.authentication.LoggedInUserProvider.Companion.FUNCTIONEEL_GEBRUIKER
import org.jboss.resteasy.core.Headers
import java.util.Optional

class BrpClientHeadersFactoryTest : BehaviorSpec({
    val originOin = "fakeOriginOin"
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("originOin is empty") {
        val brpConfiguration = createBrpConfiguration(originOin = Optional.empty())
        val brpProtocolleringContext = BrpProtocolleringContext()
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance, brpProtocolleringContext)
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
        val brpProtocolleringContext = BrpProtocolleringContext()
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance, brpProtocolleringContext)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), Headers())

            Then("BRP protocollering headers are set without api key header") {
                headers shouldContainExactly mapOf(
                    "x-origin-oin" to listOf(originOin),
                    "x-gebruiker" to listOf("username"),
                    "x-toepassing" to listOf("ZAC")
                )
            }
        }
    }

    Given("FUNCTIONEEL_GEBRUIKER is the current active user") {
        every { loggedInUserInstance.get().id } returns FUNCTIONEEL_GEBRUIKER.id

        val brpConfiguration = createBrpConfiguration()
        val brpProtocolleringContext = BrpProtocolleringContext()
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance, brpProtocolleringContext)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), Headers())

            Then("Systeem user is sent as the active user") {
                headers shouldContainExactly mapOf(
                    "x-origin-oin" to listOf(originOin),
                    "x-gebruiker" to listOf("Systeem"),
                    "x-toepassing" to listOf("ZAC")
                )
            }
        }
    }

    Given("originOIN is present and no active user") {
        every { loggedInUserInstance.get().id } throws UnsatisfiedResolutionException()

        val brpConfiguration = createBrpConfiguration()
        val brpProtocolleringContext = BrpProtocolleringContext()
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance, brpProtocolleringContext)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), Headers())

            Then("Systeem is sent as the active user") {
                headers shouldContainExactly mapOf(
                    "x-origin-oin" to listOf(originOin),
                    "x-gebruiker" to listOf("Systeem"),
                    "x-toepassing" to listOf("ZAC")
                )
            }
        }
    }

    Given("Pre-set x-origin-oin header, protocollering enabled and a valid user") {
        val outgoingHeaders = Headers<String>().apply {
            add("x-origin-oin", "pre-set-oin")
        }
        every { loggedInUserInstance.get().id } returns "username"

        val brpConfiguration = createBrpConfiguration()
        val brpProtocolleringContext = BrpProtocolleringContext()
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance, brpProtocolleringContext)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), outgoingHeaders)

            Then("pre-set header is preserved and not overridden by factory") {
                headers shouldContainExactly mapOf(
                    "x-origin-oin" to listOf("pre-set-oin"),
                    "x-gebruiker" to listOf("username"),
                    "x-toepassing" to listOf("ZAC")
                )
            }
        }
    }

    Given("Doelbinding and verwerking set in protocollering context") {
        every { loggedInUserInstance.get().id } returns "username"

        val brpConfiguration = createBrpConfiguration()
        val brpProtocolleringContext = BrpProtocolleringContext().apply {
            doelbinding = "fakePurpose"
            verwerking = "fakeVerwerking"
        }
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance, brpProtocolleringContext)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), Headers())

            Then("all protocollering headers including doelbinding and verwerking are set") {
                headers shouldContainExactly mapOf(
                    "x-origin-oin" to listOf(originOin),
                    "x-gebruiker" to listOf("username"),
                    "x-doelbinding" to listOf("fakePurpose"),
                    "x-verwerking" to listOf("fakeVerwerking"),
                    "x-toepassing" to listOf("ZAC")
                )
            }
        }
    }

    Given("x-gebruiker header name is blank (disabled)") {
        val brpConfiguration = createBrpConfiguration(headerNameGebruiker = Optional.of(""))
        val brpProtocolleringContext = BrpProtocolleringContext()
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance, brpProtocolleringContext)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), Headers())

            Then("gebruiker header is not sent") {
                headers shouldContainExactly mapOf(
                    "x-origin-oin" to listOf(originOin),
                    "x-toepassing" to listOf("ZAC")
                )
            }
        }
    }

    Given("Custom header names configured") {
        every { loggedInUserInstance.get().id } returns "username"

        val brpConfiguration = createBrpConfiguration(
            headerNameOriginOin = Optional.of("custom-origin"),
            headerNameGebruiker = Optional.of("custom-user"),
            headerNameToepassing = Optional.of("custom-toepassing")
        )
        val brpProtocolleringContext = BrpProtocolleringContext()
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance, brpProtocolleringContext)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), Headers())

            Then("custom header names are used") {
                headers shouldContainExactly mapOf(
                    "custom-origin" to listOf(originOin),
                    "custom-user" to listOf("username"),
                    "custom-toepassing" to listOf("ZAC")
                )
            }
        }
    }

    Given("User longer than $MAX_USER_HEADER_SIZE characters") {
        val longUserName = "a".repeat(MAX_USER_HEADER_SIZE + 1)
        val outgoingHeaders = Headers<String>().apply {
            add("x-gebruiker", longUserName)
        }

        val brpConfiguration = createBrpConfiguration(originOin = Optional.empty())
        val brpProtocolleringContext = BrpProtocolleringContext()
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance, brpProtocolleringContext)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), outgoingHeaders)

            Then("header is truncated to $MAX_USER_HEADER_SIZE characters") {
                headers shouldHaveSize 1
                headers shouldContainKey "x-gebruiker"
                with(headers["x-gebruiker"]?.first()) {
                    this shouldStartWith "aaa"
                    this shouldHaveLength MAX_USER_HEADER_SIZE
                }
            }
        }
    }

    Given("Header longer than $MAX_HEADER_SIZE characters") {
        val longZaakDescription = "a".repeat(MAX_HEADER_SIZE + 1)
        val outgoingHeaders = Headers<String>().apply {
            add("x-verwerking", "General@$longZaakDescription")
        }

        val brpConfiguration = createBrpConfiguration(originOin = Optional.empty())
        val brpProtocolleringContext = BrpProtocolleringContext()
        val brpClientHeadersFactory = BrpClientHeadersFactory(brpConfiguration, loggedInUserInstance, brpProtocolleringContext)

        When("headers are updated") {
            val headers = brpClientHeadersFactory.update(Headers(), outgoingHeaders)

            Then("header is truncated to $MAX_HEADER_SIZE characters") {
                headers shouldHaveSize 1
                headers shouldContainKey "x-verwerking"
                with(headers["x-verwerking"]?.first()) {
                    this shouldStartWith "General@aaa"
                    this shouldHaveLength MAX_HEADER_SIZE
                }
            }
        }
    }
})
