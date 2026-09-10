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
import jakarta.enterprise.inject.Instance
import jakarta.servlet.http.HttpSession
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ZaakInformatieObject
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createInformatieObjectType
import nl.info.test.org.flowable.task.api.createTestTask
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.app.documentcreation.model.createRestDocumentCreationAttendedData
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.LoggedInUserProvider
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.documentcreation.DocumentCreationService
import nl.info.zac.documentcreation.DocumentCreationUserStore
import nl.info.zac.documentcreation.model.DocumentCreationDataAttended
import nl.info.zac.documentcreation.model.createDocumentCreationAttendedResponse
import nl.info.zac.exception.ErrorCode.ERROR_CODE_SMARTDOCUMENTS_DISABLED
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.smartdocuments.exception.SmartDocumentsDisabledException
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

class DocumentCreationRestServiceTest : BehaviorSpec({
    val documentCreationService = mockk<DocumentCreationService>()
    val policyService = mockk<PolicyService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val bpmnService = mockk<BpmnService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val documentCreationUserStore = mockk<DocumentCreationUserStore>()
    val documentCreationRestService = DocumentCreationRestService(
        policyService = policyService,
        documentCreationService = documentCreationService,
        zrcClientService = zrcClientService,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        flowableTaskService = flowableTaskService,
        loggedInUserInstance = loggedInUserInstance,
        documentCreationUserStore = documentCreationUserStore
    )

    isolationMode = IsolationMode.InstancePerTest

    given("document creation data is provided and zaaktype can use the 'bijlage' informatieobjecttype") {
        val zaakTypeUUID = UUID.randomUUID()
        val zaak = createZaak(
            zaaktypeUri = URI("https://example.com/$zaakTypeUUID"),
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
        val documentCreationDataAttended = slot<DocumentCreationDataAttended>()
        val loggedInUser = createLoggedInUser()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ztcClientService.readInformatieobjecttypen(zaak.zaaktype) } returns listOf(
            createInformatieObjectType(omschrijving = "bijlage")
        )
        every {
            documentCreationService.createDocumentAttended(capture(documentCreationDataAttended))
        } returns documentCreationResponse
        every {
            bpmnService.isZaakProcessDriven(any())
        } returns false
        every { loggedInUserInstance.get() } returns loggedInUser

        `when`("createDocument is called by a role that is allowed to change the zaak") {
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(
                creerenDocument = true
            )
            every { flowableTaskService.findOpenTask(taskId) } returns task
            every { policyService.readTaakRechten(task).creerenDocument } returns true
            every { zaaktypeConfigurationService.isSmartDocumentsEnabled(zaakTypeUUID) } returns true

            val restDocumentCreationResponse = documentCreationRestService.createDocumentAttended(
                restDocumentCreationAttendedData
            )

            then("the document creation service is called to create the document") {
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

        `when`("createDocument is called by a role that is not allowed to create documents for tasks") {
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(
                creerenDocument = true
            )
            every { flowableTaskService.findOpenTask(taskId) } returns task
            every { policyService.readTaakRechten(task).creerenDocument } returns false

            val exception = shouldThrow<PolicyException> {
                documentCreationRestService.createDocumentAttended(restDocumentCreationAttendedData)
            }

            then("it throws exception with no message") {
                exception.message shouldBe null
            }
        }

        `when`("createDocument is called for a task that is not opened") {
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(
                creerenDocument = true
            )
            every { flowableTaskService.findOpenTask(taskId) } returns null

            val exception = shouldThrow<TaskNotFoundException> {
                documentCreationRestService.createDocumentAttended(restDocumentCreationAttendedData)
            }

            then("it throws exception with message that mentions the task id") {
                exception.message shouldBe "No open task found with task id: 'fakeTaskId'"
            }
        }

        `when`("createDocument is called by a user that has no access") {
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny()

            val exception = shouldThrow<PolicyException> {
                documentCreationRestService.createDocumentAttended(restDocumentCreationAttendedData)
            }

            then("it throws exception with no message") {
                exception.message shouldBe null
            }
        }

        `when`("createDocument is called with disabled document creation") {
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(
                creerenDocument = true
            )
            every { flowableTaskService.findOpenTask(taskId) } returns task
            every { policyService.readTaakRechten(task).creerenDocument } returns true
            every { zaaktypeConfigurationService.isSmartDocumentsEnabled(zaakTypeUUID) } returns false

            val exception = shouldThrow<SmartDocumentsDisabledException> {
                documentCreationRestService.createDocumentAttended(restDocumentCreationAttendedData)
            }

            then("it throws exception with correct message") {
                exception.errorCode shouldBe ERROR_CODE_SMARTDOCUMENTS_DISABLED
                exception.message shouldBe null
            }
        }
    }

    given("a SmartDocuments callback for a document creation started by a logged-in user") {
        val zaak = createZaak()
        val loggedInUser = createLoggedInUser()
        val userToken = "fakeUserToken"
        val informatieobjecttypeUuid = UUID.randomUUID()
        val httpSessionInstance = mockk<Instance<HttpSession>>()
        val loggedInUserProvider = LoggedInUserProvider(httpSessionInstance)
        var userWhileStoringDocument: LoggedInUser? = null

        every { httpSessionInstance.get() } returns null
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { documentCreationUserStore.findUser(userToken) } returns loggedInUser
        every {
            documentCreationService.getInformationObjecttypeUuid(zaak, "fakeTemplateGroupId", "fakeTemplateId")
        } returns informatieobjecttypeUuid
        every {
            documentCreationService.storeDocument(any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            userWhileStoringDocument = loggedInUserProvider.getLoggedInUser()
            mockk<ZaakInformatieObject>()
        }
        every {
            documentCreationService.documentCreationFinishPageUrl(any(), any(), any(), any())
        } returns URI("https://example.com/finish")

        `when`("the callback is called") {
            documentCreationRestService.createCmmnDocumentForZaakCallback(
                zaakUuid = zaak.uuid,
                templateGroupId = "fakeTemplateGroupId",
                templateId = "fakeTemplateId",
                title = "fakeTitle",
                description = null,
                creationDate = ZonedDateTime.now(),
                userName = "fakeUserDisplayName",
                userToken = userToken,
                fileId = "fakeFileId"
            )

            then("the document is stored as the user that started the document creation") {
                userWhileStoringDocument shouldBe loggedInUser
            }
        }
    }

    given("a SmartDocuments callback whose user token is no longer known") {
        val zaak = createZaak()
        val informatieobjecttypeUuid = UUID.randomUUID()
        val httpSessionInstance = mockk<Instance<HttpSession>>()
        val loggedInUserProvider = LoggedInUserProvider(httpSessionInstance)
        var userWhileStoringDocument: LoggedInUser? = null

        every { httpSessionInstance.get() } returns null
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { documentCreationUserStore.findUser("fakeExpiredToken") } returns null
        every {
            documentCreationService.getInformationObjecttypeUuid(zaak, "fakeTemplateGroupId", "fakeTemplateId")
        } returns informatieobjecttypeUuid
        every {
            documentCreationService.storeDocument(any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            userWhileStoringDocument = loggedInUserProvider.getLoggedInUser()
            mockk<ZaakInformatieObject>()
        }
        every {
            documentCreationService.documentCreationFinishPageUrl(any(), any(), any(), any())
        } returns URI("https://example.com/finish")

        `when`("the callback is called") {
            documentCreationRestService.createCmmnDocumentForZaakCallback(
                zaakUuid = zaak.uuid,
                templateGroupId = "fakeTemplateGroupId",
                templateId = "fakeTemplateId",
                title = "fakeTitle",
                description = null,
                creationDate = ZonedDateTime.now(),
                userName = "fakeUserDisplayName",
                userToken = "fakeExpiredToken",
                fileId = "fakeFileId"
            )

            then("the document is still stored, as the functionele gebruiker") {
                userWhileStoringDocument shouldBe LoggedInUserProvider.FUNCTIONEEL_GEBRUIKER
            }
        }
    }
})
