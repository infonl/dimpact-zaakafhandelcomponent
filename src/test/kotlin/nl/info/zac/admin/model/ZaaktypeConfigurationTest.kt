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
import nl.info.zac.exception.InputValidationFailedException
import java.util.UUID

class ZaaktypeConfigurationTest : BehaviorSpec({

    context("isValidForZaakCreation") {
        given("a configuration without a group") {
            val config = ZaaktypeConfiguration().apply {
                groepID = null
                bindTo(ProcessEngine.BPMN, "fakeProcessKey")
            }

            `when`("checking isValidForZaakCreation") {
                val isValid = config.isValidForZaakCreation()

                then("it returns false") {
                    isValid shouldBe false
                }
            }
        }

        given("a configuration without a process binding") {
            val config = ZaaktypeConfiguration().apply {
                groepID = "fakeGroup"
                processBinding = null
            }

            `when`("checking isValidForZaakCreation") {
                val isValid = config.isValidForZaakCreation()

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
            val config = ZaaktypeConfiguration().apply { zaaktypeUuid = UUID.randomUUID() }

            `when`("binding to CMMN") {
                config.bindTo(ProcessEngine.CMMN, "fakeCaseDefinition")
                config.getOrCreateCmmnExtension()

                then("processBinding and cmmnExtension are set") {
                    config.getProcessEngine() shouldBe ProcessEngine.CMMN
                    config.processBinding?.definitionKey shouldBe "fakeCaseDefinition"
                    config.cmmnExtension.shouldNotBeNull()
                }
            }

            `when`("rebinding to another case definition") {
                config.bindTo(ProcessEngine.CMMN, "otherFakeCaseDefinition")

                then("the definition key is replaced and the CMMN extension is kept") {
                    config.getProcessEngine() shouldBe ProcessEngine.CMMN
                    config.processBinding?.definitionKey shouldBe "otherFakeCaseDefinition"
                    config.cmmnExtension.shouldNotBeNull()
                }
            }

            `when`("rebinding to BPMN") {
                val exception = shouldThrow<InputValidationFailedException> {
                    config.bindTo(ProcessEngine.BPMN, "fakeBpmnProcess")
                }

                then("it is rejected, because a configuration never changes engine, and the CMMN binding is kept") {
                    exception.message shouldBe "Zaaktype configuration for zaaktype '${config.zaaktypeUuid}' is " +
                        "bound to CMMN and cannot be bound to BPMN"
                    config.getProcessEngine() shouldBe ProcessEngine.CMMN
                    config.processBinding?.definitionKey shouldBe "otherFakeCaseDefinition"
                    config.cmmnExtension.shouldNotBeNull()
                }
            }
        }

        given("an unbound configuration with a CMMN extension") {
            val config = ZaaktypeConfiguration().apply {
                zaaktypeUuid = UUID.randomUUID()
                getOrCreateCmmnExtension()
            }

            `when`("binding to BPMN") {
                config.bindTo(ProcessEngine.BPMN, "fakeBpmnProcess")

                then("the configuration is bound to BPMN and the CMMN extension is removed") {
                    config.getProcessEngine() shouldBe ProcessEngine.BPMN
                    config.processBinding?.definitionKey shouldBe "fakeBpmnProcess"
                    config.cmmnExtension.shouldBeNull()
                }
            }
        }
    }

    context("zaakbeeindig parameters") {
        given("a configuration with completion parameters") {
            val config = ZaaktypeConfiguration().apply {
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
                config.setZaakbeeindigParameters(listOf(param1, param2))

                then("parameters can be retrieved and queried by reason ID") {
                    config.getZaakbeeindigParameters().size shouldBe 2
                    config.readZaakbeeindigParameter(1L) shouldBe param1
                    config.readZaakbeeindigParameter(2L) shouldBe param2
                    shouldThrow<RuntimeException> {
                        config.readZaakbeeindigParameter(999L)
                    }
                }
            }

            `when`("updating parameters collection with modified resultaat") {
                val updatedParam1 = createZaaktypeCompletionParameters(
                    zaakbeeindigReden = reden1,
                    resultaattypeOmschrijving = "fakeUpdatedResultaat"
                )
                config.setZaakbeeindigParameters(listOf(updatedParam1, param2))

                then("the parameter is updated") {
                    config.readZaakbeeindigParameter(1L).resultaattypeOmschrijving shouldBe "fakeUpdatedResultaat"
                }
            }
        }
    }

    context("betrokkene and BRP default parameters") {
        given("a configuration without betrokkene or BRP parameters") {
            val config = ZaaktypeConfiguration()

            `when`("accessing betrokkene and BRP parameters") {
                val betrokkene = config.getBetrokkeneParameters()
                val brp = config.getBrpParameters()

                then("default non-null instances are returned") {
                    betrokkene.shouldNotBeNull()
                    brp.shouldNotBeNull()
                }
            }
        }
    }
})
