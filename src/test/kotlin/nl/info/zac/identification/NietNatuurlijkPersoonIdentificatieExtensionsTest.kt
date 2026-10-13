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
    given("a niet-natuurlijk persoon identificatie with only an RSIN") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            innNnpId = "fakeRsin",
            kvkNummer = null
        )

        `when`("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            then("it is RSIN") {
                identificatieType shouldBe IdentificatieType.RSIN
            }
        }
    }

    given("a niet-natuurlijk persoon identificatie with only a KVK number") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            kvkNummer = "fakeKvkNummer"
        )

        `when`("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            then("it is RSIN") {
                identificatieType shouldBe IdentificatieType.RSIN
            }
        }
    }

    given("a niet-natuurlijk persoon identificatie with a KVK number and a vestigingsnummer") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            kvkNummer = "fakeKvkNummer",
            vestigingsnummer = "fakeVestigingsnummer"
        )

        `when`("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            then("it is VN") {
                identificatieType shouldBe IdentificatieType.VN
            }
        }
    }

    given("a niet-natuurlijk persoon identificatie with an RSIN, a KVK number and a vestigingsnummer") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            innNnpId = "fakeRsin",
            kvkNummer = "fakeKvkNummer",
            vestigingsnummer = "fakeVestigingsnummer"
        )

        `when`("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            then("it is VN, because the vestigingsnummer takes precedence over the RSIN") {
                identificatieType shouldBe IdentificatieType.VN
            }
        }
    }

    given("a niet-natuurlijk persoon identificatie without an RSIN, KVK number or vestigingsnummer") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            kvkNummer = " ",
            annIdentificatie = "fakeAnnIdentificatie"
        )

        `when`("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            then("it is unknown") {
                identificatieType shouldBe null
            }
        }
    }
})
