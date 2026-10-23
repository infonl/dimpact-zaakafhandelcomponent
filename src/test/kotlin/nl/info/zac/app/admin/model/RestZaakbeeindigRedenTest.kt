/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.admin.model.ZaakbeeindigReden

class RestZaakbeeindigRedenTest : BehaviorSpec({
    context("toRestZaakbeeindigReden") {
        given("a ZaakbeeindigReden with id and naam") {
            val zaakbeeindigReden = ZaakbeeindigReden().apply {
                id = 42L
                naam = "fakeReden"
            }

            `when`("toRestZaakbeeindigReden is called") {
                val result = zaakbeeindigReden.toRestZaakbeeindigReden()

                then("it returns a REST model with matching id and naam") {
                    result.id shouldBe "42"
                    result.naam shouldBe "fakeReden"
                }
            }
        }
    }

    context("toZaakbeeindigReden") {
        given("a RestZaakbeeindigReden with id and naam") {
            val restZaakbeeindigReden = createRestZaakbeeindigReden(id = "99", name = "fakeNaam")

            `when`("toZaakbeeindigReden is called") {
                val result = restZaakbeeindigReden.toZaakbeeindigReden()

                then("it returns a domain model with parsed id and matching naam") {
                    result.id shouldBe 99L
                    result.naam shouldBe "fakeNaam"
                }
            }
        }
    }
})
