/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.planitems.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.admin.model.FormulierDefinitie
import nl.info.zac.admin.model.FormulierVeldDefinitie

class FormulierKoppelingConverterTest : BehaviorSpec({
    context("toFormulierDefinitie") {
        given("a plan item definition ID that matches a known formulier koppeling") {
            val planItemDefinitionId = "AANVULLENDE_INFORMATIE"

            `when`("toFormulierDefinitie is called") {
                val result = planItemDefinitionId.toFormulierDefinitie()

                then("it returns the matching FormulierDefinitie") {
                    result shouldBe FormulierDefinitie.AANVULLENDE_INFORMATIE
                }
            }
        }

        given("a plan item definition ID 'GOEDKEUREN'") {
            val planItemDefinitionId = "GOEDKEUREN"

            `when`("toFormulierDefinitie is called") {
                val result = planItemDefinitionId.toFormulierDefinitie()

                then("it returns FormulierDefinitie.GOEDKEUREN") {
                    result shouldBe FormulierDefinitie.GOEDKEUREN
                }
            }
        }

        given("a plan item definition ID 'ADVIES_INTERN'") {
            val planItemDefinitionId = "ADVIES_INTERN"

            `when`("toFormulierDefinitie is called") {
                val result = planItemDefinitionId.toFormulierDefinitie()

                then("it returns FormulierDefinitie.ADVIES") {
                    result shouldBe FormulierDefinitie.ADVIES
                }
            }
        }

        given("a plan item definition ID that does not match any known koppeling") {
            val planItemDefinitionId = "UNKNOWN_PLAN_ITEM"

            `when`("toFormulierDefinitie is called") {
                val result = planItemDefinitionId.toFormulierDefinitie()

                then("it returns the default FormulierDefinitie") {
                    result shouldBe FormulierDefinitie.DEFAULT_TAAKFORMULIER
                }
            }
        }
    }

    context("readFormulierVeldDefinities") {
        given("a plan item definition ID 'ADVIES_INTERN' which maps to ADVIES with veld definitions") {
            val planItemDefinitionId = "ADVIES_INTERN"

            `when`("readFormulierVeldDefinities is called") {
                val result = planItemDefinitionId.readFormulierVeldDefinities()

                then("it returns the veld definitions for ADVIES") {
                    result shouldBe setOf(FormulierVeldDefinitie.ADVIES)
                }
            }
        }

        given("a plan item definition ID 'AANVULLENDE_INFORMATIE' which has no veld definitions") {
            val planItemDefinitionId = "AANVULLENDE_INFORMATIE"

            `when`("readFormulierVeldDefinities is called") {
                val result = planItemDefinitionId.readFormulierVeldDefinities()

                then("it returns an empty set") {
                    result shouldBe emptySet()
                }
            }
        }
    }
})
