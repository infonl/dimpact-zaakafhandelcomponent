/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import nl.info.zac.app.klant.model.klant.IdentificatieType
import java.util.UUID

class BetrokkeneIdentificatieValidatorTest : BehaviorSpec({
    val validator = BetrokkeneIdentificatieValidator()

    context("Validation of BSN type identification") {
        given("A BSN identification with temporaryPersonId set and blank kvkNummer and vestigingsnummer") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.BSN,
                temporaryPersonId = UUID.randomUUID()
            )

            `when`("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                then("it should return true") {
                    result shouldBe true
                }
            }
        }

        given("A BSN identification with temporaryPersonId null") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.BSN,
                temporaryPersonId = null
            )

            `when`("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                then("it should return false") {
                    result shouldBe false
                }
            }
        }

        given("A BSN identification with kvkNummer present") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.BSN,
                temporaryPersonId = UUID.randomUUID(),
                kvkNummer = "fakeKvkNummer"
            )

            `when`("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    context("Validation of VN type identification") {
        given("A VN identification with kvkNummer, vestigingsnummer set and temporaryPersonId null") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.VN,
                kvkNummer = "fakeKvkNummer",
                vestigingsnummer = "fakeVestigingsnummer"
            )

            `when`("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                then("it should return true") {
                    result shouldBe true
                }
            }
        }

        given("A VN identification with blank kvkNummer") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.VN,
                kvkNummer = "",
                vestigingsnummer = "fakeVestigingsnummer"
            )

            `when`("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                then("it should return false") {
                    result shouldBe false
                }
            }
        }

        given("A VN identification with temporaryPersonId set") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.VN,
                kvkNummer = "fakeKvkNummer",
                vestigingsnummer = "fakeVestigingsnummer",
                temporaryPersonId = UUID.randomUUID()
            )

            `when`("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    context("Validation of RSIN type identification") {
        given("A RSIN identification with kvkNummer set, temporaryPersonId null, vestigingsnummer blank") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.RSIN,
                kvkNummer = "fakeKvkNummer"
            )

            `when`("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                then("it should return true") {
                    result shouldBe true
                }
            }
        }

        given("A RSIN identification with vestigingsnummer present") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.RSIN,
                kvkNummer = "fakeKvkNummer",
                vestigingsnummer = "fakeVestigingsnummer"
            )

            `when`("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    context("Validation of null input") {
        given("A null BetrokkeneIdentificatie") {
            `when`("isValid is called with null") {
                val result = validator.isValid(null, mockk())

                then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }
})
