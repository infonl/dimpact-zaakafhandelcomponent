/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.documentcreation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import nl.info.client.smartdocuments.model.createFile
import nl.info.client.smartdocuments.model.document.Data
import nl.info.client.smartdocuments.model.document.SmartDocument
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.documentcreation.converter.DocumentCreationDataConverter
import nl.info.zac.documentcreation.model.createData
import nl.info.zac.documentcreation.model.createDocumentCreationAttendedResponse
import nl.info.zac.documentcreation.model.createDocumentCreationDataAttended
import nl.info.zac.smartdocuments.SmartDocumentsService
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import java.net.URI
import java.net.URLEncoder
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class DocumentCreationServiceTest : BehaviorSpec({
    val smartDocumentsService = mockk<SmartDocumentsService>()
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
    val documentCreationDataConverter = mockk<DocumentCreationDataConverter>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val configurationService: ConfigurationService = mockk<ConfigurationService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val documentCreationService = DocumentCreationService(
        smartDocumentsService = smartDocumentsService,
        smartDocumentsTemplatesService = smartDocumentsTemplatesService,
        documentCreationDataConverter = documentCreationDataConverter,
        enkelvoudigInformatieObjectUpdateService = enkelvoudigInformatieObjectUpdateService,
        configurationService = configurationService,
        loggedInUserInstance = loggedInUserInstance
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Generated document information") {
        val smartDocumentId = "1"
        val taakId = "4"
        val title = "title"
        val description = "description"
        val informatieobjecttypeUuid = UUID.randomUUID()
        val creationDate = ZonedDateTime.now()
        val userName = "Full Name"
        val zaak = createZaak()
        val downloadedFile = createFile()
        val enkelvoudigInformatieObjectLockRequest = createEnkelvoudigInformatieObjectCreateLockRequest()
        val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates()

        every { smartDocumentsService.downloadDocument(smartDocumentId) } returns downloadedFile
        every {
            documentCreationDataConverter.toEnkelvoudigInformatieObjectCreateLockRequest(
                downloadedFile,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                title,
                description,
                informatieobjecttypeUuid,
                creationDate,
                userName
            )
        } returns enkelvoudigInformatieObjectLockRequest
        every {
            enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                zaak = zaak,
                enkelvoudigInformatieObjectCreateLockRequest = enkelvoudigInformatieObjectLockRequest,
                taskId = taakId,
                skipPolicyCheck = true
            )
        } returns zaakInformatieobject

        When("storing a downloaded file is requested") {
            val returnedZaakInformatieobject = documentCreationService.storeDocument(
                zaak = zaak,
                taskId = taakId,
                fileId = smartDocumentId,
                title = title,
                description = description,
                informatieobjecttypeUuid = informatieobjecttypeUuid,
                creationDate = creationDate,
                userName = userName
            )

            Then("ZaakInformatieobject is stored") {
                returnedZaakInformatieobject shouldBe zaakInformatieobject

                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                        zaak = zaak,
                        enkelvoudigInformatieObjectCreateLockRequest = enkelvoudigInformatieObjectLockRequest,
                        taskId = taakId,
                        skipPolicyCheck = true
                    )
                }
            }
        }
    }

    Given("SmartDocuments wizard finished execution") {
        val contextUrl = "https://example.com"
        every { configurationService.readContextUrl() } returns contextUrl

        When("SmartDocuments finish page URL is requested") {
            val finishPageUrl = documentCreationService.documentCreationFinishPageUrl(
                "1",
                "1",
                "document name",
                "result"
            )

            Then("correct URL is built") {
                finishPageUrl.toString() shouldBe "$contextUrl/static/smart-documents-result.html" +
                    "?zaak=1" +
                    "&taak=1" +
                    "&doc=document+name" +
                    "&result=result"
            }
        }
    }

    Given("Document creation data with a zaak and an information object type") {
        val zaakTypeUUID = UUID.randomUUID()
        val zaakTypeURI = URI("https://example.com/$zaakTypeUUID")
        val zaak = createZaak(zaaktypeUri = zaakTypeURI)
        val taskId = "fakeTaskId"
        val documentCreationData = createDocumentCreationDataAttended(
            zaak = zaak,
            taskId = taskId
        )
        val userDisplayName = "fakeDisplayName"
        val loggedInUser = createLoggedInUser(
            displayName = userDisplayName
        )
        val data = createData()
        val documentCreationAttendedResponse = createDocumentCreationAttendedResponse()
        val contextUrl = "https://example.com"
        val templateGroupName = "fakeTemplateGroupName"
        val templateName = "fakeTemplateName"
        val dataSlot = slot<Data>()
        val smartDocumentSlot = slot<SmartDocument>()

        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            documentCreationDataConverter.createData(
                loggedInUser,
                documentCreationData.zaak,
                documentCreationData.taskId
            )
        } returns data
        every {
            smartDocumentsService.createDocumentAttended(capture(dataSlot), capture(smartDocumentSlot))
        } returns documentCreationAttendedResponse
        every {
            smartDocumentsTemplatesService.getTemplateGroupName(documentCreationData.templateGroupId)
        } returns templateGroupName
        every {
            smartDocumentsTemplatesService.getTemplateName(documentCreationData.templateId)
        } returns templateName
        every { configurationService.readContextUrl() } returns contextUrl

        When("the 'create document attended' method is called") {
            val documentCreationResponse = documentCreationService.createDocumentAttended(documentCreationData)

            Then(
                """
                the smart documents service is called to create an attended document and a document creation response is returned
                """
            ) {
                with(documentCreationResponse) {
                    redirectUrl shouldBe documentCreationAttendedResponse.redirectUrl
                    message shouldBe documentCreationAttendedResponse.message
                }
                with(smartDocumentSlot.captured) {
                    selection.templateGroup shouldBe templateGroupName
                    selection.template shouldBe templateName
                    with(variables!!) {
                        outputFormats.size shouldBe 1
                        outputFormats[0].outputFormat shouldBe "docx"
                        redirectMethod shouldBe "POST"
                        redirectUrl shouldBe "$contextUrl/rest/document-creation/smartdocuments/callback" +
                            "/zaak/${zaak.uuid}" +
                            "/task/$taskId" +
                            "?title=${documentCreationData.title}" +
                            "&userName=$userDisplayName" +
                            "&creationDate=${URLEncoder.encode(
                                documentCreationData.creationDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                Charsets.UTF_8
                            )}" +
                            "&templateId=${documentCreationData.templateId}" +
                            "&templateGroupId=${documentCreationData.templateGroupId}"
                    }
                }
            }
        }
    }

    Given("A zaak exists") {
        val contextUrl = "https://example.com:2222"
        val zaakUuid = UUID.randomUUID()
        val templateGroupId = "groupId"
        val templateId = "templateId"
        val title = "title"
        val description = "description"
        val creationDate = ZonedDateTime.of(2024, 10, 7, 0, 0, 0, 0, ZoneOffset.UTC)
        val userName = "Full User Name"

        every { configurationService.readContextUrl() } returns contextUrl

        When("Document creation URL is requested for zaak") {
            val uri = documentCreationService.documentCreationCallbackUrl(
                zaakUuid = zaakUuid,
                null,
                templateGroupId,
                templateId,
                title,
                description,
                creationDate,
                userName
            )

            Then("Correct URL is provided") {
                uri.toString() shouldBe "$contextUrl/rest/document-creation/smartdocuments/callback/zaak/$zaakUuid" +
                    "?title=$title" +
                    "&userName=Full+User+Name" +
                    "&creationDate=2024-10-07T00%3A00%3A00Z" +
                    "&description=$description" +
                    "&templateId=$templateId" +
                    "&templateGroupId=$templateGroupId"
            }
        }

        When("Document creation URL is requested for taak") {
            val taakUuid = UUID.randomUUID().toString()
            val uri = documentCreationService.documentCreationCallbackUrl(
                zaakUuid,
                taakUuid,
                templateGroupId,
                templateId,
                title,
                description,
                creationDate,
                userName
            )

            Then("Correct URL is provided") {
                uri.toString() shouldBe
                    "$contextUrl/rest/document-creation/smartdocuments/callback/zaak/$zaakUuid/task/$taakUuid" +
                    "?title=$title" +
                    "&userName=Full+User+Name" +
                    "&creationDate=2024-10-07T00%3A00%3A00Z" +
                    "&description=$description" +
                    "&templateId=$templateId" +
                    "&templateGroupId=$templateGroupId"
            }
        }
    }
})
