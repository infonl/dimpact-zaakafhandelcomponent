/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin;


import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.zac.app.admin.converter.RESTMailtemplateConverter;
import net.atos.zac.app.admin.model.RESTMailtemplate;
import net.atos.zac.mailtemplates.MailTemplateService;
import net.atos.zac.mailtemplates.model.Mail;
import net.atos.zac.mailtemplates.model.MailTemplate;
import net.atos.zac.mailtemplates.model.MailTemplateVariabelen;
import net.atos.zac.policy.PolicyService;

import static net.atos.zac.policy.PolicyServiceKt.assertPolicy;

@Singleton
@Path("beheer/mailtemplates")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MailtemplateBeheerRESTService {

    @Inject
    private MailTemplateService mailTemplateService;

    @Inject
    private PolicyService policyService;

    @GET
    @Path("{id}")
    public RESTMailtemplate readMailtemplate(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        return RESTMailtemplateConverter.convert(mailTemplateService.readMailtemplate(id));
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
    public void deleteMailtemplate(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        mailTemplateService.delete(id);
    }

    @PUT
    @Path("")
    public RESTMailtemplate persistMailtemplate(final RESTMailtemplate mailtemplate) {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        return RESTMailtemplateConverter.convert(
                mailTemplateService.storeMailtemplate(RESTMailtemplateConverter.convert(mailtemplate))
        );
    }

    @GET
    @Path("variabelen/{mail}")
    public Set<MailTemplateVariabelen> ophalenVariabelenVoorMail(@PathParam("mail") final Mail mail) {
        return mail.getVariabelen();
    }
}
