/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.ontkoppeldedocumenten;

import static net.atos.zac.policy.PolicyService.assertPolicy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

import net.atos.client.zgw.drc.DRCClientService;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.zac.app.identity.converter.RESTUserConverter;
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
import net.atos.zac.util.UriUtil;

@Singleton
@Path("ontkoppeldedocumenten")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OntkoppeldeDocumentenRESTService {

    @Inject
    private OntkoppeldeDocumentenService ontkoppeldeDocumentenService;

    @Inject
    private DRCClientService drcClientService;

    @Inject
    private ZRCClientService zrcClientService;

    @Inject
    private RESTOntkoppeldDocumentConverter ontkoppeldDocumentConverter;

    @Inject
    private RESTOntkoppeldDocumentListParametersConverter listParametersConverter;

    @Inject
    private RESTUserConverter userConverter;

    @Inject
    private PolicyService policyService;

    @PUT
    @Path("")
    public RESTResultaat<RESTOntkoppeldDocument> list(final RESTOntkoppeldDocumentListParameters restListParameters) {
        assertPolicy(policyService.readWerklijstRechten().inbox());
        final OntkoppeldDocumentListParameters listParameters = listParametersConverter.convert(restListParameters);
        final OntkoppeldeDocumentenResultaat resultaat = ontkoppeldeDocumentenService.getResultaat(listParameters);
        final RESTOntkoppeldDocumentResultaat restOntkoppeldDocumentResultaat = new RESTOntkoppeldDocumentResultaat(ontkoppeldDocumentConverter.convert(resultaat.getItems()),
                                                                                                                    resultaat.getCount());
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
    public void delete(@PathParam("id") final long id) {
        assertPolicy(policyService.readWerklijstRechten().ontkoppeldeDocumentenVerwijderen());
        final Optional<OntkoppeldDocument> ontkoppeldDocument = ontkoppeldeDocumentenService.find(id);
        if (ontkoppeldDocument.isEmpty()) {
            return; // al verwijderd
        }
        final EnkelvoudigInformatieObject enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(ontkoppeldDocument.get()
                                                                                                                                           .getDocumentUUID());
        final List<ZaakInformatieobject> zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(
                                                                                                              enkelvoudigInformatieobject);
        if (!zaakInformatieobjecten.isEmpty()) {
            final UUID zaakUuid = UriUtil.uuidFromURI(zaakInformatieobjecten.get(0).getZaak());
            throw new IllegalStateException(String.format("Informatieobject is gekoppeld aan zaak '%s'", zaakUuid));
        }
        drcClientService.deleteEnkelvoudigInformatieobject(ontkoppeldDocument.get().getDocumentUUID());
        ontkoppeldeDocumentenService.delete(ontkoppeldDocument.get().getId());
    }
}
