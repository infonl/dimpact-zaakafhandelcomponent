/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model.zoekobject

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie

class BetrokkeneIdentificationTest : BehaviorSpec({

    Given("a RolNatuurlijkPersoon with identificatienummer") {

        val rol = mockk<RolNatuurlijkPersoon>()

        every { rol.identificatienummer } returns "123456789"

        When("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            Then("it should return a PERSON identification") {
                result shouldBe BetrokkeneIdentification(
                    type = BetrokkeneIdentificationType.PERSON,
                    identification = "123456789"
                )
            }
        }
    }

    Given("a RolNatuurlijkPersoon without identificatienummer") {

        val rol = mockk<RolNatuurlijkPersoon>()

        every { rol.identificatienummer } returns null

        When("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            Then("it should return null") {
                result.shouldBeNull()
            }
        }
    }

    Given("a RolNietNatuurlijkPersoon with kvk and vestigingsnummer") {

        val rol = mockk<RolNietNatuurlijkPersoon>()
        val identificatie = mockk<NietNatuurlijkPersoonIdentificatie>()

        every { rol.betrokkeneIdentificatie } returns identificatie
        every { identificatie.kvkNummer } returns "12345678"
        every { identificatie.vestigingsNummer } returns "000012345678"

        When("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            Then("it should return a KVK_VESTIGING identification") {
                result shouldBe BetrokkeneIdentification(
                    type = BetrokkeneIdentificationType.KVK_VESTIGING,
                    identification = "12345678-000012345678"
                )
            }
        }
    }

    Given("a RolNietNatuurlijkPersoon with blank vestigingsnummer") {

        val rol = mockk<RolNietNatuurlijkPersoon>()
        val identificatie = mockk<NietNatuurlijkPersoonIdentificatie>()

        every { rol.betrokkeneIdentificatie } returns identificatie
        every { identificatie.kvkNummer } returns "12345678"
        every { identificatie.vestigingsNummer } returns ""

        When("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            Then("it should return a KVK identification") {
                result shouldBe BetrokkeneIdentification(
                    type = BetrokkeneIdentificationType.KVK_INSCHRIJVING,
                    identification = "12345678"
                )
            }
        }
    }

    Given("a RolNietNatuurlijkPersoon with null vestigingsnummer") {

        val rol = mockk<RolNietNatuurlijkPersoon>()
        val identificatie = mockk<NietNatuurlijkPersoonIdentificatie>()

        every { rol.betrokkeneIdentificatie } returns identificatie
        every { identificatie.kvkNummer } returns "12345678"
        every { identificatie.vestigingsNummer } returns null

        When("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            Then("it should return a KVK identification") {
                result shouldBe BetrokkeneIdentification(
                    type = BetrokkeneIdentificationType.KVK_INSCHRIJVING,
                    identification = "12345678"
                )
            }
        }
    }

    Given("a RolNietNatuurlijkPersoon with null kvkNummer") {

        val rol = mockk<RolNietNatuurlijkPersoon>()
        val identificatie = mockk<NietNatuurlijkPersoonIdentificatie>()

        every { rol.betrokkeneIdentificatie } returns identificatie
        every { identificatie.kvkNummer } returns null
        every { identificatie.vestigingsNummer } returns "000012345678"

        When("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            Then("it should return null") {
                result.shouldBeNull()
            }
        }
    }

    Given("a RolNietNatuurlijkPersoon without betrokkeneIdentificatie") {

        val rol = mockk<RolNietNatuurlijkPersoon>()

        every { rol.betrokkeneIdentificatie } returns null

        When("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            Then("it should return null") {
                result.shouldBeNull()
            }
        }
    }

    Given("another Rol type with identificatienummer") {

        val rol = mockk<Rol<*>>()

        every { rol.identificatienummer } returns "john.doe"

        When("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            Then("it should return a USER identification") {
                result shouldBe BetrokkeneIdentification(
                    type = BetrokkeneIdentificationType.USER,
                    identification = "john.doe"
                )
            }
        }
    }

    Given("another Rol type without identificatienummer") {

        val rol = mockk<Rol<*>>()

        every { rol.identificatienummer } returns null

        When("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            Then("it should return null") {
                result.shouldBeNull()
            }
        }
    }
})
