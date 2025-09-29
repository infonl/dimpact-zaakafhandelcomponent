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
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.app.admin.model.RestZaaktypeBpmnProcessDefinition
import nl.info.zac.flowable.bpmn.model.ZaaktypeBpmnConfiguration
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@Singleton
@Path("zaaktype-bpmn-process-definitions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class ZaaktypeBpmnConfigurationRestService @Inject constructor(
    private val zaaktypeBpmnConfigurationService: ZaaktypeBpmnConfigurationService,
    private val policyService: PolicyService
) {
    @GET
    fun listZaaktypeBpmnProcessDefinitions(): List<RestZaaktypeBpmnProcessDefinition> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return zaaktypeBpmnConfigurationService.listBpmnProcessDefinitions().map {
            it.toRestZaaktypeBpmnProcessDefinition()
        }
    }

    @GET
    @Path("{processDefinitionKey}")
    fun listZaaktypeBpmnProcessDefinition(
        @NotEmpty @PathParam("processDefinitionKey") processDefinitionKey: String
    ): RestZaaktypeBpmnProcessDefinition {
        assertPolicy(policyService.readOverigeRechten().beheren)
        val processDefinitions = zaaktypeBpmnConfigurationService
            .listBpmnProcessDefinitions()
            .filter { it.bpmnProcessDefinitionKey == processDefinitionKey }

        if (processDefinitions.isEmpty()) {
            throw NotFoundException("No process definition found for key '$processDefinitionKey'")
        }
        check(processDefinitions.size == 1) { "Multiple process definitions found for key '$processDefinitionKey'" }

        return processDefinitions.first().toRestZaaktypeBpmnProcessDefinition()
    }

    @POST
    @Path("{processDefinitionKey}")
    fun createZaaktypeBpmnProcessDefinition(
        @NotEmpty @PathParam("processDefinitionKey") processDefinitionKey: String,
        @Valid restZaaktypeBpmnProcessDefinition: RestZaaktypeBpmnProcessDefinition,
        @Context uriInfo: UriInfo
    ): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        zaaktypeBpmnConfigurationService.createZaaktypeBpmnProcessDefinition(
            ZaaktypeBpmnConfiguration().apply {
                zaaktypeUuid = restZaaktypeBpmnProcessDefinition.zaaktypeUuid
                bpmnProcessDefinitionKey = processDefinitionKey
                zaaktypeOmschrijving = restZaaktypeBpmnProcessDefinition.zaaktypeOmschrijving
                productaanvraagtype = restZaaktypeBpmnProcessDefinition.productaanvraagtype
                groupId = restZaaktypeBpmnProcessDefinition.groepNaam
            }
        )
        return Response.created(uriInfo.requestUri).build()
    }

    private fun ZaaktypeBpmnConfiguration.toRestZaaktypeBpmnProcessDefinition() =
        RestZaaktypeBpmnProcessDefinition(
            zaaktypeUuid = this.zaaktypeUuid,
            bpmnProcessDefinitionKey = this.bpmnProcessDefinitionKey,
            zaaktypeOmschrijving = this.zaaktypeOmschrijving,
            groepNaam = this.groupId,
            productaanvraagtype = this.productaanvraagtype
        )
}
