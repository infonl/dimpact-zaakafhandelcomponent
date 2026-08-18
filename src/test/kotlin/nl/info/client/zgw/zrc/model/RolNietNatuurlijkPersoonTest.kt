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
import nl.info.client.zgw.model.createRolNietNatuurlijkPersoonForReads
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

        `when`("equals and hashCode are called") {
            then("innNnpId takes precedence and the instances are equal") {
                rolA shouldBe rolB
                rolA.hashCode() shouldBe rolB.hashCode()
            }
        }
    }

    given("two RolNietNatuurlijkPersoon instances with equal kvkNummer and no innNnpId") {
        val roltype = createRolType()
        val rolA = createRolNietNatuurlijkPersoon(
            rolType = roltype,
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = "fakeKvkNummer")
        )
        val rolB = createRolNietNatuurlijkPersoon(
            rolType = roltype,
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = "fakeKvkNummer")
        )

        `when`("equals and hashCode are called") {
            then("kvkNummer is used and the instances are equal") {
                rolA shouldBe rolB
                rolA.hashCode() shouldBe rolB.hashCode()
            }
        }
    }

    given("two RolNietNatuurlijkPersoon instances with equal vestigingsNummer and no kvkNummer or innNnpId") {
        val roltype = createRolType()
        val rolA = createRolNietNatuurlijkPersoon(
            rolType = roltype,
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                kvkNummer = null,
                vestigingsnummer = "fakeVestigingsNummer"
            )
        )
        val rolB = createRolNietNatuurlijkPersoon(
            rolType = roltype,
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                kvkNummer = null,
                vestigingsnummer = "fakeVestigingsNummer"
            )
        )

        `when`("equals and hashCode are called") {
            then("vestigingsNummer is used and the instances are equal") {
                rolA shouldBe rolB
                rolA.hashCode() shouldBe rolB.hashCode()
            }
        }
    }

    given("two RolNietNatuurlijkPersoon instances with equal annIdentificatie and no other identifiers") {
        val roltype = createRolType()
        val rolA = createRolNietNatuurlijkPersoon(
            rolType = roltype,
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                kvkNummer = null,
                annIdentificatie = "fakeAnnIdentificatie"
            )
        )
        val rolB = createRolNietNatuurlijkPersoon(
            rolType = roltype,
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                kvkNummer = null,
                annIdentificatie = "fakeAnnIdentificatie"
            )
        )

        `when`("equals and hashCode are called") {
            then("annIdentificatie is used and the instances are equal") {
                rolA shouldBe rolB
                rolA.hashCode() shouldBe rolB.hashCode()
            }
        }
    }

    given("two RolNietNatuurlijkPersoon instances with no identifiers at all") {
        val roltype = createRolType()
        val rolA = createRolNietNatuurlijkPersoon(
            rolType = roltype,
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = null)
        )
        val rolB = createRolNietNatuurlijkPersoon(
            rolType = roltype,
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = null)
        )

        `when`("equals and hashCode are called") {
            then("the instances are still considered equal") {
                rolA shouldBe rolB
                rolA.hashCode() shouldBe rolB.hashCode()
            }
        }
    }

    given("a RolNietNatuurlijkPersoon created via the no-arg constructor") {
        val rol = RolNietNatuurlijkPersoon()

        `when`("getNaam and getIdentificatienummer are called") {
            then("both return null since there is no betrokkeneIdentificatie") {
                rol.naam shouldBe null
                rol.identificatienummer shouldBe null
            }
        }
    }

    given("a RolNietNatuurlijkPersoon created via the uuid constructor for reads") {
        val rol = createRolNietNatuurlijkPersoonForReads(
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = "fakeKvkNummer")
        )

        `when`("getIdentificatienummer is called") {
            then("it returns the kvkNummer") {
                rol.identificatienummer shouldBe "fakeKvkNummer"
            }
        }
    }

    given("two RolNietNatuurlijkPersoon instances with the exact same betrokkeneIdentificatie reference") {
        val roltype = createRolType()
        val sharedIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = "fakeKvkNummer")
        val rolA = createRolNietNatuurlijkPersoon(rolType = roltype, nietNatuurlijkPersoonIdentificatie = sharedIdentificatie)
        val rolB = createRolNietNatuurlijkPersoon(rolType = roltype, nietNatuurlijkPersoonIdentificatie = sharedIdentificatie)

        `when`("equals is called") {
            val isEqual = rolA == rolB

            then("the instances are equal via reference identity") {
                isEqual shouldBe true
            }
        }
    }

    given("a RolNietNatuurlijkPersoon with a betrokkeneIdentificatie compared to one without") {
        val roltype = createRolType()
        val rolA = createRolNietNatuurlijkPersoon(
            rolType = roltype,
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = "fakeKvkNummer")
        )
        val rolB = createRolNietNatuurlijkPersoon(rolType = roltype, nietNatuurlijkPersoonIdentificatie = null)

        `when`("equals is called") {
            val isEqual = rolA == rolB

            then("the instances are not equal") {
                isEqual shouldBe false
            }
        }
    }
})
