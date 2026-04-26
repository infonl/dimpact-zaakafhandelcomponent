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
    }

    Context("header name getters") {

        Given("Blank header name env var for doelbinding") {
            val brpConfiguration = createBrpConfiguration(headerNameDoelbinding = Optional.of("   "))

            When("getter is called") {
                val name = brpConfiguration.getHeaderNameDoelbinding()

                Then("null is returned") {
                    name.shouldBeNull()
                }
            }
        }

        Given("Empty header name env var for gebruiker") {
            val brpConfiguration = createBrpConfiguration(headerNameGebruiker = Optional.of(""))

            When("getter is called") {
                val name = brpConfiguration.getHeaderNameGebruiker()

                Then("null is returned") {
                    name.shouldBeNull()
                }
            }
        }
    }

    Context("doelbinding per zaaktype flag") {

        Given("doelbindingPerZaaktype is false") {
            val brpConfiguration = createBrpConfiguration(doelbindingPerZaaktype = false)

            When("isDoelbindingPerZaaktype is called") {
                val result = brpConfiguration.isDoelbindingPerZaaktype()

                Then("false is returned") {
                    result.shouldBeFalse()
                }
            }
        }

        Given("doelbindingPerZaaktype is true and doelbinding header is set") {
            val brpConfiguration = createBrpConfiguration(doelbindingPerZaaktype = true)

            When("isDoelbindingPerZaaktype is called") {
                val result = brpConfiguration.isDoelbindingPerZaaktype()

                Then("true is returned") {
                    result.shouldBeTrue()
                }
            }
        }

        Given("doelbindingPerZaaktype is true but doelbinding header is not set") {
            val brpConfiguration = createBrpConfiguration(
                doelbindingPerZaaktype = true,
                headerNameDoelbinding = Optional.of(""),
                doelbindingZoekMetDefault = Optional.empty(),
                doelbindingRaadpleegMetDefault = Optional.empty()
            )

            When("isDoelbindingPerZaaktype is called") {
                val result = brpConfiguration.isDoelbindingPerZaaktype()

                Then("false is returned because doelbinding header is disabled") {
                    result.shouldBeFalse()
                }
            }
        }
    }
})
