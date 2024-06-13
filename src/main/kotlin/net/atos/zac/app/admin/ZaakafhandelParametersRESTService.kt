/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.app.admin.converter.RESTCaseDefinitionConverter
import net.atos.zac.app.admin.converter.RESTReplyToConverter
import net.atos.zac.app.admin.converter.RESTZaakafhandelParametersConverter
import net.atos.zac.app.admin.converter.RESTZaakbeeindigRedenConverter
import net.atos.zac.app.admin.model.RESTCaseDefinition
import net.atos.zac.app.admin.model.RESTReplyTo
import net.atos.zac.app.admin.model.RESTTaakFormulierDefinitie
import net.atos.zac.app.admin.model.RESTTaakFormulierVeldDefinitie
import net.atos.zac.app.admin.model.RESTZaakafhandelParameters
import net.atos.zac.app.admin.model.RESTZaakbeeindigReden
import net.atos.zac.app.zaken.converter.RESTResultaattypeConverter
import net.atos.zac.app.zaken.model.RESTResultaattype
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.CMMNService
import net.atos.zac.policy.PolicyService
import net.atos.zac.util.UriUtil
import net.atos.zac.zaaksturing.ReferentieTabelService
import net.atos.zac.zaaksturing.ZaakafhandelParameterBeheerService
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zaaksturing.model.FormulierDefinitie
import net.atos.zac.zaaksturing.model.ReferentieTabel.Systeem
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.flowable.cmmn.api.repository.CaseDefinition
import java.util.UUID

