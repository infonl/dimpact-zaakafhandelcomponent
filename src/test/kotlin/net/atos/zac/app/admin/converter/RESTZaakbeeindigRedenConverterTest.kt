/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.admin.model.ZaakbeeindigReden

class RESTZaakbeeindigRedenConverterTest : BehaviorSpec({
    Context("convertZaakbeeindigReden") {
        Given("a ZaakbeeindigReden with id and naam") {
            val zaakbeeindigReden = ZaakbeeindigReden().apply {
                id = 42L
                naam = "fakeReden"
            }

            When("convertZaakbeeindigReden is called") {
                val result = RESTZaakbeeindigRedenConverter.convertZaakbeeindigReden(zaakbeeindigReden)

                Then("it returns a REST model with matching id and naam") {
                    result.id shouldBe "42"
                    result.naam shouldBe "fakeReden"
                }
            }
        }
    }

    Context("convertZaakbeeindigRedenen") {
        Given("a list of ZaakbeeindigRedenen") {
            val reden1 = ZaakbeeindigReden().apply {
                id = 1L
                naam = "fakeReden1"
            }
            val reden2 = ZaakbeeindigReden().apply {
                id = 2L
                naam = "fakeReden2"
            }

            When("convertZaakbeeindigRedenen is called") {
                val result = RESTZaakbeeindigRedenConverter.convertZaakbeeindigRedenen(listOf(reden1, reden2))

                Then("it returns a list of REST models in order") {
                    result.size shouldBe 2
                    result[0].id shouldBe "1"
                    result[0].naam shouldBe "fakeReden1"
                    result[1].id shouldBe "2"
                    result[1].naam shouldBe "fakeReden2"
                }
            }
        }
    }

    Context("convertRESTZaakbeeindigReden") {
        Given("a RESTZaakbeeindigReden with id and naam") {
            val restZaakbeeindigReden = net.atos.zac.app.admin.model.RestZaakbeeindigReden().apply {
                id = "99"
                naam = "fakeNaam"
            }

            When("convertRESTZaakbeeindigReden is called") {
                val result = RESTZaakbeeindigRedenConverter.convertRESTZaakbeeindigReden(restZaakbeeindigReden)

                Then("it returns a domain model with parsed id and matching naam") {
                    result.id shouldBe 99L
                    result.naam shouldBe "fakeNaam"
                }
            }
        }
    }
})
