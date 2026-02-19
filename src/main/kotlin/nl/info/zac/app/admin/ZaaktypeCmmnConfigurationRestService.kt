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
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService.INADMISSIBLE_TERMINATION_ID
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService.INADMISSIBLE_TERMINATION_REASON
import net.atos.zac.admin.model.FormulierDefinitie
import net.atos.zac.app.admin.converter.RESTCaseDefinitionConverter
import net.atos.zac.app.admin.converter.RESTReplyToConverter
import net.atos.zac.app.admin.converter.RESTZaakbeeindigRedenConverter
import net.atos.zac.app.admin.model.RESTCaseDefinition
import net.atos.zac.app.admin.model.RESTReplyTo
import net.atos.zac.app.admin.model.RESTTaakFormulierDefinitie
import net.atos.zac.app.admin.model.RESTTaakFormulierVeldDefinitie
import net.atos.zac.app.admin.model.RestZaakbeeindigReden
import net.atos.zac.flowable.cmmn.CMMNService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.ReferenceTable.SystemReferenceTable.AFZENDER
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZaaktypeConfigurationType.BPMN
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZaaktypeConfigurationType.CMMN
import nl.info.zac.app.admin.converter.RestZaakafhandelParametersConverter
import nl.info.zac.app.admin.model.RestZaakafhandelParameters
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
class ZaaktypeCmmnConfigurationRestService @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val configurationService: ConfigurationService,
    private val cmmnService: CMMNService,
    private val zaaktypeConfigurationService: ZaaktypeConfigurationService,
    private val zaaktypeCmmnConfigurationService: ZaaktypeCmmnConfigurationService,
    private val zaaktypeCmmnConfigurationBeheerService: ZaaktypeCmmnConfigurationBeheerService,
    private val referenceTableService: ReferenceTableService,
    private val zaaktypeCmmnConfigurationConverter: RestZaakafhandelParametersConverter,
    private val zaaktypeBpmnConfigurationService: ZaaktypeBpmnConfigurationService,
    private val zaaktypeBpmnConfigurationBeheerService: ZaaktypeBpmnConfigurationBeheerService,
    private val caseDefinitionConverter: RESTCaseDefinitionConverter,
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
        assertPolicy(policyService.readOverigeRechten().beheren)
        return cmmnService.listCaseDefinitions()
            .map { caseDefinitionConverter.convertToRESTCaseDefinition(it, true) }
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
        assertPolicy(policyService.readOverigeRechten().beheren)
        return caseDefinitionConverter.convertToRESTCaseDefinition(caseDefinitionKey, true)
    }

    /**
     * Retrieve all zaakafhandelparameters for all available zaaktypes in the zaakregister.
     *
     * @return list of all zaakafhandelparameters
     */
    @GET
    fun listZaaktypeCmmnConfiguration(): List<RestZaakafhandelParameters> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return ztcClientService.listZaaktypen(configurationService.readDefaultCatalogusURI())
            .map { it.url.extractUuid() }
            .map(zaaktypeCmmnConfigurationService::readZaaktypeCmmnConfiguration)
            .map { zaaktypeCmmnConfigurationConverter.toRestZaakafhandelParameters(it, false) }
            .onEach { restZaakafhandelParameters ->
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(
                    restZaakafhandelParameters.zaaktype.uuid
                )?.let {
                    restZaakafhandelParameters.valide = true
                }
            }
    }

    /**
     * Retrieve the ZaaktypeConfiguration for a ZAAKTYPE
     *
     * @return RestZaakafhandelParameters for a ZAAKTYPE by uuid of the ZAAKTYPE
     */
    @GET
    @Path("{zaaktypeUUID}")
    fun readZaaktypeConfiguration(@PathParam("zaaktypeUUID") zaakTypeUUID: UUID): RestZaakafhandelParameters {
        assertPolicy(policyService.readOverigeRechten().beheren)
        zaaktypeConfigurationService.readZaaktypeConfiguration(zaakTypeUUID)?.let {
            return when (it.getConfigurationType()) {
                CMMN -> {
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID).let {
                            zaaktypeCmmnConfiguration ->
                        zaaktypeCmmnConfigurationConverter.toRestZaakafhandelParameters(
                            zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration,
                            inclusiefRelaties = true
                        )
                    }
                }
                BPMN -> {
                    zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaakTypeUUID).let {
                            zaaktypeBpmnConfiguration ->
                        zaaktypeCmmnConfigurationConverter.toRestZaakafhandelParameters(zaaktypeBpmnConfiguration!!)
                    }
                }
            }
        }

        // Use CMMN ZaakafhandelParameters as default when no configuration exists yet
        return zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID).let {
            zaaktypeCmmnConfigurationConverter.toRestZaakafhandelParameters(it, true)
        }
    }

    /**
     * Creates or updates ZaaktypeCmmnConfiguration.
     *
     * @param restZaakafhandelParameters the ZaaktypeCmmnConfiguration to save or update;
     * if the `id` field is null, a new ZaaktypeCmmnConfiguration will be created,
     * otherwise the existing ZaaktypeCmmnConfiguration will be updated
     * @throws InputValidationFailedException if the productaanvraagtype is already in use by another active zaaktype
     * @throws InputValidationFailedException if the productaanvraagtype is an empty string
     */
    @PUT
    fun createOrUpdateZaaktypeCmmnConfiguration(
        @Valid restZaakafhandelParameters: RestZaakafhandelParameters
    ): RestZaakafhandelParameters {
        assertPolicy(policyService.readOverigeRechten().beheren)

        restZaakafhandelParameters.productaanvraagtype?.also { productaanvraagtype ->
            restZaakafhandelParameters.zaaktype.omschrijving?.also {
                zaaktypeCmmnConfigurationBeheerService.checkIfProductaanvraagtypeIsNotAlreadyInUse(
                    productaanvraagtype,
                    it
                )
            }
            zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(productaanvraagtype)
        }
        restZaakafhandelParameters.defaultBehandelaarId?.let { defaultBehandelaarId ->
            restZaakafhandelParameters.defaultGroepId?.let { defaultGroepId ->
                identityService.validateIfUserIsInGroup(defaultBehandelaarId, defaultGroepId)
            }
        }
        return zaaktypeCmmnConfigurationConverter.toZaaktypeCmmnConfiguration(
            restZaakafhandelParameters
        ).let { zaakafhandelParameters ->
            val updatedZaakafhandelParameters = zaaktypeCmmnConfigurationBeheerService.storeZaaktypeCmmnConfiguration(
                zaakafhandelParameters
            )
            zaaktypeCmmnConfigurationService.cacheRemoveZaaktypeCmmnConfiguration(zaakafhandelParameters.zaaktypeUuid)
            zaaktypeCmmnConfigurationService.clearListCache()
            zaaktypeCmmnConfigurationConverter.toRestZaakafhandelParameters(
                updatedZaakafhandelParameters,
                true
            )
        }
    }

    /**
     * Retrieve all possible zaakbeeindig-redenen
     *
     * @return list of zaakbeeindig-redenen
     */
    @GET
    @Path("zaakbeeindigredenen")
    fun listZaakbeeindigRedenen(): List<RestZaakbeeindigReden> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return RESTZaakbeeindigRedenConverter.convertZaakbeeindigRedenen(
            zaaktypeCmmnConfigurationBeheerService.listZaakbeeindigRedenen()
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
        assertPolicy(policyService.readOverigeRechten().beheren)
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
    fun listReplyTos(): List<RESTReplyTo> =
        referenceTableService.readReferenceTable(AFZENDER.name).let { referenceTable ->
            referenceTableService.listReferenceTableValuesSorted(referenceTable).let {
                RESTReplyToConverter.convertReplyTos(
                    it
                )
            }
        }

    @GET
    @Path("smartdocuments-templates")
    fun listSmartDocumentsTemplates(): Set<RestSmartDocumentsTemplateGroup> {
        assertPolicy(policyService.readOverigeRechten().beheren)
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
    ): RestSmartDocumentsTemplateGroup {
        // No authorization to allow BPMN tasks (form.io) to read template group names and display them
        // We should consider a proper authorization with PABC
        return smartDocumentsTemplatesService.getTemplateGroup(group.path)
    }

    @GET
    @Path("{zaakafhandelUUID}/smartdocuments-templates-mapping")
    fun getSmartDocumentsTemplatesMapping(
        @PathParam("zaakafhandelUUID") zaakafhandelParameterUUID: UUID
    ): Set<RestMappedSmartDocumentsTemplateGroup> =
        smartDocumentsTemplatesService.getTemplatesMapping(zaakafhandelParameterUUID)

    @POST
    @Path("{zaakafhandelUUID}/smartdocuments-templates-mapping")
    fun storeSmartDocumentsTemplatesMapping(
        @PathParam("zaakafhandelUUID") zaakafhandelParameterUUID: UUID,
        restTemplateGroups: Set<RestMappedSmartDocumentsTemplateGroup>
    ) {
        assertPolicy(policyService.readOverigeRechten().beheren)

        val smartDocumentsTemplates = smartDocumentsTemplatesService.listTemplates()
        restTemplateGroups isSubsetOf smartDocumentsTemplates

        smartDocumentsTemplatesService.storeTemplatesMapping(restTemplateGroups, zaakafhandelParameterUUID)
    }

    private fun createHardcodedZaakTerminationReasons() =
        listOf(
            RestZaakbeeindigReden().apply {
                id = INADMISSIBLE_TERMINATION_ID
                naam = INADMISSIBLE_TERMINATION_REASON
            }
        )

    private fun readManagedZaakTerminationReasons(zaaktypeUUID: UUID): List<RestZaakbeeindigReden> =
        zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeUUID)
            ?.getZaakbeeindigParameters()
            ?.map { it.zaakbeeindigReden }
            ?.let { RESTZaakbeeindigRedenConverter.convertZaakbeeindigRedenen(it) }
            ?: emptyList()
}