@Singleton
@Path("zaakafhandelParameters")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
@Suppress("LongParameterList", "TooManyFunctions")
class ZaakafhandelParametersRESTService @Inject constructor(
    private val ztcClientService: ZTCClientService,
    private val configuratieService: ConfiguratieService,
    private val cmmnService: CMMNService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val zaakafhandelParameterBeheerService: ZaakafhandelParameterBeheerService,
    private val referentieTabelService: ReferentieTabelService,
    private val zaakafhandelParametersConverter: RESTZaakafhandelParametersConverter,
    private val caseDefinitionConverter: RESTCaseDefinitionConverter,
    private val resultaattypeConverter: RESTResultaattypeConverter,
    private val zaakbeeindigRedenConverter: RESTZaakbeeindigRedenConverter,
    private val restReplyToConverter: RESTReplyToConverter,
    private val policyService: PolicyService
) {

    /**
     * Retrieve all CASE_DEFINITIONs that can be linked to a ZAAKTYPE
     *
     * @return LIST of CASE_DEFINITIONs
     */
    @GET
    @Path("caseDefinition")
    fun listCaseDefinitions(): List<RESTCaseDefinition> {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return cmmnService.listCaseDefinitions()
            .map { caseDefinition: CaseDefinition? ->
                caseDefinitionConverter.convertToRESTCaseDefinition(
                    caseDefinition,
                    true
                )
            }
    }

    /**
     * Retrieving a CASE_DEFINITION including its PLAN_ITEM_DEFINITIONs
     *
     * @param caseDefinitionKey id of the CASE_DEFINITION
     * @return CASE_DEFINITION including its PLAN_ITEM_DEFINITIONs
     */
    @GET
    @Path("caseDefinition/{key}")
    fun readCaseDefinition(@PathParam("key") caseDefinitionKey: String?): RESTCaseDefinition {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return caseDefinitionConverter.convertToRESTCaseDefinition(caseDefinitionKey, true)
    }

    /**
     * Retrieve all ZAAKAFHANDELPARAMETERS for overview
     *
     * @return LIST of ZAAKAFHANDELPARAMETERS
     */
    @GET
    fun listZaakafhandelParameters(): List<RESTZaakafhandelParameters> {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return listZaaktypes()
            .map { zaaktype: ZaakType -> UriUtil.uuidFromURI(zaaktype.url) }
            .map { zaaktypeUUID: UUID? -> zaakafhandelParameterService.readZaakafhandelParameters(zaaktypeUUID) }
            .map { zaakafhandelParameters: ZaakafhandelParameters? ->
                zaakafhandelParametersConverter.convertZaakafhandelParameters(
                    zaakafhandelParameters,
                    false
                )
            }
    }

    /**
     * Retrieve the ZAAKAFHANDELPARAMETERS for a ZAAKTYPE
     *
     * @return ZAAKAFHANDELPARAMETERS for a ZAAKTYPE by uuid of the ZAAKTYPE
     */
    @GET
    @Path("{zaaktypeUUID}")
    fun readZaakafhandelParameters(@PathParam("zaaktypeUUID") zaakTypeUUID: UUID?): RESTZaakafhandelParameters {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID).let {
            zaakafhandelParametersConverter.convertZaakafhandelParameters(it, true)
        }
    }

    /**
     * Saves the ZAAKAFHANDELPARAMETERS
     *
     * @param restZaakafhandelParameters ZAAKAFHANDELPARAMETERS
     */
    @PUT
    fun updateZaakafhandelparameters(
        restZaakafhandelParameters: RESTZaakafhandelParameters?
    ): RESTZaakafhandelParameters {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        var zaakafhandelParameters = zaakafhandelParametersConverter.convertRESTZaakafhandelParameters(
            restZaakafhandelParameters
        )
        zaakafhandelParameters = if (zaakafhandelParameters.id == null) {
            zaakafhandelParameterBeheerService.createZaakafhandelParameters(zaakafhandelParameters)
        } else {
            zaakafhandelParameterBeheerService.updateZaakafhandelParameters(zaakafhandelParameters).also {
                zaakafhandelParameterService.cacheRemoveZaakafhandelParameters(zaakafhandelParameters.zaakTypeUUID)
            }
        }
        return zaakafhandelParametersConverter.convertZaakafhandelParameters(zaakafhandelParameters, true)
    }

    /**
     * Retrieve all possible zaakbeeindig-redenen
     *
     * @return list of zaakbeeindig-redenen
     */
    @GET
    @Path("zaakbeeindigRedenen")
    fun listZaakbeeindigRedenen(): List<RESTZaakbeeindigReden> {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return zaakbeeindigRedenConverter.convertZaakbeeindigRedenen(
            zaakafhandelParameterBeheerService.listZaakbeeindigRedenen()
        )
    }

    /**
     * Retrieve zaakbeeindig redenen for a zaaktype
     *
     * @return list of zaakbeeindig-redenen
     */
    @GET
    @Path("zaakbeeindigRedenen/{zaaktypeUUID}")
    fun listZaakbeeindigRedenenForZaaktype(
        @PathParam("zaaktypeUUID") zaaktypeUUID: UUID?
    ): List<RESTZaakbeeindigReden> =
        zaakafhandelParameterService.readZaakafhandelParameters(zaaktypeUUID).zaakbeeindigParameters
            .map { it.zaakbeeindigReden }
            .let {
                zaakbeeindigRedenConverter.convertZaakbeeindigRedenen(it)
            }

    /**
     * Retrieve all resultaattypes for a zaaktype
     *
     * @param zaaktypeUUID the id of the zaaktype
     * @return list of resultaattypes
     */
    @GET
    @Path("resultaattypes/{zaaktypeUUID}")
    fun listResultaattypes(@PathParam("zaaktypeUUID") zaaktypeUUID: UUID?): List<RESTResultaattype> {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return resultaattypeConverter.convertResultaattypes(
            ztcClientService.readResultaattypen(ztcClientService.readZaaktype(zaaktypeUUID!!).url)
        )
    }

    private fun listZaaktypes(): List<ZaakType> =
        ztcClientService.listZaaktypen(configuratieService.readDefaultCatalogusURI())

    /**
     * Retrieve all FORMULIER_DEFINITIEs that can be linked to a HUMAN_TASK_PLAN_ITEM
     *
     * @return lijst of FORMULIER_DEFINITIEs
     */
    @GET
    @Path("formulierDefinities")
    fun listFormulierDefinities(): List<RESTTaakFormulierDefinitie> =
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
     * Retrieve all possible replytos
     *
     * @return sorted list of replytos
     */
    @GET
    @Path("replyTo")
    fun listReplyTos(): List<RESTReplyTo> =
        restReplyToConverter.convertReplyTos(
            referentieTabelService.readReferentieTabel(Systeem.AFZENDER.name).waarden
        )
}
