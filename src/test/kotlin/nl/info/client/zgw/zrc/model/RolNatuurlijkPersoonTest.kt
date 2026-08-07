/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.client.zgw.model.createNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolNatuurlijkPersoonForReads
import nl.info.client.zgw.ztc.model.createRolType

class RolNatuurlijkPersoonTest : BehaviorSpec({
    afterEach { checkUnnecessaryStub() }

    given("a RolNatuurlijkPersoon with only inpBsn populated") {
        val rol = createRolNatuurlijkPersoon(
            natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsn")
        )

        `when`("getIdentificatienummer is called") {
            val identificatienummer = rol.identificatienummer

            then("it returns the inpBsn") {
                identificatienummer shouldBe "fakeBsn"
            }
        }

        `when`("getNaam is called and voorvoegselGeslachtsnaam is blank") {
            val naam = rol.naam

            then("it falls back to the identificatienummer") {
                naam shouldBe rol.identificatienummer
            }
        }
    }

    given("two RolNatuurlijkPersoon instances with equal inpBsn and no anpIdentificatie/inpANummer") {
        val roltype = createRolType()
        val identificatieA = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsn")
        val identificatieB = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsn")
        val rolA = createRolNatuurlijkPersoon(rolType = roltype, natuurlijkPersoonIdentificatie = identificatieA)
        val rolB = createRolNatuurlijkPersoon(rolType = roltype, natuurlijkPersoonIdentificatie = identificatieB)

        `when`("equals and hashCode are called") {
            then("the instances are equal and have equal hashCodes") {
                rolA shouldBe rolB
                rolA.hashCode() shouldBe rolB.hashCode()
            }
        }
    }

    given("two RolNatuurlijkPersoon instances with a different roltype but equal identity") {
        val identificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsn")
        val rolA = createRolNatuurlijkPersoon(rolType = createRolType(), natuurlijkPersoonIdentificatie = identificatie)
        val rolB = createRolNatuurlijkPersoon(rolType = createRolType(), natuurlijkPersoonIdentificatie = identificatie)

        `when`("equals is called") {
            val isEqual = rolA == rolB

            then("the instances are not equal") {
                isEqual shouldBe false
            }
        }
    }

    given("a RolNatuurlijkPersoon compared to an unrelated Rol subclass") {
        val roltype = createRolType()
        val rolNatuurlijkPersoon = createRolNatuurlijkPersoon(
            rolType = roltype,
            natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie()
        )
        val rolMedewerker = createRolMedewerker(rolType = roltype)

        `when`("equals is called") {
            val isEqual = rolNatuurlijkPersoon.equals(rolMedewerker)

            then("the instances are not equal") {
                isEqual shouldBe false
            }
        }
    }

    given("a RolNatuurlijkPersoon created via the no-arg constructor") {
        val rol = RolNatuurlijkPersoon()

        `when`("getNaam and getIdentificatienummer are called") {
            then("both return null since there is no betrokkeneIdentificatie") {
                rol.naam shouldBe null
                rol.identificatienummer shouldBe null
            }
        }
    }

    given("a RolNatuurlijkPersoon created via the uuid constructor for reads") {
        val rol = createRolNatuurlijkPersoonForReads(
            natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsn")
        )

        `when`("getIdentificatienummer is called") {
            then("it returns the inpBsn") {
                rol.identificatienummer shouldBe "fakeBsn"
            }
        }
    }

    given("a RolNatuurlijkPersoon with a populated voorvoegselGeslachtsnaam") {
        val rol = createRolNatuurlijkPersoon(
            natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(
                bsn = "fakeBsn",
                voorvoegselGeslachtsnaam = "van der"
            )
        )

        `when`("getNaam is called") {
            val naam = rol.naam

            then("it returns the voorvoegselGeslachtsnaam") {
                naam shouldBe "van der"
            }
        }
    }

    given("two RolNatuurlijkPersoon instances with equal anpIdentificatie but different inpBsn") {
        val roltype = createRolType()
        val rolA = createRolNatuurlijkPersoon(
            rolType = roltype,
            natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsnA", anpIdentificatie = "fakeAnp")
        )
        val rolB = createRolNatuurlijkPersoon(
            rolType = roltype,
            natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsnB", anpIdentificatie = "fakeAnp")
        )

        `when`("equals and hashCode are called") {
            then("anpIdentificatie takes precedence and the instances are equal") {
                rolA shouldBe rolB
                rolA.hashCode() shouldBe rolB.hashCode()
            }
        }
    }

    given("two RolNatuurlijkPersoon instances with equal inpANummer but different inpBsn and no anpIdentificatie") {
        val roltype = createRolType()
        val rolA = createRolNatuurlijkPersoon(
            rolType = roltype,
            natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsnA", inpANummer = "fakeANummer")
        )
        val rolB = createRolNatuurlijkPersoon(
            rolType = roltype,
            natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsnB", inpANummer = "fakeANummer")
        )

        `when`("equals and hashCode are called") {
            then("inpANummer takes precedence and the instances are equal") {
                rolA shouldBe rolB
                rolA.hashCode() shouldBe rolB.hashCode()
            }
        }
    }

    given("two RolNatuurlijkPersoon instances with the exact same betrokkeneIdentificatie reference") {
        val roltype = createRolType()
        val sharedIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsn")
        val rolA = createRolNatuurlijkPersoon(rolType = roltype, natuurlijkPersoonIdentificatie = sharedIdentificatie)
        val rolB = createRolNatuurlijkPersoon(rolType = roltype, natuurlijkPersoonIdentificatie = sharedIdentificatie)

        `when`("equals is called") {
            val isEqual = rolA == rolB

            then("the instances are equal via reference identity") {
                isEqual shouldBe true
            }
        }
    }

    given("a RolNatuurlijkPersoon with a betrokkeneIdentificatie compared to one without") {
        val roltype = createRolType()
        val rolA = createRolNatuurlijkPersoon(
            rolType = roltype,
            natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsn")
        )
        val rolB = createRolNatuurlijkPersoon(rolType = roltype, natuurlijkPersoonIdentificatie = null)

        `when`("equals is called") {
            val isEqual = rolA == rolB

            then("the instances are not equal") {
                isEqual shouldBe false
            }
        }
    }
})
