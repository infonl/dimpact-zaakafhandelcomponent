/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import net.atos.client.smartdocuments.model.document.OutputFormat
import net.atos.client.smartdocuments.model.document.Selection
import net.atos.client.smartdocuments.model.document.SmartDocument
import net.atos.client.smartdocuments.model.document.Variables
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documentcreation.converter.DocumentCreationDataConverter
import net.atos.zac.documentcreation.model.DocumentCreationAttendedResponse
import net.atos.zac.documentcreation.model.DocumentCreationDataAttended
import net.atos.zac.identity.model.getFullName
import net.atos.zac.smartdocuments.SmartDocumentsService
import net.atos.zac.smartdocuments.SmartDocumentsTemplatesService
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.logging.Level
import java.util.logging.Logger

@NoArgConstructor
@ApplicationScoped
@AllOpen
@Suppress("LongParameterList")
class DocumentCreationService @Inject constructor(
    private val smartDocumentsService: SmartDocumentsService,
    private val smartDocumentsTemplatesService: SmartDocumentsTemplatesService,
    private val documentCreationDataConverter: DocumentCreationDataConverter,
    private val enkelvoudigInformatieObjectUpdateService: EnkelvoudigInformatieObjectUpdateService,
    private val configuratieService: ConfiguratieService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    companion object {
        const val OUTPUT_FORMAT_DOCX = "DOCX"
        const val REDIRECT_METHOD = "POST"

        private val LOG = Logger.getLogger(DocumentCreationService::class.java.name)
    }

    fun createDocumentAttended(
        documentCreationDataAttended: DocumentCreationDataAttended
    ): DocumentCreationAttendedResponse =
        documentCreationDataConverter.createData(
            loggedInUser = loggedInUserInstance.get(),
            zaak = documentCreationDataAttended.zaak,
            taskId = documentCreationDataAttended.taskId
        ).runCatching {
            createSmartDocumentForAttendedFlow(documentCreationDataAttended).let {
                smartDocumentsService.createDocumentAttended(
                    data = this,
                    smartDocument = it
                )
            }
        }.onFailure {
            LOG.log(Level.WARNING, "Failed to create document with attended flow", it)
        }.getOrThrow()

    /**
     * Creates a SmartDocument object for the attended flow.
     * In this flow the description of the zaaktype of the zaak in the provided document creation data is used
     * as the SmartDocuments template group.
     */
    private fun createSmartDocumentForAttendedFlow(creationDataUnattended: DocumentCreationDataAttended) =
        SmartDocument(
            selection = Selection(
                templateGroup = smartDocumentsTemplatesService.getTemplateGroupName(
                    creationDataUnattended.templateGroupId
                ),
                template = smartDocumentsTemplatesService.getTemplateName(creationDataUnattended.templateId)
            ),
            variables = Variables(
                outputFormats = listOf(OutputFormat(OUTPUT_FORMAT_DOCX)),
                redirectMethod = REDIRECT_METHOD,
                redirectUrl = configuratieService.documentCreationCallbackUrl(
                    creationDataUnattended.zaak.uuid,
                    creationDataUnattended.taskId,
                    creationDataUnattended.templateGroupId,
                    creationDataUnattended.templateId,
                    creationDataUnattended.title,
                    creationDataUnattended.description,
                    creationDataUnattended.creationDate,
                    creationDataUnattended.author ?: loggedInUserInstance.get().getFullName()
                ).toString()
            )
        )

    /**
     * Download generated SmartDocuments file and store it as Informatieobject
     */
    fun storeDocument(
        zaak: Zaak,
        taskId: String? = null,
        fileId: String,
        templateGroupId: String,
        templateId: String,
        title: String?,
        description: String?,
        creationDate: ZonedDateTime,
        userName: String
    ): ZaakInformatieobject =
        smartDocumentsService.downloadDocument(fileId).let { file ->
            documentCreationDataConverter.toEnkelvoudigInformatieObjectCreateLockRequest(
                zaak = zaak,
                smartDocumentsFile = file,
                smartDocumentsFileType = OUTPUT_FORMAT_DOCX,
                smartDocumentsTemplateGroupId = templateGroupId,
                smartDocumentsTemplateId = templateId,
                title = title,
                description = description,
                creationDate = creationDate,
                userName = userName,
            ).let {
                enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                    zaak = zaak,
                    enkelvoudigInformatieObjectCreateLockRequest = it,
                    taskId = taskId,
                )
            }
        }
}
