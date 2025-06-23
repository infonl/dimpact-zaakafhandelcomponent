/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.ZaakafhandelParameterService.INADMISSIBLE_TERMINATION_ID
import net.atos.zac.admin.ZaakafhandelParameterService.INADMISSIBLE_TERMINATION_REASON
import net.atos.zac.admin.model.FormulierDefinitie
import net.atos.zac.app.admin.converter.RESTCaseDefinitionConverter
import net.atos.zac.app.admin.converter.RESTReplyToConverter
import net.atos.zac.app.admin.converter.RESTZaakbeeindigRedenConverter
import net.atos.zac.app.admin.model.RESTCaseDefinition
import net.atos.zac.app.admin.model.RESTReplyTo
import net.atos.zac.app.admin.model.RESTTaakFormulierDefinitie
import net.atos.zac.app.admin.model.RESTTaakFormulierVeldDefinitie
import net.atos.zac.app.admin.model.RESTZaakbeeindigReden
import net.atos.zac.flowable.cmmn.CMMNService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.ZaakafhandelParameterBeheerService
import nl.info.zac.admin.model.ReferenceTable
import nl.info.zac.app.admin.converter.RestZaakafhandelParametersConverter
import nl.info.zac.app.admin.model.RestZaakafhandelParameters
import nl.info.zac.app.zaak.model.RestResultaattype
import nl.info.zac.app.zaak.model.toRestResultaatTypes
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.exception.ErrorCode.ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE
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
import java.util.logging.Logger

