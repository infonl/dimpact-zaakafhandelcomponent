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
import java.time.LocalDate

class RestListPersonenParametersTest : BehaviorSpec({
    Context("toPersonenQuery") {
        Given("Parameters with BSN and gemeenteVanInschrijving") {
            val parameters = RestListPersonenParameters(
                bsn = "123456789",
                gemeenteVanInschrijving = "0344"
            )

            When("toPersonenQuery is called") {
                val query = parameters.toPersonenQuery()

                Then("it should return a RaadpleegMetBurgerservicenummer with gemeenteVanInschrijving set") {
                    query.shouldBeInstanceOf<RaadpleegMetBurgerservicenummer>()
                    query.gemeenteVanInschrijving shouldBe "0344"
                }
            }
        }

        Given("Parameters with geslachtsnaam, geboortedatum and gemeenteVanInschrijving") {
            val parameters = RestListPersonenParameters(
                geslachtsnaam = "Jansen",
                geboortedatum = LocalDate.of(1990, 1, 1),
                gemeenteVanInschrijving = "0599"
            )

            When("toPersonenQuery is called") {
                val query = parameters.toPersonenQuery()

                Then("it should return a ZoekMetGeslachtsnaamEnGeboortedatum with gemeenteVanInschrijving set") {
                    query.shouldBeInstanceOf<ZoekMetGeslachtsnaamEnGeboortedatum>()
                    query.gemeenteVanInschrijving shouldBe "0599"
                }
            }
        }
    }
})
