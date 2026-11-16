/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.smartdocuments.model.document

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.kvk.model.createBasisprofielAdres

class SmartDocumentsDocumentRequestDeclarationsTest : BehaviorSpec({
    given("A Dutch address from a KVK basisprofiel") {
        val basisprofielAdres = createBasisprofielAdres(
            straatnaam = "fakeStraatnaam",
            huisnummer = 7,
            huisletter = "A",
            huisnummerToevoeging = "2",
            postcode = "fakePostcode",
            plaats = "fakePlaats"
        )

        `when`("it is converted to aanvrager data") {
            val aanvragerData = basisprofielAdres.toAanvragerData("fakeNaam")

            then("it has the street, the house number, the postcode and the place of the address") {
                aanvragerData shouldBe AanvragerData(
                    naam = "fakeNaam",
                    straat = "fakeStraatnaam",
                    huisnummer = "7A2",
                    postcode = "fakePostcode",
                    woonplaats = "fakePlaats"
                )
            }
        }
    }

    given("A foreign address from a KVK basisprofiel") {
        val basisprofielAdres = createBasisprofielAdres().apply {
            straatHuisnummer = "fakeStraatHuisnummer"
            toevoegingAdres = "fakeToevoegingAdres"
            postcodeWoonplaats = "fakePostcodeWoonplaats"
            land = "fakeLand"
        }

        `when`("it is converted to aanvrager data") {
            val aanvragerData = basisprofielAdres.toAanvragerData("fakeNaam")

            then("it has the foreign street line as straat and the postcode, woonplaats and land as woonplaats") {
                aanvragerData shouldBe AanvragerData(
                    naam = "fakeNaam",
                    straat = "fakeStraatHuisnummer fakeToevoegingAdres",
                    woonplaats = "fakePostcodeWoonplaats, fakeLand"
                )
            }
        }
    }
})
