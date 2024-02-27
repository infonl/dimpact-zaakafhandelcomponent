/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin;

import static net.atos.zac.policy.PolicyService.assertPolicy;
import static net.atos.zac.zaaksturing.model.ReferentieTabel.Systeem.AFZENDER;
import static net.atos.zac.zaaksturing.model.ReferentieTabel.Systeem.DOMEIN;

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

import net.atos.zac.app.admin.converter.RESTReferentieTabelConverter;
import net.atos.zac.app.admin.converter.RESTReferentieWaardeConverter;
import net.atos.zac.app.admin.model.RESTReferentieTabel;
import net.atos.zac.policy.PolicyService;
import net.atos.zac.zaaksturing.ReferentieTabelBeheerService;
import net.atos.zac.zaaksturing.ReferentieTabelService;
import net.atos.zac.zaaksturing.model.ReferentieTabel;

@Singleton
@Path("referentietabellen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReferentieTabelRESTService {
    @Inject
    private ReferentieTabelService referentieTabelService;

    @Inject
    private ReferentieTabelBeheerService referentieTabelBeheerService;

    @Inject
    private RESTReferentieTabelConverter restReferentieTabelConverter;

    @Inject
    private RESTReferentieWaardeConverter restReferentieWaardeConverter;

    @Inject
    private PolicyService policyService;

    @GET
    public List<RESTReferentieTabel> listReferentieTabellen() {
        assertPolicy(policyService.readOverigeRechten().beheren());
        final List<ReferentieTabel> referentieTabellen = referentieTabelService.listReferentieTabellen();
        return referentieTabellen.stream()
                .map(referentieTabel -> restReferentieTabelConverter.convert(referentieTabel, false))
                .toList();
    }

    @GET
    @Path("new")
    public RESTReferentieTabel newReferentieTabel() {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return restReferentieTabelConverter.convert(
                referentieTabelBeheerService.newReferentieTabel(), true);
    }

    @POST
    public RESTReferentieTabel createReferentieTabel(final RESTReferentieTabel referentieTabel) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return restReferentieTabelConverter.convert(
                referentieTabelBeheerService.createReferentieTabel(
                        restReferentieTabelConverter.convert(referentieTabel)), true);
    }

    @GET
    @Path("{id}")
    public RESTReferentieTabel readReferentieTabel(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return restReferentieTabelConverter.convert(
                referentieTabelService.readReferentieTabel(id), true);
    }

    @PUT
    @Path("{id}")
    public RESTReferentieTabel updateReferentieTabel(@PathParam("id") final long id,
            final RESTReferentieTabel referentieTabel) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return restReferentieTabelConverter.convert(
                referentieTabelBeheerService.updateReferentieTabel(
                        restReferentieTabelConverter.convert(referentieTabel,
                                referentieTabelService.readReferentieTabel(id))), true);
    }

    @DELETE
    @Path("{id}")
    public void deleteReferentieTabel(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        referentieTabelBeheerService.deleteReferentieTabel(id);
    }

    @GET
    @Path("afzender")
    public List<String> listAfzenders() {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return restReferentieWaardeConverter.convert(
                referentieTabelService.readReferentieTabel(AFZENDER.name()).getWaarden());
    }

    @GET
    @Path("domein")
    public List<String> listDomeinen() {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return restReferentieWaardeConverter.convert(
                referentieTabelService.readReferentieTabel(DOMEIN.name()).getWaarden());
    }
}
