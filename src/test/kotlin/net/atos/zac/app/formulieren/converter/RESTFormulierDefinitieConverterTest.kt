package net.atos.zac.app.formulieren.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.zac.formulieren.model.FormulierVeldtype
import net.atos.zac.formulieren.model.createFormulierDefinitie

class RESTFormulierDefinitieConverterTest : BehaviorSpec({

    Given("Formulier definitie") {
        val formulierDefinitie = createFormulierDefinitie()
        val restFormulierDefinitieConverter = RESTFormulierDefinitieConverter()

        When("converted to RESTFormulierDefinitie including fields") {
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
                        volgorde shouldBe 1
                        systeemnaam shouldBe "Dummy system name"
                        naam shouldBe "Dummy name"
                        beschrijving shouldBe "Dummy description"
                        uitleg shouldBe "Dummy explanation"
                        veldtype shouldBe FormulierVeldtype.TEKST_VELD
                        verplicht shouldBe false
                        defaultWaarde shouldBe "Dummy value"
                        meerkeuzeOpties shouldBe "Dummy multi-options"
                        validaties shouldBe listOf("Dummy validations")
                    }
                    with(veldDefinities[1]) {
                        id shouldBe 1L
                        volgorde shouldBe 2
                        systeemnaam shouldBe "Dummy system name"
                        naam shouldBe "Dummy name"
                        beschrijving shouldBe "Dummy description"
                        uitleg shouldBe "Dummy explanation"
                        veldtype shouldBe FormulierVeldtype.TEKST_VELD
                        verplicht shouldBe false
                        defaultWaarde shouldBe "Dummy value"
                        meerkeuzeOpties shouldBe "Dummy multi-options"
                        validaties shouldBe listOf("Dummy validations")
                    }
                }
            }
        }

        When("converted to RESTFormulierDefinitie excluding fields") {
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
})
