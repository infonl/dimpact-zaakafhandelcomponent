/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ZaaktypeCmmnUsereventlistenerParametersTest : BehaviorSpec({

    context("isModifiedFrom") {
        given("two parameters with the same planItemDefinitionID and different toelichting") {
            val original = ZaaktypeCmmnUsereventlistenerParameters().apply {
                planItemDefinitionID = "fakePlanItemDefinitionId"
                toelichting = "fakeOriginalToelichting"
            }
            val candidate = ZaaktypeCmmnUsereventlistenerParameters().apply {
                planItemDefinitionID = "fakePlanItemDefinitionId"
                toelichting = "fakeNewToelichting"
            }

            `when`("isModifiedFrom is checked") {
                val isModified = candidate.isModifiedFrom(original)

                then("it returns true") {
                    isModified shouldBe true
                }
            }
        }

        given("two parameters with the same planItemDefinitionID and same toelichting") {
            val original = ZaaktypeCmmnUsereventlistenerParameters().apply {
                planItemDefinitionID = "fakePlanItemDefinitionId"
                toelichting = "fakeToelichting"
            }
            val candidate = ZaaktypeCmmnUsereventlistenerParameters().apply {
                planItemDefinitionID = "fakePlanItemDefinitionId"
                toelichting = "fakeToelichting"
            }

            `when`("isModifiedFrom is checked") {
                val isModified = candidate.isModifiedFrom(original)

                then("it returns false") {
                    isModified shouldBe false
                }
            }
        }

        given("two parameters with different planItemDefinitionID") {
            val original = ZaaktypeCmmnUsereventlistenerParameters().apply {
                planItemDefinitionID = "fakePlanItemDefinitionId1"
                toelichting = "fakeToelichting"
            }
            val candidate = ZaaktypeCmmnUsereventlistenerParameters().apply {
                planItemDefinitionID = "fakePlanItemDefinitionId2"
                toelichting = "fakeNewToelichting"
            }

            `when`("isModifiedFrom is checked") {
                val isModified = candidate.isModifiedFrom(original)

                then("it returns false") {
                    isModified shouldBe false
                }
            }
        }
    }

    context("applyChanges and resetId") {
        given("a parameter and new changes") {
            val parameter = ZaaktypeCmmnUsereventlistenerParameters().apply {
                id = 123L
                planItemDefinitionID = "fakePlanItemId"
                toelichting = "fakeOldToelichting"
            }
            val changes = ZaaktypeCmmnUsereventlistenerParameters().apply {
                toelichting = "fakeUpdatedToelichting"
            }

            `when`("changes are applied") {
                parameter.applyChanges(changes)

                then("toelichting is updated") {
                    parameter.toelichting shouldBe "fakeUpdatedToelichting"
                }
            }

            `when`("id is reset") {
                val returned = parameter.resetId()

                then("id becomes null and same instance is returned") {
                    parameter.id.shouldBeNull()
                    returned shouldBe parameter
                }
            }
        }
    }

    context("equals and hashCode") {
        given("equal and non-equal user event listener parameters") {
            val param1 = ZaaktypeCmmnUsereventlistenerParameters().apply {
                planItemDefinitionID = "fakePlanItemId"
                toelichting = "fakeToelichting"
            }
            val param2 = ZaaktypeCmmnUsereventlistenerParameters().apply {
                planItemDefinitionID = "fakePlanItemId"
                toelichting = "fakeToelichting"
            }
            val param3 = ZaaktypeCmmnUsereventlistenerParameters().apply {
                planItemDefinitionID = "fakeOtherPlanItemId"
                toelichting = "fakeToelichting"
            }

            `when`("comparing parameters") {
                then("equals and hashCode contract holds") {
                    param1 shouldBe param2
                    param1.hashCode() shouldBe param2.hashCode()
                    param1 shouldNotBe param3
                    param1 shouldNotBe "differentType"
                }
            }
        }
    }
})
