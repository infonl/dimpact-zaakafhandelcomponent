/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
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

        When("converted to REST representation including fields") {
            val restFormulierVeldDefinitie = formulierDefinitie.toRESTFormulierDefinitie(true)

            Then("correct object is returned") {
                with(restFormulierVeldDefinitie) {
                    id shouldBe 1L
                    systeemnaam shouldBe "Fake system name"
                    naam shouldBe "Fake name"
                    beschrijving shouldBe "Fake description"
                    uitleg shouldBe "Fake explanation"
                    creatiedatum shouldBe formulierDefinitie.creatiedatum
                    wijzigingsdatum shouldBe formulierDefinitie.wijzigingsdatum
                    veldDefinities.size shouldBe 2
                    with(veldDefinities[0]) {
                        id shouldBe 1L
                        systeemnaam shouldBe "Fake system name"
                        beschrijving shouldBe "Fake description"
                        veldtype shouldBe FormulierVeldtype.TEKST_VELD
                        verplicht shouldBe false
                        defaultWaarde shouldBe "Fake value"
                        meerkeuzeOpties shouldBe "Fake multi-options"
                        validaties shouldBe listOf("Fake validation 1", "Fake validation 2")
                    }
                    with(veldDefinities[1]) {
                        id shouldBe 1L
                        systeemnaam shouldBe "Fake system name"
                        beschrijving shouldBe "Fake description"
                        veldtype shouldBe FormulierVeldtype.TEKST_VELD
                        verplicht shouldBe false
                        defaultWaarde shouldBe "Fake value"
                        meerkeuzeOpties shouldBe "Fake multi-options"
                        validaties shouldBe listOf("Fake validation 1", "Fake validation 2")
                    }
                }
            }
        }

        When("converted to REST representation excluding fields") {
            val restFormulierVeldDefinitie = formulierDefinitie.toRESTFormulierDefinitie(false)

            Then("correct object is returned") {
                with(restFormulierVeldDefinitie) {
                    id shouldBe 1L
                    systeemnaam shouldBe "Fake system name"
                    naam shouldBe "Fake name"
                    beschrijving shouldBe "Fake description"
                    uitleg shouldBe "Fake explanation"
                    creatiedatum shouldBe formulierDefinitie.creatiedatum
                    wijzigingsdatum shouldBe formulierDefinitie.wijzigingsdatum
                    veldDefinities.size shouldBe 0
                }
            }
        }
    }

    Given("REST representation of Formulier") {
        val restFormulierDefinition = createRESTFormulierDefinitie()

        When("converted to Formulier definition") {
            val formulierDefinitie = restFormulierDefinition.toFormulierDefinitie()

            Then("the correct object is built") {
                with(formulierDefinitie) {
                    id shouldBe 1L
                    systeemnaam shouldBe "Fake system name"
                    naam shouldBe "Fake name"
                    beschrijving shouldBe "Fake description"
                    uitleg shouldBe "Fake explanation"
                    creatiedatum shouldBe formulierDefinitie.creatiedatum
                    wijzigingsdatum shouldBe formulierDefinitie.wijzigingsdatum
                    getVeldDefinities().size shouldBe 2
                    with(getVeldDefinities().first()) {
                        id shouldBe 1L
                        systeemnaam shouldBe "Fake system name"
                        beschrijving shouldBe "Fake description"
                        veldtype shouldBe FormulierVeldtype.TEKST_VELD
                        isVerplicht shouldBe false
                        defaultWaarde shouldBe "Fake value"
                        meerkeuzeOpties shouldBe "Fake multi-options"
                        validaties shouldBe "Fake validation 1;Fake validation 2"
                    }
                    with(getVeldDefinities().last()) {
                        id shouldBe 1L
                        systeemnaam shouldBe "Fake system name"
                        beschrijving shouldBe "Fake description"
                        veldtype shouldBe FormulierVeldtype.TEKST_VELD
                        isVerplicht shouldBe false
                        defaultWaarde shouldBe "Fake value"
                        meerkeuzeOpties shouldBe "Fake multi-options"
                        validaties shouldBe "Fake validation 1;Fake validation 2"
                    }
                }
            }
        }
    }
})
