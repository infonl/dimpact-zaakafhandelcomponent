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

    given("a RolNatuurlijkPersoon with identificatienummer") {

        val rol = mockk<RolNatuurlijkPersoon>()

        every { rol.identificatienummer } returns "123456789"

        `when`("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            then("it should return a PERSON identification") {
                result shouldBe BetrokkeneIdentification(
                    type = BetrokkeneIdentificationType.PERSON,
                    identification = "123456789"
                )
            }
        }
    }

    given("a RolNatuurlijkPersoon without identificatienummer") {

        val rol = mockk<RolNatuurlijkPersoon>()

        every { rol.identificatienummer } returns null

        `when`("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            then("it should return null") {
                result.shouldBeNull()
            }
        }
    }

    given("a RolNietNatuurlijkPersoon with kvk and vestigingsnummer") {

        val rol = mockk<RolNietNatuurlijkPersoon>()
        val identificatie = mockk<NietNatuurlijkPersoonIdentificatie>()

        every { rol.betrokkeneIdentificatie } returns identificatie
        every { identificatie.kvkNummer } returns "12345678"
        every { identificatie.vestigingsNummer } returns "000012345678"

        `when`("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            then("it should return a KVK_VESTIGING identification") {
                result shouldBe BetrokkeneIdentification(
                    type = BetrokkeneIdentificationType.KVK_VESTIGING,
                    identification = "12345678-000012345678"
                )
            }
        }
    }

    given("a RolNietNatuurlijkPersoon with blank vestigingsnummer") {

        val rol = mockk<RolNietNatuurlijkPersoon>()
        val identificatie = mockk<NietNatuurlijkPersoonIdentificatie>()

        every { rol.betrokkeneIdentificatie } returns identificatie
        every { identificatie.kvkNummer } returns "12345678"
        every { identificatie.vestigingsNummer } returns ""

        `when`("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            then("it should return a KVK identification") {
                result shouldBe BetrokkeneIdentification(
                    type = BetrokkeneIdentificationType.KVK_INSCHRIJVING,
                    identification = "12345678"
                )
            }
        }
    }

    given("a RolNietNatuurlijkPersoon with null vestigingsnummer") {

        val rol = mockk<RolNietNatuurlijkPersoon>()
        val identificatie = mockk<NietNatuurlijkPersoonIdentificatie>()

        every { rol.betrokkeneIdentificatie } returns identificatie
        every { identificatie.kvkNummer } returns "12345678"
        every { identificatie.vestigingsNummer } returns null

        `when`("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            then("it should return a KVK identification") {
                result shouldBe BetrokkeneIdentification(
                    type = BetrokkeneIdentificationType.KVK_INSCHRIJVING,
                    identification = "12345678"
                )
            }
        }
    }

    given("a RolNietNatuurlijkPersoon with null kvkNummer") {

        val rol = mockk<RolNietNatuurlijkPersoon>()
        val identificatie = mockk<NietNatuurlijkPersoonIdentificatie>()

        every { rol.betrokkeneIdentificatie } returns identificatie
        every { identificatie.kvkNummer } returns null
        every { identificatie.vestigingsNummer } returns "000012345678"

        `when`("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            then("it should return null") {
                result.shouldBeNull()
            }
        }
    }

    given("a RolNietNatuurlijkPersoon without betrokkeneIdentificatie") {

        val rol = mockk<RolNietNatuurlijkPersoon>()

        every { rol.betrokkeneIdentificatie } returns null

        `when`("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            then("it should return null") {
                result.shouldBeNull()
            }
        }
    }

    given("another Rol type with identificatienummer") {

        val rol = mockk<Rol<*>>()

        every { rol.identificatienummer } returns "john.doe"

        `when`("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            then("it should return a USER identification") {
                result shouldBe BetrokkeneIdentification(
                    type = BetrokkeneIdentificationType.USER,
                    identification = "john.doe"
                )
            }
        }
    }

    given("another Rol type without identificatienummer") {

        val rol = mockk<Rol<*>>()

        every { rol.identificatienummer } returns null

        `when`("converting to BetrokkeneIdentification") {
            val result = rol.toBetrokkeneIdentification()

            then("it should return null") {
                result.shouldBeNull()
            }
        }
    }
})
