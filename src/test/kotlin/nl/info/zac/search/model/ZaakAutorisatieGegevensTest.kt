/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ZaakAutorisatieGegevensTest : BehaviorSpec({
    context("Checking whether a user is a geautoriseerde medewerker of a zaak") {
        given("a zaakspecifiek geautoriseerde zaak with the user among its geautoriseerde medewerkers") {
            val zaakAutorisatieGegevens = ZaakAutorisatieGegevens(isZaakspecifiekGeautoriseerd = true) {
                listOf("fakeOtherUserId", "fakeUserId")
            }

            `when`("it is checked whether the user is a geautoriseerde medewerker") {
                val isGeautoriseerdeMedewerker = zaakAutorisatieGegevens.isGeautoriseerdeMedewerker("fakeUserId")

                then("the user is a geautoriseerde medewerker") {
                    isGeautoriseerdeMedewerker shouldBe true
                }
            }
        }

        given("a zaakspecifiek geautoriseerde zaak without the user among its geautoriseerde medewerkers") {
            val zaakAutorisatieGegevens = ZaakAutorisatieGegevens(isZaakspecifiekGeautoriseerd = true) {
                listOf("fakeOtherUserId")
            }

            `when`("it is checked whether the user is a geautoriseerde medewerker") {
                val isGeautoriseerdeMedewerker = zaakAutorisatieGegevens.isGeautoriseerdeMedewerker("fakeUserId")

                then("the user is not a geautoriseerde medewerker") {
                    isGeautoriseerdeMedewerker shouldBe false
                }
            }
        }

        given("a zaak that is not zaakspecifiek geautoriseerd") {
            var numberOfTimesGeautoriseerdeMedewerkersWereRead = 0
            val zaakAutorisatieGegevens = ZaakAutorisatieGegevens(isZaakspecifiekGeautoriseerd = false) {
                numberOfTimesGeautoriseerdeMedewerkersWereRead++
                listOf("fakeUserId")
            }

            `when`("it is checked whether the user is a geautoriseerde medewerker") {
                val isGeautoriseerdeMedewerker = zaakAutorisatieGegevens.isGeautoriseerdeMedewerker("fakeUserId")

                then("the user is not a geautoriseerde medewerker, even when listed") {
                    isGeautoriseerdeMedewerker shouldBe false
                }

                and("the geautoriseerde medewerkers are never read, so no rollen are looked up") {
                    numberOfTimesGeautoriseerdeMedewerkersWereRead shouldBe 0
                }
            }
        }

        given("a zaakspecifiek geautoriseerde zaak that is checked for two users") {
            var numberOfTimesGeautoriseerdeMedewerkersWereRead = 0
            val zaakAutorisatieGegevens = ZaakAutorisatieGegevens(isZaakspecifiekGeautoriseerd = true) {
                numberOfTimesGeautoriseerdeMedewerkersWereRead++
                listOf("fakeUserId")
            }

            `when`("it is checked for both users") {
                zaakAutorisatieGegevens.isGeautoriseerdeMedewerker("fakeUserId")
                zaakAutorisatieGegevens.isGeautoriseerdeMedewerker("fakeOtherUserId")

                then("the geautoriseerde medewerkers are read only once") {
                    numberOfTimesGeautoriseerdeMedewerkersWereRead shouldBe 1
                }
            }
        }
    }
})
