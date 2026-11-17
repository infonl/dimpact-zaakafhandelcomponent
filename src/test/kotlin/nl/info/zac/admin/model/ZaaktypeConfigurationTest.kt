/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID

class ZaaktypeConfigurationTest : BehaviorSpec({

    context("isValidForZaakCreation") {
        given("a configuration without a group") {
            val zaaktypeConfiguration = ZaaktypeConfiguration().apply {
                groepID = null
                bindTo(ProcessEngine.BPMN, "fakeProcessKey")
            }

            `when`("checking isValidForZaakCreation") {
                val isValid = zaaktypeConfiguration.isValidForZaakCreation()

                then("it returns false") {
                    isValid shouldBe false
                }
            }
        }

        given("a configuration without a process binding") {
            val zaaktypeConfiguration = ZaaktypeConfiguration().apply {
                groepID = "fakeGroup"
                processBinding = null
            }

            `when`("checking isValidForZaakCreation") {
                val isValid = zaaktypeConfiguration.isValidForZaakCreation()

                then("it returns false") {
                    isValid shouldBe false
                }
            }
        }

        given("a configuration bound to BPMN") {
            val validBpmnConfig = ZaaktypeConfiguration().apply {
                groepID = "fakeGroup"
                bindTo(ProcessEngine.BPMN, "fakeProcessKey")
            }
            val invalidBpmnConfig = ZaaktypeConfiguration().apply {
                groepID = "fakeGroup"
                bindTo(ProcessEngine.BPMN, "")
            }

            `when`("definitionKey is valid") {
                then("isValidForZaakCreation is true") {
                    validBpmnConfig.isValidForZaakCreation() shouldBe true
                }
            }

            `when`("definitionKey is blank") {
                then("isValidForZaakCreation is false") {
                    invalidBpmnConfig.isValidForZaakCreation() shouldBe false
                }
            }
        }

        given("a configuration bound to CMMN") {
            val validCmmnConfig = ZaaktypeConfiguration().apply {
                groepID = "fakeGroup"
                nietOntvankelijkResultaattypeOmschrijving = "fakeNietOntvankelijk"
                bindTo(ProcessEngine.CMMN, "fakeCaseKey")
            }
            val invalidCmmnBlankKey = ZaaktypeConfiguration().apply {
                groepID = "fakeGroup"
                nietOntvankelijkResultaattypeOmschrijving = "fakeNietOntvankelijk"
                bindTo(ProcessEngine.CMMN, "")
            }
            val invalidCmmnMissingNietOntvankelijk = ZaaktypeConfiguration().apply {
                groepID = "fakeGroup"
                nietOntvankelijkResultaattypeOmschrijving = null
                bindTo(ProcessEngine.CMMN, "fakeCaseKey")
            }

            `when`("case definition key is valid and nietOntvankelijk is present") {
                then("isValidForZaakCreation is true") {
                    validCmmnConfig.isValidForZaakCreation() shouldBe true
                }
            }

            `when`("case definition key is blank") {
                then("isValidForZaakCreation is false") {
                    invalidCmmnBlankKey.isValidForZaakCreation() shouldBe false
                }
            }

            `when`("nietOntvankelijkResultaattype is missing") {
                then("isValidForZaakCreation is false") {
                    invalidCmmnMissingNietOntvankelijk.isValidForZaakCreation() shouldBe false
                }
            }
        }
    }

    context("bindTo and engine properties") {
        given("a fresh configuration") {
            val zaaktypeConfiguration = ZaaktypeConfiguration().apply { zaaktypeUuid = UUID.randomUUID() }

            `when`("binding to CMMN") {
                zaaktypeConfiguration.bindTo(ProcessEngine.CMMN, "fakeCaseDefinition")

                then("it is bound to the case definition and has a CMMN extension for the settings of its plan items") {
                    zaaktypeConfiguration.getProcessEngine() shouldBe ProcessEngine.CMMN
                    zaaktypeConfiguration.processBinding?.definitionKey shouldBe "fakeCaseDefinition"
                    zaaktypeConfiguration.cmmnExtension.shouldNotBeNull()
                }
            }

            `when`("rebinding to another case definition") {
                zaaktypeConfiguration.bindTo(ProcessEngine.CMMN, "otherFakeCaseDefinition")

                then("the definition key is replaced and the CMMN extension is kept") {
                    zaaktypeConfiguration.getProcessEngine() shouldBe ProcessEngine.CMMN
                    zaaktypeConfiguration.processBinding?.definitionKey shouldBe "otherFakeCaseDefinition"
                    zaaktypeConfiguration.cmmnExtension.shouldNotBeNull()
                }
            }

            `when`("rebinding to BPMN") {
                zaaktypeConfiguration.bindTo(ProcessEngine.BPMN, "fakeBpmnProcess")

                then("it is bound to the BPMN process definition, without a CMMN extension") {
                    zaaktypeConfiguration.getProcessEngine() shouldBe ProcessEngine.BPMN
                    zaaktypeConfiguration.processBinding?.definitionKey shouldBe "fakeBpmnProcess"
                    zaaktypeConfiguration.cmmnExtension.shouldBeNull()
                }
            }

            `when`("rebinding to CMMN") {
                zaaktypeConfiguration.bindTo(ProcessEngine.CMMN, "fakeCaseDefinition")

                then("it is bound to the case definition, with a new CMMN extension") {
                    zaaktypeConfiguration.getProcessEngine() shouldBe ProcessEngine.CMMN
                    zaaktypeConfiguration.processBinding?.definitionKey shouldBe "fakeCaseDefinition"
                    zaaktypeConfiguration.cmmnExtension.shouldNotBeNull()
                }
            }
        }

        given("a CMMN configuration with the plan item settings of its case definition") {
            fun createConfigurationWithPlanItemSettings() = ZaaktypeConfiguration().apply {
                zaaktypeUuid = UUID.randomUUID()
                bindTo(ProcessEngine.CMMN, "fakeCaseDefinition")
                getOrCreateCmmnExtension().apply {
                    setHumanTaskParametersCollection(listOf(createZaaktypeCmmnHumantaskParameters()))
                    setUserEventListenerParametersCollection(
                        listOf(ZaaktypeCmmnUsereventlistenerParameters().apply { planItemDefinitionID = "fakeListener" })
                    )
                }
            }

            `when`("it is bound to the same case definition again") {
                val zaaktypeConfiguration = createConfigurationWithPlanItemSettings()
                zaaktypeConfiguration.bindTo(ProcessEngine.CMMN, "fakeCaseDefinition")

                then("the plan item settings are kept") {
                    zaaktypeConfiguration.cmmnExtension?.getHumanTaskParametersCollection()?.size shouldBe 1
                    zaaktypeConfiguration.cmmnExtension?.getUserEventListenerParametersCollection()?.size shouldBe 1
                }
            }

            `when`("it is bound to another case definition") {
                val zaaktypeConfiguration = createConfigurationWithPlanItemSettings()
                zaaktypeConfiguration.bindTo(ProcessEngine.CMMN, "otherFakeCaseDefinition")

                then("the plan item settings are kept, so that the save keeps the rows of the plan items both share") {
                    zaaktypeConfiguration.cmmnExtension?.getHumanTaskParametersCollection()?.size shouldBe 1
                    zaaktypeConfiguration.cmmnExtension?.getUserEventListenerParametersCollection()?.size shouldBe 1
                }
            }
        }

        given("an unbound configuration with a CMMN extension") {
            val zaaktypeConfiguration = ZaaktypeConfiguration().apply {
                zaaktypeUuid = UUID.randomUUID()
                getOrCreateCmmnExtension()
            }

            `when`("binding to BPMN") {
                zaaktypeConfiguration.bindTo(ProcessEngine.BPMN, "fakeBpmnProcess")

                then("the configuration is bound to BPMN and the CMMN extension is removed") {
                    zaaktypeConfiguration.getProcessEngine() shouldBe ProcessEngine.BPMN
                    zaaktypeConfiguration.processBinding?.definitionKey shouldBe "fakeBpmnProcess"
                    zaaktypeConfiguration.cmmnExtension.shouldBeNull()
                }
            }
        }
    }

    context("zaakbeeindig parameters") {
        given("a configuration with completion parameters") {
            val zaaktypeConfiguration = ZaaktypeConfiguration().apply {
                zaaktypeUuid = UUID.randomUUID()
            }
            val reden1 = ZaakbeeindigReden().apply {
                id = 1L
                naam = "Reden 1"
            }
            val reden2 = ZaakbeeindigReden().apply {
                id = 2L
                naam = "Reden 2"
            }
            val param1 = createZaaktypeCompletionParameters(zaakbeeindigReden = reden1, resultaattypeOmschrijving = "fakeResultaat1")
            val param2 = createZaaktypeCompletionParameters(zaakbeeindigReden = reden2, resultaattypeOmschrijving = "fakeResultaat2")

            `when`("setting parameters collection") {
                zaaktypeConfiguration.setZaakbeeindigParameters(listOf(param1, param2))

                then("parameters can be retrieved and queried by reason ID") {
                    zaaktypeConfiguration.getZaakbeeindigParameters().size shouldBe 2
                    zaaktypeConfiguration.readZaakbeeindigParameter(1L) shouldBe param1
                    zaaktypeConfiguration.readZaakbeeindigParameter(2L) shouldBe param2
                    shouldThrow<RuntimeException> {
                        zaaktypeConfiguration.readZaakbeeindigParameter(999L)
                    }
                }
            }

            `when`("updating parameters collection with modified resultaat") {
                val updatedParam1 = createZaaktypeCompletionParameters(
                    zaakbeeindigReden = reden1,
                    resultaattypeOmschrijving = "fakeUpdatedResultaat"
                )
                zaaktypeConfiguration.setZaakbeeindigParameters(listOf(updatedParam1, param2))

                then("the parameter is updated") {
                    zaaktypeConfiguration.readZaakbeeindigParameter(1L).resultaattypeOmschrijving shouldBe "fakeUpdatedResultaat"
                }
            }
        }
    }

    context("betrokkene and BRP default parameters") {
        given("a configuration without betrokkene or BRP parameters") {
            val zaaktypeConfiguration = ZaaktypeConfiguration()

            `when`("accessing betrokkene and BRP parameters") {
                val betrokkene = zaaktypeConfiguration.getBetrokkeneParameters()
                val brp = zaaktypeConfiguration.getBrpParameters()

                then("default non-null instances are returned") {
                    betrokkene.shouldNotBeNull()
                    brp.shouldNotBeNull()
                }
            }
        }
    }
})
