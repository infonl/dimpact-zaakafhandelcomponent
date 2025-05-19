/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.assertPolicy
import nl.info.zac.app.admin.model.RestFormioFormulier
import nl.info.zac.app.admin.model.RestFormioFormulierContent
import nl.info.zac.formio.FormioService
import nl.info.zac.util.NoArgConstructor

@Singleton
@Path("formio-formulieren")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
class FormioFormulierenRestService @Inject constructor(
    private val formioService: FormioService,
    private val policyService: PolicyService
) {
    @GET
    fun listFormulieren(): List<RestFormioFormulier> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return formioService.listFormulieren()
            .map { RestFormioFormulier(it.id, it.name, it.title) }
    }

    @POST
    fun createFormulier(restFormioFormulierContent: RestFormioFormulierContent): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        formioService.addFormulier(restFormioFormulierContent.filename, restFormioFormulierContent.content)
        return Response.created(null).build()
    }

    @DELETE
    @Path("{id}")
    fun deleteFormulier(@PathParam("id") id: Long): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        formioService.deleteFormulier(id)
        return Response.noContent().build()
    }
}
