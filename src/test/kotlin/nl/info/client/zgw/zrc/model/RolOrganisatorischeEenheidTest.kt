/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.client.zgw.model.createOrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createRolOrganisatorischeEenheidForReads
import nl.info.client.zgw.ztc.model.createRolType

class RolOrganisatorischeEenheidTest : BehaviorSpec({
    afterEach { checkUnnecessaryStub() }

    given("an OrganisatorischeEenheidIdentificatie with a blank naam") {
        val rol = createRolOrganisatorischeEenheid(
            organisatorischeEenheidIdentificatie = createOrganisatorischeEenheidIdentificatie(
                identificatie = "fakeIdentificatie",
                naam = ""
            )
        )

        `when`("getNaam is called") {
            val naam = rol.naam

            then("it falls back to the identificatienummer") {
                naam shouldBe "fakeIdentificatie"
            }
        }
    }

    given("an OrganisatorischeEenheidIdentificatie with a populated naam") {
        val rol = createRolOrganisatorischeEenheid(
            organisatorischeEenheidIdentificatie = createOrganisatorischeEenheidIdentificatie(
                identificatie = "fakeIdentificatie",
                naam = "fakeNaam"
            )
        )

        `when`("getNaam and getIdentificatienummer are called") {
            then("getNaam returns the naam and getIdentificatienummer returns the identificatie") {
                rol.naam shouldBe "fakeNaam"
                rol.identificatienummer shouldBe "fakeIdentificatie"
            }
        }
    }

    given("two RolOrganisatorischeEenheid instances with equal identificatie") {
        val roltype = createRolType()
        val rolA = createRolOrganisatorischeEenheid(
            rolType = roltype,
            organisatorischeEenheidIdentificatie = createOrganisatorischeEenheidIdentificatie(identificatie = "fakeIdentificatie")
        )
        val rolB = createRolOrganisatorischeEenheid(
            rolType = roltype,
            organisatorischeEenheidIdentificatie = createOrganisatorischeEenheidIdentificatie(identificatie = "fakeIdentificatie")
        )

        `when`("equals and hashCode are called") {
            then("the instances are equal and have equal hashCodes") {
                rolA shouldBe rolB
                rolA.hashCode() shouldBe rolB.hashCode()
            }
        }
    }

    given("a RolOrganisatorischeEenheid created via the no-arg constructor") {
        val rol = RolOrganisatorischeEenheid()

        `when`("getNaam and getIdentificatienummer are called") {
            then("both return null since there is no betrokkeneIdentificatie") {
                rol.naam shouldBe null
                rol.identificatienummer shouldBe null
            }
        }
    }

    given("a RolOrganisatorischeEenheid created via the uuid constructor for reads") {
        val rol = createRolOrganisatorischeEenheidForReads(
            organisatorischeEenheidIdentificatie = createOrganisatorischeEenheidIdentificatie(identificatie = "fakeIdentificatie")
        )

        `when`("getIdentificatienummer is called") {
            then("it returns the identificatie") {
                rol.identificatienummer shouldBe "fakeIdentificatie"
            }
        }
    }

    given("two RolOrganisatorischeEenheid instances with the exact same betrokkeneIdentificatie reference") {
        val roltype = createRolType()
        val sharedIdentificatie = createOrganisatorischeEenheidIdentificatie(identificatie = "fakeIdentificatie")
        val rolA = createRolOrganisatorischeEenheid(rolType = roltype, organisatorischeEenheidIdentificatie = sharedIdentificatie)
        val rolB = createRolOrganisatorischeEenheid(rolType = roltype, organisatorischeEenheidIdentificatie = sharedIdentificatie)

        `when`("equals is called") {
            val isEqual = rolA == rolB

            then("the instances are equal via reference identity") {
                isEqual shouldBe true
            }
        }
    }

    given("a RolOrganisatorischeEenheid with a betrokkeneIdentificatie compared to one without") {
        val roltype = createRolType()
        val rolA = createRolOrganisatorischeEenheid(
            rolType = roltype,
            organisatorischeEenheidIdentificatie = createOrganisatorischeEenheidIdentificatie(identificatie = "fakeIdentificatie")
        )
        val rolB = createRolOrganisatorischeEenheid(rolType = roltype, organisatorischeEenheidIdentificatie = null)

        `when`("equals is called") {
            val isEqual = rolA == rolB

            then("the instances are not equal") {
                isEqual shouldBe false
            }
        }
    }
})
