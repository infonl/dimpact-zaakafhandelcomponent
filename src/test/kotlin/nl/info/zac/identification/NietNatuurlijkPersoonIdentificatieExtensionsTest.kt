/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identification

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.model.createNietNatuurlijkPersoonIdentificatie
import nl.info.zac.app.klant.model.klant.IdentificatieType

class NietNatuurlijkPersoonIdentificatieExtensionsTest : BehaviorSpec({
    Given("a niet-natuurlijk persoon identificatie with only an RSIN") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            innNnpId = "fakeRsin",
            kvkNummer = null
        )

        When("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            Then("it is RSIN") {
                identificatieType shouldBe IdentificatieType.RSIN
            }
        }
    }

    Given("a niet-natuurlijk persoon identificatie with only a KVK number") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            kvkNummer = "fakeKvkNummer"
        )

        When("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            Then("it is RSIN") {
                identificatieType shouldBe IdentificatieType.RSIN
            }
        }
    }

    Given("a niet-natuurlijk persoon identificatie with a KVK number and a vestigingsnummer") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            kvkNummer = "fakeKvkNummer",
            vestigingsnummer = "fakeVestigingsnummer"
        )

        When("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            Then("it is VN") {
                identificatieType shouldBe IdentificatieType.VN
            }
        }
    }

    Given("a niet-natuurlijk persoon identificatie without an RSIN, KVK number or vestigingsnummer") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            kvkNummer = " ",
            annIdentificatie = "fakeAnnIdentificatie"
        )

        When("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            Then("it is unknown") {
                identificatieType shouldBe null
            }
        }
    }
})
