/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin;


import static nl.info.zac.policy.PolicyServiceKt.assertPolicy;

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

import net.atos.zac.admin.MailTemplateKoppelingenService;
import net.atos.zac.app.admin.converter.RESTMailtemplateKoppelingConverter;
import net.atos.zac.app.admin.model.RESTMailtemplateKoppeling;
import nl.info.zac.admin.model.ZaaktypeCmmnMailtemplateParameters;
import nl.info.zac.app.admin.converter.RestZaakafhandelParametersConverter;
import nl.info.zac.policy.PolicyService;

@Singleton
@Path("beheer/mailtemplatekoppeling")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MailtemplateKoppelingRESTService {
    private MailTemplateKoppelingenService mailTemplateKoppelingenService;
    private RestZaakafhandelParametersConverter restZaakafhandelParametersConverter;
    private PolicyService policyService;

    /**
     * No-args constructor for CDI.
     */
    public MailtemplateKoppelingRESTService() {
    }

    @Inject
    public MailtemplateKoppelingRESTService(
            final MailTemplateKoppelingenService mailTemplateKoppelingenService,
            final RestZaakafhandelParametersConverter restZaakafhandelParametersConverter,
            final PolicyService policyService
    ) {
        this.mailTemplateKoppelingenService = mailTemplateKoppelingenService;
        this.restZaakafhandelParametersConverter = restZaakafhandelParametersConverter;
        this.policyService = policyService;
    }

    @GET
    @Path("{id}")
    public RESTMailtemplateKoppeling readMailtemplateKoppeling(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten(null).getBeheren());
        return RESTMailtemplateKoppelingConverter.convert(mailTemplateKoppelingenService.readMailtemplateKoppeling(id));
    }

    @DELETE
    @Path("{id}")
    public void deleteMailtemplateKoppeling(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten(null).getBeheren());
        mailTemplateKoppelingenService.delete(id);
    }

    @GET
    public List<RESTMailtemplateKoppeling> listMailtemplateKoppelingen() {
        assertPolicy(policyService.readOverigeRechten(null).getBeheren());
        final List<ZaaktypeCmmnMailtemplateParameters> zaaktypeCmmnMailtemplateParametersList = mailTemplateKoppelingenService
                .listMailtemplateKoppelingen();
        return zaaktypeCmmnMailtemplateParametersList.stream().map(zaaktypeCmmnMailtemplateParameters -> {
            final RESTMailtemplateKoppeling restMailtemplateKoppeling = RESTMailtemplateKoppelingConverter.convert(
                    zaaktypeCmmnMailtemplateParameters);
            restMailtemplateKoppeling.zaakafhandelParameters = restZaakafhandelParametersConverter
                    .toRestZaaktypeCmmnConfiguration(zaaktypeCmmnMailtemplateParameters.getZaaktypeCmmnConfiguration(), false);
            return restMailtemplateKoppeling;
        }).toList();
    }

    @PUT
    @Path("")
    public RESTMailtemplateKoppeling storeMailtemplateKoppeling(
            final RESTMailtemplateKoppeling mailtemplateKoppeling
    ) {
        assertPolicy(policyService.readOverigeRechten(null).getBeheren());
        return RESTMailtemplateKoppelingConverter.convert(
                mailTemplateKoppelingenService.storeMailtemplateKoppeling(
                        RESTMailtemplateKoppelingConverter.convert(mailtemplateKoppeling)
                )
        );
    }
}
