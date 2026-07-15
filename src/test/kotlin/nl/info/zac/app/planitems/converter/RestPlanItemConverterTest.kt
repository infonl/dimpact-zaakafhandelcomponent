/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.planitems.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.model.createZaak
import nl.info.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.zac.admin.model.FormulierDefinitie
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.ZaaktypeCmmnUsereventlistenerParameters
import nl.info.zac.admin.model.createHumanTaskParameters
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.planitems.model.PlanItemType
import nl.info.zac.app.planitems.model.UserEventListenerActie
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType
import org.flowable.cmmn.api.runtime.PlanItemInstance
import java.net.URI
import java.util.UUID

class RestPlanItemConverterTest : BehaviorSpec({
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()

    val restPlanItemConverter =
        RestPlanItemConverter(zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService)

    afterEach { checkUnnecessaryStub() }

    context("convertPlanItems") {
        given("a zaak with two plan items") {
            val zaaktypeUUID = UUID.randomUUID()
            val zaak = createZaak(zaaktypeUri = URI("https://example.com/zaaktypes/$zaaktypeUUID"))
            val planItemInstance1 = mockk<PlanItemInstance>()
            val planItemInstance2 = mockk<PlanItemInstance>()
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()

            every { zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUUID) } returns zaaktypeCmmnConfiguration
            for (planItem in listOf(planItemInstance1, planItemInstance2)) {
                every { planItem.id } returns "fakePlanItemId"
                every { planItem.name } returns "fakePlanItemName"
                every { planItem.planItemDefinitionType } returns PlanItemDefinitionType.PROCESS_TASK
            }

            `when`("convertPlanItems is called") {
                val restPlanItems = restPlanItemConverter.convertPlanItems(
                    listOf(planItemInstance1, planItemInstance2),
                    zaak
                )

                then("it converts all plan items") {
                    restPlanItems.size shouldBe 2
                    restPlanItems.all { it.type == PlanItemType.PROCESS_TASK } shouldBe true
                }
            }
        }
    }

    context("convertPlanItem") {
        val zaakUUID = UUID.randomUUID()
        val zaaktypeCmmnConfiguration = mockk<ZaaktypeCmmnConfiguration>()

        given("a USER_EVENT_LISTENER plan item") {
            val planItemInstance = mockk<PlanItemInstance>()
            val planItemDefinitionId = UserEventListenerActie.INTAKE_AFRONDEN.name
            val userEventListenerParams = ZaaktypeCmmnUsereventlistenerParameters().apply {
                this.planItemDefinitionID = planItemDefinitionId
                this.toelichting = "fakeToelichting"
            }

            every { planItemInstance.id } returns "fakePlanItemId"
            every { planItemInstance.name } returns "fakePlanItemName"
            every { planItemInstance.planItemDefinitionType } returns PlanItemDefinitionType.USER_EVENT_LISTENER
            every { planItemInstance.planItemDefinitionId } returns planItemDefinitionId
            every {
                zaaktypeCmmnConfiguration.readUserEventListenerParameters(planItemDefinitionId)
            } returns userEventListenerParams

            `when`("convertPlanItem is called") {
                val restPlanItem = restPlanItemConverter.convertPlanItem(
                    planItemInstance,
                    zaakUUID,
                    zaaktypeCmmnConfiguration
                )

                then("it maps the type and userEventListenerActie") {
                    restPlanItem.id shouldBe "fakePlanItemId"
                    restPlanItem.naam shouldBe "fakePlanItemName"
                    restPlanItem.type shouldBe PlanItemType.USER_EVENT_LISTENER
                    restPlanItem.zaakUuid shouldBe zaakUUID
                    restPlanItem.userEventListenerActie shouldBe UserEventListenerActie.INTAKE_AFRONDEN
                    restPlanItem.toelichting shouldBe "fakeToelichting"
                }
            }
        }

        given("a HUMAN_TASK plan item with formulier definition") {
            val planItemInstance = mockk<PlanItemInstance>()
            val planItemDefinitionId = "fakePlanItemDefinitionId"
            val humanTaskParameters = createHumanTaskParameters(
                planItemDefinitionID = planItemDefinitionId,
                isActief = true,
                formulierDefinitieID = FormulierDefinitie.AANVULLENDE_INFORMATIE.name,
                groupId = "fakeGroupId",
                leadTime = null,
                referenceTables = emptyList()
            )

            every { planItemInstance.id } returns "fakeHumanTaskId"
            every { planItemInstance.name } returns "fakeHumanTaskName"
            every { planItemInstance.planItemDefinitionType } returns PlanItemDefinitionType.HUMAN_TASK
            every { planItemInstance.planItemDefinitionId } returns planItemDefinitionId
            every { zaaktypeCmmnConfiguration.findHumanTaskParameter(planItemDefinitionId) } returns humanTaskParameters

            `when`("convertPlanItem is called") {
                val restPlanItem = restPlanItemConverter.convertPlanItem(
                    planItemInstance,
                    zaakUUID,
                    zaaktypeCmmnConfiguration
                )

                then("it maps human task fields") {
                    restPlanItem.type shouldBe PlanItemType.HUMAN_TASK
                    restPlanItem.actief shouldBe true
                    restPlanItem.formulierDefinitie shouldBe FormulierDefinitie.AANVULLENDE_INFORMATIE
                    restPlanItem.groepId shouldBe "fakeGroupId"
                    restPlanItem.fataleDatum.shouldBeNull()
                }
            }
        }

        given("a PROCESS_TASK plan item") {
            val planItemInstance = mockk<PlanItemInstance>()

            every { planItemInstance.id } returns "fakeProcessTaskId"
            every { planItemInstance.name } returns "fakeProcessTaskName"
            every { planItemInstance.planItemDefinitionType } returns PlanItemDefinitionType.PROCESS_TASK

            `when`("convertPlanItem is called") {
                val restPlanItem = restPlanItemConverter.convertPlanItem(
                    planItemInstance,
                    zaakUUID,
                    zaaktypeCmmnConfiguration
                )

                then("it maps basic fields without extra PROCESS_TASK fields") {
                    restPlanItem.id shouldBe "fakeProcessTaskId"
                    restPlanItem.naam shouldBe "fakeProcessTaskName"
                    restPlanItem.type shouldBe PlanItemType.PROCESS_TASK
                    restPlanItem.zaakUuid shouldBe zaakUUID
                    restPlanItem.userEventListenerActie.shouldBeNull()
                    restPlanItem.formulierDefinitie.shouldBeNull()
                }
            }
        }

        given("a plan item with an unsupported definition type") {
            val planItemInstance = mockk<PlanItemInstance>()

            every { planItemInstance.id } returns "fakeId"
            every { planItemInstance.name } returns "fakeName"
            every { planItemInstance.planItemDefinitionType } returns "UNSUPPORTED_TYPE"

            `when`("convertPlanItem is called") {
                val exception = shouldThrow<IllegalArgumentException> {
                    restPlanItemConverter.convertPlanItem(planItemInstance, zaakUUID, zaaktypeCmmnConfiguration)
                }

                then("it throws with an informative message") {
                    exception.message shouldBe
                        "Conversie van plan item definition type 'UNSUPPORTED_TYPE' wordt niet ondersteund"
                }
            }
        }
    }
})
