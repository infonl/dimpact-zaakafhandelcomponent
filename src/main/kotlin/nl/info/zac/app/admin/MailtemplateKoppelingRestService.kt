/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.app.admin.model.RESTMailtemplateKoppeling
import nl.info.zac.admin.MailTemplateKoppelingenService
import nl.info.zac.app.admin.converter.RestZaaktypeConfigurationConverter
import nl.info.zac.app.admin.converter.toRestMailtemplateKoppeling
import nl.info.zac.app.admin.converter.toZaaktypeMailtemplateParameters
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.NoArgConstructor

@Singleton
@Path("beheer/mailtemplatekoppeling")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
class MailtemplateKoppelingRestService @Inject constructor(
    private val mailTemplateKoppelingenService: MailTemplateKoppelingenService,
    private val restZaaktypeConfigurationConverter: RestZaaktypeConfigurationConverter,
    private val policyService: PolicyService
) {
    @GET
    @Path("{id}")
    fun readMailtemplateKoppeling(@PathParam("id") id: Long): RESTMailtemplateKoppeling {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return mailTemplateKoppelingenService.readMailtemplateKoppeling(id).toRestMailtemplateKoppeling()
    }

    @DELETE
    @Path("{id}")
    fun deleteMailtemplateKoppeling(@PathParam("id") id: Long) {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        mailTemplateKoppelingenService.delete(id)
    }

    @GET
    fun listMailtemplateKoppelingen(): List<RESTMailtemplateKoppeling> {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return mailTemplateKoppelingenService.listMailtemplateKoppelingen().map { zaaktypeMailtemplateParameters ->
            zaaktypeMailtemplateParameters.toRestMailtemplateKoppeling().apply {
                zaakafhandelParameters = restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(
                    zaaktypeMailtemplateParameters.zaaktypeConfiguration,
                    false
                )
            }
        }
    }

    @PUT
    @Path("")
    fun storeMailtemplateKoppeling(mailtemplateKoppeling: RESTMailtemplateKoppeling): RESTMailtemplateKoppeling {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        return mailTemplateKoppelingenService.storeMailtemplateKoppeling(
            mailtemplateKoppeling.toZaaktypeMailtemplateParameters()
        ).toRestMailtemplateKoppeling()
    }
}
