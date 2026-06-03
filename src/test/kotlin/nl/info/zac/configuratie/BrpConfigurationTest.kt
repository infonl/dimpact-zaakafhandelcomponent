/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.configuratie

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.client.brp.util.createBrpConfiguration
import nl.info.zac.configuration.exception.BrpProtocolleringConfigurationException
import java.util.Optional

class BrpConfigurationTest : BehaviorSpec({

    Context("configuration validation") {

        Given("No default query person doelbinding configured") {
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
            val brpConfiguration = createBrpConfiguration(doelbindingRaadpleegMetDefault = Optional.of("   "))

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

        Given("Doelbinding header name is blank") {
            val brpConfiguration = createBrpConfiguration(
                doelbindingZoekMetDefault = Optional.empty(),
                doelbindingRaadpleegMetDefault = Optional.empty(),
                headerNameDoelbinding = Optional.of("")
            )

            When("configuration is validated") {
                Then("No exception is thrown because doelbinding header is disabled") {
                    brpConfiguration.validateConfiguration()
                }
            }
        }

        Given("Verwerking header name is blank") {
            val brpConfiguration = createBrpConfiguration(
                verwerkingregisterDefault = Optional.empty(),
                headerNameVerwerking = Optional.of("")
            )

            When("configuration is validated") {
                Then("No exception is thrown because verwerking header is disabled") {
                    brpConfiguration.validateConfiguration()
                }
            }
        }

        Given("Toepassing header name is set but toepassing value is missing") {
            val brpConfiguration = createBrpConfiguration(toepassingValue = Optional.empty())

            When("configuration is validated") {
                val exception = shouldThrow<BrpProtocolleringConfigurationException> {
                    brpConfiguration.validateConfiguration()
                }
                Then("Exception is thrown") {
                    exception.message shouldContain "BRP_TOEPASSING"
                }
            }
        }

        Given("Toepassing header name is blank") {
            val brpConfiguration = createBrpConfiguration(
                toepassingValue = Optional.empty(),
                headerNameToepassing = Optional.of("")
            )

            When("configuration is validated") {
                Then("No exception is thrown because toepassing header is disabled") {
                    brpConfiguration.validateConfiguration()
                }
            }
        }

        Given("API key header is set but API key value is missing") {
            val brpConfiguration = createBrpConfiguration(
                apiKey = Optional.empty(),
                headerNameApiKey = Optional.of("x-api-key")
            )

            When("configuration is validated") {
                val exception = shouldThrow<BrpProtocolleringConfigurationException> {
                    brpConfiguration.validateConfiguration()
                }
                Then("Exception is thrown mentioning BRP_API_KEY") {
                    exception.message shouldContain "BRP_API_KEY"
                }
            }
        }

        Given("API key header is blank") {
            val brpConfiguration = createBrpConfiguration(
                apiKey = Optional.empty(),
                headerNameApiKey = Optional.of("")
            )

            When("configuration is validated") {
                Then("No exception is thrown because API key header is disabled") {
                    brpConfiguration.validateConfiguration()
                }
            }
        }
    }

    Context("configuration builders") {
        Given("Username is null for gebruiker") {
            val brpConfiguration = createBrpConfiguration(systemUser = Optional.of("systemUser"))

            When("gebruiker configuration is build without username") {
                val userBrpConfiguration = brpConfiguration.buildUser { null }

                Then("Gebruiker value should return the systemUser") {
                    userBrpConfiguration.getValue() shouldBe "systemUser"
                }
            }
        }
    }

    Context("header name getters") {

        Given("Blank header name env var for doelbinding") {
            val brpConfiguration = createBrpConfiguration(headerNameDoelbinding = Optional.of("   "))

            When("getter is called") {
                val doelbindingRaadpleegMetDefault = brpConfiguration.getDoelbindingRaadpleegMetDefault()
                val doelbindingZoekMetDefault = brpConfiguration.getDoelbindingZoekMetDefault()
                val builtDoelbinding = brpConfiguration.buildDoelbinding { "someDoelbinding" }
                Then("Doelbinding configurations should report invalid") {
                    doelbindingRaadpleegMetDefault.isAvailable().shouldBeFalse()
                    doelbindingZoekMetDefault.isAvailable().shouldBeFalse()
                    builtDoelbinding.isAvailable().shouldBeFalse()
                }
                And(
                    """BrpProtocolleringConfigurationException is thrown on reading 
                    |DoelbindingRaadpleegMetDefault header, because blank headers should not be requested
                    """.trimMargin()
                ) {
                    shouldThrow<BrpProtocolleringConfigurationException> {
                        doelbindingRaadpleegMetDefault.getHeaderName()
                    }
                }
                And("DoelbindingRaadpleegMetDefault value returns null") {
                    doelbindingRaadpleegMetDefault.getValue().shouldBeNull()
                }
                And(
                    """BrpProtocolleringConfigurationException is thrown on reading 
                    |DoelbindingZoekMetDefault header, because blank headers should not be requested
                    """.trimMargin()
                ) {
                    shouldThrow<BrpProtocolleringConfigurationException> {
                        doelbindingZoekMetDefault.getHeaderName()
                    }
                }
                And("DoelbindingZoekMetDefault value returns null") {
                    doelbindingZoekMetDefault.getValue().shouldBeNull()
                }
                And(
                    """BrpProtocolleringConfigurationException is thrown on reading
                    |built Doelbinding header, because blank headers should not be requested
                    """.trimMargin()
                ) {
                    shouldThrow<BrpProtocolleringConfigurationException> {
                        builtDoelbinding.getHeaderName()
                    }
                }
                And("built doelbinding value returns null") {
                    builtDoelbinding.getValue().shouldBeNull()
                }
            }
        }

        Given("Empty header name env var for gebruiker") {
            val brpConfiguration = createBrpConfiguration(headerNameGebruiker = Optional.of(""))

            When("gebruiker configuration is build") {
                val userBrpConfiguration = brpConfiguration.buildUser { "someName" }

                Then("Gebruiker header should be reported as unavailable") {
                    userBrpConfiguration.isAvailable().shouldBeFalse()
                }
                And("Gebruiker header should throw an exception on retrieval") {
                    shouldThrow<BrpProtocolleringConfigurationException> {
                        userBrpConfiguration.getHeaderName()
                    }
                }
                And("Gebruiker value should return null on retrieval") {
                    userBrpConfiguration.getValue().shouldBeNull()
                }
            }
        }
    }

    Context("doelbinding per zaaktype flag") {

        Given("doelbindingPerZaaktype is false") {
            val brpConfiguration = createBrpConfiguration(doelbindingPerZaaktypeEnabled = false)

            When("isDoelbindingPerZaaktypeEnabled is called") {
                val result = brpConfiguration.isDoelbindingPerZaaktypeEnabled()

                Then("false is returned") {
                    result.shouldBeFalse()
                }
            }
        }

        Given("doelbindingPerZaaktype is true and doelbinding header is set") {
            val brpConfiguration = createBrpConfiguration(doelbindingPerZaaktypeEnabled = true)

            When("isDoelbindingPerZaaktypeEnabled is called") {
                val result = brpConfiguration.isDoelbindingPerZaaktypeEnabled()

                Then("true is returned") {
                    result.shouldBeTrue()
                }
            }
        }

        Given("doelbindingPerZaaktype is true but doelbinding header is not set") {
            val brpConfiguration = createBrpConfiguration(
                doelbindingPerZaaktypeEnabled = true,
                headerNameDoelbinding = Optional.of(""),
                doelbindingZoekMetDefault = Optional.empty(),
                doelbindingRaadpleegMetDefault = Optional.empty()
            )

            When("isDoelbindingPerZaaktypeEnabled is called") {
                val result = brpConfiguration.isDoelbindingPerZaaktypeEnabled()

                Then("false is returned because doelbinding header is disabled") {
                    result.shouldBeFalse()
                }
            }
        }
    }
})
