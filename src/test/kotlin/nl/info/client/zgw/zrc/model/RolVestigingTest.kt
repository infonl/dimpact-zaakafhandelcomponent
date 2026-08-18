/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.client.zgw.model.createRolVestiging
import nl.info.client.zgw.model.createVestigingIdentificatie
import nl.info.client.zgw.ztc.model.createRolType

class RolVestigingTest : BehaviorSpec({
    afterEach { checkUnnecessaryStub() }

    given("a VestigingIdentificatie with multiple handelsnaam entries") {
        val rol = createRolVestiging(
            vestigingIdentificatie = createVestigingIdentificatie(
                vestigingsNummer = "fakeVestigingsNummer",
                handelsnaam = listOf("Bakkerij Jansen", "Jansen Beheer")
            )
        )

        `when`("getNaam is called") {
            val naam = rol.naam

            then("it joins the handelsnaam entries with '; '") {
                naam shouldBe "Bakkerij Jansen; Jansen Beheer"
            }
        }
    }

    given("a VestigingIdentificatie without a handelsnaam") {
        val rol = createRolVestiging(
            vestigingIdentificatie = createVestigingIdentificatie(
                vestigingsNummer = "fakeVestigingsNummer",
                handelsnaam = null
            )
        )

        `when`("getNaam is called") {
            val naam = rol.naam

            then("it falls back to the identificatienummer (vestigingsNummer)") {
                naam shouldBe "fakeVestigingsNummer"
            }
        }
    }

    given("two RolVestiging instances with equal vestigingsNummer but different handelsnaam") {
        val roltype = createRolType()
        val rolA = createRolVestiging(
            rolType = roltype,
            vestigingIdentificatie = createVestigingIdentificatie(vestigingsNummer = "fakeVestigingsNummer", handelsnaam = listOf("A"))
        )
        val rolB = createRolVestiging(
            rolType = roltype,
            vestigingIdentificatie = createVestigingIdentificatie(vestigingsNummer = "fakeVestigingsNummer", handelsnaam = listOf("B"))
        )

        `when`("equals and hashCode are called") {
            then("the instances are equal based on vestigingsNummer alone") {
                rolA shouldBe rolB
                rolA.hashCode() shouldBe rolB.hashCode()
            }
        }
    }

    given("a RolVestiging created via the no-arg constructor") {
        val rol = RolVestiging()

        `when`("getNaam and getIdentificatienummer are called") {
            then("both return null since there is no betrokkeneIdentificatie") {
                rol.naam shouldBe null
                rol.identificatienummer shouldBe null
            }
        }
    }

    given("two RolVestiging instances with the exact same betrokkeneIdentificatie reference") {
        val roltype = createRolType()
        val sharedIdentificatie = createVestigingIdentificatie(vestigingsNummer = "fakeVestigingsNummer")
        val rolA = createRolVestiging(rolType = roltype, vestigingIdentificatie = sharedIdentificatie)
        val rolB = createRolVestiging(rolType = roltype, vestigingIdentificatie = sharedIdentificatie)

        `when`("equals is called") {
            val isEqual = rolA == rolB

            then("the instances are equal via reference identity") {
                isEqual shouldBe true
            }
        }
    }

    given("a RolVestiging with a betrokkeneIdentificatie compared to one without") {
        val roltype = createRolType()
        val rolA = createRolVestiging(
            rolType = roltype,
            vestigingIdentificatie = createVestigingIdentificatie(vestigingsNummer = "fakeVestigingsNummer")
        )
        val rolB = createRolVestiging(rolType = roltype, vestigingIdentificatie = null)

        `when`("equals is called") {
            val isEqual = rolA == rolB

            then("the instances are not equal") {
                isEqual shouldBe false
            }
        }
    }
})
