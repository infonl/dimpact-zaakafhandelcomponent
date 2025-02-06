/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.bag;

import static java.util.stream.Collectors.joining;
import static net.atos.zac.policy.PolicyService.assertPolicy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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

import net.atos.client.bag.BagClientService;
import net.atos.client.bag.model.BevraagAdressenParameters;
import net.atos.client.zgw.shared.model.Results;
import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.Objecttype;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectListParameters;
import net.atos.zac.app.bag.converter.RestAdresConverter;
import net.atos.zac.app.bag.converter.RestBagConverter;
import net.atos.zac.app.bag.converter.RestNummeraanduidingConverter;
import net.atos.zac.app.bag.converter.RestOpenbareRuimteConverter;
import net.atos.zac.app.bag.converter.RestPandConverter;
import net.atos.zac.app.bag.converter.RestWoonplaatsConverter;
import net.atos.zac.app.bag.model.BAGObjectType;
import net.atos.zac.app.bag.model.RESTBAGAdres;
import net.atos.zac.app.bag.model.RESTBAGObject;
import net.atos.zac.app.bag.model.RESTBAGObjectGegevens;
import net.atos.zac.app.bag.model.RESTListAdressenParameters;
import net.atos.zac.app.shared.RESTResultaat;
import net.atos.zac.policy.PolicyService;

@Path("bag")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class BagRestService {
    private BagClientService bagClientService;
    private ZrcClientService zrcClientService;
    private PolicyService policyService;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public BagRestService() {
    }

    @Inject
    public BagRestService(
            BagClientService bagClientService,
            ZrcClientService zrcClientService,
            PolicyService policyService
    ) {
        this.bagClientService = bagClientService;
        this.zrcClientService = zrcClientService;
        this.policyService = policyService;
    }

    @PUT
    @Path("adres")
    public RESTResultaat<RESTBAGAdres> listAdressen(final RESTListAdressenParameters listAdressenParameters) {
        final BevraagAdressenParameters bevraagAdressenParameters = new BevraagAdressenParameters();
        bevraagAdressenParameters.setQ(listAdressenParameters.trefwoorden);
        bevraagAdressenParameters.setExpand(getExpand(BAGObjectType.NUMMERAANDUIDING, BAGObjectType.OPENBARE_RUIMTE, BAGObjectType.PAND,
                BAGObjectType.WOONPLAATS));
        return new RESTResultaat<>(bagClientService.listAdressen(bevraagAdressenParameters).stream()
                .map(RestAdresConverter::convertToREST)
                .toList());
    }

    @GET
    @Path("/{type}/{id}")
    public RESTBAGObject read(@PathParam("type") final BAGObjectType type, @PathParam("id") final String id) {
        return switch (type) {
            case ADRES -> RestAdresConverter.convertToREST(bagClientService.readAdres(id));
            case WOONPLAATS -> RestWoonplaatsConverter.convertToREST(bagClientService.readWoonplaats(id));
            case PAND -> RestPandConverter.convertToREST(bagClientService.readPand(id));
            case OPENBARE_RUIMTE -> RestOpenbareRuimteConverter.convertToREST(bagClientService.readOpenbareRuimte(id));
            case NUMMERAANDUIDING -> RestNummeraanduidingConverter.convertToREST(bagClientService.readNummeraanduiding(id));
            case ADRESSEERBAAR_OBJECT -> null; //(Nog) geen zelfstandige entiteit
        };
    }

    @POST
    public void create(final RESTBAGObjectGegevens bagObjectGegevens) {
        final Zaak zaak = zrcClientService.readZaak(bagObjectGegevens.zaakUuid);
        assertPolicy(policyService.readZaakRechten(zaak).toevoegenBagObject());
        if (isNogNietGekoppeld(bagObjectGegevens.getBagObject(), zaak)) {
            zrcClientService.createZaakobject(RestBagConverter.convertToZaakobject(bagObjectGegevens.getBagObject(), zaak));
        }
    }

    @DELETE
    public void delete(final RESTBAGObjectGegevens bagObjectGegevens) {
        final Zaak zaak = zrcClientService.readZaak(bagObjectGegevens.zaakUuid);
        assertPolicy(policyService.readZaakRechten(zaak).behandelen());
        final Zaakobject zaakobject = zrcClientService.readZaakobject(bagObjectGegevens.uuid);
        zrcClientService.deleteZaakobject(zaakobject, bagObjectGegevens.redenWijzigen);
    }

    @GET
    @Path("zaak/{zaakUuid}")
    public List<RESTBAGObjectGegevens> listBagobjectenVoorZaak(@PathParam("zaakUuid") final UUID zaakUUID) {
        final ZaakobjectListParameters zaakobjectListParameters = new ZaakobjectListParameters();
        final Zaak zaak = zrcClientService.readZaak(zaakUUID);
        assertPolicy(policyService.readZaakRechten(zaak).lezen());
        zaakobjectListParameters.setZaak(zaak.getUrl());
        final Results<Zaakobject> zaakobjecten = zrcClientService.listZaakobjecten(zaakobjectListParameters);
        if (zaakobjecten.getCount() > 0) {
            return zaakobjecten.getResults().stream()
                    .filter(Zaakobject::isBagObject)
                    .map(RestBagConverter::convertToRESTBAGObjectGegevens)
                    .toList();
        } else {
            return Collections.emptyList();
        }
    }

    private String getExpand(final BAGObjectType... bagObjectTypes) {
        return Arrays.stream(bagObjectTypes)
                .map(BAGObjectType::getExpand)
                .collect(joining(","));
    }

    private boolean isNogNietGekoppeld(final RESTBAGObject restbagObject, final Zaak zaak) {
        final ZaakobjectListParameters zaakobjectListParameters = new ZaakobjectListParameters();
        zaakobjectListParameters.setZaak(zaak.getUrl());
        zaakobjectListParameters.setObject(restbagObject.url);
        switch (restbagObject.getBagObjectType()) {
            case ADRES -> zaakobjectListParameters.setObjectType(Objecttype.ADRES);
            case NUMMERAANDUIDING -> zaakobjectListParameters.setObjectType(Objecttype.OVERIGE);
            case WOONPLAATS -> zaakobjectListParameters.setObjectType(Objecttype.WOONPLAATS);
            case PAND -> zaakobjectListParameters.setObjectType(Objecttype.PAND);
            case OPENBARE_RUIMTE -> zaakobjectListParameters.setObjectType(Objecttype.OPENBARE_RUIMTE);
        }
        final Results<Zaakobject> zaakobjecten = zrcClientService.listZaakobjecten(zaakobjectListParameters);
        return zaakobjecten.getResults().isEmpty();
    }
}
