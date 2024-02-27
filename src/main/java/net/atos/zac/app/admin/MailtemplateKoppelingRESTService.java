/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin;

import static net.atos.zac.policy.PolicyService.assertPolicy;

import java.util.List;

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

import net.atos.zac.app.admin.converter.RESTMailtemplateKoppelingConverter;
import net.atos.zac.app.admin.converter.RESTZaakafhandelParametersConverter;
import net.atos.zac.app.admin.model.RESTMailtemplateKoppeling;
import net.atos.zac.policy.PolicyService;
import net.atos.zac.zaaksturing.MailTemplateKoppelingenService;
import net.atos.zac.zaaksturing.model.MailtemplateKoppeling;

@Singleton
@Path("beheer/mailtemplatekoppeling")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MailtemplateKoppelingRESTService {

    @Inject
    private MailTemplateKoppelingenService mailTemplateKoppelingenService;

    @Inject
    private RESTMailtemplateKoppelingConverter restMailtemplateKoppelingConverter;

    @Inject
    private RESTZaakafhandelParametersConverter restZaakafhandelParametersConverter;

    @Inject
    private PolicyService policyService;

    @GET
    @Path("{id}")
    public RESTMailtemplateKoppeling readMailtemplateKoppeling(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return restMailtemplateKoppelingConverter.convert(mailTemplateKoppelingenService.readMailtemplateKoppeling(id));
    }

    @DELETE
    @Path("{id}")
    public void deleteMailtemplateKoppeling(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        mailTemplateKoppelingenService.delete(id);
    }

    @GET
    public List<RESTMailtemplateKoppeling> listMailtemplateKoppelingen() {
        assertPolicy(policyService.readOverigeRechten().beheren());
        final List<MailtemplateKoppeling> mailtemplateKoppelingList = mailTemplateKoppelingenService.listMailtemplateKoppelingen();
        return mailtemplateKoppelingList.stream().map(mailtemplateKoppeling -> {
            final RESTMailtemplateKoppeling restMailtemplateKoppeling = restMailtemplateKoppelingConverter.convert(mailtemplateKoppeling);
            restMailtemplateKoppeling.zaakafhandelParameters = restZaakafhandelParametersConverter
                    .convertZaakafhandelParameters(mailtemplateKoppeling.getZaakafhandelParameters(), false);
            return restMailtemplateKoppeling;
        }).toList();
    }

    @PUT
    @Path("")
    public RESTMailtemplateKoppeling storeMailtemplateKoppeling(
            final RESTMailtemplateKoppeling mailtemplateKoppeling
    ) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return restMailtemplateKoppelingConverter.convert(
                mailTemplateKoppelingenService.storeMailtemplateKoppeling(
                        restMailtemplateKoppelingConverter.convert(mailtemplateKoppeling)));
    }
}
