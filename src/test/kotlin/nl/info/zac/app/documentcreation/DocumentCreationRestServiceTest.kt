/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.documentcreation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createInformatieObjectType
import nl.info.test.org.flowable.task.api.createTestTask
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.documentcreation.model.createRestDocumentCreationAttendedData
import nl.info.zac.documentcreation.BpmnDocumentCreationService
import nl.info.zac.documentcreation.CmmnDocumentCreationService
import nl.info.zac.documentcreation.DocumentCreationService
import nl.info.zac.documentcreation.model.CmmnDocumentCreationDataAttended
import nl.info.zac.documentcreation.model.createDocumentCreationAttendedResponse
import nl.info.zac.exception.ErrorCode.ERROR_CODE_SMARTDOCUMENTS_DISABLED
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.smartdocuments.exception.SmartDocumentsDisabledException
import java.net.URI
import java.util.UUID

class DocumentCreationRestServiceTest : BehaviorSpec({
    val documentCreationService = mockk<DocumentCreationService>()
    val cmmnDocumentCreationService = mockk<CmmnDocumentCreationService>()
    val bpmnDocumentCreationService = mockk<BpmnDocumentCreationService>()
    val policyService = mockk<PolicyService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val bpmnService = mockk<BpmnService>()
    val documentCreationRestService = DocumentCreationRestService(
        policyService = policyService,
        documentCreationService = documentCreationService,
        cmmnDocumentCreationService = cmmnDocumentCreationService,
        bpmnDocumentCreationService = bpmnDocumentCreationService,
        zrcClientService = zrcClientService,
        zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
        flowableTaskService = flowableTaskService
    )

    isolationMode = IsolationMode.InstancePerTest

    Given("document creation data is provided and zaaktype can use the 'bijlage' informatieobjecttype") {
        val zaakTypeUUID = UUID.randomUUID()
        val zaak = createZaak(
            zaakTypeURI = URI("https://example.com/$zaakTypeUUID"),
        )
        val taskId = "fakeTaskId"
        val task = createTestTask()
        val restDocumentCreationAttendedData = createRestDocumentCreationAttendedData(
            zaakUuid = zaak.uuid,
            taskId = taskId,
            smartDocumentsTemplateGroupId = "groupId",
            smartDocumentsTemplateId = "templateId",
            title = "Title",
        )
        val documentCreationResponse = createDocumentCreationAttendedResponse()
        val documentCreationDataAttended = slot<CmmnDocumentCreationDataAttended>()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ztcClientService.readInformatieobjecttypen(zaak.zaaktype) } returns listOf(
            createInformatieObjectType(omschrijving = "bijlage")
        )
        every {
            cmmnDocumentCreationService.createCmmnDocumentAttended(capture(documentCreationDataAttended))
        } returns documentCreationResponse
        every {
            bpmnService.isProcessDriven(any())
        } returns false

        When("createDocument is called by a role that is allowed to change the zaak") {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(
                creerenDocument = true
            )
            every { flowableTaskService.findOpenTask(taskId) } returns task
            every { policyService.readTaakRechten(task).creerenDocument } returns true
            every { zaaktypeCmmnConfigurationService.isSmartDocumentsEnabled(zaakTypeUUID) } returns true

            val restDocumentCreationResponse = documentCreationRestService.createDocumentAttended(
                restDocumentCreationAttendedData
            )

            Then("the document creation service is called to create the document") {
                restDocumentCreationResponse.message shouldBe documentCreationResponse.message
                restDocumentCreationResponse.redirectURL shouldBe documentCreationResponse.redirectUrl
                with(documentCreationDataAttended.captured) {
                    this.zaak shouldBe zaak
                    taskId shouldBe restDocumentCreationAttendedData.taskId
                    templateId shouldBe restDocumentCreationAttendedData.smartDocumentsTemplateId
                    templateGroupId shouldBe restDocumentCreationAttendedData.smartDocumentsTemplateGroupId
                }
            }
        }

        When("createDocument is called by a role that is not allowed to create documents for tasks") {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(
                creerenDocument = true
            )
            every { flowableTaskService.findOpenTask(taskId) } returns task
            every { policyService.readTaakRechten(task).creerenDocument } returns false

            val exception = shouldThrow<PolicyException> {
                documentCreationRestService.createDocumentAttended(restDocumentCreationAttendedData)
            }

            Then("it throws exception with no message") {
                exception.message shouldBe null
            }
        }

        When("createDocument is called for a task that is not opened") {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(
                creerenDocument = true
            )
            every { flowableTaskService.findOpenTask(taskId) } returns null

            val exception = shouldThrow<TaskNotFoundException> {
                documentCreationRestService.createDocumentAttended(restDocumentCreationAttendedData)
            }

            Then("it throws exception with message that mentions the task id") {
                exception.message shouldBe "No open task found with task id: 'fakeTaskId'"
            }
        }

        When("createDocument is called by a user that has no access") {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny()

            val exception = shouldThrow<PolicyException> {
                documentCreationRestService.createDocumentAttended(restDocumentCreationAttendedData)
            }

            Then("it throws exception with no message") {
                exception.message shouldBe null
            }
        }

        When("createDocument is called with disabled document creation") {
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(
                creerenDocument = true
            )
            every { flowableTaskService.findOpenTask(taskId) } returns task
            every { policyService.readTaakRechten(task).creerenDocument } returns true
            every { zaaktypeCmmnConfigurationService.isSmartDocumentsEnabled(zaakTypeUUID) } returns false
            every {
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID)
            } returns zaaktypeCmmnConfiguration

            val exception = shouldThrow<SmartDocumentsDisabledException> {
                documentCreationRestService.createDocumentAttended(restDocumentCreationAttendedData)
            }

            Then("it throws exception with correct message") {
                exception.errorCode shouldBe ERROR_CODE_SMARTDOCUMENTS_DISABLED
                exception.message shouldBe null
            }
        }
    }
})
