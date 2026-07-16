/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import nl.info.client.kvk.vestigingsprofiel.model.generated.Adres
import nl.info.client.kvk.zoeken.model.generated.BinnenlandsAdres
import nl.info.client.kvk.zoeken.model.generated.BuitenlandsAdres
import nl.info.client.kvk.basisprofiel.model.generated.Adres as BasisprofielAdres

private const val NBSP = "\u00A0"

class RestBedrijfAdresTest : BehaviorSpec({

    afterEach {
        checkUnnecessaryStub()
    }

    given("a regular Dutch address with all fields including huisnummerToevoeging") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatnaam = "fakeStraatnaam1"
            huisnummer = 12
            huisletter = "A"
            huisnummerToevoeging = "bis"
            postcode = "1234AB"
            plaats = "fakePlaats1"
        }

        `when`("converted to RestBedrijfAdres") {
            val result = adres.toRestBedrijfAdres()

            then("address is formatted as straat huisnummer+huisletter huisnummerToevoeging, postcode plaats") {
                result.volledigAdres shouldBe "fakeStraatnaam1${NBSP}12A${NBSP}bis, 1234AB${NBSP}fakePlaats1"
            }
        }
    }

    given("a regular Dutch address without huisnummerToevoeging") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatnaam = "fakeStraatnaam1"
            huisnummer = 12
            huisletter = "A"
            postcode = "1234AB"
            plaats = "fakePlaats1"
        }

        `when`("converted to RestBedrijfAdres") {
            val result = adres.toRestBedrijfAdres()

            then("address is formatted as straat huisnummer+huisletter, postcode plaats") {
                result.volledigAdres shouldBe "fakeStraatnaam1${NBSP}12A, 1234AB${NBSP}fakePlaats1"
            }
        }
    }

    given("a regular Dutch address without huisletter and huisnummerToevoeging") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatnaam = "fakeStraatnaam1"
            huisnummer = 12
            postcode = "1234AB"
            plaats = "fakePlaats1"
        }

        `when`("converted to RestBedrijfAdres") {
            val result = adres.toRestBedrijfAdres()

            then("address is formatted as straat huisnummer, postcode plaats") {
                result.volledigAdres shouldBe "fakeStraatnaam1${NBSP}12, 1234AB${NBSP}fakePlaats1"
            }
        }
    }

    given("a postbus address") {
        val adres = Adres().apply {
            type = "correspondentieadres"
            indAfgeschermd = "nee"
            postbusnummer = 1234
            postcode = "3440AD"
            plaats = "fakePlaats2"
        }

        `when`("converted to RestBedrijfAdres") {
            val result = adres.toRestBedrijfAdres()

            then("address is formatted as Postbus postbusnummer, postcode plaats") {
                result.volledigAdres shouldBe "Postbus 1234, 3440AD${NBSP}fakePlaats2"
            }
        }
    }

    given("a postbus address without postcode") {
        val adres = Adres().apply {
            type = "correspondentieadres"
            indAfgeschermd = "nee"
            postbusnummer = 1234
            plaats = "fakePlaats2"
        }

        `when`("converted to RestBedrijfAdres") {
            val result = adres.toRestBedrijfAdres()

            then("address is formatted as Postbus postbusnummer, plaats") {
                result.volledigAdres shouldBe "Postbus 1234, fakePlaats2"
            }
        }
    }

    given("a foreign address with all fields") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatHuisnummer = "fakeStraatnaam3 1"
            toevoegingAdres = "fakeToevoeging3"
            postcodeWoonplaats = "12345 fakePlaats3"
            land = "fakeLand3"
        }

        `when`("converted to RestBedrijfAdres") {
            val result = adres.toRestBedrijfAdres()

            then("address is formatted as straatHuisnummer toevoegingAdres, postcodeWoonplaats, land") {
                result.volledigAdres shouldBe "fakeStraatnaam3${NBSP}1${NBSP}fakeToevoeging3, 12345${NBSP}fakePlaats3, fakeLand3"
            }
        }
    }

    given("a foreign address without toevoegingAdres") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatHuisnummer = "fakeStraatnaam3 1"
            postcodeWoonplaats = "12345 fakePlaats3"
            land = "fakeLand3"
        }

        `when`("converted to RestBedrijfAdres") {
            val result = adres.toRestBedrijfAdres()

            then("address is formatted as straatHuisnummer, postcodeWoonplaats, land") {
                result.volledigAdres shouldBe "fakeStraatnaam3${NBSP}1, 12345${NBSP}fakePlaats3, fakeLand3"
            }
        }
    }

    given("a Dutch address that also has a land field set") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatnaam = "fakeStraatnaam1"
            huisnummer = 12
            postcode = "1234AB"
            plaats = "fakePlaats1"
            land = "Nederland"
        }

        `when`("converted to RestBedrijfAdres") {
            val result = adres.toRestBedrijfAdres()

            then("address is treated as regular Dutch address, not as foreign") {
                result.volledigAdres shouldBe "fakeStraatnaam1${NBSP}12, 1234AB${NBSP}fakePlaats1"
            }
        }
    }

    given("a vestigingsprofiel address with both volledigAdres and individual fields set") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            volledigAdres = "Precomputed adres 1"
            straatnaam = "fakeStraatnaam1"
            huisnummer = 12
            postcode = "1234AB"
            plaats = "fakePlaats1"
        }

        `when`("converted to RestBedrijfAdres") {
            val result = adres.toRestBedrijfAdres()

            then("individual fields are used for formatting, volledigAdres is ignored") {
                result.volledigAdres shouldBe "fakeStraatnaam1${NBSP}12, 1234AB${NBSP}fakePlaats1"
            }
        }
    }

    given("a vestigingsprofiel address with only volledigAdres set and no individual fields") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            volledigAdres = "Precomputed adres 1"
        }

        `when`("converted to RestBedrijfAdres") {
            val result = adres.toRestBedrijfAdres()

            then("volledigAdres is used as fallback since no individual address fields are present") {
                result.volledigAdres shouldBe "Precomputed adres 1"
            }
        }
    }

    given("a BinnenlandsAdres with street fields") {
        val adres = BinnenlandsAdres().apply {
            straatnaam = "fakeStraatnaam1"
            huisnummer = 12
            huisletter = "A"
            postcode = "1234AB"
            plaats = "fakePlaats1"
        }

        `when`("toFormattedAddress is called") {
            val result = adres.toFormattedAddress()

            then("formatted as straatnaam huisnummer+huisletter, postcode plaats") {
                result shouldBe "fakeStraatnaam1${NBSP}12A, 1234AB${NBSP}fakePlaats1"
            }
        }
    }

    given("a BinnenlandsAdres with postbusnummer") {
        val adres = BinnenlandsAdres().apply {
            postbusnummer = 1234
            postcode = "3440AD"
            plaats = "fakePlaats2"
        }

        `when`("toFormattedAddress is called") {
            val result = adres.toFormattedAddress()

            then("formatted as Postbus postbusnummer, postcode plaats") {
                result shouldBe "Postbus 1234, 3440AD${NBSP}fakePlaats2"
            }
        }
    }

    given("a BuitenlandsAdres with all fields") {
        val adres = BuitenlandsAdres().apply {
            straatHuisnummer = "fakeStraatnaam3 1"
            postcodeWoonplaats = "12345 fakePlaats3"
            land = "fakeLand3"
        }

        `when`("toFormattedAddress is called") {
            val result = adres.toFormattedAddress()

            then(
                "formatted as straatHuisnummer, postcodeWoonplaats, land with non-breaking spaces replacing regular spaces"
            ) {
                result shouldBe "fakeStraatnaam3${NBSP}1, 12345${NBSP}fakePlaats3, fakeLand3"
            }
        }
    }

    given("a BasisprofielAdres with postbusnummer") {
        val adres = BasisprofielAdres().apply {
            type = "correspondentieadres"
            indAfgeschermd = "nee"
            postbusnummer = 1234
            postcode = "3440AD"
            plaats = "fakePlaats2"
        }

        `when`("toFormattedAddress is called") {
            val result = adres.toFormattedAddress()

            then("formatted as Postbus postbusnummer, postcode plaats") {
                result shouldBe "Postbus 1234, 3440AD${NBSP}fakePlaats2"
            }
        }
    }

    given("a BasisprofielAdres with straatHuisnummer (foreign address) including toevoegingAdres") {
        val adres = BasisprofielAdres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatHuisnummer = "fakeStraatnaam3 1"
            toevoegingAdres = "fakeToevoeging3"
            postcodeWoonplaats = "12345 fakePlaats3"
            land = "fakeLand3"
        }

        `when`("toFormattedAddress is called") {
            val result = adres.toFormattedAddress()

            then("formatted as straatHuisnummer toevoegingAdres, postcodeWoonplaats, land") {
                result shouldBe "fakeStraatnaam3${NBSP}1${NBSP}fakeToevoeging3, 12345${NBSP}fakePlaats3, fakeLand3"
            }
        }
    }

    given("a BasisprofielAdres with straatnaam fields including huisnummerToevoeging") {
        val adres = BasisprofielAdres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatnaam = "fakeStraatnaam1"
            huisnummer = 12
            huisletter = "A"
            huisnummerToevoeging = "bis"
            postcode = "1234AB"
            plaats = "fakePlaats1"
        }

        `when`("toFormattedAddress is called") {
            val result = adres.toFormattedAddress()

            then("formatted as straatnaam huisnummer+huisletter huisnummerToevoeging, postcode plaats") {
                result shouldBe "fakeStraatnaam1${NBSP}12A${NBSP}bis, 1234AB${NBSP}fakePlaats1"
            }
        }
    }

    given("a BasisprofielAdres with both volledigAdres and individual fields set") {
        val adres = BasisprofielAdres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            volledigAdres = "Precomputed adres 2"
            straatnaam = "fakeStraatnaam1"
            huisnummer = 12
            postcode = "1234AB"
            plaats = "fakePlaats1"
        }

        `when`("converted to RestBedrijfAdres") {
            val result = adres.toRestBedrijfAdres()

            then("individual fields are used for formatting, volledigAdres is ignored") {
                result.volledigAdres shouldBe "fakeStraatnaam1${NBSP}12, 1234AB${NBSP}fakePlaats1"
            }
        }
    }

    given("a BasisprofielAdres with only volledigAdres set and no individual fields") {
        val adres = BasisprofielAdres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            volledigAdres = "Precomputed adres 2"
        }

        `when`("converted to RestBedrijfAdres") {
            val result = adres.toRestBedrijfAdres()

            then("volledigAdres is used as fallback since no individual address fields are present") {
                result.volledigAdres shouldBe "Precomputed adres 2"
            }
        }
    }

    given("the string 'ja'") {
        `when`("isIndicatie is called") {
            then("returns true") {
                "ja".isIndicatie() shouldBe true
            }
        }
    }

    given("the string 'Ja' (uppercase)") {
        `when`("isIndicatie is called") {
            then("returns true regardless of case") {
                "Ja".isIndicatie() shouldBe true
            }
        }
    }

    given("the string 'nee'") {
        `when`("isIndicatie is called") {
            then("returns false") {
                "nee".isIndicatie() shouldBe false
            }
        }
    }

    given("an unexpected value 'unknown'") {
        `when`("isIndicatie is called") {
            val exception = shouldThrow<IllegalStateException> {
                "unknown".isIndicatie()
            }
            then("an IllegalStateException is thrown") {
                exception shouldNotBe null
            }
        }
    }
})
