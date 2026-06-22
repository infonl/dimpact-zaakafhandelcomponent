/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.task.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import jakarta.json.JsonObject
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.flowable.util.TaskUtil
import nl.info.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.createHumanTaskParameters
import nl.info.zac.app.identity.converter.RestGroupConverter
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.task.model.TaakStatus
import nl.info.zac.app.zaak.model.createRestUser
import nl.info.zac.flowable.bpmn.BpmnProcessDefinitionTaskFormService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createTaakRechten
import nl.info.zac.policy.output.createTaakRechtenAllDeny
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.identitylink.api.IdentityLinkType
import org.flowable.task.api.TaskInfo
import java.util.UUID

class RestTaskConverterTest : BehaviorSpec({
    val groepConverter = mockk<RestGroupConverter>()
    val medewerkerConverter = mockk<RestUserConverter>()
    val policyService = mockk<PolicyService>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val bpmnProcessDefinitionTaskFormService = mockk<BpmnProcessDefinitionTaskFormService>()

    val restTaskConverter = RestTaskConverter(
        groepConverter = groepConverter,
        medewerkerConverter = medewerkerConverter,
        policyService = policyService,
        zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
        bpmnProcessDefinitionTaskFormService = bpmnProcessDefinitionTaskFormService
    )

    mockkStatic(TaakVariabelenService::class)
    mockkStatic(TaskUtil::class)

    afterSpec {
        unmockkStatic(TaakVariabelenService::class)
        unmockkStatic(TaskUtil::class)
    }

    afterEach { checkUnnecessaryStub() }

    Context("convert") {
        val zaakUUID = UUID.randomUUID()
        val zaaktypeUUID = UUID.randomUUID()
        val fakeZaaktypeOmschrijving = "fakeZaaktypeOmschrijving"
        val taskDefinitionKey = "fakePlanItemDefinitionID"

        Given("a CMMN task with full read access") {
            val taskInfo = mockk<TaskInfo>()
            val taakRechten = createTaakRechten()
            val zaaktypeCmmnConfiguration = mockk<ZaaktypeCmmnConfiguration>()
            val humanTaskParameters = createHumanTaskParameters(
                planItemDefinitionID = taskDefinitionKey,
                formulierDefinitieID = "AANVULLENDE_INFORMATIE",
                referenceTables = emptyList()
            )

            every { TaakVariabelenService.readZaaktypeOmschrijving(taskInfo) } returns fakeZaaktypeOmschrijving
            every { TaakVariabelenService.readZaakUUID(taskInfo) } returns zaakUUID
            every { TaakVariabelenService.readZaakIdentificatie(taskInfo) } returns "fakeZaakIdentificatie"
            every { TaakVariabelenService.readZaaktypeUUID(taskInfo) } returns zaaktypeUUID
            every { TaakVariabelenService.readTaskInformation(taskInfo) } returns mapOf()
            every { TaakVariabelenService.readTaskData(taskInfo) } returns mapOf()
            every { TaakVariabelenService.readTaskDocuments(taskInfo) } returns emptyList()
            every { TaskUtil.getTaakStatus(taskInfo) } returns TaakStatus.NIET_TOEGEKEND
            every { TaskUtil.isCmmnTask(taskInfo) } returns true

            every { taskInfo.id } returns "fakeTaskId"
            every { taskInfo.name } returns "fakeTaskName"
            every { taskInfo.assignee } returns "fakeAssigneeId"
            every { taskInfo.description } returns "fakeToelichting"
            every { taskInfo.createTime } returns null
            every { taskInfo.claimTime } returns null
            every { taskInfo.dueDate } returns null
            every { taskInfo.taskDefinitionKey } returns taskDefinitionKey
            every { taskInfo.identityLinks } returns emptyList()

            every { policyService.readTaakRechten(taskInfo, fakeZaaktypeOmschrijving) } returns taakRechten
            every {
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUUID)
            } returns zaaktypeCmmnConfiguration
            every { zaaktypeCmmnConfiguration.getHumanTaskParametersCollection() } returns setOf(humanTaskParameters)
            every { medewerkerConverter.convertUserId("fakeAssigneeId") } returns createRestUser(id = "fakeAssigneeId")

            When("convert is called") {
                val restTask = restTaskConverter.convert(taskInfo)

                Then("basic fields are mapped correctly") {
                    restTask.id shouldBe "fakeTaskId"
                    restTask.naam shouldBe "fakeTaskName"
                    restTask.zaakUuid shouldBe zaakUUID
                    restTask.zaakIdentificatie shouldBe "fakeZaakIdentificatie"
                    restTask.zaaktypeUUID shouldBe zaaktypeUUID
                    restTask.toelichting shouldBe "fakeToelichting"
                    restTask.status shouldBe TaakStatus.NIET_TOEGEKEND
                }

                Then("CMMN formulier definition id is set") {
                    restTask.formulierDefinitieId shouldBe "AANVULLENDE_INFORMATIE"
                    restTask.formioFormulier.shouldBeNull()
                }
            }
        }

        Given("a BPMN task with full read access") {
            val taskInfo = mockk<TaskInfo>()
            val taakRechten = createTaakRechten()
            val fakeFormioFormulier = mockk<JsonObject>()
            val processDefinitionId = "fakeProcessDefinitionId"
            val formKey = "fakeFormKey"

            every { TaakVariabelenService.readZaaktypeOmschrijving(taskInfo) } returns fakeZaaktypeOmschrijving
            every { TaakVariabelenService.readZaakUUID(taskInfo) } returns zaakUUID
            every { TaakVariabelenService.readZaakIdentificatie(taskInfo) } returns "fakeZaakIdentificatie"
            every { TaakVariabelenService.readZaaktypeUUID(taskInfo) } returns zaaktypeUUID
            every { TaakVariabelenService.readTaskInformation(taskInfo) } returns mapOf()
            every { TaakVariabelenService.readTaskData(taskInfo) } returns mapOf()
            every { TaakVariabelenService.readTaskDocuments(taskInfo) } returns emptyList()
            every { TaskUtil.getTaakStatus(taskInfo) } returns TaakStatus.TOEGEKEND
            every { TaskUtil.isCmmnTask(taskInfo) } returns false

            every { taskInfo.id } returns "fakeBpmnTaskId"
            every { taskInfo.name } returns "fakeBpmnTaskName"
            every { taskInfo.assignee } returns null
            every { taskInfo.description } returns null
            every { taskInfo.createTime } returns null
            every { taskInfo.claimTime } returns null
            every { taskInfo.dueDate } returns null
            every { taskInfo.processDefinitionId } returns processDefinitionId
            every { taskInfo.formKey } returns formKey
            every { taskInfo.identityLinks } returns emptyList()

            every { policyService.readTaakRechten(taskInfo, fakeZaaktypeOmschrijving) } returns taakRechten
            every { bpmnProcessDefinitionTaskFormService.readForm(processDefinitionId, formKey) } returns fakeFormioFormulier

            When("convert is called") {
                val restTask = restTaskConverter.convert(taskInfo)

                Then("formio formulier is set and formulierDefinitieId is null") {
                    restTask.formioFormulier shouldBe fakeFormioFormulier
                    restTask.formulierDefinitieId.shouldBeNull()
                }
            }
        }

        Given("a task with no read access") {
            val taskInfo = mockk<TaskInfo>()
            val taakRechten = createTaakRechtenAllDeny()

            every { TaakVariabelenService.readZaaktypeOmschrijving(taskInfo) } returns fakeZaaktypeOmschrijving
            every { TaakVariabelenService.readZaakUUID(taskInfo) } returns zaakUUID
            every { TaakVariabelenService.readZaakIdentificatie(taskInfo) } returns "fakeZaakIdentificatie"
            every { TaakVariabelenService.readZaaktypeUUID(taskInfo) } returns zaaktypeUUID
            every { TaskUtil.getTaakStatus(taskInfo) } returns TaakStatus.NIET_TOEGEKEND
            every { TaskUtil.isCmmnTask(taskInfo) } returns false

            every { taskInfo.id } returns "fakeTaskId"
            every { taskInfo.name } returns "fakeTaskName"
            every { taskInfo.processDefinitionId } returns "fakeProcessDefinitionId"
            every { taskInfo.formKey } returns "fakeFormKey"

            every { policyService.readTaakRechten(taskInfo, fakeZaaktypeOmschrijving) } returns taakRechten
            every { bpmnProcessDefinitionTaskFormService.readForm(any(), any()) } returns mockk()

            When("convert is called") {
                val restTask = restTaskConverter.convert(taskInfo)

                Then("sensitive fields are null") {
                    restTask.toelichting.shouldBeNull()
                    restTask.creatiedatumTijd.shouldBeNull()
                    restTask.toekenningsdatumTijd.shouldBeNull()
                    restTask.fataledatum.shouldBeNull()
                    restTask.behandelaar.shouldBeNull()
                    restTask.groep.shouldBeNull()
                    restTask.taakinformatie.shouldBeNull()
                    restTask.taakdata.shouldBeNull()
                    restTask.taakdocumenten.shouldBeNull()
                    restTask.zaaktypeOmschrijving.shouldBeNull()
                }
            }
        }

        Given("a list of two tasks") {
            val taskInfo1 = mockk<TaskInfo>()
            val taskInfo2 = mockk<TaskInfo>()
            val taakRechten = createTaakRechten()
            val zaaktypeCmmnConfiguration = mockk<ZaaktypeCmmnConfiguration>()
            val humanTaskParameters = createHumanTaskParameters(
                planItemDefinitionID = taskDefinitionKey,
                formulierDefinitieID = "AANVULLENDE_INFORMATIE",
                referenceTables = emptyList()
            )

            for (taskInfo in listOf(taskInfo1, taskInfo2)) {
                every { TaakVariabelenService.readZaaktypeOmschrijving(taskInfo) } returns fakeZaaktypeOmschrijving
                every { TaakVariabelenService.readZaakUUID(taskInfo) } returns zaakUUID
                every { TaakVariabelenService.readZaakIdentificatie(taskInfo) } returns "fakeZaakIdentificatie"
                every { TaakVariabelenService.readZaaktypeUUID(taskInfo) } returns zaaktypeUUID
                every { TaakVariabelenService.readTaskInformation(taskInfo) } returns mapOf()
                every { TaakVariabelenService.readTaskData(taskInfo) } returns mapOf()
                every { TaakVariabelenService.readTaskDocuments(taskInfo) } returns emptyList()
                every { TaskUtil.getTaakStatus(taskInfo) } returns TaakStatus.NIET_TOEGEKEND
                every { TaskUtil.isCmmnTask(taskInfo) } returns true
                every { taskInfo.id } returns "fakeId"
                every { taskInfo.name } returns "fakeName"
                every { taskInfo.assignee } returns null
                every { taskInfo.description } returns null
                every { taskInfo.createTime } returns null
                every { taskInfo.claimTime } returns null
                every { taskInfo.dueDate } returns null
                every { taskInfo.taskDefinitionKey } returns taskDefinitionKey
                every { taskInfo.identityLinks } returns emptyList()
                every { policyService.readTaakRechten(taskInfo, fakeZaaktypeOmschrijving) } returns taakRechten
                every {
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUUID)
                } returns zaaktypeCmmnConfiguration
            }
            every { zaaktypeCmmnConfiguration.getHumanTaskParametersCollection() } returns setOf(humanTaskParameters)

            When("convert is called with the list") {
                val restTasks = restTaskConverter.convert(listOf(taskInfo1, taskInfo2))

                Then("both tasks are converted") {
                    restTasks.size shouldBe 2
                }
            }
        }
    }

    Context("extractGroupId") {
        Given("a CANDIDATE identity link") {
            val identityLink = mockk<IdentityLinkInfo>()
            every { identityLink.type } returns IdentityLinkType.CANDIDATE
            every { identityLink.groupId } returns "fakeGroupId"

            When("extractGroupId is called") {
                val groupId = restTaskConverter.extractGroupId(listOf(identityLink))

                Then("it returns the group id") {
                    groupId shouldBe "fakeGroupId"
                }
            }
        }

        Given("no CANDIDATE identity link") {
            val identityLink = mockk<IdentityLinkInfo>()
            every { identityLink.type } returns IdentityLinkType.PARTICIPANT

            When("extractGroupId is called") {
                val groupId = restTaskConverter.extractGroupId(listOf(identityLink))

                Then("it returns null") {
                    groupId.shouldBeNull()
                }
            }
        }

        Given("an empty identity link list") {
            When("extractGroupId is called") {
                val groupId = restTaskConverter.extractGroupId(emptyList())

                Then("it returns null") {
                    groupId.shouldBeNull()
                }
            }
        }
    }
})
