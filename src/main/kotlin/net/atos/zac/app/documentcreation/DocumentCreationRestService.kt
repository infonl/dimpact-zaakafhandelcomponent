/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.documentcreation

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
import net.atos.client.zgw.util.extractUuid
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.app.documentcreation.model.RestDocumentCreationAttendedData
import net.atos.zac.app.documentcreation.model.RestDocumentCreationAttendedResponse
import net.atos.zac.documentcreation.DocumentCreationService
import net.atos.zac.documentcreation.model.DocumentCreationDataAttended
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.PolicyService.assertPolicy
import net.atos.zac.smartdocuments.exception.SmartDocumentsDisabledException
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
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
    private val documentCreationService: DocumentCreationService,
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
        zrcClientService.readZaak(restDocumentCreationAttendedData.zaakUuid).also {
            assertPolicy(policyService.readZaakRechten(it).creeerenDocument)
            restDocumentCreationAttendedData.taskId?.let {
                val task = flowableTaskService.findOpenTask(it)
                    ?: throw TaskNotFoundException("No open task found with task id: '$it'")
                assertPolicy(policyService.readTaakRechten(task).creeerenDocument)
            }
            it.zaaktype.extractUuid().let {
                if (!zaakafhandelParameterService.isSmartDocumentsEnabled(it)) {
                    throw SmartDocumentsDisabledException("SmartDocuments is disabled")
                }
            }
        }.let {
            DocumentCreationDataAttended(
                zaak = it,
                taskId = restDocumentCreationAttendedData.taskId,
                templateId = restDocumentCreationAttendedData.smartDocumentsTemplateId,
                templateGroupId = restDocumentCreationAttendedData.smartDocumentsTemplateGroupId,
                title = restDocumentCreationAttendedData.title,
                description = restDocumentCreationAttendedData.description,
                author = restDocumentCreationAttendedData.author,
                creationDate = restDocumentCreationAttendedData.creationDate
            )
                .let(documentCreationService::createDocumentAttended)
                .let { response -> RestDocumentCreationAttendedResponse(response.redirectUrl, response.message) }
        }

    /**
     * SmartDocuments callback
     *
     * Called when SmartDocument Wizard "Finish" button is clicked. The URL provided as "redirectUrl" to
     * SmartDocuments contains all the parameters needed to store the document for a zaak:
     * zaak ID, template and template group IDs, username and created document ID
     */
    @POST
    @Path("/smartdocuments/callback/zaak/{zaakUuid}")
    @Produces(MediaType.TEXT_HTML)
    @Suppress("LongParameterList")
    fun createDocumentForZaakCallback(
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
                    zaakId = zaak.identificatie,
                    documentName = title,
                    result = SmartDocumentsWizardResult.CANCELLED
                )
            } else {
                runCatching {
                    documentCreationService.storeDocument(
                        zaak = zaak,
                        fileId = fileId,
                        templateGroupId = templateGroupId,
                        templateId = templateId,
                        title = title,
                        description = description,
                        creationDate = creationDate,
                        userName = userName
                    ).let {
                        buildWizardFinishPageRedirectResponse(
                            zaakId = zaak.identificatie,
                            documentName = title,
                            result = SmartDocumentsWizardResult.SUCCESS
                        )
                    }
                }.onFailure {
                    LOG.log(Level.WARNING, it) {
                        "Failed to create document for zaak ${zaak.identificatie}"
                    }
                }.getOrElse {
                    buildWizardFinishPageRedirectResponse(
                        zaakId = zaak.identificatie,
                        documentName = title,
                        result = SmartDocumentsWizardResult.FAILURE
                    )
                }
            }
        }

    /**
     * SmartDocuments callback
     *
     * Called when SmartDocument Wizard "Finish" button is clicked. The URL provided as "redirectUrl" to
     * SmartDocuments contains all the parameters needed to store the document for a task:
     * zaak and task IDs, template and template group IDs, username and created document ID
     */
    @POST
    @Path("/smartdocuments/callback/zaak/{zaakUuid}/task/{taskId}")
    @Produces(MediaType.TEXT_HTML)
    @Suppress("LongParameterList")
    fun createDocumentForTaskCallback(
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
                    zaakId = zaak.identificatie,
                    taskId = taskId,
                    documentName = title,
                    result = SmartDocumentsWizardResult.CANCELLED
                )
            } else {
                runCatching {
                    documentCreationService.storeDocument(
                        zaak = zaak,
                        taskId = taskId,
                        fileId = fileId,
                        templateGroupId = templateGroupId,
                        templateId = templateId,
                        title = title,
                        description = description,
                        creationDate = creationDate,
                        userName = userName
                    ).let {
                        buildWizardFinishPageRedirectResponse(
                            zaakId = zaak.identificatie,
                            taskId = taskId,
                            documentName = title,
                            result = SmartDocumentsWizardResult.SUCCESS
                        )
                    }
                }.onFailure {
                    LOG.log(Level.WARNING, it) {
                        "Failed to create document for zaak ${zaak.identificatie} and task $taskId"
                    }
                }.getOrElse {
                    buildWizardFinishPageRedirectResponse(
                        zaakId = zaak.identificatie,
                        taskId = taskId,
                        documentName = title,
                        result = SmartDocumentsWizardResult.FAILURE
                    )
                }
            }
        }

    private fun buildWizardFinishPageRedirectResponse(
        zaakId: String,
        taskId: String? = null,
        documentName: String? = null,
        result: SmartDocumentsWizardResult
    ) =
        Response.seeOther(
            documentCreationService.documentCreationFinishPageUrl(
                zaakId = zaakId,
                taskId = taskId,
                documentName = documentName,
                result = result.toString().lowercase()
            )
        ).build()
}
