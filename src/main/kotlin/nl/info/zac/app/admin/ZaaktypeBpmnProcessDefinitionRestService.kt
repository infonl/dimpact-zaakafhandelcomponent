/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import nl.info.zac.app.admin.model.RestZaaktypeBpmnProcessDefinition
import nl.info.zac.flowable.bpmn.ZaaktypeBpmnProcessDefinitionService
import nl.info.zac.flowable.bpmn.model.ZaaktypeBpmnProcessDefinition
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@Singleton
@Path("bpmn-process-definition")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class ZaaktypeBpmnProcessDefinitionRestService @Inject constructor(
    private val zaaktypeBpmnProcessDefinitionService: ZaaktypeBpmnProcessDefinitionService,
    private val policyService: PolicyService
) {
    @POST
    @Path("{processDefinitionKey}/connect")
    fun connectWithZaaktype(
        @NotEmpty @PathParam("processDefinitionKey") processDefinitionKey: String,
        @Valid restZaaktypeBpmnProcessDefinition: RestZaaktypeBpmnProcessDefinition
    ): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        zaaktypeBpmnProcessDefinitionService.createZaaktypeBpmnProcessDefinition(
            ZaaktypeBpmnProcessDefinition().apply {
                zaaktypeUuid = restZaaktypeBpmnProcessDefinition.zaaktypeUuid
                bpmnProcessDefinitionKey = processDefinitionKey
                zaaktypeOmschrijving = restZaaktypeBpmnProcessDefinition.zaaktypeOmschrijving
                productaanvraagtype = restZaaktypeBpmnProcessDefinition.productaanvraagtype
            }
        )
        return Response.created(null).build()
    }
}
