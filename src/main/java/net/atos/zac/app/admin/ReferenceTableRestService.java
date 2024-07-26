/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin;

import static net.atos.zac.admin.model.ReferenceTable.Systeem.AFZENDER;
import static net.atos.zac.admin.model.ReferenceTable.Systeem.COMMUNICATIEKANAAL;
import static net.atos.zac.admin.model.ReferenceTable.Systeem.DOMEIN;
import static net.atos.zac.admin.model.ReferenceTable.Systeem.SERVER_ERROR_ERROR_PAGINA_TEKST;
import static net.atos.zac.configuratie.ConfiguratieService.COMMUNICATIEKANAAL_EFORMULIER;
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

import net.atos.zac.admin.ReferenceTableAdminService;
import net.atos.zac.admin.ReferenceTableService;
import net.atos.zac.admin.model.ReferenceTable;
import net.atos.zac.app.admin.converter.RestReferenceTableConverter;
import net.atos.zac.app.admin.converter.RestReferenceValueConverter;
import net.atos.zac.app.admin.model.RestReferenceTable;
import net.atos.zac.policy.PolicyService;

@Singleton
@Path("referentietabellen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReferenceTableRestService {
    private ReferenceTableService referenceTableService;
    private ReferenceTableAdminService referenceTableAdminService;
    private PolicyService policyService;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public ReferenceTableRestService() {
    }

    @Inject
    public ReferenceTableRestService(
            final ReferenceTableService referenceTableService,
            final ReferenceTableAdminService referenceTableAdminService,
            final PolicyService policyService
    ) {
        this.referenceTableService = referenceTableService;
        this.referenceTableAdminService = referenceTableAdminService;
        this.policyService = policyService;
    }

    @GET
    public List<RestReferenceTable> listReferenceTables() {
        assertPolicy(policyService.readOverigeRechten().beheren());
        final List<ReferenceTable> referentieTabellen = referenceTableService.listReferenceTables();
        return referentieTabellen.stream()
                .map(referentieTabel -> RestReferenceTableConverter.convert(referentieTabel, false))
                .toList();
    }

    @GET
    @Path("new")
    public RestReferenceTable newReferenceTable() {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return RestReferenceTableConverter.convert(
                referenceTableAdminService.newReferenceTable(),
                true
        );
    }

    @POST
    public RestReferenceTable createReferenceTable(final RestReferenceTable referentieTabel) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return RestReferenceTableConverter.convert(
                referenceTableAdminService.createReferenceTable(
                        RestReferenceTableConverter.convert(referentieTabel)
                ),
                true
        );
    }

    @GET
    @Path("{id}")
    public RestReferenceTable readReferenceTable(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return RestReferenceTableConverter.convert(
                referenceTableService.readReferenceTable(id),
                true
        );
    }

    @PUT
    @Path("{id}")
    public RestReferenceTable updateReferenceTable(
            @PathParam("id") final long id,
            final RestReferenceTable referentieTabel
    ) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return RestReferenceTableConverter.convert(
                referenceTableAdminService.updateReferenceTable(
                        RestReferenceTableConverter.convert(referentieTabel,
                                referenceTableService.readReferenceTable(id)
                        )
                ),
                true
        );
    }

    @DELETE
    @Path("{id}")
    public void deleteReferenceTable(@PathParam("id") final long id) {
        assertPolicy(policyService.readOverigeRechten().beheren());
        referenceTableAdminService.deleteReferenceTable(id);
    }

    @GET
    @Path("afzender")
    public List<String> listEmailSenders() {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return RestReferenceValueConverter.convert(
                referenceTableService.readReferenceTable(AFZENDER.name()).getWaarden()
        );
    }

    @GET
    @Path("communicatiekanaal/{inclusiefEFormulier}")
    public List<String> listCommunicationChannels(
            @PathParam("inclusiefEFormulier") final boolean inclusiefEFormulier
    ) {
        return RestReferenceValueConverter.convert(
                referenceTableService.readReferenceTable(COMMUNICATIEKANAAL.name()).getWaarden()
        )
                .stream()
                .filter(communicatiekanaal -> inclusiefEFormulier || !communicatiekanaal.equals(COMMUNICATIEKANAAL_EFORMULIER))
                .toList();
    }

    @GET
    @Path("domein")
    public List<String> listDomains() {
        assertPolicy(policyService.readOverigeRechten().beheren());
        return RestReferenceValueConverter.convert(
                referenceTableService.readReferenceTable(DOMEIN.name()).getWaarden()
        );
    }

    @GET
    @Path("server-error-text")
    public List<String> listServerErrorPageTexts() {
        return RestReferenceValueConverter.convert(
                referenceTableService.readReferenceTable(SERVER_ERROR_ERROR_PAGINA_TEKST.name()).getWaarden()
        );
    }
}