@Singleton
@Path("zaakafhandelparameters")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
@Suppress("LongParameterList", "TooManyFunctions")
class ZaakafhandelParametersRestService @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val configuratieService: ConfiguratieService,
    private val cmmnService: CMMNService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val zaakafhandelParameterBeheerService: ZaakafhandelParameterBeheerService,
    private val referenceTableService: ReferenceTableService,
    private val zaakafhandelParametersConverter: RestZaakafhandelParametersConverter,
    private val caseDefinitionConverter: RESTCaseDefinitionConverter,
    private val smartDocumentsTemplatesService: SmartDocumentsTemplatesService,
    private val policyService: PolicyService,
    private val identityService: IdentityService
) {
    companion object {
        private val LOG = Logger.getLogger(ZaakafhandelParametersRestService::class.java.name)
    }

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
    fun listZaakafhandelParameters(): List<RestZaakafhandelParameters> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return ztcClientService.listZaaktypen(configuratieService.readDefaultCatalogusURI())
            .map { it.url.extractUuid() }
            .map(zaakafhandelParameterService::readZaakafhandelParameters)
            .map { zaakafhandelParametersConverter.toRestZaakafhandelParameters(it, false) }
    }

    /**
     * Retrieve the ZAAKAFHANDELPARAMETERS for a ZAAKTYPE
     *
     * @return ZAAKAFHANDELPARAMETERS for a ZAAKTYPE by uuid of the ZAAKTYPE
     */
    @GET
    @Path("{zaaktypeUUID}")
    fun readZaakafhandelParameters(@PathParam("zaaktypeUUID") zaakTypeUUID: UUID): RestZaakafhandelParameters {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID).let {
            zaakafhandelParametersConverter.toRestZaakafhandelParameters(it, true)
        }
    }

    /**
     * Creates or updates zaakafhandelparameters.
     *
     * @param restZaakafhandelParameters the zaakafhandelparameters to save or update;
     * if the `id` field is null, a new zaakafhandelparameters will be created,
     * otherwise the existing zaakafhandelparameters will be updated
     * @throws InputValidationFailedException if the productaanvraagtype is already in use by another active zaaktype
     */
    @PUT
    fun createOrUpdateZaakafhandelparameters(
        restZaakafhandelParameters: RestZaakafhandelParameters
    ): RestZaakafhandelParameters {
        assertPolicy(policyService.readOverigeRechten().beheren)
        restZaakafhandelParameters.productaanvraagtype?.also {
            checkIfProductaanvraagtypeIsNotAlreadyInUse(it, restZaakafhandelParameters.zaaktype.omschrijving)
        }
        restZaakafhandelParameters.defaultBehandelaarId?.let { defaultBehandelaarId ->
            restZaakafhandelParameters.defaultGroepId?.let { defaultGroepId ->
                identityService.validateIfUserIsInGroup(defaultBehandelaarId, defaultGroepId)
            }
        }
        return zaakafhandelParametersConverter.toZaakafhandelParameters(
            restZaakafhandelParameters
        ).let { zaakafhandelParameters ->
            val updatedZaakafhandelParameters = zaakafhandelParameters.id?.let {
                zaakafhandelParameterBeheerService.storeZaakafhandelParameters(zaakafhandelParameters).also {
                    zaakafhandelParameterService.cacheRemoveZaakafhandelParameters(zaakafhandelParameters.zaakTypeUUID)
                    zaakafhandelParameterService.clearListCache()
                }
            } ?: zaakafhandelParameterBeheerService.storeZaakafhandelParameters(zaakafhandelParameters)
            zaakafhandelParametersConverter.toRestZaakafhandelParameters(
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
    fun listZaakbeeindigRedenen(): List<RESTZaakbeeindigReden> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return RESTZaakbeeindigRedenConverter.convertZaakbeeindigRedenen(
            zaakafhandelParameterBeheerService.listZaakbeeindigRedenen()
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
        @PathParam("zaaktypeUUID") zaaktypeUUID: UUID?
    ): List<RESTZaakbeeindigReden> =
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
        referenceTableService.readReferenceTable(ReferenceTable.Systeem.AFZENDER.name).let { referenceTable ->
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
    ): List<String> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return smartDocumentsTemplatesService.listGroupTemplateNames(group.path)
    }

    @PUT
    @Path("smartdocuments-template-group")
    fun getSmartDocumentsGroup(
        group: RestSmartDocumentsPath
    ): RestSmartDocumentsTemplateGroup {
        assertPolicy(policyService.readOverigeRechten().beheren)
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
            RESTZaakbeeindigReden().apply {
                id = INADMISSIBLE_TERMINATION_ID
                naam = INADMISSIBLE_TERMINATION_REASON
            }
        )

    private fun readManagedZaakTerminationReasons(zaaktypeUUID: UUID?): List<RESTZaakbeeindigReden> =
        zaakafhandelParameterService.readZaakafhandelParameters(zaaktypeUUID).zaakbeeindigParameters
            .map { it.zaakbeeindigReden }
            .let { RESTZaakbeeindigRedenConverter.convertZaakbeeindigRedenen(it) }

    private fun checkIfProductaanvraagtypeIsNotAlreadyInUse(
        productaanvraagtype: String,
        zaaktypeOmschrijving: String
    ) {
        val activeZaakafhandelparametersForProductaanvraagtype = zaakafhandelParameterBeheerService
            .findActiveZaakafhandelparametersByProductaanvraagtype(productaanvraagtype)
        if (activeZaakafhandelparametersForProductaanvraagtype.size > 1) {
            LOG.warning(
                "Productaanvraagtype '$productaanvraagtype' is already in use by multiple active zaaktypes: '" +
                    activeZaakafhandelparametersForProductaanvraagtype.joinToString(", ") { it.toString() } + "'. " +
                    "This indicates a configuration error in the zaakafhandelparameters. " +
                    "There should be at most only one active zaakafhandelparameters for each productaanvraagtype."
            )
            throw InputValidationFailedException(ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE)
        }
        if (activeZaakafhandelparametersForProductaanvraagtype.size == 1 &&
            activeZaakafhandelparametersForProductaanvraagtype.first().zaaktypeOmschrijving != zaaktypeOmschrijving
        ) {
            LOG.info(
                "Productaanvraagtype '$productaanvraagtype' is already in use by another active zaaktype " +
                    "with zaaktype omschrijving: '${activeZaakafhandelparametersForProductaanvraagtype.first().zaaktypeOmschrijving}' " +
                    "and zaaktype UUID: '${activeZaakafhandelparametersForProductaanvraagtype.first().zaakTypeUUID}'. " +
                    "Please use a unique productaanvraagtype per active zaakafhandelparameters."
            )
            throw InputValidationFailedException(ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE)
        }
    }
}
