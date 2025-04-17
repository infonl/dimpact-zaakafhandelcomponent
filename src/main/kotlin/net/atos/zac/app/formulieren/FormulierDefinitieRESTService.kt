/*
 * SPDX-FileCopyrightText: 2023 - 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.formulieren

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.app.formulieren.converter.RESTFormulierDefinitieConverter
import net.atos.zac.app.formulieren.model.RESTFormulierDefinitie
import net.atos.zac.formulieren.FormulierDefinitieService
import net.atos.zac.formulieren.model.FormulierDefinitie
import net.atos.zac.policy.PolicyService

@Singleton
@Path("formulierdefinities")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class FormulierDefinitieRESTService {
    @Inject
    private val service: FormulierDefinitieService? = null

    @Inject
    private val converter: RESTFormulierDefinitieConverter? = null

    @Inject
    private val policyService: PolicyService? = null

    @GET
    fun listFormDefinitions(): MutableList<RESTFormulierDefinitie?> {
        PolicyService.assertPolicy(policyService!!.readOverigeRechten().beheren)
        return service!!.listFormulierDefinities().stream()
            .map<RESTFormulierDefinitie?> { formulierDefinitie: FormulierDefinitie? ->
                converter!!.convert(
                    formulierDefinitie,
                    false
                )
            }
            .toList()
    }

    @POST
    fun createFormDefinition(restFormulierDefinitie: RESTFormulierDefinitie?): RESTFormulierDefinitie? {
        PolicyService.assertPolicy(policyService!!.readOverigeRechten().beheren)
        return converter!!.convert(
            service!!.createFormulierDefinitie(converter.convert(restFormulierDefinitie)), true
        )
    }

    @GET
    @Path("{id}")
    fun readFormDefinition(@PathParam("id") id: Long): RESTFormulierDefinitie? {
        PolicyService.assertPolicy(policyService!!.readOverigeRechten().beheren)
        return converter!!.convert(
            service!!.readFormulierDefinitie(id), true
        )
    }

    @GET
    @Path("runtime/{systeemnaam}")
    fun findFormDefinition(@PathParam("systeemnaam") systeemnaam: String?): RESTFormulierDefinitie? {
        PolicyService.assertPolicy(policyService!!.readOverigeRechten().beheren)
        return converter!!.convert(service!!.readFormulierDefinitie(systeemnaam), true)
    }

    @PUT
    fun updateFormDefinition(restFormulierDefinitie: RESTFormulierDefinitie?): RESTFormulierDefinitie? {
        PolicyService.assertPolicy(policyService!!.readOverigeRechten().beheren)
        return converter!!.convert(
            service!!.updateFormulierDefinitie(converter.convert(restFormulierDefinitie)), true
        )
    }

    @DELETE
    @Path("{id}")
    fun deleteFormDefinition(@PathParam("id") id: Long) {
        PolicyService.assertPolicy(policyService!!.readOverigeRechten().beheren)
        service!!.deleteFormulierDefinitie(id)
    }
}
