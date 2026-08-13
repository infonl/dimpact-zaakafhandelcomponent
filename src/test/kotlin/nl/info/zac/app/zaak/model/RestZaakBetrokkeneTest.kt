/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.model.createNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createNietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createOrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolNietNatuurlijkPersoon
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createRolVestiging
import nl.info.client.zgw.zrc.model.RolMedewerker
import nl.info.client.zgw.zrc.model.generated.VestigingIdentificatie
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.identification.IdentificationService
import java.util.UUID

class RestZaakBetrokkeneTest : BehaviorSpec({
    afterEach { checkUnnecessaryStub() }

    given("a RolNatuurlijkPersoon with a bsn and no identificationService") {
        val rol = createRolNatuurlijkPersoon(natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsn"))

        `when`("toRestZaakBetrokkene is called") {
            val restZaakBetrokkene = rol.toRestZaakBetrokkene()

            then("it returns a RestZaakBetrokkene with the bsn and no temporaryPersonId") {
                restZaakBetrokkene?.bsn shouldBe "fakeBsn"
                restZaakBetrokkene?.identificatieType shouldBe IdentificatieType.BSN
                restZaakBetrokkene?.temporaryPersonId.shouldBeNull()
            }
        }
    }

    given("a RolNatuurlijkPersoon with a bsn and an identificationService") {
        val rol = createRolNatuurlijkPersoon(natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsn"))
        val temporaryPersonId = UUID.randomUUID()
        val identificationService = mockk<IdentificationService> {
            every { replaceBsnWithKey("fakeBsn") } returns temporaryPersonId
        }

        `when`("toRestZaakBetrokkene is called") {
            val restZaakBetrokkene = rol.toRestZaakBetrokkene(identificationService)

            then("it returns a RestZaakBetrokkene with the replaced temporaryPersonId") {
                restZaakBetrokkene?.temporaryPersonId shouldBe temporaryPersonId
            }
        }
    }

    given("a RolNatuurlijkPersoon without a betrokkeneIdentificatie") {
        val rol = createRolNatuurlijkPersoon(natuurlijkPersoonIdentificatie = null)

        `when`("toRestZaakBetrokkene is called") {
            val restZaakBetrokkene = rol.toRestZaakBetrokkene()

            then("it returns null") {
                restZaakBetrokkene.shouldBeNull()
            }
        }
    }

    given("a RolNietNatuurlijkPersoon with a vestigingsnummer") {
        val rol = createRolNietNatuurlijkPersoon(
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                vestigingsnummer = "fakeVestigingsnummer",
                kvkNummer = "fakeKvkNummer"
            )
        )

        `when`("toRestZaakBetrokkene is called") {
            val restZaakBetrokkene = rol.toRestZaakBetrokkene()

            then("it returns a RestZaakBetrokkene with identificatieType VN") {
                restZaakBetrokkene?.identificatieType shouldBe IdentificatieType.VN
                restZaakBetrokkene?.vestigingsnummer shouldBe "fakeVestigingsnummer"
                restZaakBetrokkene?.kvkNummer shouldBe "fakeKvkNummer"
            }
        }
    }

    given("a RolNietNatuurlijkPersoon without a vestigingsnummer") {
        val rol = createRolNietNatuurlijkPersoon(
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(vestigingsnummer = null)
        )

        `when`("toRestZaakBetrokkene is called") {
            val restZaakBetrokkene = rol.toRestZaakBetrokkene()

            then("it returns a RestZaakBetrokkene with identificatieType RSIN") {
                restZaakBetrokkene?.identificatieType shouldBe IdentificatieType.RSIN
            }
        }
    }

    given("a RolNietNatuurlijkPersoon without a betrokkeneIdentificatie") {
        val rol = createRolNietNatuurlijkPersoon(nietNatuurlijkPersoonIdentificatie = null)

        `when`("toRestZaakBetrokkene is called") {
            val restZaakBetrokkene = rol.toRestZaakBetrokkene()

            then("it returns null") {
                restZaakBetrokkene.shouldBeNull()
            }
        }
    }

    given("a RolVestiging with a vestigingsnummer") {
        val rol = createRolVestiging()

        `when`("toRestZaakBetrokkene is called") {
            val restZaakBetrokkene = rol.toRestZaakBetrokkene()

            then("it returns a RestZaakBetrokkene with identificatieType VN") {
                restZaakBetrokkene?.identificatieType shouldBe IdentificatieType.VN
            }
        }
    }

    given("a RolVestiging without a vestigingsnummer") {
        val rol = createRolVestiging(
            vestigingIdentificatie = VestigingIdentificatie().apply { kvkNummer = "fakeKvkNummer" }
        )

        `when`("toRestZaakBetrokkene is called") {
            val restZaakBetrokkene = rol.toRestZaakBetrokkene()

            then("it returns null") {
                restZaakBetrokkene.shouldBeNull()
            }
        }
    }

    given("a RolVestiging without a betrokkeneIdentificatie") {
        val rol = createRolVestiging(vestigingIdentificatie = null)

        `when`("toRestZaakBetrokkene is called") {
            val restZaakBetrokkene = rol.toRestZaakBetrokkene()

            then("it returns null") {
                restZaakBetrokkene.shouldBeNull()
            }
        }
    }

    given("a RolOrganisatorischeEenheid with a naam") {
        val rol = createRolOrganisatorischeEenheid(
            organisatorischeEenheidIdentificatie = createOrganisatorischeEenheidIdentificatie(naam = "fakeNaam")
        )

        `when`("toRestZaakBetrokkene is called") {
            val restZaakBetrokkene = rol.toRestZaakBetrokkene()

            then("it returns a RestZaakBetrokkene with the naam") {
                restZaakBetrokkene?.naam shouldBe "fakeNaam"
            }
        }
    }

    given("a RolOrganisatorischeEenheid without a betrokkeneIdentificatie") {
        val rol = createRolOrganisatorischeEenheid(organisatorischeEenheidIdentificatie = null)

        `when`("toRestZaakBetrokkene is called") {
            val restZaakBetrokkene = rol.toRestZaakBetrokkene()

            then("it returns null") {
                restZaakBetrokkene.shouldBeNull()
            }
        }
    }

    given("a RolMedewerker with a naam") {
        val rol = createRolMedewerker()

        `when`("toRestZaakBetrokkene is called") {
            val restZaakBetrokkene = rol.toRestZaakBetrokkene()

            then("it returns a RestZaakBetrokkene with the naam") {
                restZaakBetrokkene?.naam shouldBe rol.naam
            }
        }
    }

    given("a RolMedewerker without a betrokkeneIdentificatie") {
        val rol = createRolMedewerker(medewerkerIdentificatie = null)

        `when`("toRestZaakBetrokkene is called") {
            val restZaakBetrokkene = rol.toRestZaakBetrokkene()

            then("it returns null") {
                restZaakBetrokkene.shouldBeNull()
            }
        }
    }

    given("a Rol without a betrokkeneType") {
        val rol = RolMedewerker()

        `when`("toRestZaakBetrokkene is called") {
            val restZaakBetrokkene = rol.toRestZaakBetrokkene()

            then("it returns null") {
                restZaakBetrokkene.shouldBeNull()
            }
        }
    }

    given("a list of roles where some do not have a betrokkeneIdentificatie") {
        val roles = listOf(
            createRolMedewerker(),
            createRolOrganisatorischeEenheid(organisatorischeEenheidIdentificatie = null)
        )

        `when`("toRestZaakBetrokkenen is called") {
            val restZaakBetrokkenen = roles.toRestZaakBetrokkenen()

            then("only the roles with a betrokkeneIdentificatie are included") {
                restZaakBetrokkenen.size shouldBe 1
            }
        }
    }
})
