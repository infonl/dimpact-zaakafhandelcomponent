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

    Context("Validation of BSN type identification") {
        Given("A BSN identification with temporaryPersonId set and blank kvkNummer and vestigingsnummer") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.BSN,
                temporaryPersonId = UUID.randomUUID()
            )

            When("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                Then("it should return true") {
                    result shouldBe true
                }
            }
        }

        Given("A BSN identification with temporaryPersonId null") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.BSN,
                temporaryPersonId = null
            )

            When("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("A BSN identification with kvkNummer present") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.BSN,
                temporaryPersonId = UUID.randomUUID(),
                kvkNummer = "fakeKvkNummer"
            )

            When("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    Context("Validation of VN type identification") {
        Given("A VN identification with kvkNummer, vestigingsnummer set and temporaryPersonId null") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.VN,
                kvkNummer = "fakeKvkNummer",
                vestigingsnummer = "fakeVestigingsnummer"
            )

            When("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                Then("it should return true") {
                    result shouldBe true
                }
            }
        }

        Given("A VN identification with blank kvkNummer") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.VN,
                kvkNummer = "",
                vestigingsnummer = "fakeVestigingsnummer"
            )

            When("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("A VN identification with temporaryPersonId set") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.VN,
                kvkNummer = "fakeKvkNummer",
                vestigingsnummer = "fakeVestigingsnummer",
                temporaryPersonId = UUID.randomUUID()
            )

            When("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    Context("Validation of RSIN type identification") {
        Given("A RSIN identification with kvkNummer set, temporaryPersonId null, vestigingsnummer blank") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.RSIN,
                kvkNummer = "fakeKvkNummer"
            )

            When("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                Then("it should return true") {
                    result shouldBe true
                }
            }
        }

        Given("A RSIN identification with vestigingsnummer present") {
            val identificatie = BetrokkeneIdentificatie(
                type = IdentificatieType.RSIN,
                kvkNummer = "fakeKvkNummer",
                vestigingsnummer = "fakeVestigingsnummer"
            )

            When("isValid is called") {
                val result = validator.isValid(identificatie, mockk())

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    Context("Validation of null input") {
        Given("A null BetrokkeneIdentificatie") {
            When("isValid is called with null") {
                val result = validator.isValid(null, mockk())

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }
})
