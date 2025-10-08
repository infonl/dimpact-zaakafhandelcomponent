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
import jakarta.ws.rs.core.MediaType
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.admin.exception.MultipleZaaktypeConfigurationsFoundException
import nl.info.zac.app.admin.model.RestZaaktypeBpmnConfiguration
import nl.info.zac.flowable.bpmn.model.ZaaktypeBpmnConfiguration
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@Singleton
@Path("zaaktype-bpmn-configuration")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class ZaaktypeBpmnConfigurationRestService @Inject constructor(
    private val zaaktypeBpmnConfigurationService: ZaaktypeBpmnConfigurationService,
    private val policyService: PolicyService
) {
    @GET
    fun listZaaktypeBpmnConfigurations(): List<RestZaaktypeBpmnConfiguration> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return zaaktypeBpmnConfigurationService.listZaaktypeBpmnConfigurations().map {
            it.toRestZaaktypeBpmnConfiguration()
        }
    }

    @GET
    @Path("{processDefinitionKey}")
    fun getZaaktypeBpmnConfiguration(
        @NotEmpty @PathParam("processDefinitionKey") processDefinitionKey: String
    ): RestZaaktypeBpmnConfiguration {
        assertPolicy(policyService.readOverigeRechten().beheren)
        val processDefinitions = zaaktypeBpmnConfigurationService
            .listZaaktypeBpmnConfigurations()
            .filter { it.bpmnProcessDefinitionKey == processDefinitionKey }

        if (processDefinitions.isEmpty()) {
            throw NotFoundException(
                "No zaaktype configuration found for process definition key '$processDefinitionKey'"
            )
        }
        if (processDefinitions.size != 1) {
            throw MultipleZaaktypeConfigurationsFoundException(
                "Multiple zaaktype configrations found for process definition key '$processDefinitionKey'"
            )
        }

        return processDefinitions.first().toRestZaaktypeBpmnConfiguration()
    }

    @POST
    @Path("{processDefinitionKey}")
    fun createZaaktypeBpmnConfiguration(
        @NotEmpty @PathParam("processDefinitionKey") processDefinitionKey: String,
        @Valid restZaaktypeBpmnProcessDefinition: RestZaaktypeBpmnConfiguration
    ): RestZaaktypeBpmnConfiguration {
        assertPolicy(policyService.readOverigeRechten().beheren)
        val bpmnConfiguration = zaaktypeBpmnConfigurationService.storeZaaktypeBpmnConfiguration(
            ZaaktypeBpmnConfiguration().apply {
                id = restZaaktypeBpmnProcessDefinition.id
                zaaktypeUuid = restZaaktypeBpmnProcessDefinition.zaaktypeUuid
                bpmnProcessDefinitionKey = processDefinitionKey
                zaaktypeOmschrijving = restZaaktypeBpmnProcessDefinition.zaaktypeOmschrijving
                productaanvraagtype = restZaaktypeBpmnProcessDefinition.productaanvraagtype
                groupId = restZaaktypeBpmnProcessDefinition.groepNaam
            }
        )
        return bpmnConfiguration.toRestZaaktypeBpmnConfiguration()
    }

    private fun ZaaktypeBpmnConfiguration.toRestZaaktypeBpmnConfiguration() =
        RestZaaktypeBpmnConfiguration(
            id = this.id,
            zaaktypeUuid = this.zaaktypeUuid,
            bpmnProcessDefinitionKey = this.bpmnProcessDefinitionKey,
            zaaktypeOmschrijving = this.zaaktypeOmschrijving,
            groepNaam = this.groupId,
            productaanvraagtype = this.productaanvraagtype
        )
}
