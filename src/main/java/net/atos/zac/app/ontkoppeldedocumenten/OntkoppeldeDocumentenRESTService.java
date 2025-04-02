/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.ontkoppeldedocumenten;

import static net.atos.zac.policy.PolicyService.assertPolicy;
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

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.jetty.http.HttpStatus;

import net.atos.client.zgw.drc.DrcClientService;
import net.atos.client.zgw.shared.exception.ZgwErrorException;
import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.zac.app.identity.converter.RestUserConverter;
import net.atos.zac.app.ontkoppeldedocumenten.converter.RESTOntkoppeldDocumentConverter;
import net.atos.zac.app.ontkoppeldedocumenten.converter.RESTOntkoppeldDocumentListParametersConverter;
import net.atos.zac.app.ontkoppeldedocumenten.model.RESTOntkoppeldDocument;
import net.atos.zac.app.ontkoppeldedocumenten.model.RESTOntkoppeldDocumentListParameters;
import net.atos.zac.app.ontkoppeldedocumenten.model.RESTOntkoppeldDocumentResultaat;
import net.atos.zac.app.shared.RESTResultaat;
import net.atos.zac.documenten.OntkoppeldeDocumentenService;
import net.atos.zac.documenten.model.OntkoppeldDocument;
import net.atos.zac.documenten.model.OntkoppeldDocumentListParameters;
import net.atos.zac.documenten.model.OntkoppeldeDocumentenResultaat;
import net.atos.zac.policy.PolicyService;
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;

@Singleton
@Path("ontkoppeldedocumenten")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OntkoppeldeDocumentenRESTService {
    private OntkoppeldeDocumentenService ontkoppeldeDocumentenService;
    private DrcClientService drcClientService;
    private ZrcClientService zrcClientService;
    private RESTOntkoppeldDocumentConverter ontkoppeldDocumentConverter;
    private RESTOntkoppeldDocumentListParametersConverter listParametersConverter;
    private RestUserConverter userConverter;
    private PolicyService policyService;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public OntkoppeldeDocumentenRESTService() {
    }

    @Inject
    public OntkoppeldeDocumentenRESTService(
            OntkoppeldeDocumentenService ontkoppeldeDocumentenService,
            DrcClientService drcClientService,
            ZrcClientService zrcClientService,
            RESTOntkoppeldDocumentConverter ontkoppeldDocumentConverter,
            RESTOntkoppeldDocumentListParametersConverter listParametersConverter,
            RestUserConverter userConverter,
            PolicyService policyService
    ) {
        this.ontkoppeldeDocumentenService = ontkoppeldeDocumentenService;
        this.drcClientService = drcClientService;
        this.zrcClientService = zrcClientService;
        this.ontkoppeldDocumentConverter = ontkoppeldDocumentConverter;
        this.listParametersConverter = listParametersConverter;
        this.userConverter = userConverter;
        this.policyService = policyService;
    }


    private static final Logger LOG = Logger.getLogger(OntkoppeldeDocumentenRESTService.class.getName());

    @PUT
    @Path("")
    public RESTResultaat<RESTOntkoppeldDocument> listDetachedDocuments(final RESTOntkoppeldDocumentListParameters restListParameters) {
        assertPolicy(policyService.readWerklijstRechten().inbox());
        final OntkoppeldDocumentListParameters listParameters = listParametersConverter.convert(restListParameters);
        final OntkoppeldeDocumentenResultaat resultaat = ontkoppeldeDocumentenService.getResultaat(listParameters);
        var ontkoppeldeDocumenten = resultaat.getItems();
        var informationObjectTypeUUIDs = ontkoppeldeDocumenten.stream().map(
                ontkoppeldeDocument -> extractUuid(
                        drcClientService
                                .readEnkelvoudigInformatieobject(ontkoppeldeDocument.getDocumentUUID())
                                .getInformatieobjecttype()
                )
        ).toList();
        final RESTOntkoppeldDocumentResultaat restOntkoppeldDocumentResultaat = new RESTOntkoppeldDocumentResultaat(
                ontkoppeldDocumentConverter.convert(ontkoppeldeDocumenten, informationObjectTypeUUIDs),
                resultaat.getCount()
        );
        final List<String> ontkoppeldDoor = resultaat.getOntkoppeldDoorFilter();
        if (CollectionUtils.isEmpty(ontkoppeldDoor)) {
            if (restListParameters.ontkoppeldDoor != null) {
                restOntkoppeldDocumentResultaat.filterOntkoppeldDoor = List.of(restListParameters.ontkoppeldDoor);
            }
        } else {
            restOntkoppeldDocumentResultaat.filterOntkoppeldDoor = userConverter.convertUserIds(ontkoppeldDoor);
        }
        return restOntkoppeldDocumentResultaat;
    }

    @DELETE
    @Path("{id}")
    public void deleteDetachedDocument(@PathParam("id") final long id) {
        assertPolicy(policyService.readWerklijstRechten().ontkoppeldeDocumentenVerwijderen());
        final Optional<OntkoppeldDocument> ontkoppeldDocument = ontkoppeldeDocumentenService.find(id);
        if (ontkoppeldDocument.isEmpty()) {
            return; // al verwijderd
        }
        EnkelvoudigInformatieObject enkelvoudigInformatieobject = null;
        final UUID documentUUID = ontkoppeldDocument
                .get().getDocumentUUID();
        try {
            enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(documentUUID);
        } catch (ZgwErrorException e) {
            if (e.getZgwError().getStatus() != HttpStatus.NOT_FOUND_404) {
                throw e;
            }
            LOG.info(String.format("Document met UUID '%s' wel gevonden in de database, maar niet in OpenZaak", documentUUID));
        }
        if (enkelvoudigInformatieobject != null) {
            final List<ZaakInformatieobject> zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(
                    enkelvoudigInformatieobject);
            if (!zaakInformatieobjecten.isEmpty()) {
                final UUID zaakUuid = extractUuid(zaakInformatieobjecten.getFirst().getZaak());
                throw new IllegalStateException(String.format("Informatieobject is gekoppeld aan zaak '%s'", zaakUuid));
            }
            drcClientService.deleteEnkelvoudigInformatieobject(documentUUID);
        }

        ontkoppeldeDocumentenService.delete(ontkoppeldDocument.get().getId());
    }
}
