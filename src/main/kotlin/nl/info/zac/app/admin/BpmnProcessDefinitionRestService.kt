/*
 * SPDX-FileCopyrightText: 2026 Dimpact, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.Response.Status
import nl.info.zac.app.admin.model.BpmnProcessDefinitionTaskFormContent
import nl.info.zac.app.admin.model.RestBpmnProcessDefinition
import nl.info.zac.app.admin.model.RestBpmnProcessDefinitionDetails
import nl.info.zac.app.admin.model.RestBpmnProcessDefinitionForm
import nl.info.zac.app.admin.model.RestProcessDefinitionContent
import nl.info.zac.flowable.bpmn.BpmnProcessDefinitionTaskFormService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.flowable.bpmn.model.BpmnProcessDefinitionTaskForm
import nl.info.zac.flowable.bpmn.model.isUsedIn
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.NoArgConstructor

@Singleton
@Path("bpmn-process-definitions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
class BpmnProcessDefinitionRestService @Inject constructor(
    private val bpmnService: BpmnService,
    private val policyService: PolicyService,
    private val bpmnProcessDefinitionTaskFormService: BpmnProcessDefinitionTaskFormService,
    private val bpmnProcessDefinitionDownloadService: BpmnProcessDefinitionDownloadService
) {
    @GET
    fun listProcessDefinitions(
        @QueryParam("details") @DefaultValue("false") details: Boolean
    ): List<RestBpmnProcessDefinition> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        if (details) {
            return listProcessDefinitionsWithDetails()
        }
        return bpmnService.listProcessDefinitions()
            .map {
                RestBpmnProcessDefinition(it.id, it.name, it.version, it.key)
            }
    }

    private fun listProcessDefinitionsWithDetails(): List<RestBpmnProcessDefinition> {
        val uniqueBpmnProcessDefinitionKeysFromProcessInstances =
            bpmnService.findUniqueBpmnProcessDefinitionKeysFromProcessInstances()
        val uniqueBpmnProcessDefinitionKeysFromConfigurations =
            bpmnService.findUniqueBpmnProcessDefinitionKeysFromConfigurations()
        val uploadedForms = bpmnProcessDefinitionTaskFormService.listForms()
        val uploadedFormTitleMap = uploadedForms.associateBy(
            { "[${it.bpmnProcessDefinitionKey}]-[${it.bpmnProcessDefinitionVersion}]-[${it.name}]" },
            { it.title }
        )

        return bpmnService.listProcessDefinitions()
            .map {
                val metadata = bpmnService.getProcessDefinitionMetadata(it)
                RestBpmnProcessDefinition(
                    it.id,
                    it.name,
                    it.version,
                    it.key,
                    RestBpmnProcessDefinitionDetails(
                        inUse = uniqueBpmnProcessDefinitionKeysFromProcessInstances.contains(it.key) ||
                            uniqueBpmnProcessDefinitionKeysFromConfigurations.contains(it.key),
                        documentation = metadata.documentation,
                        modificationDate = metadata.modificationDate,
                        uploadDate = metadata.uploadDate,
                        forms = getRestBpmnProcessDefinitionForms(
                            it.key,
                            it.version,
                            metadata.formKeys,
                            uploadedFormTitleMap
                        ),
                        orphanedForms = getRestBpmnProcessDefinitionOrphanedForms(
                            it.key,
                            it.version,
                            metadata.formKeys,
                            uploadedForms
                        )
                    )
                )
            }
    }

    private fun getRestBpmnProcessDefinitionForms(
        bpmnProcessDefinitionKey: String,
        bpmnProcessDefinitionVersion: Int,
        formKeys: List<String>,
        uploadedFormTitleMap: Map<String, String>
    ) =
        formKeys.map {
            val key = "[$bpmnProcessDefinitionKey]-[$bpmnProcessDefinitionVersion]-[$it]"
            val uploadedFormTitle = uploadedFormTitleMap[key]
            RestBpmnProcessDefinitionForm(
                it,
                uploadedFormTitle,
                uploadedFormTitle != null
            )
        }

    private fun getRestBpmnProcessDefinitionOrphanedForms(
        bpmnProcessDefinitionKey: String,
        bpmnProcessDefinitionVersion: Int,
        formKeys: List<String>,
        forms: List<BpmnProcessDefinitionTaskForm>
    ) =
        forms.filter {
            it.bpmnProcessDefinitionKey == bpmnProcessDefinitionKey &&
                it.bpmnProcessDefinitionVersion == bpmnProcessDefinitionVersion &&
                !it.isUsedIn(formKeys)
        }.map {
            RestBpmnProcessDefinitionForm(
                it.name,
                it.title,
                uploaded = true
            )
        }

    @POST
    fun createProcessDefinition(processDefinitionContent: RestProcessDefinitionContent): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        bpmnService.addProcessDefinition(processDefinitionContent.filename, processDefinitionContent.content)
        return Response.created(null).build()
    }

    @DELETE
    @Path("{key}")
    fun deleteProcessDefinition(@PathParam("key") key: String): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        if (bpmnService.isProcessDefinitionInUse(key)) {
            return Response.status(Status.BAD_REQUEST)
                .entity(mapOf("message" to "BPMN process definition '$key' cannot be deleted as it is in use"))
                .build()
        }
        bpmnService.deleteProcessDefinition(key)
        return Response.noContent().build()
    }

    @GET
    @Path("{key}/download")
    @Produces("application/zip")
    fun downloadProcessDefinition(@PathParam("key") key: String): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        val processDefinition = bpmnService.readProcessDefinitionByProcessDefinitionKey(key)
        return Response.ok(bpmnProcessDefinitionDownloadService.getProcessDefinitionAndTaskFormsAsZipStream(processDefinition))
            .header(
                "Content-Disposition",
                """attachment; filename="${processDefinition.key}-v${processDefinition.version}.zip""""
            )
            .build()
    }

    @POST
    @Path("{key}/forms")
    fun createForm(
        @PathParam("key") key: String,
        bpmnProcessDefinitionTaskFormContent: BpmnProcessDefinitionTaskFormContent
    ): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        bpmnProcessDefinitionTaskFormService.addForm(
            key,
            bpmnProcessDefinitionTaskFormContent.filename,
            bpmnProcessDefinitionTaskFormContent.content
        )
        return Response.status(Status.CREATED).build()
    }

    @DELETE
    @Path("{key}/forms/{name}")
    fun deleteForm(
        @PathParam("key") key: String,
        @PathParam("name") name: String
    ): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        if (bpmnService.isProcessDefinitionInUse(key) &&
            !isFormOrphaned(key, name)
        ) {
            return Response.status(Status.BAD_REQUEST)
                .entity(
                    mapOf(
                        "message" to "BPMN process definition form '$name' cannot be deleted as " +
                            "it is in use by process definition '$key'"
                    )
                )
                .build()
        }
        bpmnProcessDefinitionTaskFormService.deleteForm(key, name)
        return Response.noContent().build()
    }

    private fun isFormOrphaned(processDefinitionKey: String, form: String) =
        bpmnService.findProcessDefinitionByProcessDefinitionKey(processDefinitionKey)?.let {
            !bpmnService.getProcessDefinitionMetadata(it).formKeys.contains(form)
        } ?: true
}
