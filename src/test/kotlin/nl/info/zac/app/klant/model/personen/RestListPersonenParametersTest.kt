/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.personen

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import nl.info.client.brp.model.generated.RaadpleegMetBurgerservicenummer
import nl.info.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatum
import nl.info.client.brp.model.generated.ZoekMetNaamEnGemeenteVanInschrijving
import nl.info.client.brp.model.generated.ZoekMetPostcodeEnHuisnummer
import nl.info.client.brp.model.generated.ZoekMetStraatHuisnummerEnGemeenteVanInschrijving
import java.time.LocalDate

class RestListPersonenParametersTest : BehaviorSpec({
    context("toPersonenQuery") {
        given("Parameters with BSN and gemeenteVanInschrijving") {
            val parameters = RestListPersonenParameters(
                bsn = "fakeBsn",
                gemeenteVanInschrijving = "0344"
            )

            `when`("toPersonenQuery is called") {
                val query = parameters.toPersonenQuery()

                then("it should return a RaadpleegMetBurgerservicenummer with all properties set") {
                    query.shouldBeInstanceOf<RaadpleegMetBurgerservicenummer>()
                    query.burgerservicenummer shouldBe listOf("fakeBsn")
                    query.gemeenteVanInschrijving shouldBe "0344"
                }
            }
        }

        given("Parameters with geslachtsnaam, geboortedatum, voornamen, voorvoegsel and gemeenteVanInschrijving") {
            val parameters = RestListPersonenParameters(
                geslachtsnaam = "fakeGeslachtsnaam",
                geboortedatum = LocalDate.of(1990, 1, 1),
                voornamen = "fakeVoornamen",
                voorvoegsel = "fakeVoorvoegsel",
                gemeenteVanInschrijving = "0599"
            )

            `when`("toPersonenQuery is called") {
                val query = parameters.toPersonenQuery()

                then("it should return a ZoekMetGeslachtsnaamEnGeboortedatum with all properties set") {
                    query.shouldBeInstanceOf<ZoekMetGeslachtsnaamEnGeboortedatum>()
                    query.geslachtsnaam shouldBe "fakeGeslachtsnaam"
                    query.geboortedatum shouldBe LocalDate.of(1990, 1, 1)
                    query.voornamen shouldBe "fakeVoornamen"
                    query.voorvoegsel shouldBe "fakeVoorvoegsel"
                    query.inclusiefOverledenPersonen shouldBe true
                    query.gemeenteVanInschrijving shouldBe "0599"
                }
            }
        }

        given("Parameters with geslachtsnaam, voornamen, voorvoegsel and gemeenteVanInschrijving") {
            val parameters = RestListPersonenParameters(
                geslachtsnaam = "fakeGeslachtsnaam",
                voornamen = "fakeVoornamen",
                voorvoegsel = "fakeVoorvoegsel",
                gemeenteVanInschrijving = "0363"
            )

            `when`("toPersonenQuery is called") {
                val query = parameters.toPersonenQuery()

                then("it should return a ZoekMetNaamEnGemeenteVanInschrijving with all properties set") {
                    query.shouldBeInstanceOf<ZoekMetNaamEnGemeenteVanInschrijving>()
                    query.geslachtsnaam shouldBe "fakeGeslachtsnaam"
                    query.voornamen shouldBe "fakeVoornamen"
                    query.voorvoegsel shouldBe "fakeVoorvoegsel"
                    query.inclusiefOverledenPersonen shouldBe true
                    query.gemeenteVanInschrijving shouldBe "0363"
                }
            }
        }

        given("Parameters with postcode, huisnummer and gemeenteVanInschrijving") {
            val parameters = RestListPersonenParameters(
                postcode = "1234AB",
                huisnummer = 42,
                gemeenteVanInschrijving = "0518"
            )

            `when`("toPersonenQuery is called") {
                val query = parameters.toPersonenQuery()

                then("it should return a ZoekMetPostcodeEnHuisnummer with all properties set") {
                    query.shouldBeInstanceOf<ZoekMetPostcodeEnHuisnummer>()
                    query.postcode shouldBe "1234AB"
                    query.huisnummer shouldBe 42
                    query.inclusiefOverledenPersonen shouldBe true
                    query.gemeenteVanInschrijving shouldBe "0518"
                }
            }
        }

        given("Parameters with straat, huisnummer and gemeenteVanInschrijving") {
            val parameters = RestListPersonenParameters(
                straat = "fakeStraat",
                huisnummer = 10,
                gemeenteVanInschrijving = "0772"
            )

            `when`("toPersonenQuery is called") {
                val query = parameters.toPersonenQuery()

                then(
                    "it should return a ZoekMetStraatHuisnummerEnGemeenteVanInschrijving " +
                        "with all properties set"
                ) {
                    query.shouldBeInstanceOf<ZoekMetStraatHuisnummerEnGemeenteVanInschrijving>()
                    query.straat shouldBe "fakeStraat"
                    query.huisnummer shouldBe 10
                    query.inclusiefOverledenPersonen shouldBe true
                    query.gemeenteVanInschrijving shouldBe "0772"
                }
            }
        }
    }
})
