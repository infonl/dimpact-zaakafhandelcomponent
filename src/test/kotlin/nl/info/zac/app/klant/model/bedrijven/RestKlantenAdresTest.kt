/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.kvk.vestigingsprofiel.model.generated.Adres

private const val NBSP = " "

class RestKlantenAdresTest : BehaviorSpec({

    Given("a regular Dutch address with all fields including huisnummerToevoeging") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatnaam = "Dorpstraat"
            huisnummer = 12
            huisletter = "A"
            huisnummerToevoeging = "bis"
            postcode = "1234AB"
            plaats = "Amsterdam"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is formatted as straat huisnummer+huisletter huisnummerToevoeging, postcode plaats") {
                result.volledigAdres shouldBe "Dorpstraat${NBSP}12A${NBSP}bis, 1234AB${NBSP}Amsterdam"
            }
        }
    }

    Given("a regular Dutch address without huisnummerToevoeging") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatnaam = "Dorpstraat"
            huisnummer = 12
            huisletter = "A"
            postcode = "1234AB"
            plaats = "Amsterdam"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is formatted as straat huisnummer+huisletter, postcode plaats") {
                result.volledigAdres shouldBe "Dorpstraat${NBSP}12A, 1234AB${NBSP}Amsterdam"
            }
        }
    }

    Given("a regular Dutch address without huisletter and huisnummerToevoeging") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatnaam = "Dorpstraat"
            huisnummer = 12
            postcode = "1234AB"
            plaats = "Amsterdam"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is formatted as straat huisnummer, postcode plaats") {
                result.volledigAdres shouldBe "Dorpstraat${NBSP}12, 1234AB${NBSP}Amsterdam"
            }
        }
    }

    Given("a postbus address") {
        val adres = Adres().apply {
            type = "correspondentieadres"
            indAfgeschermd = "nee"
            postbusnummer = 1234
            postcode = "3440AD"
            plaats = "Woerden"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is formatted as Postbus postbusnummer, postcode plaats") {
                result.volledigAdres shouldBe "Postbus 1234, 3440AD${NBSP}Woerden"
            }
        }
    }

    Given("a postbus address without postcode") {
        val adres = Adres().apply {
            type = "correspondentieadres"
            indAfgeschermd = "nee"
            postbusnummer = 1234
            plaats = "Woerden"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is formatted as Postbus postbusnummer, plaats") {
                result.volledigAdres shouldBe "Postbus 1234, Woerden"
            }
        }
    }

    Given("a foreign address with all fields") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatHuisnummer = "Rue de la Paix 1"
            toevoegingAdres = "Apt 2"
            postcodeWoonplaats = "75001 Paris"
            land = "France"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is formatted as straatHuisnummer toevoegingAdres, postcodeWoonplaats, land") {
                result.volledigAdres shouldBe "Rue${NBSP}de${NBSP}la${NBSP}Paix${NBSP}1${NBSP}Apt${NBSP}2, 75001${NBSP}Paris, France"
            }
        }
    }

    Given("a foreign address without toevoegingAdres") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatHuisnummer = "Rue de la Paix 1"
            postcodeWoonplaats = "75001 Paris"
            land = "France"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is formatted as straatHuisnummer, postcodeWoonplaats, land") {
                result.volledigAdres shouldBe "Rue${NBSP}de${NBSP}la${NBSP}Paix${NBSP}1, 75001${NBSP}Paris, France"
            }
        }
    }

    Given("a Dutch address that also has a land field set") {
        val adres = Adres().apply {
            type = "bezoekadres"
            indAfgeschermd = "nee"
            straatnaam = "Dorpstraat"
            huisnummer = 12
            postcode = "1234AB"
            plaats = "Amsterdam"
            land = "Nederland"
        }

        When("converted to RestKlantenAdres") {
            val result = adres.toRestKlantenAdres()

            Then("address is treated as regular Dutch address, not as foreign") {
                result.volledigAdres shouldBe "Dorpstraat${NBSP}12, 1234AB${NBSP}Amsterdam"
            }
        }
    }
})
