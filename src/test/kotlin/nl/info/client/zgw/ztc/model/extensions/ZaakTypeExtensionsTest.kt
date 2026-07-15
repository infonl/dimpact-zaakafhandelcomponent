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

    context("Service norm") {
        given("servicenorm was never set") {
            val zaakType = createZaakType(servicenorm = null)

            `when`("calling isServicenormAvailable") {
                val result = zaakType.isServicenormAvailable()

                then("it should return false") {
                    result shouldBe false
                }
            }
        }

        given("servicenorm is not set") {
            val zaakType = createZaakType(servicenorm = "P0Y0M0W0D")

            `when`("calling isServicenormAvailable") {
                val result = zaakType.isServicenormAvailable()

                then("it should return false") {
                    result shouldBe false
                }
            }
        }

        given("servicenorm is set") {
            val zaakType = createZaakType(servicenorm = "P0Y0M0W30D")

            `when`("calling isServicenormAvailable") {
                val result = zaakType.isServicenormAvailable()

                then("it should return true") {
                    result shouldBe true
                }
            }
        }

        given("servicenorm has unexpected format") {
            val zaakType = createZaakType(servicenorm = "P0Y0M0W30D1H")

            `when`("calling isServicenormAvailable") {
                val result = shouldThrow<DateTimeException> {
                    zaakType.isServicenormAvailable()
                }

                then("it should error") {
                    result.message shouldBe "Text cannot be parsed to a Period"
                }
            }
        }
    }

    context("Extension period") {
        given("zaaktype with no extension allowed") {
            val zaakType = createZaakType()

            `when`("extension term days are calculated") {
                val result = zaakType.extensionPeriodDays()

                then("it should be null") {
                    result shouldBe null
                }
            }
        }

        given("zaaktype has extension allowed, but no extension period set") {
            val zaakType = createZaakType(verlengingMogelijk = true)
            `when`("extension term days are calculated") {
                val result = zaakType.extensionPeriodDays()

                then("it should be null") {
                    result shouldBe null
                }
            }
        }

        given("zaaktype has extension allowed, and extension period set") {
            val zaakType = createZaakType(verlengingMogelijk = true, verlengingstermijn = "P10D")
            `when`("extension term days are calculated") {
                val result = zaakType.extensionPeriodDays()

                then("it should be calculated correctly") {
                    result shouldBe 10
                }
            }
        }

        given("zaaktype has extension allowed, and extension period set blank") {
            val zaakType = createZaakType(verlengingMogelijk = true, verlengingstermijn = "  ")
            `when`("extension term days are calculated") {
                val result = zaakType.extensionPeriodDays()

                then("it should be calculated as null") {
                    result shouldBe null
                }
            }
        }
    }
})
