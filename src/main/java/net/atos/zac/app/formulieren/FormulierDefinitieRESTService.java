/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.formulieren;

import static net.atos.zac.policy.PolicyService.assertPolicy;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.zac.app.formulieren.converter.RESTFormulierDefinitieConverter;
import net.atos.zac.app.formulieren.model.RESTFormulierDefinitie;
import net.atos.zac.formulieren.FormulierDefinitieService;
import net.atos.zac.policy.PolicyService;

@Singleton
@Path("formulierDefinities")
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
    public List<RESTFormulierDefinitie> list() {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        return service.listFormulierDefinities().stream()
                .map(formulierDefinitie -> converter.convert(formulierDefinitie, false, false))
                .toList();
    }

    @POST
    public RESTFormulierDefinitie create(final RESTFormulierDefinitie restFormulierDefinitie) {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        return converter.convert(
                service.createFormulierDefinitie(
                        converter.convert(restFormulierDefinitie)), true, false);
    }

    @GET
    @Path("{id}")
    public RESTFormulierDefinitie read(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        return converter.convert(
                service.readFormulierDefinitie(id), true, false);
    }

    @GET
    @Path("runtime/{systeemnaam}")
    public RESTFormulierDefinitie find(@PathParam("systeemnaam") final String systeemnaam) {
        return converter.convert(service.readFormulierDefinitie(systeemnaam), true, true);
    }

    @PUT
    public RESTFormulierDefinitie update(final RESTFormulierDefinitie restFormulierDefinitie) {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        return converter.convert(
                service.updateFormulierDefinitie(
                        converter.convert(restFormulierDefinitie)), true, false);
    }

    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten().getBeheren());
        service.deleteFormulierDefinitie(id);
    }
}
