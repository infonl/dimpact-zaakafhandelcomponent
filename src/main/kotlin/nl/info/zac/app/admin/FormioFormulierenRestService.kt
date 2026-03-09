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
import nl.info.zac.app.admin.model.RestFormioFormulier
import nl.info.zac.app.admin.model.RestFormioFormulierContent
import nl.info.zac.flowable.bpmn.BpmnProcessDefinitionTaskFormService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.lang3.NotImplementedException

@Singleton
@Path("formio-formulieren")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
class FormioFormulierenRestService @Inject constructor(
    private val bpmnProcessDefinitionTaskFormService: BpmnProcessDefinitionTaskFormService,
    private val policyService: PolicyService
) {
    @GET
    fun listFormulieren(): List<RestFormioFormulier> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return bpmnProcessDefinitionTaskFormService.listForms()
            .map { RestFormioFormulier(it.id, it.bpmnProcessDefinitionKey, it.name, it.title) }
    }

    @POST
    @Suppress("UnusedParameter")
    fun createFormulier(restFormioFormulierContent: RestFormioFormulierContent): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        throw NotImplementedException("Creating formio form moved to new endpoint")
    }

    @DELETE
    @Path("{id}")
    @Suppress("UnusedParameter")
    fun deleteFormulier(@PathParam("id") id: Long): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        throw NotImplementedException("Deleting formio form moved to new endpoint")
    }
}
