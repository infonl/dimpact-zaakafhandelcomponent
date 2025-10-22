/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.configuratie

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import nl.info.client.brp.util.createBrpConfiguration
import java.util.Optional

class BrpConfigurationTest : BehaviorSpec({

    Given("No default query persson doelbinding configured") {
        When("configuration is created") {
            val exception = shouldThrow<IllegalStateException> {
                createBrpConfiguration(queryPersonenDefaultDoelbinding = Optional.empty())
            }
            Then("Exception is thrown") {
                exception.message shouldContain "BRP_DOELBINDING_ZOEKMET"
            }
        }
    }

    Given("No default retrieve persoon doelbinding configured") {
        When("configuration is created") {
            val exception = shouldThrow<IllegalStateException> {
                createBrpConfiguration(retrievePersoonDefaultDoelbinding = Optional.empty())
            }
            Then("Exception is thrown") {
                exception.message shouldContain "BRP_DOELBINDING_RAADPLEEGMET"
            }
        }
    }

    Given("No default verwerkingsregister configured") {
        When("configuration is created") {
            val exception = shouldThrow<IllegalStateException> {
                createBrpConfiguration(verwerkingregisterDefault = Optional.empty())
            }
            Then("Exception is thrown") {
                exception.message shouldContain "BRP_VERWERKINGSREGISTER"
            }
        }
    }

    Given("No default audit log provider configured") {
        When("configuration is created") {
            val exception = shouldThrow<IllegalStateException> {
                createBrpConfiguration(auditLogProvider = Optional.empty())
            }
            Then("Exception is thrown") {
                exception.message shouldContain "BRP_PROTOCOLLERING"
            }
        }
    }

    Given("BRP protocollering disabled") {
        val brpConfiguration = createBrpConfiguration(originOin = Optional.empty())

        When("reading BRP audit log provider") {
            val protocolleringProvider = brpConfiguration.readBrpProtocolleringProvider()

            Then("empty string is returned") {
                protocolleringProvider shouldContain ""
            }
        }
    }

    Given("Invalid BRP audit log provider specified") {
        When("configuration is created") {
            val exception = shouldThrow<IllegalStateException> {
                createBrpConfiguration(auditLogProvider = Optional.of("FakeProvider"))
            }

            Then("Exception is thrown") {
                exception.message shouldContain "FakeProvider"
            }
        }
    }
})
