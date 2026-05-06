/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.documentcreation

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.HttpMethod
import jakarta.ws.rs.core.UriBuilder
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.zac.util.MediaTypes
import nl.info.client.smartdocuments.model.document.OutputFormat
import nl.info.client.smartdocuments.model.document.Selection
import nl.info.client.smartdocuments.model.document.SmartDocument
import nl.info.client.smartdocuments.model.document.Variables
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuration.ConfigurationService
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
    private val configurationService: ConfigurationService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
) {
    companion object {
        private const val SMART_DOCUMENTS_WIZARD_FINISH_PAGE = "static/smart-documents-result.html"
        private const val SMART_DOCUMENTS_REDIRECT_URL_BASE =
            "rest/document-creation/smartdocuments/callback/zaak/{zaakUuid}"

        private val LOG = Logger.getLogger(DocumentCreationService::class.java.name)
    }

    /**
     * Download a generated SmartDocuments file and store it as an Informatieobject
     */
    fun storeDocument(
        zaak: Zaak,
        taskId: String? = null,
        fileId: String,
        title: String,
        description: String?,
        informatieobjecttypeUuid: UUID,
        creationDate: ZonedDateTime,
        userName: String
    ): ZaakInformatieobject =
        smartDocumentsService.downloadDocument(fileId).let { file ->
            documentCreationDataConverter.toEnkelvoudigInformatieObjectCreateLockRequest(
                file = file,
                format = MediaTypes.Application.MS_WORD_OPEN_XML.mediaType,
                informatieobjecttypeUuid = informatieobjecttypeUuid,
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

    @Suppress("MaxLineLength")
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
            LOG.log(
                Level.WARNING,
                "Failed to create SmartDocument for zaak with uuid: '${documentCreationDataAttended.zaak.uuid}' using attended flow",
                it
            )
        }.getOrThrow()

    fun getInformationObjecttypeUuid(zaak: Zaak, templateGroupId: String, templateId: String) =
        smartDocumentsTemplatesService.getInformationObjectTypeUUID(
            zaaktypeUUID = zaak.zaaktype.extractUuid(),
            templateGroupId = templateGroupId,
            templateId = templateId
        )

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
        val builder = createDocumentCreationUriBuilder(
            title,
            description,
            creationDate,
            userName
        ).apply {
            queryParam("templateId", templateId)
            queryParam("templateGroupId", templateGroupId)
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

    fun createDocumentCreationUriBuilder(
        title: String,
        description: String?,
        creationDate: ZonedDateTime,
        userName: String
    ): UriBuilder =
        UriBuilder
            .fromUri(configurationService.readContextUrl())
            .queryParam("title", title)
            .queryParam("userName", userName)
            .queryParam("creationDate", creationDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .apply {
                if (description != null) {
                    queryParam("description", description)
                }
            }

    fun documentCreationFinishPageUrl(
        zaakId: String,
        taskId: String? = null,
        documentName: String? = null,
        result: String
    ): URI =
        UriBuilder
            .fromUri(configurationService.readContextUrl())
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

    private fun createSmartDocumentForAttendedFlow(creationDataAttended: DocumentCreationDataAttended) =
        SmartDocument(
            selection = Selection(
                templateGroup = smartDocumentsTemplatesService.getTemplateGroupName(
                    creationDataAttended.templateGroupId
                ),
                template = smartDocumentsTemplatesService.getTemplateName(creationDataAttended.templateId)
            ),
            variables = Variables(
                // SmartDocuments use file extensions (without the leading `.`) instead of media types
                // as the output format
                outputFormats = listOf(
                    OutputFormat(MediaTypes.Application.MS_WORD_OPEN_XML.extensions.first().drop(1))
                ),
                redirectMethod = HttpMethod.POST,
                redirectUrl = documentCreationCallbackUrl(
                    zaakUuid = creationDataAttended.zaak.uuid,
                    taskId = creationDataAttended.taskId,
                    templateGroupId = creationDataAttended.templateGroupId,
                    templateId = creationDataAttended.templateId,
                    title = creationDataAttended.title,
                    description = creationDataAttended.description,
                    creationDate = creationDataAttended.creationDate,
                    userName = creationDataAttended.author ?: loggedInUserInstance.get().getFullName(),
                ).toString()
            )
        )
}
