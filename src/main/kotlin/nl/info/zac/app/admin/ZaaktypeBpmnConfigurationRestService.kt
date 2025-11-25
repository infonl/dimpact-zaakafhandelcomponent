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
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.exception.MultipleZaaktypeConfigurationsFoundException
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.app.admin.model.RestZaaktypeBpmnConfiguration
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime

@Singleton
@Path("zaaktype-bpmn-configuration")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class ZaaktypeBpmnConfigurationRestService @Inject constructor(
    private val zaaktypeBpmnConfigurationService: ZaaktypeBpmnConfigurationService,
    private val zaaktypeBpmnConfigurationBeheerService: ZaaktypeBpmnConfigurationBeheerService,
    private val zaaktypeCmmnConfigurationBeheerService: ZaaktypeCmmnConfigurationBeheerService,
    private val policyService: PolicyService
) {
    @GET
    fun listZaaktypeBpmnConfigurations(): List<RestZaaktypeBpmnConfiguration> {
        assertPolicy(policyService.readOverigeRechten().startenZaak || policyService.readOverigeRechten().beheren)
        return zaaktypeBpmnConfigurationBeheerService.listConfigurations().map {
            it.toRestZaaktypeBpmnConfiguration()
        }
    }

    @GET
    @Path("{processDefinitionKey}")
    fun getZaaktypeBpmnConfiguration(
        @NotEmpty @PathParam("processDefinitionKey") processDefinitionKey: String
    ): RestZaaktypeBpmnConfiguration {
        assertPolicy(policyService.readOverigeRechten().startenZaak || policyService.readOverigeRechten().beheren)
        val processDefinitions = zaaktypeBpmnConfigurationBeheerService
            .listConfigurations()
            .filter { it.bpmnProcessDefinitionKey == processDefinitionKey }

        if (processDefinitions.isEmpty()) {
            throw NotFoundException(
                "No zaaktype configuration found for process definition key '$processDefinitionKey'"
            )
        }
        if (processDefinitions.size != 1) {
            throw MultipleZaaktypeConfigurationsFoundException(
                "Multiple zaaktype configurations found for process definition key '$processDefinitionKey'"
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
        return ZaaktypeBpmnConfiguration().apply {
            id = restZaaktypeBpmnProcessDefinition.id
            zaaktypeUuid = restZaaktypeBpmnProcessDefinition.zaaktypeUuid
            bpmnProcessDefinitionKey = processDefinitionKey
            zaaktypeOmschrijving = restZaaktypeBpmnProcessDefinition.zaaktypeOmschrijving
            productaanvraagtype = restZaaktypeBpmnProcessDefinition.productaanvraagtype
            groepID = restZaaktypeBpmnProcessDefinition.groepNaam
                ?: throw NullPointerException("restZaaktypeBpmnProcessDefinition.groepNaam is null")
            creatiedatum = restZaaktypeBpmnProcessDefinition.creatiedatum ?: ZonedDateTime.now()
        }.let {
            it.productaanvraagtype?.let { productaanvraagtype ->
                zaaktypeCmmnConfigurationBeheerService.checkIfProductaanvraagtypeIsNotAlreadyInUse(
                    productaanvraagtype,
                    it.zaaktypeOmschrijving
                )
                zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(it)
            }
            zaaktypeBpmnConfigurationBeheerService.storeConfiguration(it).toRestZaaktypeBpmnConfiguration()
        }
    }

    private fun ZaaktypeBpmnConfiguration.toRestZaaktypeBpmnConfiguration() =
        RestZaaktypeBpmnConfiguration(
            id = this.id,
            zaaktypeUuid = this.zaaktypeUuid,
            bpmnProcessDefinitionKey = this.bpmnProcessDefinitionKey,
            zaaktypeOmschrijving = this.zaaktypeOmschrijving,
            groepNaam = this.groepID,
            productaanvraagtype = this.productaanvraagtype,
            creatiedatum = this.creatiedatum,
        )
}
