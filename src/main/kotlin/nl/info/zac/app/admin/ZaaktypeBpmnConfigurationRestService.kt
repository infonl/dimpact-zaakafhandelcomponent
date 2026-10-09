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
import nl.info.zac.admin.ResultaattypeReferenceService
import nl.info.zac.admin.ZaaktypeConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.exception.MultipleZaaktypeConfigurationsFoundException
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.app.admin.converter.RestZaakbeeindigParameterConverter
import nl.info.zac.app.admin.model.RestZaaktypeBpmnConfiguration
import nl.info.zac.app.admin.model.toRestBetrokkeneKoppelingen
import nl.info.zac.app.admin.model.toRestBrpDoelbindingen
import nl.info.zac.app.admin.model.toRestSmartDocuments
import nl.info.zac.app.admin.model.toZaaktypeBetrokkenParameters
import nl.info.zac.app.admin.model.toZaaktypeBrpParameters
import nl.info.zac.app.admin.model.toZaaktypeConfiguration
import nl.info.zac.app.zaak.model.toRestResultaatType
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.smartdocuments.SmartDocumentsService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@Singleton
@Path("zaaktype-bpmn-configuration")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@Suppress("LongParameterList")
@NoArgConstructor
class ZaaktypeBpmnConfigurationRestService @Inject constructor(
    private val zaaktypeConfigurationService: ZaaktypeConfigurationService,
    private val zaaktypeConfigurationBeheerService: ZaaktypeConfigurationBeheerService,
    private val policyService: PolicyService,
    private val resultaattypeReferenceService: ResultaattypeReferenceService,
    private val restZaakbeeindigParameterConverter: RestZaakbeeindigParameterConverter,
    private val smartDocumentsService: SmartDocumentsService
) {
    @GET
    fun listZaaktypeBpmnConfigurations(): List<RestZaaktypeBpmnConfiguration> {
        assertPolicy(policyService.readOverigeRechten().canStartenZaak || policyService.readOverigeRechten().canBeheren)
        return zaaktypeConfigurationService.listConfigurationsBoundTo(ProcessEngine.BPMN).map {
            it.toRestZaaktypeBpmnConfiguration()
        }
    }

    @GET
    @Path("{processDefinitionKey}")
    fun getZaaktypeBpmnConfiguration(
        @NotEmpty @PathParam("processDefinitionKey") processDefinitionKey: String
    ): RestZaaktypeBpmnConfiguration {
        assertPolicy(policyService.readOverigeRechten().canStartenZaak || policyService.readOverigeRechten().canBeheren)
        val processDefinitions = zaaktypeConfigurationService
            .listConfigurationsBoundTo(ProcessEngine.BPMN)
            .filter { it.processBinding?.definitionKey == processDefinitionKey }
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
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        checkNotNull(restZaaktypeBpmnConfiguration.groepNaam) { "groepNaam must not be null" }
        restZaaktypeBpmnConfiguration.productaanvraagtype?.let {
            zaaktypeConfigurationBeheerService.checkProductaanvraagtypeIsNotInUse(
                productaanvraagtype = it,
                zaaktypeOmschrijving = restZaaktypeBpmnConfiguration.zaaktypeOmschrijving
            )
        }
        val zaaktypeConfiguration = zaaktypeConfigurationBeheerService.findConfiguration(
            restZaaktypeBpmnConfiguration.zaaktypeUuid
        )?.apply {
            // update the existing configuration with the values of the REST object, and keep the settings that it
            // does not carry
            bindTo(ProcessEngine.BPMN, restZaaktypeBpmnConfiguration.bpmnProcessDefinitionKey)
            groepID = restZaaktypeBpmnConfiguration.groepNaam
            defaultBehandelaarId = restZaaktypeBpmnConfiguration.defaultBehandelaarId
            productaanvraagtype = restZaaktypeBpmnConfiguration.productaanvraagtype
            zaaktypeBetrokkeneParameters = restZaaktypeBpmnConfiguration.betrokkeneKoppelingen?.toZaaktypeBetrokkenParameters(this)
            zaaktypeBrpParameters = restZaaktypeBpmnConfiguration.brpDoelbindingen?.toZaaktypeBrpParameters(this)
            isSmartDocumentsEnabled = restZaaktypeBpmnConfiguration.smartDocuments?.isEnabledForZaaktype ?: false
        } ?: restZaaktypeBpmnConfiguration.toZaaktypeConfiguration()
        zaaktypeConfiguration.apply {
            nietOntvankelijkResultaattypeOmschrijving = restZaaktypeBpmnConfiguration.zaakNietOntvankelijkResultaattype
                ?.let { resultaattypeReferenceService.readOmschrijving(it.id) }
            setZaakbeeindigParameters(
                restZaakbeeindigParameterConverter.toZaaktypeCompletionParameters(
                    restZaaktypeBpmnConfiguration.zaakbeeindigParameters
                )
            )
        }
        return zaaktypeConfigurationBeheerService.storeConfiguration(zaaktypeConfiguration)
            .toRestZaaktypeBpmnConfiguration()
    }

    private fun ZaaktypeConfiguration.toRestZaaktypeBpmnConfiguration() = RestZaaktypeBpmnConfiguration(
        id = this.id,
        zaaktypeUuid = this.zaaktypeUuid,
        bpmnProcessDefinitionKey = checkNotNull(this.processBinding) {
            "Zaaktype configuration for zaaktype '$zaaktypeUuid' is not bound to a process"
        }.definitionKey,
        zaaktypeOmschrijving = this.zaaktypeOmschrijving,
        groepNaam = this.groepID,
        defaultBehandelaarId = this.defaultBehandelaarId,
        productaanvraagtype = this.productaanvraagtype,
        creatiedatum = this.creatiedatum,
        zaakNietOntvankelijkResultaattype = resultaattypeReferenceService.findNietOntvankelijkResultaattype(this)
            ?.toRestResultaatType(),
        zaakbeeindigParameters = restZaakbeeindigParameterConverter.convertZaakbeeindigParameters(
            this.getZaakbeeindigParameters()
        ),
        smartDocuments = this.toRestSmartDocuments(smartDocumentsService.isEnabled())
    ).apply {
        zaaktypeBetrokkeneParameters?.let { betrokkeneKoppelingen = it.toRestBetrokkeneKoppelingen() }
        zaaktypeBrpParameters?.let { brpDoelbindingen = it.toRestBrpDoelbindingen() }
    }
}
