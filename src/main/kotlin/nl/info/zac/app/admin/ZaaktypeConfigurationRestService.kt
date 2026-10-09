/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.zac.app.admin.converter.RestCaseDefinitionConverter
import net.atos.zac.app.admin.converter.RESTZaakbeeindigRedenConverter
import net.atos.zac.app.admin.model.RESTCaseDefinition
import net.atos.zac.app.admin.model.RESTTaakFormulierDefinitie
import net.atos.zac.app.admin.model.RESTTaakFormulierVeldDefinitie
import net.atos.zac.app.admin.model.RestZaakbeeindigReden
import net.atos.zac.flowable.cmmn.CmmnService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.ZaaktypeConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.ZaaktypeConfigurationService.Companion.INADMISSIBLE_TERMINATION_ID
import nl.info.zac.admin.ZaaktypeConfigurationService.Companion.INADMISSIBLE_TERMINATION_REASON
import nl.info.zac.admin.model.FormulierDefinitie
import nl.info.zac.admin.model.ReferenceTable.SystemReferenceTable.AFZENDER
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.app.admin.converter.RestZaaktypeConfigurationConverter
import nl.info.zac.app.admin.model.RestReplyTo
import nl.info.zac.app.admin.model.RestZaaktypeConfiguration
import nl.info.zac.app.admin.model.toRestReplyTos
import nl.info.zac.app.zaak.model.RestResultaattype
import nl.info.zac.app.zaak.model.toRestResultaatTypes
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.identity.IdentityService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import nl.info.zac.smartdocuments.rest.RestMappedSmartDocumentsTemplateGroup
import nl.info.zac.smartdocuments.rest.RestSmartDocumentsPath
import nl.info.zac.smartdocuments.rest.RestSmartDocumentsTemplateGroup
import nl.info.zac.smartdocuments.rest.isSubsetOf
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@Singleton
@Path("zaakafhandelparameters")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
@Suppress("LongParameterList", "TooManyFunctions")
class ZaaktypeConfigurationRestService @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val configurationService: ConfigurationService,
    private val cmmnService: CmmnService,
    private val zaaktypeConfigurationService: ZaaktypeConfigurationService,
    private val zaaktypeConfigurationBeheerService: ZaaktypeConfigurationBeheerService,
    private val referenceTableService: ReferenceTableService,
    private val restZaaktypeConfigurationConverter: RestZaaktypeConfigurationConverter,
    private val caseDefinitionConverter: RestCaseDefinitionConverter,
    private val smartDocumentsTemplatesService: SmartDocumentsTemplatesService,
    private val policyService: PolicyService,
    private val identityService: IdentityService
) {
    /**
     * Retrieve all case definitions that can be linked to a ZAAKTYPE
     *
     * @return list of all case definitions
     */
    @GET
    @Path("case-definitions")
    fun listCaseDefinitions(): List<RESTCaseDefinition> {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return cmmnService.listCaseDefinitions()
            .map { caseDefinitionConverter.convertToRestCaseDefinition(it, true) }
    }

    /**
     * Retrieving a CASE_DEFINITION including its PLAN_ITEM_DEFINITIONs
     *
     * @param caseDefinitionKey id of the CASE_DEFINITION
     * @return CASE_DEFINITION including its PLAN_ITEM_DEFINITIONs
     */
    @GET
    @Path("case-definitions/{key}")
    fun readCaseDefinition(@PathParam("key") caseDefinitionKey: String): RESTCaseDefinition {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return caseDefinitionConverter.convertToRestCaseDefinition(caseDefinitionKey, true)
    }

    @GET
    fun listZaaktypeConfigurations(): List<RestZaaktypeConfiguration> {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return ztcClientService.listZaaktypen(configurationService.readDefaultCatalogusURI())
            .map { findOrCreateConfiguration(it.url.extractUuid()) }
            .map { restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(it, false) }
    }

    @GET
    @Path("{zaaktypeUUID}")
    fun readZaaktypeConfiguration(@PathParam("zaaktypeUUID") zaakTypeUUID: UUID): RestZaaktypeConfiguration {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(
            zaaktypeConfiguration = findOrCreateConfiguration(zaakTypeUUID),
            inclusiefRelaties = true
        )
    }

    /**
     * Creates or updates the configuration of a zaaktype version and binds it to its CMMN case definition.
     *
     * @param restZaaktypeConfiguration the configuration to store; when the zaaktype version already has a
     * configuration, that configuration is updated
     * @throws InputValidationFailedException if the productaanvraagtype is already in use by another active zaaktype
     * @throws InputValidationFailedException if the productaanvraagtype is an empty string
     */
    @PUT
    fun createOrUpdateZaaktypeCmmnConfiguration(
        @Valid restZaaktypeConfiguration: RestZaaktypeConfiguration
    ): RestZaaktypeConfiguration {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        restZaaktypeConfiguration.productaanvraagtype?.also { productaanvraagtype ->
            zaaktypeConfigurationBeheerService.checkProductaanvraagtypeIsNotInUse(
                productaanvraagtype = productaanvraagtype,
                zaaktypeOmschrijving = restZaaktypeConfiguration.zaaktype.omschrijving.orEmpty()
            )
        }
        restZaaktypeConfiguration.defaultBehandelaarId?.let { defaultBehandelaarId ->
            restZaaktypeConfiguration.defaultGroepId?.let { defaultGroepId ->
                identityService.validateIfUserIsInGroup(defaultBehandelaarId, defaultGroepId)
            }
        }
        return restZaaktypeConfigurationConverter.toZaaktypeConfiguration(restZaaktypeConfiguration)
            .let(zaaktypeConfigurationBeheerService::storeConfiguration)
            .let { restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(it, true) }
    }

    /**
     * Retrieve all possible zaakbeeindig-redenen
     *
     * @return list of zaakbeeindig-redenen
     */
    @GET
    @Path("zaakbeeindigredenen")
    fun listZaakbeeindigRedenen(): List<RestZaakbeeindigReden> {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return RESTZaakbeeindigRedenConverter.convertZaakbeeindigRedenen(
            zaaktypeConfigurationService.listZaakbeeindigRedenen()
        )
    }

    /**
     * Retrieve zaakbeeindig redenen for a zaaktype
     *
     * @return list of zaakbeeindig-redenen
     */
    @GET
    @Path("zaakbeeindigredenen/{zaaktypeUUID}")
    fun listZaakbeeindigRedenenForZaaktype(
        @PathParam("zaaktypeUUID") zaaktypeUUID: UUID
    ): List<RestZaakbeeindigReden> =
        createHardcodedZaakTerminationReasons() + readManagedZaakTerminationReasons(zaaktypeUUID)

    /**
     * Retrieve all resultaattypes for a zaaktype.
     * This function is identical except for the policy check to [nl.info.zac.app.zaak.ZaakRestService.listResultaattypesForZaaktype]
     * and should be merged with that function.
     *
     * @param zaaktypeUUID the id of the zaaktype
     * @return list of resultaattypes
     */
    @GET
    @Path("resultaattypes/{zaaktypeUUID}")
    fun listResultaattypesForZaaktypeForAdmins(@PathParam("zaaktypeUUID") zaaktypeUUID: UUID): List<RestResultaattype> {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return ztcClientService.readResultaattypen(
            ztcClientService.readZaaktype(zaaktypeUUID).url
        ).toRestResultaatTypes()
    }

    /**
     * Retrieve all formulier definities that can be linked to a HUMAN_TASK_PLAN_ITEM
     *
     * @return lijst of formulier definities
     */
    @GET
    @Path("formulierdefinities")
    fun listTaskFormDefinitions(): List<RESTTaakFormulierDefinitie> =
        FormulierDefinitie.entries.toTypedArray()
            .map {
                RESTTaakFormulierDefinitie(
                    it.name,
                    it.veldDefinities.map { formulierVeldDefinitie ->
                        RESTTaakFormulierVeldDefinitie(
                            formulierVeldDefinitie.name,
                            formulierVeldDefinitie.defaultTabel.name
                        )
                    }
                )
            }

    /**
     * Retrieve all possible reply-tos
     *
     * @return sorted list of reply-tos
     */
    @GET
    @Path("replyTo")
    fun listReplyTos(): List<RestReplyTo> =
        referenceTableService.readSystemReferenceTable(AFZENDER).let { referenceTable ->
            referenceTableService.listReferenceTableValuesSorted(referenceTable).toRestReplyTos()
        }

    @GET
    @Path("smartdocuments-templates")
    fun listSmartDocumentsTemplates(): Set<RestSmartDocumentsTemplateGroup> {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return smartDocumentsTemplatesService.listTemplates()
    }

    @PUT
    @Path("smartdocuments-group-template-names")
    fun listSmartDocumentsGroupTemplateNames(
        group: RestSmartDocumentsPath
    ) =
        // No authorization to allow BPMN tasks (form.io) to read template names and display them
        // We should consider a proper authorization with PABC
        smartDocumentsTemplatesService.listGroupTemplateNames(group.path)

    @PUT
    @Path("smartdocuments-template-group")
    fun getSmartDocumentsGroup(
        group: RestSmartDocumentsPath
    ): RestSmartDocumentsTemplateGroup =
        // No authorization to allow BPMN tasks (form.io) to read template group names and display them
        // We should consider a proper authorization with PABC
        smartDocumentsTemplatesService.getTemplateGroup(group.path)

    @GET
    @Path("{zaakafhandelUUID}/smartdocuments-templates-mapping")
    fun getSmartDocumentsTemplatesMapping(
        @PathParam("zaakafhandelUUID") zaaktypeUuid: UUID
    ): Set<RestMappedSmartDocumentsTemplateGroup> =
        smartDocumentsTemplatesService.getTemplatesMapping(zaaktypeUuid)

    @POST
    @Path("{zaakafhandelUUID}/smartdocuments-templates-mapping")
    fun storeSmartDocumentsTemplatesMapping(
        @PathParam("zaakafhandelUUID") zaaktypeUuid: UUID,
        restTemplateGroups: Set<RestMappedSmartDocumentsTemplateGroup>
    ) {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        val smartDocumentsTemplates = smartDocumentsTemplatesService.listTemplates()
        restTemplateGroups isSubsetOf smartDocumentsTemplates
        smartDocumentsTemplatesService.storeTemplatesMapping(restTemplateGroups, zaaktypeUuid)
    }

    private fun createHardcodedZaakTerminationReasons() =
        listOf(
            RestZaakbeeindigReden().apply {
                id = INADMISSIBLE_TERMINATION_ID
                naam = INADMISSIBLE_TERMINATION_REASON
            }
        )

    private fun readManagedZaakTerminationReasons(zaaktypeUUID: UUID): List<RestZaakbeeindigReden> =
        zaaktypeConfigurationService.findConfiguration(zaaktypeUUID)
            ?.getZaakbeeindigParameters()
            ?.map { it.zaakbeeindigReden }
            ?.let { RESTZaakbeeindigRedenConverter.convertZaakbeeindigRedenen(it) }.orEmpty()

    /**
     * Returns the configuration of the zaaktype version, or a new one when the zaaktype version has none yet, so that
     * a beheerder can configure it.
     */
    private fun findOrCreateConfiguration(zaaktypeUuid: UUID) =
        zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)
            ?: ZaaktypeConfiguration().apply { this.zaaktypeUuid = zaaktypeUuid }
}
