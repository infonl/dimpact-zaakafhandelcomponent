/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.inboxdocumenten;

import static nl.info.client.zgw.util.UriUtilsKt.extractUuid;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.client.zgw.drc.DrcClientService;
import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.zac.app.inboxdocumenten.converter.RESTInboxDocumentConverter;
import net.atos.zac.app.inboxdocumenten.converter.RESTInboxDocumentListParametersConverter;
import net.atos.zac.app.inboxdocumenten.model.RESTInboxDocument;
import net.atos.zac.app.inboxdocumenten.model.RESTInboxDocumentListParameters;
import net.atos.zac.app.shared.RESTResultaat;
import net.atos.zac.documenten.InboxDocumentenService;
import net.atos.zac.documenten.model.InboxDocument;
import net.atos.zac.documenten.model.InboxDocumentListParameters;
import net.atos.zac.policy.PolicyService;
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;

@Singleton
@Path("inboxdocumenten")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InboxDocumentenRESTService {

    @Inject
    private InboxDocumentenService inboxDocumentenService;

    @Inject
    private DrcClientService drcClientService;

    @Inject
    private ZrcClientService zrcClientService;

    @Inject
    private RESTInboxDocumentListParametersConverter listParametersConverter;

    @Inject
    private PolicyService policyService;

    private static final Logger LOG = Logger.getLogger(InboxDocumentenRESTService.class.getName());

    @PUT
    @Path("")
    public RESTResultaat<RESTInboxDocument> listInboxDocuments(final RESTInboxDocumentListParameters restListParameters) {
        PolicyService.assertPolicy(policyService.readWerklijstRechten().inbox());
        final InboxDocumentListParameters listParameters = listParametersConverter.convert(restListParameters);
        var inboxDocuments = inboxDocumentenService.list(listParameters);
        var informationObjectTypeUUIDs = inboxDocuments.stream().map(
                inboxDocument -> extractUuid(
                        drcClientService
                                .readEnkelvoudigInformatieobject(inboxDocument.getEnkelvoudiginformatieobjectUUID())
                                .getInformatieobjecttype()
                )
        ).toList();
        return new RESTResultaat<>(
                RESTInboxDocumentConverter.convert(inboxDocuments, informationObjectTypeUUIDs),
                inboxDocumentenService.count(listParameters)
        );
    }

    @DELETE
    @Path("{id}")
    public void deleteInboxDocument(@PathParam("id") final long id) {
        PolicyService.assertPolicy(policyService.readWerklijstRechten().inbox());
        final Optional<InboxDocument> inboxDocument = inboxDocumentenService.find(id);
        if (inboxDocument.isEmpty()) {
            return; // reeds verwijderd
        }
        final EnkelvoudigInformatieObject enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(
                inboxDocument.get().getEnkelvoudiginformatieobjectUUID());
        final List<ZaakInformatieobject> zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(
                enkelvoudigInformatieobject);
        if (!zaakInformatieobjecten.isEmpty()) {
            final UUID zaakUuid = extractUuid(zaakInformatieobjecten.getFirst().getZaak());
            LOG.warning(
                    String.format(
                            "Het inbox-document is verwijderd maar het informatieobject is niet verwijderd. Reden: informatieobject '%s' is gekoppeld aan zaak '%s'.",
                            enkelvoudigInformatieobject.getIdentificatie(), zaakUuid));
        } else {
            drcClientService.deleteEnkelvoudigInformatieobject(
                    inboxDocument.get().getEnkelvoudiginformatieobjectUUID());
        }
        inboxDocumentenService.delete(id);
    }
}
