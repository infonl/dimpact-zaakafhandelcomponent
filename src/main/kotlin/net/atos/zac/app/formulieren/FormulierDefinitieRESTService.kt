/*
 * SPDX-FileCopyrightText: 2023 - 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.formulieren

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.app.formulieren.converter.toFormulierDefinitie
import net.atos.zac.app.formulieren.converter.toRESTFormulierDefinitie
import net.atos.zac.app.formulieren.model.RESTFormulierDefinitie
import net.atos.zac.formulieren.FormulierDefinitieService
import net.atos.zac.policy.PolicyService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@Singleton
@Path("formulierdefinities")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class FormulierDefinitieRESTService @Inject constructor(
    private val service: FormulierDefinitieService,
    private val policyService: PolicyService
) {

    @GET
    fun listFormDefinitions() =
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren).run {
            service.listFormulierDefinities().map { it.toRESTFormulierDefinitie(false) }
        }

    @POST
    fun createFormDefinition(@Valid restFormulierDefinitie: RESTFormulierDefinitie) =
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren).run {
            service.createFormulierDefinitie(
                restFormulierDefinitie.toFormulierDefinitie()
            ).toRESTFormulierDefinitie(true)
        }

    @GET
    @Path("{id}")
    fun readFormDefinition(@PathParam("id") id: Long) =
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren).run {
            service.readFormulierDefinitie(id).toRESTFormulierDefinitie(true)
        }

    @GET
    @Path("runtime/{systeemnaam}")
    fun findFormDefinition(@PathParam("systeemnaam") systeemnaam: String) =
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren).run {
            service.readFormulierDefinitie(systeemnaam).toRESTFormulierDefinitie(true)
        }

    @PUT
    fun updateFormDefinition(@Valid restFormulierDefinitie: RESTFormulierDefinitie) =
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren).run {
            service.updateFormulierDefinitie(
                restFormulierDefinitie.toFormulierDefinitie()
            ).toRESTFormulierDefinitie(true)
        }

    @DELETE
    @Path("{id}")
    fun deleteFormDefinition(@PathParam("id") id: Long) {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        service.deleteFormulierDefinitie(id)
    }
}
