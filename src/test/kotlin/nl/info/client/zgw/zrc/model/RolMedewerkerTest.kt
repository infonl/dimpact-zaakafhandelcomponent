/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.client.zgw.model.createMedewerkerIdentificatie
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolMedewerkerForReads
import nl.info.client.zgw.ztc.model.createRolType

class RolMedewerkerTest : BehaviorSpec({
    afterEach { checkUnnecessaryStub() }

    given("a MedewerkerIdentificatie with achternaam, voorletters and voorvoegselAchternaam") {
        val rol = createRolMedewerker(
            medewerkerIdentificatie = createMedewerkerIdentificatie(
                identificatie = "fakeIdentificatie",
                achternaam = "Berg",
                voorletters = "J.",
                voorvoegselAchternaam = "van"
            )
        )

        `when`("getNaam is called") {
            val naam = rol.naam

            then("it composes voorletters, voorvoegselAchternaam and achternaam") {
                naam shouldBe "J. van Berg"
            }
        }
    }

    given("a MedewerkerIdentificatie with a blank achternaam") {
        val rol = createRolMedewerker(
            medewerkerIdentificatie = createMedewerkerIdentificatie(
                identificatie = "fakeIdentificatie",
                achternaam = "",
                voorletters = "J."
            )
        )

        `when`("getNaam is called") {
            val naam = rol.naam

            then("it falls back to the identificatie") {
                naam shouldBe "fakeIdentificatie"
            }
        }
    }

    given("a RolMedewerker without a betrokkeneIdentificatie") {
        val rol = createRolMedewerker(medewerkerIdentificatie = null)

        `when`("getNaam and getIdentificatienummer are called") {
            then("both return null") {
                rol.naam shouldBe null
                rol.identificatienummer shouldBe null
            }
        }
    }

    given("two RolMedewerker instances without a betrokkeneIdentificatie") {
        val roltype = createRolType()
        val rolA = createRolMedewerker(rolType = roltype, medewerkerIdentificatie = null)
        val rolB = createRolMedewerker(rolType = roltype, medewerkerIdentificatie = null)

        `when`("equals and hashCode are called") {
            then("the instances are equal and share the same -1 identity hashCode contribution") {
                rolA shouldBe rolB
                rolA.hashCode() shouldBe rolB.hashCode()
            }
        }
    }

    given("two RolMedewerker instances with equal identificatie") {
        val roltype = createRolType()
        val rolA = createRolMedewerker(
            rolType = roltype,
            medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeIdentificatie", achternaam = "Berg")
        )
        val rolB = createRolMedewerker(
            rolType = roltype,
            medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeIdentificatie", achternaam = "Different")
        )

        `when`("equals is called") {
            val isEqual = rolA == rolB

            then("the instances are equal regardless of other fields") {
                isEqual shouldBe true
            }
        }
    }

    given("a RolMedewerker created via the no-arg constructor") {
        val rol = RolMedewerker()

        `when`("getNaam and getIdentificatienummer are called") {
            then("both return null since there is no betrokkeneIdentificatie") {
                rol.naam shouldBe null
                rol.identificatienummer shouldBe null
            }
        }
    }

    given("a RolMedewerker created via the uuid constructor for reads") {
        val rol = createRolMedewerkerForReads(
            medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeIdentificatie")
        )

        `when`("getIdentificatienummer is called") {
            then("it returns the identificatie") {
                rol.identificatienummer shouldBe "fakeIdentificatie"
            }
        }
    }

    given("two RolMedewerker instances with the exact same betrokkeneIdentificatie reference") {
        val roltype = createRolType()
        val sharedIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeIdentificatie")
        val rolA = createRolMedewerker(rolType = roltype, medewerkerIdentificatie = sharedIdentificatie)
        val rolB = createRolMedewerker(rolType = roltype, medewerkerIdentificatie = sharedIdentificatie)

        `when`("equals is called") {
            val isEqual = rolA == rolB

            then("the instances are equal via reference identity") {
                isEqual shouldBe true
            }
        }
    }

    given("a RolMedewerker with a betrokkeneIdentificatie compared to one without") {
        val roltype = createRolType()
        val rolA = createRolMedewerker(
            rolType = roltype,
            medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeIdentificatie")
        )
        val rolB = createRolMedewerker(rolType = roltype, medewerkerIdentificatie = null)

        `when`("equals is called") {
            val isEqual = rolA == rolB

            then("the instances are not equal") {
                isEqual shouldBe false
            }
        }
    }
})
