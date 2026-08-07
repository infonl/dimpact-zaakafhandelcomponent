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

        `when`("equals is called") {
            val isEqual = rolA == rolB

            then("the instances are equal based on vestigingsNummer alone") {
                isEqual shouldBe true
            }
        }
    }
})
