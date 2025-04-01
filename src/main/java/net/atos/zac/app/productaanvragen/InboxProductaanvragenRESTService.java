/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.productaanvragen;

import static net.atos.zac.policy.PolicyService.assertPolicy;

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

import net.atos.client.zgw.drc.DrcClientService;
import net.atos.zac.app.productaanvragen.converter.RESTInboxProductaanvraagConverter;
import net.atos.zac.app.productaanvragen.converter.RESTInboxProductaanvraagListParametersConverter;
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraag;
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraagListParameters;
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraagResultaat;
import net.atos.zac.app.shared.RESTResultaat;
import net.atos.zac.policy.PolicyService;
import net.atos.zac.productaanvraag.InboxProductaanvraagService;
import net.atos.zac.productaanvraag.model.InboxProductaanvraagListParameters;
import net.atos.zac.productaanvraag.model.InboxProductaanvraagResultaat;
import net.atos.zac.util.MediaTypes;
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;

@Singleton
@Path("inbox-productaanvragen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InboxProductaanvragenRESTService {

    @Inject
    private DrcClientService drcClientService;

    @Inject
    private PolicyService policyService;

    @Inject
    private InboxProductaanvraagService inboxProductaanvraagService;

    @Inject
    private RESTInboxProductaanvraagListParametersConverter listParametersConverter;

    @PUT
    @Path("")
    public RESTResultaat<RESTInboxProductaanvraag> listInboxProductaanvragen(final RESTInboxProductaanvraagListParameters restListParameters) {
        assertPolicy(policyService.readWerklijstRechten().inbox());
        final InboxProductaanvraagListParameters listParameters = listParametersConverter.convert(restListParameters);
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
        assertPolicy(policyService.readWerklijstRechten().inbox());
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
        PolicyService.assertPolicy(policyService.readWerklijstRechten().inboxProductaanvragenVerwijderen());
        inboxProductaanvraagService.delete(id);
    }
}
