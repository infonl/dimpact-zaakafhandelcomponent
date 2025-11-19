/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.atos.zac.app.admin.converter.RESTMailtemplateConverter
import net.atos.zac.app.admin.model.RESTMailtemplate
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.MailTemplateVariables
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
@Singleton
@Path("beheer/mailtemplates")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class MailtemplateBeheerRestService @Inject constructor(
    private val mailTemplateService: MailTemplateService,
    private val policyService: PolicyService
) {

    @GET
    @Path("{id}")
    fun readMailtemplate(@PathParam("id") @Positive id: Long): RESTMailtemplate {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return RESTMailtemplateConverter.convert(mailTemplateService.readMailtemplate(id))
    }

    @GET
    fun listMailtemplates(): List<RESTMailtemplate> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        val mailTemplates = mailTemplateService.listMailtemplates()
        return mailTemplates.map { RESTMailtemplateConverter.convert(it) }
    }

    @GET
    @Path("/koppelbaar")
    fun listkoppelbareMailtemplates(): List<RESTMailtemplate> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        val mailTemplates = mailTemplateService.listKoppelbareMailtemplates()
        return mailTemplates.map { RESTMailtemplateConverter.convert(it) }
    }

    @DELETE
    @Path("{id}")
    fun deleteMailtemplate(@PathParam("id") @Positive id: Long) {
        assertPolicy(policyService.readOverigeRechten().beheren)
        mailTemplateService.delete(id)
    }

    @POST
    @Path("")
    fun createMailtemplate(@Valid mailtemplate: RESTMailtemplate): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        if (mailtemplate.id != null) {
            mailtemplate.id = null // Ignore provided ID
        }
        val createdTemplate = mailTemplateService.createMailtemplate(
            RESTMailtemplateConverter.convertForCreate(mailtemplate)
        )
        val response = RESTMailtemplateConverter.convert(createdTemplate)
        return Response.status(Response.Status.CREATED).entity(response).build()
    }

    @PUT
    @Path("{id}")
    fun updateMailtemplate(
        @PathParam("id") @Positive id: Long,
        @Valid mailtemplate: RESTMailtemplate
    ): RESTMailtemplate {
        assertPolicy(policyService.readOverigeRechten().beheren)
        val updatedTemplate = mailTemplateService.updateMailtemplate(
            id, RESTMailtemplateConverter.convertForUpdate(mailtemplate)
        )
        return RESTMailtemplateConverter.convert(updatedTemplate)
    }

    @GET
    @Path("variabelen/{mail}")
    fun getMailTemplateVariables(@PathParam("mail") mail: Mail): Set<MailTemplateVariables> {
        return mail.mailTemplateVariables
    }
}
