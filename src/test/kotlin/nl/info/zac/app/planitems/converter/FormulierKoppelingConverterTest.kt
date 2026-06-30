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
    Context("toFormulierDefinitie") {
        Given("a plan item definition ID that matches a known formulier koppeling") {
            val planItemDefinitionId = "AANVULLENDE_INFORMATIE"

            When("toFormulierDefinitie is called") {
                val result = planItemDefinitionId.toFormulierDefinitie()

                Then("it returns the matching FormulierDefinitie") {
                    result shouldBe FormulierDefinitie.AANVULLENDE_INFORMATIE
                }
            }
        }

        Given("a plan item definition ID 'GOEDKEUREN'") {
            val planItemDefinitionId = "GOEDKEUREN"

            When("toFormulierDefinitie is called") {
                val result = planItemDefinitionId.toFormulierDefinitie()

                Then("it returns FormulierDefinitie.GOEDKEUREN") {
                    result shouldBe FormulierDefinitie.GOEDKEUREN
                }
            }
        }

        Given("a plan item definition ID 'ADVIES_INTERN'") {
            val planItemDefinitionId = "ADVIES_INTERN"

            When("toFormulierDefinitie is called") {
                val result = planItemDefinitionId.toFormulierDefinitie()

                Then("it returns FormulierDefinitie.ADVIES") {
                    result shouldBe FormulierDefinitie.ADVIES
                }
            }
        }

        Given("a plan item definition ID that does not match any known koppeling") {
            val planItemDefinitionId = "UNKNOWN_PLAN_ITEM"

            When("toFormulierDefinitie is called") {
                val result = planItemDefinitionId.toFormulierDefinitie()

                Then("it returns the default FormulierDefinitie") {
                    result shouldBe FormulierDefinitie.DEFAULT_TAAKFORMULIER
                }
            }
        }
    }

    Context("readFormulierVeldDefinities") {
        Given("a plan item definition ID 'ADVIES_INTERN' which maps to ADVIES with veld definitions") {
            val planItemDefinitionId = "ADVIES_INTERN"

            When("readFormulierVeldDefinities is called") {
                val result = planItemDefinitionId.readFormulierVeldDefinities()

                Then("it returns the veld definitions for ADVIES") {
                    result shouldBe setOf(FormulierVeldDefinitie.ADVIES)
                }
            }
        }

        Given("a plan item definition ID 'AANVULLENDE_INFORMATIE' which has no veld definitions") {
            val planItemDefinitionId = "AANVULLENDE_INFORMATIE"

            When("readFormulierVeldDefinities is called") {
                val result = planItemDefinitionId.readFormulierVeldDefinities()

                Then("it returns an empty set") {
                    result shouldBe emptySet()
                }
            }
        }
    }
})
