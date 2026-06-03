/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import org.jboss.resteasy.core.Headers

class BrpClientHeadersFactoryTest : BehaviorSpec({

    afterEach {
        checkUnnecessaryStub()
    }

    Given("An empty protocollering context") {
        val brpProtocolleringContext = BrpProtocolleringContext()
        val factory = BrpClientHeadersFactory(brpProtocolleringContext)
        val existingHeaders = Headers<String>().apply { add("header", "value") }

        When("headers are updated") {
            val headers = factory.update(Headers(), existingHeaders)

            Then("no protocollering headers are added") {
                headers shouldContainExactly mapOf("header" to listOf("value"))
            }
        }
    }

    Given("Protocollering context has origin OIN, gebruiker and toepassing headers") {
        val brpProtocolleringContext = BrpProtocolleringContext().apply {
            headers["x-origin-oin"] = "fakeOriginOin"
            headers["x-gebruiker"] = "username"
            headers["x-toepassing"] = "ZAC"
        }
        val factory = BrpClientHeadersFactory(brpProtocolleringContext)

        When("headers are updated") {
            val result = factory.update(Headers(), Headers())

            Then("all context headers appear in outgoing headers") {
                result shouldContainExactly mapOf(
                    "x-origin-oin" to listOf("fakeOriginOin"),
                    "x-gebruiker" to listOf("username"),
                    "x-toepassing" to listOf("ZAC")
                )
            }
        }
    }

    Given("Pre-set x-origin-oin in outgoing headers and context also has x-origin-oin") {
        val brpProtocolleringContext = BrpProtocolleringContext().apply {
            headers["x-origin-oin"] = "context-oin"
            headers["x-gebruiker"] = "username"
        }
        val factory = BrpClientHeadersFactory(brpProtocolleringContext)
        val outgoingHeaders = Headers<String>().apply { add("x-origin-oin", "pre-set-oin") }

        When("headers are updated") {
            val result = factory.update(Headers(), outgoingHeaders)

            Then("pre-set header is preserved and not overridden by context value") {
                result shouldContainExactly mapOf(
                    "x-origin-oin" to listOf("pre-set-oin"),
                    "x-gebruiker" to listOf("username")
                )
            }
        }
    }

    Given("Protocollering context has doelbinding and verwerking in addition to other headers") {
        val brpProtocolleringContext = BrpProtocolleringContext().apply {
            headers["x-origin-oin"] = "fakeOriginOin"
            headers["x-gebruiker"] = "username"
            headers["x-doelbinding"] = "fakePurpose"
            headers["x-verwerking"] = "fakeVerwerking"
            headers["x-toepassing"] = "ZAC"
        }
        val factory = BrpClientHeadersFactory(brpProtocolleringContext)

        When("headers are updated") {
            val result = factory.update(Headers(), Headers())

            Then("all context headers including doelbinding and verwerking are in outgoing headers") {
                result shouldContainExactly mapOf(
                    "x-origin-oin" to listOf("fakeOriginOin"),
                    "x-gebruiker" to listOf("username"),
                    "x-doelbinding" to listOf("fakePurpose"),
                    "x-verwerking" to listOf("fakeVerwerking"),
                    "x-toepassing" to listOf("ZAC")
                )
            }
        }
    }

    Given("Custom header names are set in the protocollering context") {
        val brpProtocolleringContext = BrpProtocolleringContext().apply {
            headers["custom-origin"] = "fakeOriginOin"
            headers["custom-user"] = "username"
            headers["custom-toepassing"] = "ZAC"
        }
        val factory = BrpClientHeadersFactory(brpProtocolleringContext)

        When("headers are updated") {
            val result = factory.update(Headers(), Headers())

            Then("custom header names are used in outgoing headers") {
                result shouldContainExactly mapOf(
                    "custom-origin" to listOf("fakeOriginOin"),
                    "custom-user" to listOf("username"),
                    "custom-toepassing" to listOf("ZAC")
                )
            }
        }
    }

    Given("Factory passes through values as-is without truncation") {
        val longValue = "a".repeat(300)
        val brpProtocolleringContext = BrpProtocolleringContext().apply {
            headers["x-verwerking"] = longValue
        }
        val factory = BrpClientHeadersFactory(brpProtocolleringContext)

        When("headers are updated") {
            val result = factory.update(Headers(), Headers())

            Then("the value is passed through unchanged — truncation is BrpConfigurationValueImpl's responsibility") {
                result["x-verwerking"]?.first()?.length shouldBe 300
            }
        }
    }

    Given("Protocollering context is missing the gebruiker header (disabled via blank header name)") {
        val brpProtocolleringContext = BrpProtocolleringContext().apply {
            headers["x-origin-oin"] = "fakeOriginOin"
            headers["x-toepassing"] = "ZAC"
        }
        val factory = BrpClientHeadersFactory(brpProtocolleringContext)

        When("headers are updated") {
            val result = factory.update(Headers(), Headers())

            Then("gebruiker header is absent from outgoing headers") {
                result shouldNotContainKey "x-gebruiker"
                result shouldContainExactly mapOf(
                    "x-origin-oin" to listOf("fakeOriginOin"),
                    "x-toepassing" to listOf("ZAC")
                )
            }
        }
    }

    Given("Protocollering context has all headers except doelbinding (doelbinding disabled)") {
        val brpProtocolleringContext = BrpProtocolleringContext().apply {
            headers["x-origin-oin"] = "fakeOriginOin"
            headers["x-gebruiker"] = "username"
            headers["x-verwerking"] = "fakeVerwerking"
            headers["x-toepassing"] = "ZAC"
        }
        val factory = BrpClientHeadersFactory(brpProtocolleringContext)

        When("headers are updated") {
            val result = factory.update(Headers(), Headers())

            Then("all other headers are present but doelbinding is absent") {
                result shouldNotContainKey "x-doelbinding"
                result shouldContainExactly mapOf(
                    "x-origin-oin" to listOf("fakeOriginOin"),
                    "x-gebruiker" to listOf("username"),
                    "x-verwerking" to listOf("fakeVerwerking"),
                    "x-toepassing" to listOf("ZAC")
                )
            }
        }
    }

    Given("Protocollering context has toepassing header") {
        val brpProtocolleringContext = BrpProtocolleringContext().apply {
            headers["x-toepassing"] = "ZAC"
        }
        val factory = BrpClientHeadersFactory(brpProtocolleringContext)

        When("headers are updated") {
            val result = factory.update(Headers(), Headers())

            Then("toepassing header is in outgoing headers") {
                result shouldContainExactly mapOf("x-toepassing" to listOf("ZAC"))
            }
        }
    }

    Given("Protocollering context does not have toepassing header (disabled via blank header name)") {
        val brpProtocolleringContext = BrpProtocolleringContext()
        val factory = BrpClientHeadersFactory(brpProtocolleringContext)

        When("headers are updated") {
            val result = factory.update(Headers(), Headers())

            Then("toepassing header is absent from outgoing headers") {
                result shouldNotContainKey "x-toepassing"
            }
        }
    }
})
