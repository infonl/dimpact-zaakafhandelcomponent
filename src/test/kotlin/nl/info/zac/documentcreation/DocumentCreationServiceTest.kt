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
import io.mockk.verify
import nl.info.client.smartdocuments.model.createFile
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobject
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.documentcreation.converter.DocumentCreationDataConverter
import nl.info.zac.smartdocuments.SmartDocumentsService
import java.time.ZonedDateTime
import java.util.UUID

class DocumentCreationServiceTest : BehaviorSpec({
    val smartDocumentsService = mockk<SmartDocumentsService>()
    val documentCreationDataConverter = mockk<DocumentCreationDataConverter>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val configuratieService: ConfiguratieService = mockk<ConfiguratieService>()
    val documentCreationService = DocumentCreationService(
        smartDocumentsService = smartDocumentsService,
        documentCreationDataConverter = documentCreationDataConverter,
        enkelvoudigInformatieObjectUpdateService = enkelvoudigInformatieObjectUpdateService,
        configuratieService = configuratieService
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
        val zaakInformatieobject = createZaakInformatieobject()

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
        every { configuratieService.readContextUrl() } returns contextUrl

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
})
