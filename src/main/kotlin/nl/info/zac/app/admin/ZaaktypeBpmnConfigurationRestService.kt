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
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.app.admin.model.RestZaaktypeBpmnConfiguration
import nl.info.zac.app.admin.model.toRestZaaktypeOverzicht
import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.flowable.bpmn.model.ZaaktypeBpmnConfiguration
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@Singleton
@Path("zaaktype-bpmn-configurations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class ZaaktypeBpmnConfigurationRestService @Inject constructor(
    private val zaaktypeBpmnConfigurationService: ZaaktypeBpmnConfigurationService,
    private val policyService: PolicyService,
    private val ztcClientService: ZtcClientService
) {
    @GET
    fun listZaaktypeBpmnConfigurations(): List<RestZaaktypeBpmnConfiguration> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return zaaktypeBpmnConfigurationService.listBpmnProcessDefinitions().map {
            it.toRestZaaktypeBpmnConfiguration()
        }
    }

    @GET
    @Path("{processDefinitionKey}")
    fun listZaaktypeBpmnProcessDefinition(
        @NotEmpty @PathParam("processDefinitionKey") processDefinitionKey: String
    ): RestZaaktypeBpmnConfiguration {
        assertPolicy(policyService.readOverigeRechten().beheren)
        val processDefinitions = zaaktypeBpmnConfigurationService
            .listBpmnProcessDefinitions()
            .filter { it.bpmnProcessDefinitionKey == processDefinitionKey }

        if (processDefinitions.isEmpty()) {
            throw NotFoundException("No process definition found for key '$processDefinitionKey'")
        }
        check(processDefinitions.size == 1) { "Multiple process definitions found for key '$processDefinitionKey'" }

        return processDefinitions.first().toRestZaaktypeBpmnConfiguration()
    }

    @POST
    @Path("{processDefinitionKey}")
    fun createZaaktypeBpmnProcessDefinition(
        @NotEmpty @PathParam("processDefinitionKey") processDefinitionKey: String,
        @Valid restZaaktypeBpmnProcessDefinition: RestZaaktypeBpmnConfiguration,
        @Context uriInfo: UriInfo
    ): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        zaaktypeBpmnConfigurationService.createZaaktypeBpmnProcessDefinition(
            ZaaktypeBpmnConfiguration().apply {
                zaaktypeUuid = restZaaktypeBpmnProcessDefinition.zaaktype.uuid
                    ?: throw InputValidationFailedException(ErrorCode.ERROR_CODE_CASE_TYPE_UUID_REQUIRED)
                zaaktypeOmschrijving = restZaaktypeBpmnProcessDefinition.zaaktype.omschrijving
                    ?: throw InputValidationFailedException(ErrorCode.ERROR_CODE_CASE_TYPE_DESCRIPTION_REQUIRED)
                bpmnProcessDefinitionKey = processDefinitionKey
                productaanvraagtype = restZaaktypeBpmnProcessDefinition.productaanvraagtype
                groupId = restZaaktypeBpmnProcessDefinition.groepNaam
            }
        )
        return Response.created(uriInfo.requestUri).build()
    }

    private fun ZaaktypeBpmnConfiguration.toRestZaaktypeBpmnConfiguration() =
        RestZaaktypeBpmnConfiguration(
            zaaktype = ztcClientService.readZaaktype(this.zaaktypeUuid).toRestZaaktypeOverzicht(),
            bpmnProcessDefinitionKey = this.bpmnProcessDefinitionKey,
            groepNaam = this.groupId,
            productaanvraagtype = this.productaanvraagtype
        )
}
