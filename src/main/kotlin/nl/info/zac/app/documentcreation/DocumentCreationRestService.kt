/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
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
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.PolicyService.assertPolicy
import nl.info.client.zgw.util.extractUuid
import nl.info.zac.app.documentcreation.model.RestDocumentCreationAttendedData
import nl.info.zac.app.documentcreation.model.RestDocumentCreationAttendedResponse
import nl.info.zac.documentcreation.BpmnDocumentCreationService
import nl.info.zac.documentcreation.CmmnDocumentCreationService
import nl.info.zac.documentcreation.model.BpmnDocumentCreationDataAttended
import nl.info.zac.documentcreation.model.CmmnDocumentCreationDataAttended
import nl.info.zac.documentcreation.model.DocumentCreationAttendedResponse
import nl.info.zac.smartdocuments.exception.SmartDocumentsDisabledException
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

@Singleton
@Path("document-creation")
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
class DocumentCreationRestService @Inject constructor(
    private val policyService: PolicyService,
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
            templateId = restDocumentCreationAttendedData.smartDocumentsTemplateId,
            templateGroupId = restDocumentCreationAttendedData.smartDocumentsTemplateGroupId,
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
            templateName = restDocumentCreationAttendedData.smartDocumentsTemplateName!!,
            templateGroupName = restDocumentCreationAttendedData.smartDocumentsTemplateGroupName!!,
            informatieobjecttypeUuid = restDocumentCreationAttendedData.informatieobjecttypeUuid!!,
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
        zrcClientService.readZaak(zaakUuid).let { zaak ->
            if (fileId.isBlank()) {
                buildWizardFinishPageRedirectResponse(
                    cmmnDocumentCreationService.documentCreationFinishPageUrl(
                        zaakId = zaak.identificatie,
                        documentName = title,
                        result = SmartDocumentsWizardResult.CANCELLED.toString().lowercase()
                    )
                )
            } else {
                runCatching {
                    cmmnDocumentCreationService.getInformationObjecttypeUuid(zaak, templateGroupId, templateId).let {
                        cmmnDocumentCreationService.storeDocument(
                            zaak = zaak,
                            fileId = fileId,
                            title = title,
                            description = description,
                            informatieobjecttypeUuid = it,
                            creationDate = creationDate,
                            userName = userName
                        ).let {
                            buildWizardFinishPageRedirectResponse(
                                cmmnDocumentCreationService.documentCreationFinishPageUrl(
                                    zaakId = zaak.identificatie,
                                    documentName = title,
                                    result = SmartDocumentsWizardResult.SUCCESS.toString().lowercase()
                                )
                            )
                        }
                    }
                }.onFailure {
                    LOG.log(Level.WARNING, it) {
                        "Failed to create document for zaak ${zaak.identificatie}"
                    }
                }.getOrElse {
                    buildWizardFinishPageRedirectResponse(
                        cmmnDocumentCreationService.documentCreationFinishPageUrl(
                            zaakId = zaak.identificatie,
                            documentName = title,
                            result = SmartDocumentsWizardResult.FAILURE.toString().lowercase()
                        )
                    )
                }
            }
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
        zrcClientService.readZaak(zaakUuid).let { zaak ->
            if (fileId.isBlank()) {
                buildWizardFinishPageRedirectResponse(
                    cmmnDocumentCreationService.documentCreationFinishPageUrl(
                        zaakId = zaak.identificatie,
                        taskId = taskId,
                        documentName = title,
                        result = SmartDocumentsWizardResult.CANCELLED.toString().lowercase()
                    )
                )
            } else {
                runCatching {
                    cmmnDocumentCreationService.getInformationObjecttypeUuid(zaak, templateGroupId, templateId).let {
                        cmmnDocumentCreationService.storeDocument(
                            zaak = zaak,
                            taskId = taskId,
                            fileId = fileId,
                            informatieobjecttypeUuid = it,
                            title = title,
                            description = description,
                            creationDate = creationDate,
                            userName = userName
                        ).let {
                            buildWizardFinishPageRedirectResponse(
                                cmmnDocumentCreationService.documentCreationFinishPageUrl(
                                    zaakId = zaak.identificatie,
                                    taskId = taskId,
                                    documentName = title,
                                    result = SmartDocumentsWizardResult.SUCCESS.toString().lowercase()
                                )
                            )
                        }
                    }
                }.onFailure {
                    LOG.log(Level.WARNING, it) {
                        "Failed to create document for zaak ${zaak.identificatie} and task $taskId"
                    }
                }.getOrElse {
                    buildWizardFinishPageRedirectResponse(
                        cmmnDocumentCreationService.documentCreationFinishPageUrl(
                            zaakId = zaak.identificatie,
                            taskId = taskId,
                            documentName = title,
                            result = SmartDocumentsWizardResult.FAILURE.toString().lowercase()
                        )
                    )
                }
            }
        }

    /**
     * SmartDocuments callback for BPMN task
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
        zrcClientService.readZaak(zaakUuid).let { zaak ->
            if (fileId.isBlank()) {
                buildWizardFinishPageRedirectResponse(
                    bpmnDocumentCreationService.documentCreationFinishPageUrl(
                        zaakId = zaak.identificatie,
                        taskId = taskId,
                        documentName = title,
                        result = SmartDocumentsWizardResult.CANCELLED.toString().lowercase()
                    )
                )
            } else {
                runCatching {
                    bpmnDocumentCreationService.storeDocument(
                        zaak = zaak,
                        taskId = taskId,
                        fileId = fileId,
                        informatieobjecttypeUuid = informatieobjecttypeUuid,
                        title = title,
                        description = description,
                        creationDate = creationDate,
                        userName = userName
                    ).let {
                        buildWizardFinishPageRedirectResponse(
                            bpmnDocumentCreationService.documentCreationFinishPageUrl(
                                zaakId = zaak.identificatie,
                                taskId = taskId,
                                documentName = title,
                                result = SmartDocumentsWizardResult.SUCCESS.toString().lowercase()
                            )
                        )
                    }
                }.onFailure {
                    LOG.log(Level.WARNING, it) {
                        "Failed to create document for zaak ${zaak.identificatie} and task $taskId"
                    }
                }.getOrElse {
                    buildWizardFinishPageRedirectResponse(
                        bpmnDocumentCreationService.documentCreationFinishPageUrl(
                            zaakId = zaak.identificatie,
                            taskId = taskId,
                            documentName = title,
                            result = SmartDocumentsWizardResult.FAILURE.toString().lowercase()
                        )
                    )
                }
            }
        }

    private fun buildWizardFinishPageRedirectResponse(location: URI) =
        Response.seeOther(location).build()
}
