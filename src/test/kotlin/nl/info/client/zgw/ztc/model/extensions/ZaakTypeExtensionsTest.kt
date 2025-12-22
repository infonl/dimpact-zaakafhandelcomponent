/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.ztc.model.extensions

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.ztc.model.createZaakType
import java.time.DateTimeException

class ZaakTypeExtensionsTest : BehaviorSpec({

    Context("Service norm") {
        Given("servicenorm was never set") {
            val zaakType = createZaakType(servicenorm = null)

            When("calling isServicenormAvailable") {
                val result = zaakType.isServicenormAvailable()

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("servicenorm is not set") {
            val zaakType = createZaakType(servicenorm = "P0Y0M0W0D")

            When("calling isServicenormAvailable") {
                val result = zaakType.isServicenormAvailable()

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("servicenorm is set") {
            val zaakType = createZaakType(servicenorm = "P0Y0M0W30D")

            When("calling isServicenormAvailable") {
                val result = zaakType.isServicenormAvailable()

                Then("it should return true") {
                    result shouldBe true
                }
            }
        }

        Given("servicenorm has unexpected format") {
            val zaakType = createZaakType(servicenorm = "P0Y0M0W30D1H")

            When("calling isServicenormAvailable") {
                val result = shouldThrow<DateTimeException> {
                    zaakType.isServicenormAvailable()
                }

                Then("it should error") {
                    result.message shouldBe "Text cannot be parsed to a Period"
                }
            }
        }
    }

    Context("Extension period") {
        Given("zaaktype with no extension allowed") {
            val zaakType = createZaakType()

            When("extension term days are calculated") {
                val result = zaakType.extensionPeriodDays()

                Then("it should be null") {
                    result shouldBe null
                }
            }
        }

        Given("zaaktype has extension allowed, but no extension period set") {
            val zaakType = createZaakType(verlengingMogelijk = true)
            When("extension term days are calculated") {
                val result = zaakType.extensionPeriodDays()

                Then("it should be null") {
                    result shouldBe null
                }
            }
        }

        Given("zaaktype has extension allowed, and extension period set") {
            val zaakType = createZaakType(verlengingMogelijk = true, verlengingstermijn = "P10D")
            When("extension term days are calculated") {
                val result = zaakType.extensionPeriodDays()

                Then("it should be calculated correctly") {
                    result shouldBe 10
                }
            }
        }

        Given("zaaktype has extension allowed, and extension period set blank") {
            val zaakType = createZaakType(verlengingMogelijk = true, verlengingstermijn = "  ")
            When("extension term days are calculated") {
                val result = zaakType.extensionPeriodDays()

                Then("it should be calculated as null") {
                    result shouldBe null
                }
            }
        }
    }
})
