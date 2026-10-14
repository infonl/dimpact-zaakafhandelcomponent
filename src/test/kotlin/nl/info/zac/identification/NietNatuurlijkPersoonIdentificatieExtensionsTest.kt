/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identification

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.kvk.KvkClientService
import nl.info.client.kvk.model.createResultaatItem
import nl.info.client.zgw.model.createNietNatuurlijkPersoonIdentificatie
import nl.info.zac.app.klant.model.klant.IdentificatieType

class NietNatuurlijkPersoonIdentificatieExtensionsTest : BehaviorSpec({
    val kvkClientService = mockk<KvkClientService>()

    afterEach {
        checkUnnecessaryStub()
        clearAllMocks()
    }

    given("a niet-natuurlijk persoon identificatie with only an RSIN") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            innNnpId = "fakeRsin",
            kvkNummer = null
        )

        `when`("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            then("it is RSIN") {
                identificatieType shouldBe IdentificatieType.RSIN
            }
        }
    }

    given("a niet-natuurlijk persoon identificatie with only a KVK number") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            kvkNummer = "fakeKvkNummer"
        )

        `when`("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            then("it is RSIN") {
                identificatieType shouldBe IdentificatieType.RSIN
            }
        }
    }

    given("a niet-natuurlijk persoon identificatie with a KVK number and a vestigingsnummer") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            kvkNummer = "fakeKvkNummer",
            vestigingsnummer = "fakeVestigingsnummer"
        )

        `when`("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            then("it is VN") {
                identificatieType shouldBe IdentificatieType.VN
            }
        }
    }

    given("a niet-natuurlijk persoon identificatie with an RSIN, a KVK number and a vestigingsnummer") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            innNnpId = "fakeRsin",
            kvkNummer = "fakeKvkNummer",
            vestigingsnummer = "fakeVestigingsnummer"
        )

        `when`("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            then("it is VN, because the vestigingsnummer takes precedence over the RSIN") {
                identificatieType shouldBe IdentificatieType.VN
            }
        }
    }

    given("a niet-natuurlijk persoon identificatie without an RSIN, KVK number or vestigingsnummer") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            kvkNummer = " ",
            annIdentificatie = "fakeAnnIdentificatie"
        )

        `when`("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            then("it is unknown") {
                identificatieType shouldBe null
            }
        }
    }

    given("a niet-natuurlijk persoon identificatie with an RSIN and a blank vestigingsnummer") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            innNnpId = "fakeRsin",
            vestigingsnummer = " "
        )

        `when`("the identificatie type is determined") {
            val identificatieType = nietNatuurlijkPersoonIdentificatie.toIdentificatieType()

            then("it is RSIN") {
                identificatieType shouldBe IdentificatieType.RSIN
            }
        }
    }

    given("a niet-natuurlijk persoon identificatie with an RSIN and a KVK number") {
        val rsin = "fakeRsin"
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            innNnpId = rsin,
            kvkNummer = "fakeKvkNummer"
        )
        val resultaatItem = createResultaatItem()
        every { kvkClientService.findRechtspersoonByRsin(rsin) } returns resultaatItem

        `when`("the rechtspersoon or vestiging is looked up in KVK") {
            val foundResultaatItem = kvkClientService.findRechtspersoonOrVestiging(nietNatuurlijkPersoonIdentificatie)

            then("the rechtspersoon is found by RSIN") {
                foundResultaatItem shouldBe resultaatItem
                verify(exactly = 0) { kvkClientService.findRechtspersoonByKvkNummer(any()) }
            }
        }
    }

    given("a niet-natuurlijk persoon identificatie with a blank RSIN and a KVK number") {
        val kvkNummer = "fakeKvkNummer"
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            innNnpId = " ",
            kvkNummer = kvkNummer
        )
        val resultaatItem = createResultaatItem()
        every { kvkClientService.findRechtspersoonByKvkNummer(kvkNummer) } returns resultaatItem

        `when`("the rechtspersoon or vestiging is looked up in KVK") {
            val foundResultaatItem = kvkClientService.findRechtspersoonOrVestiging(nietNatuurlijkPersoonIdentificatie)

            then("the rechtspersoon is found by KVK number") {
                foundResultaatItem shouldBe resultaatItem
                verify(exactly = 0) { kvkClientService.findRechtspersoonByRsin(any()) }
            }
        }
    }

    given("a niet-natuurlijk persoon identificatie with an RSIN, a KVK number and a vestigingsnummer for KVK lookup") {
        val vestigingsnummer = "fakeVestigingsnummer"
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            innNnpId = "fakeRsin",
            kvkNummer = "fakeKvkNummer",
            vestigingsnummer = vestigingsnummer
        )
        val resultaatItem = createResultaatItem()
        every { kvkClientService.findVestiging(vestigingsnummer) } returns resultaatItem

        `when`("the rechtspersoon or vestiging is looked up in KVK") {
            val foundResultaatItem = kvkClientService.findRechtspersoonOrVestiging(nietNatuurlijkPersoonIdentificatie)

            then("the vestiging is found by vestigingsnummer") {
                foundResultaatItem shouldBe resultaatItem
                verify(exactly = 0) {
                    kvkClientService.findRechtspersoonByRsin(any())
                    kvkClientService.findRechtspersoonByKvkNummer(any())
                }
            }
        }
    }

    given("a niet-natuurlijk persoon identificatie without an RSIN, KVK number or vestigingsnummer for KVK lookup") {
        val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
            kvkNummer = null,
            annIdentificatie = "fakeAnnIdentificatie"
        )

        `when`("the rechtspersoon or vestiging is looked up in KVK") {
            val foundResultaatItem = kvkClientService.findRechtspersoonOrVestiging(nietNatuurlijkPersoonIdentificatie)

            then("nothing is found and KVK is not called") {
                foundResultaatItem shouldBe null
                verify(exactly = 0) {
                    kvkClientService.findRechtspersoonByRsin(any())
                    kvkClientService.findRechtspersoonByKvkNummer(any())
                    kvkClientService.findVestiging(any())
                }
            }
        }
    }
})
