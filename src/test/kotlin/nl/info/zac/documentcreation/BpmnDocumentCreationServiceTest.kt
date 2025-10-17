/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.documentcreation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.enterprise.inject.Instance
import nl.info.client.smartdocuments.model.document.Data
import nl.info.client.smartdocuments.model.document.SmartDocument
import nl.info.client.zgw.model.createZaak
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.documentcreation.converter.DocumentCreationDataConverter
import nl.info.zac.documentcreation.model.createBpmnDocumentCreationDataAttended
import nl.info.zac.documentcreation.model.createData
import nl.info.zac.documentcreation.model.createDocumentCreationAttendedResponse
import nl.info.zac.smartdocuments.SmartDocumentsService
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import java.net.URI
import java.net.URLEncoder
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class BpmnDocumentCreationServiceTest : BehaviorSpec({
    val smartDocumentsService = mockk<SmartDocumentsService>()
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
    val documentCreationDataConverter = mockk<DocumentCreationDataConverter>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val configuratieService: ConfiguratieService = mockk<ConfiguratieService>()
    val documentCreationService = DocumentCreationService(
        smartDocumentsService = smartDocumentsService,
        documentCreationDataConverter = documentCreationDataConverter,
        enkelvoudigInformatieObjectUpdateService = enkelvoudigInformatieObjectUpdateService,
        configuratieService = configuratieService
    )
    val bpmnDocumentCreationService = BpmnDocumentCreationService(
        smartDocumentsService = smartDocumentsService,
        smartDocumentsTemplatesService = smartDocumentsTemplatesService,
        documentCreationDataConverter = documentCreationDataConverter,
        documentCreationService = documentCreationService,
        loggedInUserInstance = loggedInUserInstance
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Document creation data with a zaak and an information object type") {
        val zaakTypeUUID = UUID.randomUUID()
        val zaakTypeURI = URI("https://example.com/$zaakTypeUUID")
        val zaak = createZaak(zaakTypeURI = zaakTypeURI)
        val taskId = "fakeTaskId"
        val documentCreationData = createBpmnDocumentCreationDataAttended(
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
        every { configuratieService.readContextUrl() } returns contextUrl

        When("the 'create document attended' method is called") {
            val documentCreationResponse = bpmnDocumentCreationService.createBpmnDocumentAttended(documentCreationData)

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
                    selection.templateGroup shouldBe "fakeGroup"
                    selection.template shouldBe "fakeTemplate"
                    with(variables!!) {
                        outputFormats.size shouldBe 1
                        outputFormats[0].outputFormat shouldBe "docx"
                        redirectMethod shouldBe "POST"
                        redirectUrl shouldBe "$contextUrl/rest/document-creation/smartdocuments/bpmn-callback" +
                            "/zaak/${zaak.uuid}" +
                            "/task/$taskId" +
                            "?title=${URLEncoder.encode(documentCreationData.title, Charsets.UTF_8)}" +
                            "&userName=${URLEncoder.encode(userDisplayName, Charsets.UTF_8)}" +
                            "&creationDate=${URLEncoder.encode(
                                documentCreationData.creationDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                Charsets.UTF_8
                            )}" +
                            "&templateName=" +
                            "${URLEncoder.encode(documentCreationData.templateName, Charsets.UTF_8)}" +
                            "&templateGroupName=" +
                            "${URLEncoder.encode(documentCreationData.templateGroupName, Charsets.UTF_8)}" +
                            "&informatieobjecttypeUuid=" + documentCreationData.informatieobjecttypeUuid
                    }
                }
            }
        }
    }

    Given("A zaak exists") {
        val contextUrl = "https://example.com:2222"
        val zaakUuid = UUID.randomUUID()
        val informatieobjecttypeUuid = UUID.randomUUID()
        val templateGroupName = "groupName"
        val templateName = "templateName"
        val title = "title"
        val description = "description"
        val creationDate = ZonedDateTime.of(2024, 10, 7, 0, 0, 0, 0, ZoneOffset.UTC)
        val userName = "Full User Name"

        every { configuratieService.readContextUrl() } returns contextUrl

        When("Document creation URL is requested without taak id") {
            val exception = shouldThrow<IllegalArgumentException> {
                bpmnDocumentCreationService.documentCreationCallbackUrl(
                    zaakUuid = zaakUuid,
                    null,
                    informatieobjecttypeUuid,
                    templateGroupName,
                    templateName,
                    title,
                    description,
                    creationDate,
                    userName
                )
            }

            Then("correct exception is thrown") {
                exception.message shouldContain "taskId"
            }
        }

        When("Document creation URL is requested for taak") {
            val taakId = "12"
            val uri = bpmnDocumentCreationService.documentCreationCallbackUrl(
                zaakUuid,
                taakId,
                informatieobjecttypeUuid,
                templateGroupName,
                templateName,
                title,
                description,
                creationDate,
                userName
            )

            Then("Correct URl is provided") {
                uri.toString() shouldBe
                    "$contextUrl/rest/document-creation/smartdocuments/bpmn-callback/zaak/$zaakUuid/task/$taakId" +
                    "?title=$title" +
                    "&userName=Full+User+Name" +
                    "&creationDate=2024-10-07T00%3A00%3A00Z" +
                    "&description=$description" +
                    "&templateName=$templateName" +
                    "&templateGroupName=$templateGroupName" +
                    "&informatieobjecttypeUuid=$informatieobjecttypeUuid"
            }
        }
    }
})
