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
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.servlet.http.HttpSession
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import nl.info.client.smartdocuments.model.createFile
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
import nl.info.zac.smartdocuments.SmartDocumentsService
import nl.info.zac.smartdocuments.exception.SmartDocumentsDisabledException
import nl.info.zac.smartdocuments.exception.SmartDocumentsUnsupportedOutputFormatException
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import java.util.logging.Logger
import java.util.logging.LogRecord
import java.util.logging.Level
import java.util.logging.Handler

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
    val smartDocumentsService = mockk<SmartDocumentsService>()
    val documentCreationRestService = DocumentCreationRestService(
        policyService = policyService,
        documentCreationService = documentCreationService,
        zrcClientService = zrcClientService,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        flowableTaskService = flowableTaskService,
        loggedInUserInstance = loggedInUserInstance,
        documentCreationUserStore = documentCreationUserStore,
        smartDocumentsService = smartDocumentsService
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
        val documentCreationToken = UUID.randomUUID()
        val informatieobjecttypeUuid = UUID.randomUUID()
        val httpSessionInstance = mockk<Instance<HttpSession>>()
        val loggedInUserProvider = LoggedInUserProvider(httpSessionInstance)
        var userWhileStoringDocument: LoggedInUser? = null

        every { httpSessionInstance.get() } returns null
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { documentCreationUserStore.consumeUser(documentCreationToken, any()) } returns loggedInUser
        every { smartDocumentsService.downloadDocument("fakeFileId") } returns createFile()
        every {
            documentCreationService.getInformationObjecttypeUuid(zaak, "fakeTemplateGroupId", "fakeTemplateId")
        } returns informatieobjecttypeUuid
        every {
            documentCreationService.storeDownloadedDocument(any(), any(), any(), any(), any(), any(), any(), any())
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
                documentCreationToken = documentCreationToken,
                fileId = "fakeFileId"
            )

            then("the document is stored as the user that started the document creation") {
                userWhileStoringDocument shouldBe loggedInUser
            }
        }
    }

    given("a SmartDocuments callback whose document creation token is no longer known") {
        val zaak = createZaak()
        val expiredDocumentCreationToken = UUID.randomUUID()
        val informatieobjecttypeUuid = UUID.randomUUID()
        val httpSession = mockk<HttpSession>()
        val httpSessionInstance = mockk<Instance<HttpSession>>()
        val loggedInUserProvider = LoggedInUserProvider(httpSessionInstance)
        var userWhileStoringDocument: LoggedInUser? = null
        var userWhileReadingZaak: LoggedInUser? = null

        // the browser posting the callback still carries a ZAC session cookie
        every { httpSessionInstance.get() } returns httpSession
        every {
            httpSession.getAttribute(LoggedInUserProvider.LOGGED_IN_USER_SESSION_ATTRIBUTE)
        } returns createLoggedInUser(id = "fakeSessionUserId")
        every { zrcClientService.readZaak(zaak.uuid) } answers {
            userWhileReadingZaak = loggedInUserProvider.getLoggedInUser()
            zaak
        }
        every { documentCreationUserStore.consumeUser(expiredDocumentCreationToken, any()) } returns null
        every { smartDocumentsService.downloadDocument("fakeFileId") } returns createFile()
        every {
            documentCreationService.getInformationObjecttypeUuid(zaak, "fakeTemplateGroupId", "fakeTemplateId")
        } returns informatieobjecttypeUuid
        every {
            documentCreationService.storeDownloadedDocument(any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            userWhileStoringDocument = loggedInUserProvider.getLoggedInUser()
            mockk<ZaakInformatieObject>()
        }
        every {
            documentCreationService.documentCreationFinishPageUrl(any(), any(), any(), any())
        } returns URI("https://example.com/finish")

        `when`("the callback is called") {
            val logRecords = captureLogRecords {
                documentCreationRestService.createCmmnDocumentForZaakCallback(
                    zaakUuid = zaak.uuid,
                    templateGroupId = "fakeTemplateGroupId",
                    templateId = "fakeTemplateId",
                    title = "fakeTitle",
                    description = null,
                    creationDate = ZonedDateTime.now(),
                    userName = "fakeUserDisplayName",
                    documentCreationToken = expiredDocumentCreationToken,
                    fileId = "fakeFileId"
                )
            }

            then("the document is still stored, as the functionele gebruiker") {
                userWhileStoringDocument shouldBe LoggedInUserProvider.FUNCTIONEEL_GEBRUIKER
            }

            and("the reads the callback makes along the way run as that same user") {
                userWhileReadingZaak shouldBe LoggedInUserProvider.FUNCTIONEEL_GEBRUIKER
            }

            and("the fallback is logged, so a lost token does not go unnoticed") {
                logRecords.any { it.level == Level.WARNING && it.message.contains(zaak.uuid.toString()) } shouldBe true
            }
        }
    }

    given("a SmartDocuments callback whose session belongs to a different user than its token") {
        val zaak = createZaak()
        val documentCreationUser = createLoggedInUser(id = "fakeDocumentCreationUserId")
        val sessionUser = createLoggedInUser(id = "fakeSessionUserId")
        val documentCreationToken = UUID.randomUUID()
        val informatieobjecttypeUuid = UUID.randomUUID()
        val httpSession = mockk<HttpSession>()
        val httpSessionInstance = mockk<Instance<HttpSession>>()
        val loggedInUserProvider = LoggedInUserProvider(httpSessionInstance)
        var userWhileStoringDocument: LoggedInUser? = null
        var userWhileReadingZaak: LoggedInUser? = null

        every { httpSessionInstance.get() } returns httpSession
        every {
            httpSession.getAttribute(LoggedInUserProvider.LOGGED_IN_USER_SESSION_ATTRIBUTE)
        } returns sessionUser
        every { zrcClientService.readZaak(zaak.uuid) } answers {
            userWhileReadingZaak = loggedInUserProvider.getLoggedInUser()
            zaak
        }
        every { documentCreationUserStore.consumeUser(documentCreationToken, any()) } returns documentCreationUser
        every { smartDocumentsService.downloadDocument("fakeFileId") } returns createFile()
        every {
            documentCreationService.getInformationObjecttypeUuid(zaak, "fakeTemplateGroupId", "fakeTemplateId")
        } returns informatieobjecttypeUuid
        every {
            documentCreationService.storeDownloadedDocument(any(), any(), any(), any(), any(), any(), any(), any())
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
                documentCreationToken = documentCreationToken,
                fileId = "fakeFileId"
            )

            then("the token decides who created the document, not the session") {
                userWhileStoringDocument shouldBe documentCreationUser
            }

            and("the reads the callback makes along the way run as that same user") {
                userWhileReadingZaak shouldBe documentCreationUser
            }
        }
    }

    given("a SmartDocuments callback for a wizard that was cancelled") {
        val zaak = createZaak()
        val documentCreationToken = UUID.randomUUID()
        val httpSessionInstance = mockk<Instance<HttpSession>>()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { documentCreationUserStore.consumeUser(documentCreationToken, any()) } returns createLoggedInUser()
        every {
            documentCreationService.documentCreationFinishPageUrl(any(), any(), any(), any())
        } returns URI("https://example.com/finish")

        `when`("the callback is called without a document") {
            documentCreationRestService.createCmmnDocumentForZaakCallback(
                zaakUuid = zaak.uuid,
                templateGroupId = "fakeTemplateGroupId",
                templateId = "fakeTemplateId",
                title = "fakeTitle",
                description = null,
                creationDate = ZonedDateTime.now(),
                userName = "fakeUserDisplayName",
                documentCreationToken = documentCreationToken,
                fileId = ""
            )

            then("the token is still spent, so a cancelled wizard cannot leave it open to replay") {
                verify(exactly = 1) { documentCreationUserStore.consumeUser(documentCreationToken, any()) }
            }
        }
    }

    given("a SmartDocuments callback whose document cannot be stored") {
        val zaak = createZaak()
        val documentCreationToken = UUID.randomUUID()
        val informatieobjecttypeUuid = UUID.randomUUID()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { documentCreationUserStore.consumeUser(documentCreationToken, any()) } returns createLoggedInUser()
        every { smartDocumentsService.downloadDocument("fakeFileId") } returns createFile()
        every {
            documentCreationService.getInformationObjecttypeUuid(zaak, "fakeTemplateGroupId", "fakeTemplateId")
        } returns informatieobjecttypeUuid
        every {
            documentCreationService.storeDownloadedDocument(any(), any(), any(), any(), any(), any(), any(), any())
        } throws IllegalStateException("fakeStoreFailure")
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
                documentCreationToken = documentCreationToken,
                fileId = "fakeFileId"
            )

            then("the wizard is sent to the failure page instead of the error reaching SmartDocuments") {
                verify(exactly = 1) {
                    documentCreationService.documentCreationFinishPageUrl(any(), any(), any(), "failure")
                }
            }
        }
    }

    given("a SmartDocuments callback whose downloaded document has an unsupported output format") {
        val zaak = createZaak()
        val documentCreationToken = UUID.randomUUID()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { documentCreationUserStore.consumeUser(documentCreationToken, any()) } returns createLoggedInUser()
        every {
            smartDocumentsService.downloadDocument("fakeFileId")
        } throws SmartDocumentsUnsupportedOutputFormatException("fakeUnsupportedFormat")
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
                documentCreationToken = documentCreationToken,
                fileId = "fakeFileId"
            )

            then("the wizard is sent to the unsupported-output-format page") {
                verify(exactly = 1) {
                    documentCreationService.documentCreationFinishPageUrl(any(), any(), any(), "unsupported-output-format")
                }
            }

and("the file is downloaded before the zaak is read") {
    io.mockk.verifyOrder {
        smartDocumentsService.downloadDocument("fakeFileId")
        zrcClientService.readZaak(zaak.uuid)
    }
    verify(exactly = 1) { zrcClientService.readZaak(zaak.uuid) }
}
        }
    }
})

private fun captureLogRecords(block: () -> Unit): List<LogRecord> {
    val logger = Logger.getLogger(DocumentCreationRestService::class.java.name)
    val records = mutableListOf<LogRecord>()
    val handler = object : Handler() {
        override fun publish(record: LogRecord) {
            records.add(record)
        }
        override fun flush() = Unit
        override fun close() = Unit
    }
    logger.addHandler(handler)
    try {
        block()
    } finally {
        logger.removeHandler(handler)
    }
    return records
}
