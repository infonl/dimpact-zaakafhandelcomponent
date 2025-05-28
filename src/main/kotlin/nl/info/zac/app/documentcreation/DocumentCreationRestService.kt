/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.documentcreation

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.FormParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.app.documentcreation.model.RestDocumentCreationAttendedData
import nl.info.zac.app.documentcreation.model.RestDocumentCreationAttendedResponse
import nl.info.zac.documentcreation.BpmnDocumentCreationService
import nl.info.zac.documentcreation.CmmnDocumentCreationService
import nl.info.zac.documentcreation.DocumentCreationService
import nl.info.zac.documentcreation.model.BpmnDocumentCreationDataAttended
import nl.info.zac.documentcreation.model.CmmnDocumentCreationDataAttended
import nl.info.zac.documentcreation.model.DocumentCreationAttendedResponse
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.smartdocuments.exception.SmartDocumentsDisabledException
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

@Singleton
@Path("document-creation")
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
@Suppress("LongParameterList")
class DocumentCreationRestService @Inject constructor(
    private val policyService: PolicyService,
    private val documentCreationService: DocumentCreationService,
    private val cmmnDocumentCreationService: CmmnDocumentCreationService,
    private val bpmnDocumentCreationService: BpmnDocumentCreationService,
    private val zrcClientService: ZrcClientService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val flowableTaskService: FlowableTaskService
) {
    companion object {
        enum class SmartDocumentsWizardResult {
            SUCCESS,
            CANCELLED,
            FAILURE
        }

        private val LOG = Logger.getLogger(DocumentCreationRestService::class.java.name)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/create-document-attended")
    fun createDocumentAttended(
        @Valid restDocumentCreationAttendedData: RestDocumentCreationAttendedData
    ): RestDocumentCreationAttendedResponse =
        zrcClientService.readZaak(restDocumentCreationAttendedData.zaakUuid).also { zaak ->
            assertPolicy(policyService.readZaakRechten(zaak).creeerenDocument)
            restDocumentCreationAttendedData.taskId?.let {
                val task = flowableTaskService.findOpenTask(it)
                    ?: throw TaskNotFoundException("No open task found with task id: '$it'")
                assertPolicy(policyService.readTaakRechten(task).creeerenDocument)
            }
        }.let { zaak ->
            if (restDocumentCreationAttendedData.informatieobjecttypeUuid != null) {
                createBpmnDocument(zaak, restDocumentCreationAttendedData)
            } else {
                createCmmnDocument(zaak, restDocumentCreationAttendedData)
            }
                .let { RestDocumentCreationAttendedResponse(it.redirectUrl, it.message) }
        }

    @Suppress("ThrowsCount")
    private fun createCmmnDocument(
        zaak: Zaak,
        restDocumentCreationAttendedData: RestDocumentCreationAttendedData
    ): DocumentCreationAttendedResponse {
        if (!zaakafhandelParameterService.isSmartDocumentsEnabled(zaak.zaaktype.extractUuid())) {
            throw SmartDocumentsDisabledException()
        }
        return CmmnDocumentCreationDataAttended(
            zaak = zaak,
            taskId = restDocumentCreationAttendedData.taskId,
            templateId = restDocumentCreationAttendedData.smartDocumentsTemplateId
                ?: throw IllegalArgumentException("SmartDocuments template ID is required"),
            templateGroupId = restDocumentCreationAttendedData.smartDocumentsTemplateGroupId
                ?: throw IllegalArgumentException("SmartDocuments template group ID is required"),
            title = restDocumentCreationAttendedData.title,
            description = restDocumentCreationAttendedData.description,
            author = restDocumentCreationAttendedData.author,
            creationDate = restDocumentCreationAttendedData.creationDate
        ).let(cmmnDocumentCreationService::createCmmnDocumentAttended)
    }

    private fun createBpmnDocument(zaak: Zaak, restDocumentCreationAttendedData: RestDocumentCreationAttendedData) =
        BpmnDocumentCreationDataAttended(
            zaak = zaak,
            taskId = restDocumentCreationAttendedData.taskId,
            templateName = restDocumentCreationAttendedData.smartDocumentsTemplateName
                ?: throw IllegalArgumentException("SmartDocuments template name is required"),
            templateGroupName = restDocumentCreationAttendedData.smartDocumentsTemplateGroupName
                ?: throw IllegalArgumentException("SmartDocuments template group name is required"),
            informatieobjecttypeUuid = restDocumentCreationAttendedData.informatieobjecttypeUuid
                ?: throw IllegalArgumentException("Information object type UUID is required"),
            title = restDocumentCreationAttendedData.title,
            description = restDocumentCreationAttendedData.description,
            author = restDocumentCreationAttendedData.author,
            creationDate = restDocumentCreationAttendedData.creationDate
        ).let(bpmnDocumentCreationService::createBpmnDocumentAttended)

    /**
     * SmartDocuments callback for CMMN zaak
     *
     * Called when SmartDocument Wizard "Finish" button is clicked. The URL provided as "redirectUrl" to
     * SmartDocuments contains all the parameters needed to store the document for a zaak:
     * zaak ID, template and template group IDs, username and created document ID
     */
    @POST
    @Path("/smartdocuments/cmmn-callback/zaak/{zaakUuid}")
    @Produces(MediaType.TEXT_HTML)
    @Suppress("LongParameterList")
    fun createCmmnDocumentForZaakCallback(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @QueryParam("templateGroupId") templateGroupId: String,
        @QueryParam("templateId") templateId: String,
        @QueryParam("title") title: String,
        @QueryParam("description") description: String?,
        @QueryParam("creationDate") creationDate: ZonedDateTime,
        @QueryParam("userName") userName: String,
        @FormParam("sdDocument") @DefaultValue("") fileId: String,
    ): Response =
        createDocument(
            zaakUuid = zaakUuid,
            title = title,
            description = description,
            creationDate = creationDate,
            userName = userName,
            fileId = fileId
        ) {
            cmmnDocumentCreationService.getInformationObjecttypeUuid(it, templateGroupId, templateId)
        }

    /**
     * SmartDocuments callback for CMMN task
     *
     * Called when SmartDocument Wizard "Finish" button is clicked. The URL provided as "redirectUrl" to
     * SmartDocuments contains all the parameters needed to store the document for a task:
     * zaak and task IDs, template and template group IDs, username and created document ID
     */
    @POST
    @Path("/smartdocuments/cmmn-callback/zaak/{zaakUuid}/task/{taskId}")
    @Produces(MediaType.TEXT_HTML)
    @Suppress("LongParameterList")
    fun createCmmnDocumentForTaskCallback(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @PathParam("taskId") taskId: String,
        @QueryParam("templateGroupId") templateGroupId: String,
        @QueryParam("templateId") templateId: String,
        @QueryParam("title") title: String,
        @QueryParam("description") description: String?,
        @QueryParam("creationDate") creationDate: ZonedDateTime,
        @QueryParam("userName") userName: String,
        @FormParam("sdDocument") @DefaultValue("") fileId: String,
    ): Response =
        createDocument(
            zaakUuid = zaakUuid,
            taskId = taskId,
            title = title,
            description = description,
            creationDate = creationDate,
            userName = userName,
            fileId = fileId
        ) {
            cmmnDocumentCreationService.getInformationObjecttypeUuid(it, templateGroupId, templateId)
        }

    /**
     * SmartDocuments callback for a BPMN task
     *
     * Called when SmartDocument Wizard "Finish" button is clicked. The URL provided as "redirectUrl" to
     * SmartDocuments contains all the parameters needed to store the document for a task:
     * zaak and task IDs, template and template group IDs, username and created document ID
     */
    @POST
    @Path("/smartdocuments/bpmn-callback/zaak/{zaakUuid}/task/{taskId}")
    @Produces(MediaType.TEXT_HTML)
    @Suppress("LongParameterList")
    fun createBpmnDocumentForTaskCallback(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @PathParam("taskId") taskId: String,
        @QueryParam("informatieobjecttypeUuid") informatieobjecttypeUuid: UUID,
        @QueryParam("title") title: String,
        @QueryParam("description") description: String?,
        @QueryParam("creationDate") creationDate: ZonedDateTime,
        @QueryParam("userName") userName: String,
        @FormParam("sdDocument") @DefaultValue("") fileId: String,
    ): Response =
        createDocument(
            zaakUuid = zaakUuid,
            taskId = taskId,
            title = title,
            description = description,
            creationDate = creationDate,
            userName = userName,
            fileId = fileId
        ) {
            informatieobjecttypeUuid
        }

    private fun createDocument(
        zaakUuid: UUID,
        taskId: String? = null,
        title: String,
        description: String?,
        creationDate: ZonedDateTime,
        userName: String,
        fileId: String,
        fetchInformatieobjecttypeUuidFunction: (zaak: Zaak) -> UUID,
    ) =
        zrcClientService.readZaak(zaakUuid).let { zaak ->
            if (fileId.isBlank()) {
                Response.seeOther(
                    documentCreationService.documentCreationFinishPageUrl(
                        zaakId = zaak.identificatie,
                        taskId = taskId,
                        documentName = title,
                        result = SmartDocumentsWizardResult.CANCELLED.toString().lowercase()
                    )
                ).build()
            } else {
                runCatching {
                    fetchInformatieobjecttypeUuidFunction(zaak).let {
                        documentCreationService.storeDocument(
                            zaak = zaak,
                            taskId = taskId,
                            fileId = fileId,
                            title = title,
                            description = description,
                            informatieobjecttypeUuid = it,
                            creationDate = creationDate,
                            userName = userName
                        ).let {
                            Response.seeOther(
                                documentCreationService.documentCreationFinishPageUrl(
                                    zaakId = zaak.identificatie,
                                    taskId = taskId,
                                    documentName = title,
                                    result = SmartDocumentsWizardResult.SUCCESS.toString().lowercase()
                                )
                            ).build()
                        }
                    }
                }.onFailure {
                    LOG.log(Level.WARNING, it) {
                        "Failed to create document for zaak ${zaak.identificatie}" +
                            if (taskId != null) " and task $taskId" else ""
                    }
                }.getOrElse {
                    Response.seeOther(
                        documentCreationService.documentCreationFinishPageUrl(
                            zaakId = zaak.identificatie,
                            taskId = taskId,
                            documentName = title,
                            result = SmartDocumentsWizardResult.FAILURE.toString().lowercase()
                        )
                    ).build()
                }
            }
        }
}
