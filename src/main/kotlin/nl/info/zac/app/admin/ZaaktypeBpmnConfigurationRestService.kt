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
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.exception.MultipleZaaktypeConfigurationsFoundException
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.app.admin.converter.RestZaakbeeindigParameterConverter
import nl.info.zac.app.admin.model.RestZaaktypeBpmnConfiguration
import nl.info.zac.app.admin.model.toRestBetrokkeneKoppelingen
import nl.info.zac.app.admin.model.toRestBrpDoelbindingen
import nl.info.zac.app.admin.model.toZaaktypeCompletionParametersList
import nl.info.zac.app.admin.model.toZaaktypeBetrokkenParameters
import nl.info.zac.app.admin.model.toZaaktypeBpmnConfiguration
import nl.info.zac.app.admin.model.toZaaktypeBrpParameters
import nl.info.zac.app.zaak.model.toRestResultaatType
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

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
    private val ztcClientService: ZtcClientService,
    private val restZaakbeeindigParameterConverter: RestZaakbeeindigParameterConverter
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
    fun createOrUpdateZaaktypeBpmnConfiguration(
        @Valid restZaaktypeBpmnConfiguration: RestZaaktypeBpmnConfiguration
    ): RestZaaktypeBpmnConfiguration {
        assertPolicy(policyService.readOverigeRechten().beheren)
        checkNotNull(restZaaktypeBpmnConfiguration.groepNaam) { "groepNaam must not be null" }
        restZaaktypeBpmnConfiguration.productaanvraagtype?.let {
            checkIfProductaanvraagtypeIsNotAlreadyInUse(
                productaanvraagtype = it,
                zaaktypeDescription = restZaaktypeBpmnConfiguration.zaaktypeOmschrijving,
                zaaktypeUuid = restZaaktypeBpmnConfiguration.zaaktypeUuid
            )
        }
        val zaaktypeBpmnConfiguration = zaaktypeBpmnConfigurationBeheerService.findConfiguration(
            restZaaktypeBpmnConfiguration.zaaktypeUuid
        )?.apply {
            // update existing zaaktype BPMN configuration with values from REST object
            bpmnProcessDefinitionKey = restZaaktypeBpmnConfiguration.bpmnProcessDefinitionKey
            groepID = restZaaktypeBpmnConfiguration.groepNaam
            defaultBehandelaarId = restZaaktypeBpmnConfiguration.defaultBehandelaarId
            productaanvraagtype = restZaaktypeBpmnConfiguration.productaanvraagtype
            zaaktypeBetrokkeneParameters = restZaaktypeBpmnConfiguration.betrokkeneKoppelingen?.toZaaktypeBetrokkenParameters(this)
            zaaktypeBrpParameters = restZaaktypeBpmnConfiguration.brpDoelbindingen?.toZaaktypeBrpParameters(this)
            nietOntvankelijkResultaattype = restZaaktypeBpmnConfiguration.zaakNietOntvankelijkResultaattype?.id
            setZaakbeeindigParameters(restZaaktypeBpmnConfiguration.zaakbeeindigParameters.toZaaktypeCompletionParametersList())
        } ?: restZaaktypeBpmnConfiguration.toZaaktypeBpmnConfiguration()
        return zaaktypeBpmnConfigurationBeheerService.storeConfiguration(
            zaaktypeBpmnConfiguration
        ).toRestZaaktypeBpmnConfiguration()
    }

    private fun checkIfProductaanvraagtypeIsNotAlreadyInUse(
        productaanvraagtype: String,
        zaaktypeDescription: String,
        zaaktypeUuid: UUID
    ) {
        zaaktypeCmmnConfigurationBeheerService.checkIfProductaanvraagtypeIsNotAlreadyInUse(
            productaanvraagtype = productaanvraagtype,
            zaaktypeOmschrijving = zaaktypeDescription
        )
        zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(
            productaanvraagtype = productaanvraagtype,
            zaaktypeUuid = zaaktypeUuid
        )
    }

    private fun ZaaktypeBpmnConfiguration.toRestZaaktypeBpmnConfiguration() = RestZaaktypeBpmnConfiguration(
        id = this.id,
        zaaktypeUuid = this.zaaktypeUuid,
        bpmnProcessDefinitionKey = this.bpmnProcessDefinitionKey,
        zaaktypeOmschrijving = this.zaaktypeOmschrijving,
        groepNaam = this.groepID,
        defaultBehandelaarId = this.defaultBehandelaarId,
        productaanvraagtype = this.productaanvraagtype,
        creatiedatum = this.creatiedatum,
        zaakNietOntvankelijkResultaattype = this.nietOntvankelijkResultaattype?.let {
            ztcClientService.readResultaattype(it).toRestResultaatType()
        },
        zaakbeeindigParameters = restZaakbeeindigParameterConverter.convertZaakbeeindigParameters(
            this.getZaakbeeindigParameters()
        )
    ).apply {
        zaaktypeBetrokkeneParameters?.let { betrokkeneKoppelingen = it.toRestBetrokkeneKoppelingen() }
        zaaktypeBrpParameters?.let { brpDoelbindingen = it.toRestBrpDoelbindingen() }
    }
}
