/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class ZaaktypeCmmnHumantaskParametersTest : BehaviorSpec({

    context("setReferentieTabellen") {
        given("a reference table coupling that belongs to another human task") {
            val originalHumanTaskParameters = createHumanTaskParameters()
            val originalReferentieTabel = createHumanTaskReferentieTabel(
                zaaktypeCmmnHumantaskParameters = originalHumanTaskParameters,
                field = "ADVIES"
            )
            originalHumanTaskParameters.setReferentieTabellen(listOf(originalReferentieTabel))
            val otherHumanTaskParameters = createHumanTaskParameters()

            `when`("that coupling is set on the other human task") {
                otherHumanTaskParameters.setReferentieTabellen(
                    originalHumanTaskParameters.getReferentieTabellen()
                )

                then("the other human task gets an unsaved coupling of its own") {
                    with(otherHumanTaskParameters.getReferentieTabellen().single()) {
                        veld shouldBe "ADVIES"
                        tabel shouldBeSameInstanceAs originalReferentieTabel.tabel
                        id.shouldBeNull()
                        humantask shouldBeSameInstanceAs otherHumanTaskParameters
                    }
                }

                and("the original human task keeps its own coupling") {
                    with(originalHumanTaskParameters.getReferentieTabellen().single()) {
                        this shouldNotBeSameInstanceAs
                            otherHumanTaskParameters.getReferentieTabellen().single()
                        humantask shouldBeSameInstanceAs originalHumanTaskParameters
                    }
                }
            }
        }

        given("a human task whose own couplings are passed back to it") {
            val humanTaskParameters = createHumanTaskParameters(
                referenceTables = listOf(createHumanTaskReferentieTabel(field = "ADVIES"))
            )

            `when`("its own couplings are set on it again") {
                humanTaskParameters.setReferentieTabellen(humanTaskParameters.getReferentieTabellen())

                then("the coupling is retained") {
                    humanTaskParameters.getReferentieTabellen().single().veld shouldBe "ADVIES"
                }
            }
        }
    }

    context("applyChanges") {
        given("changes that leave the reference table couplings untouched") {
            val referentieTabel = createHumanTaskReferentieTabel(field = "ADVIES")
            val humanTaskParameters = createHumanTaskParameters(
                groupId = "fakeGroupId",
                referenceTables = listOf(referentieTabel)
            )
            val existingReferentieTabel = humanTaskParameters.getReferentieTabellen().single()
            val changes = createHumanTaskParameters(
                groupId = "fakeOtherGroupId",
                referenceTables = listOf(createHumanTaskReferentieTabel(field = "ADVIES"))
            )

            `when`("the changes are applied") {
                humanTaskParameters.applyChanges(changes)

                then("the other fields are updated") {
                    humanTaskParameters.groepID shouldBe "fakeOtherGroupId"
                }

                and("the existing couplings are left in place") {
                    humanTaskParameters.getReferentieTabellen()
                        .single() shouldBeSameInstanceAs existingReferentieTabel
                }
            }
        }

        given("changes that couple the human task to another reference table") {
            val humanTaskParameters = createHumanTaskParameters(
                referenceTables = listOf(createHumanTaskReferentieTabel(field = "ADVIES"))
            )
            val newReferenceTable = createReferenceTable(id = 5678L, code = "ANDERE_TABEL")
            val changes = createHumanTaskParameters(
                referenceTables = listOf(
                    createHumanTaskReferentieTabel(referenceTable = newReferenceTable, field = "ADVIES")
                )
            )
            val changedReferentieTabel = changes.getReferentieTabellen().single()

            `when`("the changes are applied") {
                humanTaskParameters.applyChanges(changes)

                then("the human task is coupled to the new reference table") {
                    with(humanTaskParameters.getReferentieTabellen().single()) {
                        tabel shouldBeSameInstanceAs newReferenceTable
                        humantask shouldBeSameInstanceAs humanTaskParameters
                    }
                }

                and("the coupling of the changes is not handed over to the human task") {
                    humanTaskParameters.getReferentieTabellen()
                        .single() shouldNotBeSameInstanceAs changedReferentieTabel
                    changedReferentieTabel.humantask shouldBeSameInstanceAs changes
                }
            }
        }
    }

    context("formulierDefinitieID fallback and explicit") {
        given("a human task parameters instance without explicit formulierDefinitieID") {
            val params = createHumanTaskParameters(
                id = 999L,
                planItemDefinitionID = "GOEDKEUREN",
                formulierDefinitieID = null
            )

            `when`("formulierDefinitieID is read without explicit value") {
                val formId = params.getFormulierDefinitieID()

                then("it falls back to the planItemDefinitionID converted to form definition name") {
                    formId shouldBe "GOEDKEUREN"
                }
            }

            `when`("formulierDefinitieID is set explicitly") {
                params.setFormulierDefinitieID("CUSTOM_FORM")

                then("it returns the explicit value") {
                    params.getFormulierDefinitieID() shouldBe "CUSTOM_FORM"
                }
            }
        }
    }

    context("equals, hashCode, and resetId") {
        given("a human task parameters instance") {
            val params = createHumanTaskParameters(
                id = 999L,
                planItemDefinitionID = "GOEDKEUREN",
                formulierDefinitieID = "CUSTOM_FORM"
            )

            `when`("resetId is called") {
                val returned = params.resetId()

                then("id becomes null and same instance is returned") {
                    params.id.shouldBeNull()
                    returned shouldBe params
                }
            }

            `when`("equals and hashCode are evaluated") {
                val otherEqual = createHumanTaskParameters(
                    id = 111L,
                    planItemDefinitionID = "GOEDKEUREN",
                    formulierDefinitieID = "CUSTOM_FORM"
                )
                val otherDifferent = createHumanTaskParameters(
                    id = 111L,
                    planItemDefinitionID = "OTHER_TASK",
                    formulierDefinitieID = "CUSTOM_FORM"
                )

                then("equality behaves as expected") {
                    params shouldBe otherEqual
                    params.hashCode() shouldBe otherEqual.hashCode()
                    params shouldNotBe otherDifferent
                    params shouldNotBe "differentType"
                }
            }
        }
    }
})
