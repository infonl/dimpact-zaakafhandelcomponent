/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.zac.app.admin.model.RestFormioFormulier
import nl.info.zac.flowable.bpmn.BpmnProcessDefinitionTaskFormService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.NoArgConstructor

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
}
