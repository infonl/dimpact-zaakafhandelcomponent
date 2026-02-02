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
import nl.info.zac.app.admin.model.toBetrokkeneKoppelingen
import nl.info.zac.app.admin.model.toBrpDoelbindingen
import nl.info.zac.app.admin.model.toRestBetrokkeneKoppelingen
import nl.info.zac.app.admin.model.toRestBrpDoelbindingen
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
    private val policyService: PolicyService,
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

    /**
     * Creates or updates a Zaaktype BPMN configuration.
     * In future, we should split this into two separate endpoints for create (POST) and update (PUT),
     * each with their own data input classes.
     */
    @POST
    @Path("{processDefinitionKey}")
    fun createOrUpdateZaaktypeBpmnConfiguration(
        @NotEmpty @PathParam("processDefinitionKey") processDefinitionKey: String,
        @Valid restZaaktypeBpmnConfiguration: RestZaaktypeBpmnConfiguration
    ): RestZaaktypeBpmnConfiguration {
        assertPolicy(policyService.readOverigeRechten().beheren)
        checkNotNull(restZaaktypeBpmnConfiguration.groepNaam) {
            "groepNaam must not be null"
        }
        return ZaaktypeBpmnConfiguration().apply {
            id = restZaaktypeBpmnConfiguration.id
            zaaktypeUuid = restZaaktypeBpmnConfiguration.zaaktypeUuid
            bpmnProcessDefinitionKey = processDefinitionKey
            zaaktypeOmschrijving = restZaaktypeBpmnConfiguration.zaaktypeOmschrijving
            productaanvraagtype = restZaaktypeBpmnConfiguration.productaanvraagtype
            defaultBehandelaarId = restZaaktypeBpmnProcessDefinition.defaultBehandelaarId
            groepID = restZaaktypeBpmnConfiguration.groepNaam
            creatiedatum = restZaaktypeBpmnConfiguration.creatiedatum ?: ZonedDateTime.now()
        }.let {
            it.productaanvraagtype?.let { productaanvraagtype ->
                zaaktypeCmmnConfigurationBeheerService.checkIfProductaanvraagtypeIsNotAlreadyInUse(
                    productaanvraagtype,
                    it.zaaktypeOmschrijving
                )
                zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(it)
            }
            it.zaaktypeBetrokkeneParameters = restZaaktypeBpmnConfiguration.betrokkeneKoppelingen?.toBetrokkeneKoppelingen(it)
            it.zaaktypeBrpParameters = restZaaktypeBpmnConfiguration.brpDoelbindingen?.toBrpDoelbindingen(it)
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
            defaultBehandelaarId = this.defaultBehandelaarId,
            productaanvraagtype = this.productaanvraagtype,
            creatiedatum = this.creatiedatum
        ).apply {
            zaaktypeBetrokkeneParameters?.let { betrokkeneKoppelingen = it.toRestBetrokkeneKoppelingen() }
            zaaktypeBrpParameters?.let { brpDoelbindingen = it.toRestBrpDoelbindingen() }
        }
}
