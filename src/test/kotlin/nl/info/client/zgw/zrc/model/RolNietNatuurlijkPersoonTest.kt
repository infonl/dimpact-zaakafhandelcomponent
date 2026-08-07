/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.client.zgw.model.createNietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createRolNietNatuurlijkPersoon
import nl.info.client.zgw.ztc.model.createRolType

class RolNietNatuurlijkPersoonTest : BehaviorSpec({
    afterEach { checkUnnecessaryStub() }

    given("a KVK-only initiator (kvkNummer populated, vestigingsNummer blank)") {
        val rol = createRolNietNatuurlijkPersoon(
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                kvkNummer = "fakeKvkNummer",
                vestigingsnummer = null
            )
        )

        `when`("getIdentificatienummer is called") {
            val identificatienummer = rol.identificatienummer

            then("it returns the kvkNummer") {
                identificatienummer shouldBe "fakeKvkNummer"
            }
        }
    }

    given("a legacy RSIN-only initiator (kvkNummer blank, innNnpId populated)") {
        val rol = createRolNietNatuurlijkPersoon(
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                innNnpId = "fakeInnNnpId",
                kvkNummer = null
            )
        )

        `when`("getIdentificatienummer is called") {
            val identificatienummer = rol.identificatienummer

            then("it returns the innNnpId") {
                identificatienummer shouldBe "fakeInnNnpId"
            }
        }
    }

    given("a vestiging-type initiator with both kvkNummer and vestigingsNummer") {
        val rol = createRolNietNatuurlijkPersoon(
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                kvkNummer = "fakeKvkNummer",
                vestigingsnummer = "fakeVestigingsNummer"
            )
        )

        `when`("getIdentificatienummer is called") {
            val identificatienummer = rol.identificatienummer

            then("it returns the vestigingsNummer") {
                identificatienummer shouldBe "fakeVestigingsNummer"
            }
        }
    }

    given("a NietNatuurlijkPersoonIdentificatie with a blank statutaireNaam") {
        val rol = createRolNietNatuurlijkPersoon(
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = "fakeKvkNummer")
        )

        `when`("getNaam is called") {
            val naam = rol.naam

            then("it falls back to the identificatienummer") {
                naam shouldBe rol.identificatienummer
            }
        }
    }

    given("two RolNietNatuurlijkPersoon instances with equal innNnpId but different kvkNummer") {
        val roltype = createRolType()
        val rolA = createRolNietNatuurlijkPersoon(
            rolType = roltype,
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                innNnpId = "fakeInnNnpId",
                kvkNummer = "fakeKvkNummerA"
            )
        )
        val rolB = createRolNietNatuurlijkPersoon(
            rolType = roltype,
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                innNnpId = "fakeInnNnpId",
                kvkNummer = "fakeKvkNummerB"
            )
        )

        `when`("equals is called") {
            val isEqual = rolA == rolB

            then("innNnpId takes precedence and the instances are equal") {
                isEqual shouldBe true
            }
        }
    }
})
