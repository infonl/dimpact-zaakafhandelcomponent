/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.detacheddocuments;

import static nl.info.client.zgw.util.ZgwUriUtilsKt.extractUuid;
import static nl.info.zac.policy.PolicyServiceKt.assertPolicy;

import java.util.List;
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

import net.atos.client.zgw.shared.exception.ZgwErrorException;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.zac.app.detacheddocuments.converter.RestDetachedDocumentConverter;
import net.atos.zac.app.detacheddocuments.converter.RestDetachedDocumentListParametersConverter;
import net.atos.zac.app.detacheddocuments.model.RestDetachedDocument;
import net.atos.zac.app.detacheddocuments.model.RestDetachedDocumentListParameters;
import net.atos.zac.app.detacheddocuments.model.RestDetachedDocumentResult;
import net.atos.zac.app.shared.RESTResultaat;
import nl.info.client.zgw.drc.DrcClientService;
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import nl.info.client.zgw.zrc.ZrcClientService;
import nl.info.zac.app.identity.converter.RestUserConverter;
import nl.info.zac.document.detacheddocument.DetachedDocumentService;
import nl.info.zac.document.detacheddocument.model.DetachedDocument;
import nl.info.zac.document.detacheddocument.model.DetachedDocumentListParameters;
import nl.info.zac.document.detacheddocument.model.DetachedDocumentResult;
import nl.info.zac.policy.PolicyService;

@Singleton
@Path("ontkoppeldedocumenten")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DetachedDocumentRestService {
    private DetachedDocumentService detachedDocumentService;
    private DrcClientService drcClientService;
    private ZrcClientService zrcClientService;
    private RestDetachedDocumentConverter restDetachedDocumentConverter;
    private RestDetachedDocumentListParametersConverter listParametersConverter;
    private RestUserConverter userConverter;
    private PolicyService policyService;

    /**
     * No-arg constructor for CDI.
     */
    public DetachedDocumentRestService() {
    }

    @Inject
    public DetachedDocumentRestService(
            DetachedDocumentService detachedDocumentService,
            DrcClientService drcClientService,
            ZrcClientService zrcClientService,
            RestDetachedDocumentConverter restDetachedDocumentConverter,
            RestDetachedDocumentListParametersConverter listParametersConverter,
            RestUserConverter userConverter,
            PolicyService policyService
    ) {
        this.detachedDocumentService = detachedDocumentService;
        this.drcClientService = drcClientService;
        this.zrcClientService = zrcClientService;
        this.restDetachedDocumentConverter = restDetachedDocumentConverter;
        this.listParametersConverter = listParametersConverter;
        this.userConverter = userConverter;
        this.policyService = policyService;
    }


    private static final Logger LOG = Logger.getLogger(DetachedDocumentRestService.class.getName());

    @PUT
    @Path("")
    public RESTResultaat<RestDetachedDocument> listDetachedDocuments(final RestDetachedDocumentListParameters restListParameters) {
        assertPolicy(policyService.readWerklijstRechten().getInbox());
        final DetachedDocumentListParameters listParameters = listParametersConverter.convert(restListParameters);
        final DetachedDocumentResult resultaat = detachedDocumentService.getDetachedDocumentResult(listParameters);
        var ontkoppeldeDocumenten = resultaat.getItems();
        var informationObjectTypeUUIDs = ontkoppeldeDocumenten.stream().map(
                ontkoppeldeDocument -> extractUuid(
                        drcClientService
                                .readEnkelvoudigInformatieobject(ontkoppeldeDocument.documentUUID)
                                .getInformatieobjecttype()
                )
        ).toList();
        final RestDetachedDocumentResult restDetachedDocumentResult = new RestDetachedDocumentResult(
                restDetachedDocumentConverter.convert(ontkoppeldeDocumenten, informationObjectTypeUUIDs),
                resultaat.getCount()
        );
        final List<String> ontkoppeldDoor = resultaat.getDetachedByFilter();
        if (CollectionUtils.isEmpty(ontkoppeldDoor)) {
            if (restListParameters.ontkoppeldDoor != null) {
                restDetachedDocumentResult.filterOntkoppeldDoor = List.of(restListParameters.ontkoppeldDoor);
            }
        } else {
            restDetachedDocumentResult.filterOntkoppeldDoor = userConverter.convertUserIds(ontkoppeldDoor);
        }
        return restDetachedDocumentResult;
    }

    @DELETE
    @Path("{id}")
    public void deleteDetachedDocument(@PathParam("id") final long id) {
        assertPolicy(policyService.readWerklijstRechten().getOntkoppeldeDocumentenVerwijderen());
        final DetachedDocument detachedDocument = detachedDocumentService.find(id);
        if (detachedDocument == null) {
            // detached document record does not exist; ignore silently
            return;
        }
        EnkelvoudigInformatieObject enkelvoudigInformatieobject = null;
        final UUID documentUUID = detachedDocument.getDocumentUUID();
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

        detachedDocumentService.delete(detachedDocument.getId());
    }
}
