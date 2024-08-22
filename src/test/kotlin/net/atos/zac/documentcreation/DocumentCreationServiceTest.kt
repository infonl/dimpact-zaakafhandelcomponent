package net.atos.zac.documentcreation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.enterprise.inject.Instance
import net.atos.client.smartdocuments.model.document.OutputFormat
import net.atos.client.smartdocuments.model.document.SmartDocument
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createInformatieObjectType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.documentcreation.converter.DocumentCreationDataConverter
import net.atos.zac.documentcreation.model.createData
import net.atos.zac.documentcreation.model.createDocumentCreationAttendedResponse
import net.atos.zac.documentcreation.model.createDocumentCreationDataAttended
import net.atos.zac.documentcreation.model.createDocumentCreationDataUnattended
import net.atos.zac.documentcreation.model.createDocumentCreationUnattendedResponse
import net.atos.zac.smartdocuments.SmartDocumentsService
import net.atos.zac.smartdocuments.SmartDocumentsTemplatesService
import java.net.URI
import java.util.UUID

class DocumentCreationServiceTest : BehaviorSpec({
    val smartDocumentsService = mockk<SmartDocumentsService>()
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
    val documentCreationDataConverter = mockk<DocumentCreationDataConverter>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val documentCreationService = DocumentCreationService(
        smartDocumentsService = smartDocumentsService,
        smartDocumentsTemplatesService = smartDocumentsTemplatesService,
        documentCreationDataConverter = documentCreationDataConverter,
        loggedInUserInstance = loggedInUserInstance,
        ztcClientService = ztcClientService,
        zrcClientService = zrcClientService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Document creation data with a zaak and an information object type") {
        val zaakTypeUUID = UUID.randomUUID()
        val zaakTypeURI = URI("https://example.com/$zaakTypeUUID")
        val zaakType = createZaakType(uri = zaakTypeURI)
        val documentCreationData = createDocumentCreationDataAttended(
            zaak = createZaak(zaakTypeURI = zaakTypeURI),
            informatieobjecttype = createInformatieObjectType()
        )
        val externalZaakUrl = URI("https://example.com/dummyExternalZaakUrl")
        val loggedInUser = createLoggedInUser()
        val data = createData()
        val documentCreationAttendedResponse = createDocumentCreationAttendedResponse()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { zrcClientService.createUrlExternToZaak(documentCreationData.zaak.uuid) } returns externalZaakUrl
        every {
            documentCreationDataConverter.createData(
                loggedInUser,
                documentCreationData.zaak,
                documentCreationData.taskId
            )
        } returns data
        every { ztcClientService.readZaaktype(documentCreationData.zaak.zaaktype) } returns zaakType
        every {
            smartDocumentsService.createDocumentAttended(any(), any(), any())
        } returns documentCreationAttendedResponse

        When("the 'create document attended' method is called") {
            val documentCreationResponse = documentCreationService.createDocumentAttended(documentCreationData)

            Then(
                """
                the smart documents service is called to create an attended document and a document creation response is returned
                """
            ) {
                with(documentCreationResponse) {
                    redirectUrl shouldBe documentCreationAttendedResponse.redirectUrl
                    message shouldBe null
                }
            }
        }
    }
    Given("Document creation data with a zaak, a template group name and a template name") {
        val templateGroupId = "1"
        val templateId = "2"
        val templateGroupName = "dummyTemplateGroupName"
        val templateName = "dummyTemplateName"
        val documentCreationData = createDocumentCreationDataUnattended(
            templateGroupId = templateGroupId,
            templateId = templateId,
            zaak = createZaak()
        )
        val loggedInUser = createLoggedInUser()
        val data = createData()
        val message = "dummyMessage"
        val documentCreationUnattendedResponse = createDocumentCreationUnattendedResponse(message)
        val smartDocumentSlot = slot<SmartDocument>()
        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            documentCreationDataConverter.createData(
                loggedInUser,
                documentCreationData.zaak,
                documentCreationData.taskId
            )
        } returns data
        every { smartDocumentsTemplatesService.getTemplateGroupName(templateGroupId) } returns templateGroupName
        every { smartDocumentsTemplatesService.getTemplateName(templateId) } returns templateName
        every {
            smartDocumentsService.createDocumentUnattended(data, capture(smartDocumentSlot))
        } returns documentCreationUnattendedResponse

        When("the 'create document unattended' method is called") {
            val documentCreationResponse = documentCreationService.createDocumentUnattended(documentCreationData)

            Then(
                """
                the create unattended SmartDocuments document method is called with the template group and name and 
                 outputformat 'docx' and a document creation response is returned
                """
            ) {
                with(documentCreationResponse) {
                    this.message shouldBe message
                }
                with(smartDocumentSlot.captured) {
                    selection.templateGroup shouldBe templateGroupName
                    selection.template shouldBe templateName
                    variables!!.outputFormats shouldBe listOf(OutputFormat("docx"))
                }
            }
        }
    }
})
