/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.productaanvragen;

import static nl.info.zac.app.productaanvragen.converter.RestInboxProductaanvraagListParametersConverter.toInboxProductaanvraagListParameters;
import static nl.info.zac.policy.PolicyServiceKt.assertPolicy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

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
import jakarta.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;

import net.atos.zac.app.productaanvragen.converter.RESTInboxProductaanvraagConverter;
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraag;
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraagListParameters;
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraagResultaat;
import net.atos.zac.app.shared.RESTResultaat;
import nl.info.zac.productaanvraag.InboxProductaanvraagService;
import nl.info.zac.productaanvraag.model.InboxProductaanvraagListParameters;
import nl.info.zac.productaanvraag.model.InboxProductaanvraagResultaat;
import net.atos.zac.util.MediaTypes;
import nl.info.client.zgw.drc.DrcClientService;
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import nl.info.zac.policy.PolicyService;

@Singleton
@Path("inbox-productaanvragen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InboxProductaanvraagRestService {

    @Inject
    private DrcClientService drcClientService;

    @Inject
    private PolicyService policyService;

    @Inject
    private InboxProductaanvraagService inboxProductaanvraagService;

    /**
     * No-arg constructor required for CDI.
     */
    public InboxProductaanvraagRestService() {
    }

    public InboxProductaanvraagRestService(
            final DrcClientService drcClientService,
            final PolicyService policyService,
            final InboxProductaanvraagService inboxProductaanvraagService
    ) {
        this.drcClientService = drcClientService;
        this.policyService = policyService;
        this.inboxProductaanvraagService = inboxProductaanvraagService;
    }

    @PUT
    @Path("")
    public RESTResultaat<RESTInboxProductaanvraag> listInboxProductaanvragen(
            final RESTInboxProductaanvraagListParameters restListParameters
    ) {
        assertPolicy(policyService.readWerklijstRechten().getInbox());
        final InboxProductaanvraagListParameters listParameters = toInboxProductaanvraagListParameters(restListParameters);
        final InboxProductaanvraagResultaat resultaat = inboxProductaanvraagService.list(listParameters);
        final RESTInboxProductaanvraagResultaat restInboxProductaanvraagResultaat = new RESTInboxProductaanvraagResultaat(
                RESTInboxProductaanvraagConverter.convert(resultaat.getItems()), resultaat.getCount());
        final List<String> types = resultaat.getTypeFilter();
        if (CollectionUtils.isEmpty(types)) {
            if (restListParameters.type != null) {
                restInboxProductaanvraagResultaat.filterType = List.of(restListParameters.type);
            }
        } else {
            restInboxProductaanvraagResultaat.filterType = types;
        }
        return restInboxProductaanvraagResultaat;
    }

    @GET
    @Path("/{uuid}/pdfPreview")
    public Response pdfPreview(@PathParam("uuid") final UUID uuid) {
        assertPolicy(policyService.readWerklijstRechten().getInbox());
        EnkelvoudigInformatieObject enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(uuid);
        try (ByteArrayInputStream is = drcClientService.downloadEnkelvoudigInformatieobject(uuid)) {
            return Response.ok(is)
                    .header("Content-Disposition",
                            "inline; filename=\"%s\"".formatted(enkelvoudigInformatieobject.getBestandsnaam()))
                    .header("Content-Type", MediaTypes.Application.PDF.getMediaType()).build();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DELETE
    @Path("{id}")
    public void deleteInboxProductaanvraag(@PathParam("id") final long id) {
        assertPolicy(policyService.readWerklijstRechten().getInboxProductaanvragenVerwijderen());
        inboxProductaanvraagService.delete(id);
    }
}
