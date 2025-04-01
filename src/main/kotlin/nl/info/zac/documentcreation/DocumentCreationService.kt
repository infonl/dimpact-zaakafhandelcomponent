/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.documentcreation

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.core.UriBuilder
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import net.atos.zac.util.MediaTypes
import nl.info.client.smartdocuments.model.document.OutputFormat
import nl.info.client.smartdocuments.model.document.Selection
import nl.info.client.smartdocuments.model.document.SmartDocument
import nl.info.client.smartdocuments.model.document.Variables
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.documentcreation.converter.DocumentCreationDataConverter
import nl.info.zac.documentcreation.model.DocumentCreationAttendedResponse
import nl.info.zac.documentcreation.model.DocumentCreationDataAttended
import nl.info.zac.identity.model.getFullName
import nl.info.zac.smartdocuments.SmartDocumentsService
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
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
        const val REDIRECT_METHOD = "POST"

        private const val SMART_DOCUMENTS_REDIRECT_URL_BASE =
            "rest/document-creation/smartdocuments/callback/zaak/{zaakUuid}"
        private const val SMART_DOCUMENTS_WIZARD_FINISH_PAGE = "static/smart-documents-result.html"

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
                outputFormats = listOf(OutputFormat(MediaTypes.Application.MS_WORD_OPEN_XML.mediaType)),
                redirectMethod = REDIRECT_METHOD,
                redirectUrl = documentCreationCallbackUrl(
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
        title: String,
        description: String?,
        creationDate: ZonedDateTime,
        userName: String
    ): ZaakInformatieobject =
        smartDocumentsService.downloadDocument(fileId).let { file ->
            documentCreationDataConverter.toEnkelvoudigInformatieObjectCreateLockRequest(
                zaak = zaak,
                file = file,
                format = MediaTypes.Application.MS_WORD_OPEN_XML.mediaType,
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
                    // We open SmartDocuments in a new tab. This means that authorization token we have from Keycloak
                    // will expire in some time (60-90 seconds usually). After this time no policy checks can be done,
                    // as we no longer have a valid token. All policy checks need to be performed on document creation
                    // request time.
                    skipPolicyCheck = true
                )
            }
        }

    fun documentCreationCallbackUrl(
        zaakUuid: UUID,
        taskId: String?,
        templateGroupId: String,
        templateId: String,
        title: String,
        description: String?,
        creationDate: ZonedDateTime,
        userName: String
    ): URI {
        val builder = UriBuilder
            .fromUri(configuratieService.readContextUrl())
            .queryParam("templateId", templateId)
            .queryParam("templateGroupId", templateGroupId)
            .queryParam("title", title)
            .queryParam("userName", userName)
            .queryParam("creationDate", creationDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))

        if (description != null) {
            builder.queryParam("description", description)
        }

        return if (taskId != null) {
            builder
                .path("$SMART_DOCUMENTS_REDIRECT_URL_BASE/task/{taskId}")
                .build(zaakUuid.toString(), taskId)
        } else {
            builder
                .path(SMART_DOCUMENTS_REDIRECT_URL_BASE)
                .build(zaakUuid.toString())
        }
    }

    fun documentCreationFinishPageUrl(
        zaakId: String,
        taskId: String? = null,
        documentName: String? = null,
        result: String
    ): URI =
        UriBuilder
            .fromUri(configuratieService.readContextUrl())
            .path(SMART_DOCUMENTS_WIZARD_FINISH_PAGE)
            .queryParam("zaak", zaakId)
            .apply {
                if (taskId != null) {
                    queryParam("taak", taskId)
                }
                if (documentName != null) {
                    queryParam("doc", documentName)
                }
            }
            .queryParam("result", result)
            .build()
}
