/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.documentcreation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.client.smartdocuments.model.createFile
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObjectCreateLockRequest
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakInformatieobject
import net.atos.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documentcreation.converter.DocumentCreationDataConverter
import net.atos.zac.documentcreation.model.createData
import net.atos.zac.documentcreation.model.createDocumentCreationAttendedResponse
import net.atos.zac.documentcreation.model.createDocumentCreationDataAttended
import net.atos.zac.identity.model.User
import net.atos.zac.identity.model.getFullName
import net.atos.zac.smartdocuments.SmartDocumentsService
import net.atos.zac.smartdocuments.SmartDocumentsTemplatesService
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

class DocumentCreationServiceTest : BehaviorSpec({
    val smartDocumentsService = mockk<SmartDocumentsService>()
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
    val documentCreationDataConverter = mockk<DocumentCreationDataConverter>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val configuratieService: ConfiguratieService = mockk<ConfiguratieService>()
    val documentCreationService = DocumentCreationService(
        smartDocumentsService = smartDocumentsService,
        smartDocumentsTemplatesService = smartDocumentsTemplatesService,
        documentCreationDataConverter = documentCreationDataConverter,
        enkelvoudigInformatieObjectUpdateService = enkelvoudigInformatieObjectUpdateService,
        configuratieService = configuratieService,
        loggedInUserInstance = loggedInUserInstance
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Document creation data with a zaak and an information object type") {
        val zaakTypeUUID = UUID.randomUUID()
        val zaakTypeURI = URI("https://example.com/$zaakTypeUUID")
        val documentCreationData = createDocumentCreationDataAttended(
            zaak = createZaak(zaakTypeURI = zaakTypeURI)
        )
        val loggedInUser = createLoggedInUser()
        val data = createData()
        val documentCreationAttendedResponse = createDocumentCreationAttendedResponse()
        val fullName = "Full Name"

        every { loggedInUserInstance.get() } returns loggedInUser
        mockkStatic(User::getFullName)
        every { any<User>().getFullName() } returns fullName
        every {
            documentCreationDataConverter.createData(
                loggedInUserInstance.get(),
                documentCreationData.zaak,
                documentCreationData.taskId
            )
        } returns data
        every { smartDocumentsService.createDocumentAttended(any(), any()) } returns documentCreationAttendedResponse
        every {
            smartDocumentsTemplatesService.getTemplateGroupName(documentCreationData.templateGroupId)
        } returns UUID.randomUUID().toString()
        every {
            smartDocumentsTemplatesService.getTemplateName(documentCreationData.templateId)
        } returns UUID.randomUUID().toString()
        every {
            configuratieService.documentCreationCallbackUrl(
                documentCreationData.zaak.uuid,
                documentCreationData.taskId,
                documentCreationData.templateGroupId,
                documentCreationData.templateId,
                documentCreationData.title,
                documentCreationData.description,
                documentCreationData.creationDate,
                fullName
            )
        } returns URI("https://sd.host.com")

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
            }
        }
    }

    Given("Generated document information") {
        val smartDocumentId = "1"
        val templateGroupId = "2"
        val templateId = "3"
        val taakId = "4"
        val title = "title"
        val description = "description"
        val creationDate = ZonedDateTime.now()
        val userName = "Full Name"
        val zaak = createZaak()
        val downloadedFile = createFile()
        val enkelvoudigInformatieObjectLockRequest = createEnkelvoudigInformatieObjectCreateLockRequest()
        val zaakInformatieobject = createZaakInformatieobject()

        every { smartDocumentsService.downloadDocument(smartDocumentId) } returns downloadedFile
        every {
            documentCreationDataConverter.toEnkelvoudigInformatieObjectCreateLockRequest(
                zaak,
                downloadedFile,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                templateGroupId,
                templateId,
                title,
                description,
                creationDate,
                userName
            )
        } returns enkelvoudigInformatieObjectLockRequest
        every {
            enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                zaak,
                enkelvoudigInformatieObjectLockRequest,
                taakId
            )
        } returns zaakInformatieobject

        When("storing a downloaded file is requested") {
            val returnedZaakInformatieobject = documentCreationService.storeDocument(
                zaak = zaak,
                taskId = taakId,
                fileId = smartDocumentId,
                templateGroupId = templateGroupId,
                templateId = templateId,
                title = title,
                description = description,
                creationDate = creationDate,
                userName = userName
            )

            Then("ZaakInformatieobject is stored") {
                returnedZaakInformatieobject shouldBe zaakInformatieobject

                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                        zaak,
                        enkelvoudigInformatieObjectLockRequest,
                        taakId
                    )
                }
            }
        }
    }
})
