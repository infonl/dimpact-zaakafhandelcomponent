/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.util.UUID

class ZaaktypeCmmnExtensionTest : BehaviorSpec({

    context("managing human task parameters") {
        given("a CMMN extension with human tasks") {
            val extension = ZaaktypeCmmnExtension()
            val humanTask1 = createHumanTaskParameters(planItemDefinitionID = "HUMAN_TASK_1")
            val humanTask2 = createHumanTaskParameters(planItemDefinitionID = "HUMAN_TASK_2")

            `when`("setting human task collection") {
                extension.setHumanTaskParametersCollection(listOf(humanTask1, humanTask2))

                then("tasks can be queried by plan item definition id") {
                    extension.getHumanTaskParametersCollection() shouldHaveSize 2
                    extension.findHumanTaskParameter("HUMAN_TASK_1") shouldBe humanTask1
                    extension.findHumanTaskParameter("HUMAN_TASK_2") shouldBe humanTask2
                    extension.findHumanTaskParameter("NON_EXISTING").shouldBeNull()
                }
            }

            `when`("updating human task collection with modifications") {
                val updatedTask1 = createHumanTaskParameters(
                    planItemDefinitionID = "HUMAN_TASK_1",
                    groupId = "newGroupId"
                )
                extension.setHumanTaskParametersCollection(listOf(updatedTask1))

                then("changes are applied to the existing instance") {
                    val found = extension.findHumanTaskParameter("HUMAN_TASK_1")
                    found?.groepID shouldBe "newGroupId"
                }
            }
        }
    }

    context("managing user event listener parameters") {
        given("a CMMN extension with user event listeners") {
            val extension = ZaaktypeCmmnExtension().apply {
                zaaktypeConfiguration = ZaaktypeConfiguration().apply {
                    zaaktypeUuid = UUID.randomUUID()
                }
            }
            val uel1 = ZaaktypeCmmnUsereventlistenerParameters().apply {
                planItemDefinitionID = "UEL_1"
                toelichting = "Toelichting 1"
            }
            val uel2 = ZaaktypeCmmnUsereventlistenerParameters().apply {
                planItemDefinitionID = "UEL_2"
                toelichting = "Toelichting 2"
            }

            `when`("setting user event listener collection") {
                extension.setUserEventListenerParametersCollection(listOf(uel1, uel2))

                then("listeners can be read by plan item definition id") {
                    extension.getUserEventListenerParametersCollection() shouldHaveSize 2
                    extension.readUserEventListenerParameters("UEL_1") shouldBe uel1
                    extension.readUserEventListenerParameters("UEL_2") shouldBe uel2
                    shouldThrow<RuntimeException> {
                        extension.readUserEventListenerParameters("NON_EXISTING")
                    }
                }
            }

            `when`("updating user event listener collection with modifications") {
                val updatedUel1 = ZaaktypeCmmnUsereventlistenerParameters().apply {
                    planItemDefinitionID = "UEL_1"
                    toelichting = "Updated Toelichting 1"
                }
                extension.setUserEventListenerParametersCollection(listOf(updatedUel1))

                then("changes are applied to the existing element") {
                    val found = extension.readUserEventListenerParameters("UEL_1")
                    found.toelichting shouldBe "Updated Toelichting 1"
                }
            }
        }
    }

    context("binding to ZaaktypeConfiguration") {
        given("a configuration and extension") {
            val config = ZaaktypeConfiguration()
            val extension = ZaaktypeCmmnExtension().apply {
                zaaktypeConfiguration = config
            }

            `when`("checking the back-reference") {
                then("zaaktypeConfiguration matches") {
                    extension.zaaktypeConfiguration shouldBeSameInstanceAs config
                }
            }
        }
    }
})
