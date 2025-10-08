/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContainExactly
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

    Context("iConnect audit log provider") {
        Given("originOin is empty") {
            val brpClientHeadersFactory = BRPClientHeadersFactory(
                apiKey = Optional.of(apiKey),
                originOIN = Optional.empty(),
                auditLogProvider = Optional.empty(),
                loggedInUserInstance = loggedInUserInstance
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
                apiKey = Optional.of(apiKey),
                originOIN = Optional.of(originOin),
                auditLogProvider = Optional.of("iConnect"),
                loggedInUserInstance = loggedInUserInstance
            )

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

        Given("originOIN is present, no custom doelbinding or verwerking and a missing active user") {
            every { loggedInUserInstance.get().id } throws UnsatisfiedResolutionException()

            val brpClientHeadersFactory = BRPClientHeadersFactory(
                apiKey = Optional.of(apiKey),
                originOIN = Optional.of(originOin),
                auditLogProvider = Optional.empty(),
                loggedInUserInstance = loggedInUserInstance
            )

            When("headers are updated") {
                val headers = brpClientHeadersFactory.update(Headers(), Headers())

                Then("correct BRP protocollering headers are generated") {
                    headers shouldContainExactly mapOf(
                        "X-API-KEY" to listOf(apiKey),
                        "X-ORIGIN-OIN" to listOf(originOin),
                        "X-GEBRUIKER" to listOf("BurgerZelf")
                    )
                }
            }
        }

        Given(
            "Previously set BRP headers, no custom doelbinding and verwerking, protocolering enabled and a valid user"
        ) {
            val outgoingHeaders = Headers<String>().apply {
                add("X-API-KEY", apiKey)
                add("X-DOELBINDING", purpose)
            }
            every { loggedInUserInstance.get().id } returns "username"

            val brpClientHeadersFactory = BRPClientHeadersFactory(
                apiKey = Optional.of(apiKey),
                originOIN = Optional.of(originOin),
                auditLogProvider = Optional.of("iConnect"),
                loggedInUserInstance = loggedInUserInstance
            )

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
    }

    Context("2Secure audit log provider") {
        Given("originOin is empty") {
            val brpClientHeadersFactory = BRPClientHeadersFactory(
                apiKey = Optional.of(apiKey),
                originOIN = Optional.empty(),
                auditLogProvider = Optional.of("2Secure"),
                loggedInUserInstance = loggedInUserInstance
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
                apiKey = Optional.of(apiKey),
                originOIN = Optional.of(originOin),
                auditLogProvider = Optional.of("2Secure"),
                loggedInUserInstance = loggedInUserInstance
            )

            When("headers are updated") {
                val headers = brpClientHeadersFactory.update(Headers(), Headers())

                Then("correct BRP protocollering headers are generated") {
                    headers shouldContainExactly mapOf(
                        "X-REQUEST-APPLICATION" to listOf(apiKey),
                        "X-REQUEST-ORGANIZATION" to listOf(originOin),
                        "X-REQUEST-USER" to listOf("username")
                    )
                }
            }
        }

        Given("originOIN is present, no custom doelbinding or verwerking and a missing active user") {
            every { loggedInUserInstance.get().id } throws UnsatisfiedResolutionException()

            val brpClientHeadersFactory = BRPClientHeadersFactory(
                apiKey = Optional.of(apiKey),
                originOIN = Optional.of(originOin),
                auditLogProvider = Optional.of("2Secure"),
                loggedInUserInstance = loggedInUserInstance
            )

            When("headers are updated") {
                val headers = brpClientHeadersFactory.update(Headers(), Headers())

                Then("correct BRP protocollering headers are generated") {
                    headers shouldContainExactly mapOf(
                        "X-REQUEST-APPLICATION" to listOf(apiKey),
                        "X-REQUEST-ORGANIZATION" to listOf(originOin),
                        "X-REQUEST-USER" to listOf("BurgerZelf")
                    )
                }
            }
        }

        Given(
            "Previously set BRP headers, no custom doelbinding and verwerking, protocolering enabled and a valid user"
        ) {
            val outgoingHeaders = Headers<String>().apply {
                add("X-API-KEY", apiKey)
                add("X-REQUEST-ORGANIZATION", "different organization")
            }
            every { loggedInUserInstance.get().id } returns "username"

            val brpClientHeadersFactory = BRPClientHeadersFactory(
                apiKey = Optional.of(apiKey),
                originOIN = Optional.of(originOin),
                auditLogProvider = Optional.of("2Secure"),
                loggedInUserInstance = loggedInUserInstance
            )

            When("headers are updated") {
                val headers = brpClientHeadersFactory.update(Headers(), outgoingHeaders)

                Then("correct BRP protocollering headers are generated") {
                    headers shouldContainExactly mapOf(
                        "X-API-KEY" to listOf("apiKey"),
                        "X-REQUEST-ORGANIZATION" to listOf("different organization"),
                        "X-REQUEST-APPLICATION" to listOf(apiKey),
                        "X-REQUEST-USER" to listOf("username")
                    )
                }
            }
        }
    }
})
