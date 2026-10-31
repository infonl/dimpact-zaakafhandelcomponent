/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.formulieren.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.zac.app.formulieren.model.createFormulierDefinitie
import net.atos.zac.app.formulieren.model.createRESTFormulierDefinitie
import net.atos.zac.formulieren.model.FormulierVeldtype

class RESTFormulierDefinitieConverterTest : BehaviorSpec({

    Given("Formulier definition") {
        val formulierDefinitie = createFormulierDefinitie()
        val restFormulierDefinitieConverter = RESTFormulierDefinitieConverter()

        When("converted to REST representation including fields") {
            val restFormulierVeldDefinitie = restFormulierDefinitieConverter.convert(formulierDefinitie, true)

            Then("correct object is returned") {
                with(restFormulierVeldDefinitie) {
                    id shouldBe 1L
                    systeemnaam shouldBe "Dummy system name"
                    naam shouldBe "Dummy name"
                    beschrijving shouldBe "Dummy description"
                    uitleg shouldBe "Dummy explanation"
                    creatiedatum shouldBe formulierDefinitie.creatiedatum
                    wijzigingsdatum shouldBe formulierDefinitie.wijzigingsdatum
                    veldDefinities.size shouldBe 2
                    with(veldDefinities[0]) {
                        id shouldBe 1L
                        systeemnaam shouldBe "Dummy system name"
                        beschrijving shouldBe "Dummy description"
                        veldtype shouldBe FormulierVeldtype.TEKST_VELD
                        verplicht shouldBe false
                        defaultWaarde shouldBe "Dummy value"
                        meerkeuzeOpties shouldBe "Dummy multi-options"
                        validaties shouldBe listOf("Dummy validation 1", "Dummy validation 2")
                    }
                    with(veldDefinities[1]) {
                        id shouldBe 1L
                        systeemnaam shouldBe "Dummy system name"
                        beschrijving shouldBe "Dummy description"
                        veldtype shouldBe FormulierVeldtype.TEKST_VELD
                        verplicht shouldBe false
                        defaultWaarde shouldBe "Dummy value"
                        meerkeuzeOpties shouldBe "Dummy multi-options"
                        validaties shouldBe listOf("Dummy validation 1", "Dummy validation 2")
                    }
                }
            }
        }

        When("converted to REST representation excluding fields") {
            val restFormulierVeldDefinitie = restFormulierDefinitieConverter.convert(formulierDefinitie, false)

            Then("correct object is returned") {
                with(restFormulierVeldDefinitie) {
                    id shouldBe 1L
                    systeemnaam shouldBe "Dummy system name"
                    naam shouldBe "Dummy name"
                    beschrijving shouldBe "Dummy description"
                    uitleg shouldBe "Dummy explanation"
                    creatiedatum shouldBe formulierDefinitie.creatiedatum
                    wijzigingsdatum shouldBe formulierDefinitie.wijzigingsdatum
                    veldDefinities shouldBe null
                }
            }
        }
    }

    Given("REST representation of Formulier") {
        val restFormulierDefinition = createRESTFormulierDefinitie()
        val restFormulierDefinitieConverter = RESTFormulierDefinitieConverter()

        When("converted to Formulier definition") {
            val formulierDefinitie = restFormulierDefinitieConverter.convert(restFormulierDefinition)

            Then("the correct object is built") {
                with(formulierDefinitie) {
                    id shouldBe 1L
                    systeemnaam shouldBe "Dummy system name"
                    naam shouldBe "Dummy name"
                    beschrijving shouldBe "Dummy description"
                    uitleg shouldBe "Dummy explanation"
                    creatiedatum shouldBe formulierDefinitie.creatiedatum
                    wijzigingsdatum shouldBe formulierDefinitie.wijzigingsdatum
                    veldDefinities.size shouldBe 2
                    with(veldDefinities.first()) {
                        id shouldBe 1L
                        systeemnaam shouldBe "Dummy system name"
                        beschrijving shouldBe "Dummy description"
                        veldtype shouldBe FormulierVeldtype.TEKST_VELD
                        isVerplicht shouldBe false
                        defaultWaarde shouldBe "Dummy value"
                        meerkeuzeOpties shouldBe "Dummy multi-options"
                        validaties shouldBe "Dummy validation 1;Dummy validation 2"
                    }
                    with(veldDefinities.last()) {
                        id shouldBe 1L
                        systeemnaam shouldBe "Dummy system name"
                        beschrijving shouldBe "Dummy description"
                        veldtype shouldBe FormulierVeldtype.TEKST_VELD
                        isVerplicht shouldBe false
                        defaultWaarde shouldBe "Dummy value"
                        meerkeuzeOpties shouldBe "Dummy multi-options"
                        validaties shouldBe "Dummy validation 1;Dummy validation 2"
                    }
                }
            }
        }
    }
})
