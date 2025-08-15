/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin;

import static nl.info.zac.policy.PolicyServiceKt.assertPolicy;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import net.atos.zac.app.admin.converter.RESTMailtemplateConverter;
import net.atos.zac.app.admin.model.RESTMailtemplate;
import nl.info.zac.mailtemplates.MailTemplateService;
import nl.info.zac.mailtemplates.model.Mail;
import nl.info.zac.mailtemplates.model.MailTemplate;
import nl.info.zac.mailtemplates.model.MailTemplateVariables;
import nl.info.zac.policy.PolicyService;

@Singleton
@Path("beheer/mailtemplates")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MailtemplateBeheerRestService {
    private MailTemplateService mailTemplateService;
    private PolicyService policyService;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public MailtemplateBeheerRestService() {
    }

    @Inject
    public MailtemplateBeheerRestService(final MailTemplateService mailTemplateService, final PolicyService policyService) {
        this.mailTemplateService = mailTemplateService;
        this.policyService = policyService;
    }

    @GET
    @Path("{id}")
    public RESTMailtemplate readMailtemplate(@PathParam("id") @Positive final long id) {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        
        return RESTMailtemplateConverter.convert(mailTemplateService.readMailtemplate(id));
        // MailTemplateNotFoundException is already thrown by service and maps to 404
    }

    @GET
    public List<RESTMailtemplate> listMailtemplates() {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        final List<MailTemplate> mailTemplates = mailTemplateService.listMailtemplates();
        return mailTemplates.stream().map(RESTMailtemplateConverter::convert).toList();
    }

    @GET
    @Path("/koppelbaar")
    public List<RESTMailtemplate> listkoppelbareMailtemplates() {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        final List<MailTemplate> mailTemplates = mailTemplateService.listKoppelbareMailtemplates();
        return mailTemplates.stream().map(RESTMailtemplateConverter::convert).toList();
    }

    @DELETE
    @Path("{id}")
    public void deleteMailtemplate(@PathParam("id") @Positive final long id) {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        
        mailTemplateService.delete(id);
    }

    @POST
    @Path("")
    public Response createMailtemplate(@Valid final RESTMailtemplate mailtemplate) {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        
        // Explicitly ignore any provided ID in POST requests (requirement 1.1, 3.5)
        if (mailtemplate.id != null) {
            mailtemplate.id = null; // Ignore provided ID
        }
        
        final MailTemplate createdTemplate = mailTemplateService.createMailtemplate(
                RESTMailtemplateConverter.convertForCreate(mailtemplate)
        );
        final RESTMailtemplate response = RESTMailtemplateConverter.convert(createdTemplate);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("{id}")
    public RESTMailtemplate updateMailtemplate(@PathParam("id") @Positive final long id, @Valid final RESTMailtemplate mailtemplate) {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        
        final MailTemplate updatedTemplate = mailTemplateService.updateMailtemplate(
                id, RESTMailtemplateConverter.convertForUpdate(mailtemplate)
        );
        return RESTMailtemplateConverter.convert(updatedTemplate);
        // MailTemplateNotFoundException is already thrown by service and maps to 404
    }

    @PUT
    @Path("")
    @Deprecated
    public RESTMailtemplate persistMailtemplate(final RESTMailtemplate mailtemplate) {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        return RESTMailtemplateConverter.convert(
                mailTemplateService.storeMailtemplate(RESTMailtemplateConverter.convert(mailtemplate))
        );
    }

    @GET
    @Path("variabelen/{mail}")
    public Set<MailTemplateVariables> getMailTemplateVariables(@PathParam("mail") final Mail mail) {
        return mail.getMailTemplateVariables();
    }


}
