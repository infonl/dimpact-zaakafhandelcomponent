/*
 * SPDX-FileCopyrightText: 2023 - 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.formulieren;

import static net.atos.zac.policy.PolicyService.assertPolicy;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import net.atos.zac.app.formulieren.converter.RESTFormulierDefinitieConverter;
import net.atos.zac.app.formulieren.model.RESTFormulierDefinitie;
import net.atos.zac.formulieren.FormulierDefinitieService;
import net.atos.zac.policy.PolicyService;

@Singleton
@Path("formulierdefinities")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FormulierDefinitieRESTService {

    @Inject
    private FormulierDefinitieService service;

    @Inject
    private RESTFormulierDefinitieConverter converter;

    @Inject
    private PolicyService policyService;

    @GET
    public List<RESTFormulierDefinitie> listFormDefinitions() {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return service.listFormulierDefinities().stream()
                .map(formulierDefinitie -> converter.convert(formulierDefinitie, false))
                .toList();
    }

    @POST
    public RESTFormulierDefinitie createFormDefinition(final RESTFormulierDefinitie restFormulierDefinitie) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return converter.convert(
                service.createFormulierDefinitie(converter.convert(restFormulierDefinitie)), true);
    }

    @GET
    @Path("{id}")
    public RESTFormulierDefinitie readFormDefinition(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return converter.convert(
                service.readFormulierDefinitie(id), true);
    }

    @GET
    @Path("runtime/{systeemnaam}")
    public RESTFormulierDefinitie findFormDefinition(@PathParam("systeemnaam") final String systeemnaam) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return converter.convert(service.readFormulierDefinitie(systeemnaam), true);
    }

    @PUT
    public RESTFormulierDefinitie updateFormDefinition(final RESTFormulierDefinitie restFormulierDefinitie) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return converter.convert(
                service.updateFormulierDefinitie(converter.convert(restFormulierDefinitie)), true);
    }

    @DELETE
    @Path("{id}")
    public void deleteFormDefinition(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        service.deleteFormulierDefinitie(id);
    }
}
