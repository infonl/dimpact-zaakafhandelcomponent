/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.assertPolicy
import nl.info.zac.app.admin.model.RestProcessDefinition
import nl.info.zac.app.admin.model.RestProcessDefinitionContent
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.util.NoArgConstructor

@Singleton
@Path("process-definitions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
class ProcessDefinitionRestService @Inject constructor(
    private val bpmnService: BpmnService,
    private val policyService: PolicyService
) {
    @GET
    fun listProcessDefinitions(): List<RestProcessDefinition> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return bpmnService.listProcessDefinitions()
            .map { RestProcessDefinition(it.id, it.name, it.version, it.key) }
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
        bpmnService.deleteProcessDefinition(key)
        return Response.noContent().build()
    }
}
