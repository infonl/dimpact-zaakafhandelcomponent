/*
 * SPDX-FileCopyrightText: 2021 Atos
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
import net.atos.zac.zaaksturing.model.FormulierVeldDefinitie
import net.atos.zac.zaaksturing.model.ReferentieTabel.Systeem
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters
import net.atos.zac.zaaksturing.model.ZaakbeeindigParameter
import org.flowable.cmmn.api.repository.CaseDefinition
import java.util.Arrays
import java.util.UUID
import java.util.stream.Collectors

@Singleton
@Path("zaakafhandelParameters")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class ZaakafhandelParametersRESTService {
    @Inject
    private val ztcClientService: ZTCClientService? = null

    @Inject
    private val configuratieService: ConfiguratieService? = null

    @Inject
    private val cmmnService: CMMNService? = null

    @Inject
    private val zaakafhandelParameterService: ZaakafhandelParameterService? = null

    @Inject
    private val zaakafhandelParameterBeheerService: ZaakafhandelParameterBeheerService? = null

    @Inject
    private val referentieTabelService: ReferentieTabelService? = null

    @Inject
    private val zaakafhandelParametersConverter: RESTZaakafhandelParametersConverter? = null

    @Inject
    private val caseDefinitionConverter: RESTCaseDefinitionConverter? = null

    @Inject
    private val resultaattypeConverter: RESTResultaattypeConverter? = null

    @Inject
    private val zaakbeeindigRedenConverter: RESTZaakbeeindigRedenConverter? = null

    @Inject
    private val restReplyToConverter: RESTReplyToConverter? = null

    @Inject
    private val policyService: PolicyService? = null

    /**
     * Retrieve all CASE_DEFINITIONs that can be linked to a ZAAKTYPE
     *
     * @return LIST of CASE_DEFINITIONs
     */
    @GET
    @Path("caseDefinition")
    fun listCaseDefinitions(): List<RESTCaseDefinition> {
        PolicyService.assertPolicy(policyService!!.readOverigeRechten().beheren)
        val caseDefinitions = cmmnService!!.listCaseDefinitions()
        return caseDefinitions.stream()
            .map { caseDefinition: CaseDefinition? ->
                caseDefinitionConverter!!.convertToRESTCaseDefinition(
                    caseDefinition,
                    true
                )
            }
            .toList()
    }

    /**
     * Retrieving a CASE_DEFINITION including it's PLAN_ITEM_DEFINITIONs
     *
     * @param caseDefinitionKey id of the CASE_DEFINITION
     * @return CASE_DEFINITION including it's PLAN_ITEM_DEFINITIONs
     */
    @GET
    @Path("caseDefinition/{key}")
    fun readCaseDefinition(@PathParam("key") caseDefinitionKey: String?): RESTCaseDefinition {
        PolicyService.assertPolicy(policyService!!.readOverigeRechten().beheren)
        return caseDefinitionConverter!!.convertToRESTCaseDefinition(caseDefinitionKey, true)
    }

    /**
     * Retrieve all ZAAKAFHANDELPARAMETERS for overview
     *
     * @return LIST of ZAAKAFHANDELPARAMETERS
     */
    @GET
    fun listZaakafhandelParameters(): List<RESTZaakafhandelParameters> {
        PolicyService.assertPolicy(policyService!!.readOverigeRechten().beheren)
        return listZaaktypes().stream()
            .map { zaaktype: ZaakType -> UriUtil.uuidFromURI(zaaktype.url) }
            .map { zaaktypeUUID: UUID? -> zaakafhandelParameterService!!.readZaakafhandelParameters(zaaktypeUUID) }
            .map { zaakafhandelParameters: ZaakafhandelParameters? ->
                zaakafhandelParametersConverter!!.convertZaakafhandelParameters(
                    zaakafhandelParameters,
                    false
                )
            }
            .toList()
    }

    /**
     * Retrieve the ZAAKAFHANDELPARAMETERS for a ZAAKTYPE
     *
     * @return ZAAKAFHANDELPARAMETERS for a ZAAKTYPE by uuid of the ZAAKTYPE
     */
    @GET
    @Path("{zaaktypeUUID}")
    fun readZaakafhandelParameters(@PathParam("zaaktypeUUID") zaakTypeUUID: UUID?): RESTZaakafhandelParameters {
        PolicyService.assertPolicy(policyService!!.readOverigeRechten().beheren)
        val zaakafhandelParameters = zaakafhandelParameterService!!.readZaakafhandelParameters(zaakTypeUUID)
        return zaakafhandelParametersConverter!!.convertZaakafhandelParameters(zaakafhandelParameters, true)
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
        PolicyService.assertPolicy(policyService!!.readOverigeRechten().beheren)
        var zaakafhandelParameters = zaakafhandelParametersConverter!!.convertRESTZaakafhandelParameters(
            restZaakafhandelParameters
        )
        if (zaakafhandelParameters.id == null) {
            zaakafhandelParameters = zaakafhandelParameterBeheerService!!.createZaakafhandelParameters(
                zaakafhandelParameters
            )
        } else {
            zaakafhandelParameters = zaakafhandelParameterBeheerService!!.updateZaakafhandelParameters(
                zaakafhandelParameters
            )
            zaakafhandelParameterService!!.cacheRemoveZaakafhandelParameters(zaakafhandelParameters.zaakTypeUUID)
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
        PolicyService.assertPolicy(policyService!!.readOverigeRechten().beheren)
        return zaakbeeindigRedenConverter!!.convertZaakbeeindigRedenen(
            zaakafhandelParameterBeheerService!!.listZaakbeeindigRedenen()
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
    ): List<RESTZaakbeeindigReden> {
        val zaakbeeindigRedenen = zaakafhandelParameterService!!.readZaakafhandelParameters(
            zaaktypeUUID
        )
            .zaakbeeindigParameters.stream()
            .map { obj: ZaakbeeindigParameter -> obj.zaakbeeindigReden }
            .toList()
        return zaakbeeindigRedenConverter!!.convertZaakbeeindigRedenen(zaakbeeindigRedenen)
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
        PolicyService.assertPolicy(policyService!!.readOverigeRechten().beheren)
        return resultaattypeConverter!!.convertResultaattypes(
            ztcClientService!!.readResultaattypen(ztcClientService.readZaaktype(zaaktypeUUID!!).url)
        )
    }

    private fun listZaaktypes(): List<ZaakType> {
        return ztcClientService!!.listZaaktypen(configuratieService!!.readDefaultCatalogusURI())
    }

    /**
     * Retrieve all FORMULIER_DEFINITIEs that can be linked to a HUMAN_TASK_PLAN_ITEM
     *
     * @return lijst of FORMULIER_DEFINITIEs
     */
    @GET
    @Path("formulierDefinities")
    fun listFormulierDefinities(): List<RESTTaakFormulierDefinitie> {
        return Arrays.stream(FormulierDefinitie.entries.toTypedArray())
            .map { formulierDefinitie: FormulierDefinitie ->
                RESTTaakFormulierDefinitie(formulierDefinitie.name,
                    formulierDefinitie.veldDefinities
                        .stream()
                        .map { formulierVeldDefinitie: FormulierVeldDefinitie ->
                            RESTTaakFormulierVeldDefinitie(
                                formulierVeldDefinitie.name,
                                formulierVeldDefinitie.defaultTabel
                                    .name
                            )
                        }
                        .collect(Collectors.toList()))
            }
            .collect(Collectors.toList())
    }

    /**
     * Retrieve all possible replytos
     *
     * @return sorted list of replytos
     */
    @GET
    @Path("replyTo")
    fun listReplyTos(): List<RESTReplyTo> {
        return restReplyToConverter!!.convertReplyTos(
            referentieTabelService!!.readReferentieTabel(Systeem.AFZENDER.name).waarden
        )
    }
}
