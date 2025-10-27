/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.configuratie

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import nl.info.client.brp.util.createBrpConfiguration
import nl.info.zac.configuratie.exception.BrpProtocolleringConfigurationException
import java.util.Optional

class BrpConfigurationTest : BehaviorSpec({

    Context("configuration validation") {

        Given("No default query persson doelbinding configured") {
            val brpConfiguration = createBrpConfiguration(doelbindingZoekMetDefault = Optional.empty())

            When("configuration is validated by Weld") {
                val exception = shouldThrow<BrpProtocolleringConfigurationException> {
                    brpConfiguration.validateConfiguration()
                }
                Then("Exception is thrown") {
                    exception.message shouldContain "BRP_DOELBINDING_ZOEKMET"
                }
            }
        }

        Given("No default retrieve persoon doelbinding configured") {
            val brpConfiguration = createBrpConfiguration(doelbindingRaadpleegMetDefault = Optional.empty())

            When("configuration is validated by Weld") {
                val exception = shouldThrow<BrpProtocolleringConfigurationException> {
                    brpConfiguration.validateConfiguration()
                }
                Then("Exception is thrown") {
                    exception.message shouldContain "BRP_DOELBINDING_RAADPLEEGMET"
                }
            }
        }

        Given("No default verwerkingsregister configured") {
            val brpConfiguration = createBrpConfiguration(verwerkingregisterDefault = Optional.empty())

            When("configuration is validated by Weld") {
                val exception = shouldThrow<BrpProtocolleringConfigurationException> {
                    brpConfiguration.validateConfiguration()
                }
                Then("Exception is thrown") {
                    exception.message shouldContain "BRP_VERWERKINGSREGISTER"
                }
            }
        }

        Given("No default audit log provider configured") {
            val brpConfiguration = createBrpConfiguration(auditLogProvider = Optional.empty())

            When("configuration is validated by Weld") {
                val exception = shouldThrow<BrpProtocolleringConfigurationException> {
                    brpConfiguration.validateConfiguration()
                }
                Then("Exception is thrown") {
                    exception.message shouldContain "BRP_PROTOCOLLERING"
                }
            }
        }

        Given("Invalid BRP audit log provider specified") {
            val brpConfiguration = createBrpConfiguration(auditLogProvider = Optional.of("FakeProvider"))

            When("configuration is validated by Weld") {
                val exception = shouldThrow<BrpProtocolleringConfigurationException> {
                    brpConfiguration.validateConfiguration()
                }

                Then("Exception is thrown") {
                    exception.message shouldContain "FakeProvider"
                }
            }
        }
    }

    Context("BRP protocollering proxy") {
        Given("BRP protocollering disabled") {
            val brpConfiguration = createBrpConfiguration(originOin = Optional.empty())

            When("reading BRP audit log provider") {
                val protocolleringProvider = brpConfiguration.readBrpProtocolleringProvider()

                Then("empty string is returned") {
                    protocolleringProvider shouldContain ""
                }
            }
        }
    }
})
