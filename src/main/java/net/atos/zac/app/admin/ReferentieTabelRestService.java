/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin;

import static net.atos.zac.policy.PolicyService.assertPolicy;
import static net.atos.zac.zaaksturing.model.ReferentieTabel.Systeem.AFZENDER;
import static net.atos.zac.zaaksturing.model.ReferentieTabel.Systeem.COMMUNICATIEKANAAL;
import static net.atos.zac.zaaksturing.model.ReferentieTabel.Systeem.DOMEIN;
import static net.atos.zac.zaaksturing.model.ReferentieTabel.Systeem.SERVER_ERROR_ERROR_PAGINA_TEKST;

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
public class ReferentieTabelRestService {
    private ReferentieTabelService referentieTabelService;
    private ReferentieTabelBeheerService referentieTabelBeheerService;
    private PolicyService policyService;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public ReferentieTabelRestService() {
    }

    @Inject
    public ReferentieTabelRestService(
            final ReferentieTabelService referentieTabelService,
            final ReferentieTabelBeheerService referentieTabelBeheerService,
            final PolicyService policyService
    ) {
        this.referentieTabelService = referentieTabelService;
        this.referentieTabelBeheerService = referentieTabelBeheerService;
        this.policyService = policyService;
    }

    @GET
    public List<RESTReferentieTabel> listReferentieTabellen() {
        assertPolicy(policyService.readOverigeRechten().beheren());
        final List<ReferentieTabel> referentieTabellen = referentieTabelService.listReferentieTabellen();
        return referentieTabellen.stream()
                .map(referentieTabel -> RESTReferentieTabelConverter.convert(referentieTabel, false))
                .toList();
    }

    @GET
    @Path("new")
    public RESTReferentieTabel newReferentieTabel() {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return RESTReferentieTabelConverter.convert(
                referentieTabelBeheerService.newReferentieTabel(),
                true
        );
    }

    @POST
    public RESTReferentieTabel createReferentieTabel(final RESTReferentieTabel referentieTabel) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return RESTReferentieTabelConverter.convert(
                referentieTabelBeheerService.createReferentieTabel(
                        RESTReferentieTabelConverter.convert(referentieTabel)
                ),
                true
        );
    }

    @GET
    @Path("{id}")
    public RESTReferentieTabel readReferentieTabel(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return RESTReferentieTabelConverter.convert(
                referentieTabelService.readReferentieTabel(id),
                true
        );
    }

    @PUT
    @Path("{id}")
    public RESTReferentieTabel updateReferentieTabel(
            @PathParam("id") final long id,
            final RESTReferentieTabel referentieTabel
    ) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return RESTReferentieTabelConverter.convert(
                referentieTabelBeheerService.updateReferentieTabel(
                        RESTReferentieTabelConverter.convert(referentieTabel,
                                referentieTabelService.readReferentieTabel(id)
                        )
                ),
                true
        );
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
        return RESTReferentieWaardeConverter.convert(
                referentieTabelService.readReferentieTabel(AFZENDER.name()).getWaarden()
        );
    }

    @GET
    @Path("communicatiekanalen")
    public List<String> listCommunicatiekanalen() {
        return RESTReferentieWaardeConverter.convert(
                referentieTabelService.readReferentieTabel(COMMUNICATIEKANAAL.name()).getWaarden()
        );
    }

    @GET
    @Path("domein")
    public List<String> listDomeinen() {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return RESTReferentieWaardeConverter.convert(
                referentieTabelService.readReferentieTabel(DOMEIN.name()).getWaarden()
        );
    }

    @GET
    @Path("server-error-text")
    public List<String> listServerErrorPageTexts() {
        return RESTReferentieWaardeConverter.convert(
                referentieTabelService.readReferentieTabel(SERVER_ERROR_ERROR_PAGINA_TEKST.name()).getWaarden()
        );
    }
}
