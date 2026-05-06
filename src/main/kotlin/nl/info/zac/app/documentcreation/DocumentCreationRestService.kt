/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.documentcreation

import jakarta.enterprise.inject.Instance
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
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.app.documentcreation.model.RestDocumentCreationAttendedData
import nl.info.zac.app.documentcreation.model.RestDocumentCreationAttendedResponse
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.documentcreation.DocumentCreationService
import nl.info.zac.documentcreation.model.DocumentCreationAttendedResponse
import nl.info.zac.documentcreation.model.DocumentCreationDataAttended
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
    private val zrcClientService: ZrcClientService,
    private val zaaktypeConfigurationService: ZaaktypeConfigurationService,
    private val flowableTaskService: FlowableTaskService,
    private val loggedInUserInstance: Instance<LoggedInUser>
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
            assertPolicy(policyService.readZaakRechten(zaak, loggedInUserInstance.get()).creerenDocument)
            restDocumentCreationAttendedData.taskId?.let {
                val task = flowableTaskService.findOpenTask(it)
                    ?: throw TaskNotFoundException("No open task found with task id: '$it'")
                assertPolicy(policyService.readTaakRechten(task).creerenDocument)
            }
        }.let { zaak ->
            createDocument(zaak, restDocumentCreationAttendedData)
                .let { RestDocumentCreationAttendedResponse(it.redirectUrl, it.message) }
        }

    @Suppress("ThrowsCount")
    private fun createDocument(
        zaak: Zaak,
        restDocumentCreationAttendedData: RestDocumentCreationAttendedData
    ): DocumentCreationAttendedResponse {
        if (!zaaktypeConfigurationService.isSmartDocumentsEnabled(zaak.zaaktype.extractUuid())) {
            throw SmartDocumentsDisabledException()
        }
        return DocumentCreationDataAttended(
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
        ).let(documentCreationService::createDocumentAttended)
    }

    /**
     * SmartDocuments callback for CMMN zaak
     *
     * Called when SmartDocument Wizard "Finish" button is clicked. The URL provided as "redirectUrl" to
     * SmartDocuments contains all the parameters needed to store the document for a zaak:
     * zaak ID, template and template group IDs, username and created document ID
     */
    @POST
    @Path("/smartdocuments/callback/zaak/{zaakUuid}")
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
            documentCreationService.getInformationObjecttypeUuid(it, templateGroupId, templateId)
        }

    /**
     * SmartDocuments callback for CMMN task
     *
     * Called when SmartDocument Wizard "Finish" button is clicked. The URL provided as "redirectUrl" to
     * SmartDocuments contains all the parameters needed to store the document for a task:
     * zaak and task IDs, template and template group IDs, username and created document ID
     */
    @POST
    @Path("/smartdocuments/callback/zaak/{zaakUuid}/task/{taskId}")
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
            documentCreationService.getInformationObjecttypeUuid(it, templateGroupId, templateId)
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
