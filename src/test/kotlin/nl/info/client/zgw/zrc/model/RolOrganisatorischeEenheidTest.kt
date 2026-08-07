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
})
