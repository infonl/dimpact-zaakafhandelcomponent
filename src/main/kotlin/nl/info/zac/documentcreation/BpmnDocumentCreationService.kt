/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.documentcreation

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.HttpMethod
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.util.MediaTypes
import nl.info.client.smartdocuments.model.document.OutputFormat
import nl.info.client.smartdocuments.model.document.Selection
import nl.info.client.smartdocuments.model.document.SmartDocument
import nl.info.client.smartdocuments.model.document.Variables
import nl.info.client.zgw.util.extractUuid
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.documentcreation.converter.DocumentCreationDataConverter
import nl.info.zac.documentcreation.model.BpmnDocumentCreationDataAttended
import nl.info.zac.documentcreation.model.DocumentCreationAttendedResponse
import nl.info.zac.identity.model.getFullName
import nl.info.zac.smartdocuments.SmartDocumentsService
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

@NoArgConstructor
@ApplicationScoped
@AllOpen
@Suppress("LongParameterList")
class BpmnDocumentCreationService @Inject constructor(
    private val smartDocumentsService: SmartDocumentsService,
    private val smartDocumentsTemplatesService: SmartDocumentsTemplatesService,
    private val documentCreationService: DocumentCreationService,
    private val documentCreationDataConverter: DocumentCreationDataConverter,
    private val loggedInUserInstance: Instance<LoggedInUser>,
) {
    companion object {
        private const val SMART_DOCUMENTS_REDIRECT_URL_BASE =
            "rest/document-creation/smartdocuments/bpmn-callback/zaak/{zaakUuid}"

        private val LOG = Logger.getLogger(BpmnDocumentCreationService::class.java.name)
    }

    fun createBpmnDocumentAttended(
        documentCreationDataAttended: BpmnDocumentCreationDataAttended
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
    private fun createSmartDocumentForAttendedFlow(creationDataUnattended: BpmnDocumentCreationDataAttended) =
        SmartDocument(
            selection = Selection(
                templateGroup = creationDataUnattended.templateGroupName,
                template = creationDataUnattended.templateName,
            ),
            variables = Variables(
                // SmartDocuments use file extensions (without the leading `.`) instead of media types
                // as the output format
                outputFormats = listOf(
                    OutputFormat(MediaTypes.Application.MS_WORD_OPEN_XML.extensions.first().drop(1))
                ),
                redirectMethod = HttpMethod.POST,
                redirectUrl = documentCreationCallbackUrl(
                    zaakUuid = creationDataUnattended.zaak.uuid,
                    taskId = creationDataUnattended.taskId,
                    informatieobjecttypeUuid = creationDataUnattended.informatieobjecttypeUuid,
                    templateGroupName = creationDataUnattended.templateGroupName,
                    templateName = creationDataUnattended.templateName,
                    title = creationDataUnattended.title,
                    description = creationDataUnattended.description,
                    creationDate = creationDataUnattended.creationDate,
                    userName = creationDataUnattended.author ?: loggedInUserInstance.get().getFullName(),
                ).toString()
            )
        )

    fun getInformationObjecttypeUuid(zaak: Zaak, templateGroupId: String, templateId: String) =
        smartDocumentsTemplatesService.getInformationObjectTypeUUID(
            zaakafhandelParametersUUID = zaak.zaaktype.extractUuid(),
            templateGroupId = templateGroupId,
            templateId = templateId
        )

    fun documentCreationCallbackUrl(
        zaakUuid: UUID,
        taskId: String?,
        informatieobjecttypeUuid: UUID,
        templateGroupName: String,
        templateName: String,
        title: String,
        description: String?,
        creationDate: ZonedDateTime,
        userName: String,
    ) =
        documentCreationService.createDocumentCreationUriBulder(title, description, creationDate, userName).apply {
            queryParam("templateName", templateName)
            queryParam("templateGroupName", templateGroupName)
            queryParam("informatieobjecttypeUuid", informatieobjecttypeUuid)
            path("$SMART_DOCUMENTS_REDIRECT_URL_BASE/task/{taskId}")
        }.build(zaakUuid.toString(), taskId)
}
