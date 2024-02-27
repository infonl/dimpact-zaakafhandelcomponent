/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.contactmomenten;

import static net.atos.zac.util.UriUtil.uuidFromURI;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.client.contactmomenten.ContactmomentenClientService;
import net.atos.client.contactmomenten.model.ContactMoment;
import net.atos.client.contactmomenten.model.KlantContactMoment;
import net.atos.client.contactmomenten.model.KlantcontactmomentListParameters;
import net.atos.client.klanten.KlantenClientService;
import net.atos.client.klanten.model.Klant;
import net.atos.zac.app.contactmomenten.converter.ContactmomentConverter;
import net.atos.zac.app.contactmomenten.model.RESTContactmoment;
import net.atos.zac.app.contactmomenten.model.RESTListContactmomentenParameters;
import net.atos.zac.app.shared.RESTResultaat;

@Path("contactmomenten")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class ContactmomentenRESTService {

    // Aantal items wat Open klant (waarschijnlijk) terug geeft per pagina
    private final static int NUM_ITEMS_PER_PAGE = 100;

    @Inject
    private KlantenClientService klantenClientService;

    @Inject
    private ContactmomentenClientService contactmomentenClientService;

    @Inject
    private ContactmomentConverter contactmomentConverter;

    @PUT
    public RESTResultaat<RESTContactmoment> listContactmomenten(final RESTListContactmomentenParameters parameters) {
        final Optional<Klant> klantOptional = parameters.bsn != null ?
                klantenClientService.findPersoon(parameters.bsn) :
                klantenClientService.findVestiging(parameters.vestigingsnummer);
        return klantOptional.map(klant -> listContactmomenten(klant, parameters.page, parameters.pageSize))
                .orElseGet(() -> new RESTResultaat<>());
    }

    private RESTResultaat<RESTContactmoment> listContactmomenten(
            final Klant klant,
            final Integer page,
            final Integer pageSize
    ) {
        final var klantcontactmomentListParameters = new KlantcontactmomentListParameters();
        klantcontactmomentListParameters.setPage(1 + page * pageSize / 100);
        klantcontactmomentListParameters.setKlant(klant.getUrl());
        final var klantcontactmomentenResponse = contactmomentenClientService.listKlantcontactmomenten(klantcontactmomentListParameters);
        final List<RESTContactmoment> contactmomenten = klantcontactmomentenResponse.getResults().stream()
                .skip(page * pageSize % NUM_ITEMS_PER_PAGE)
                .limit(pageSize)
                .map(this::convertToRESTContactmoment)
                .toList();
        return new RESTResultaat<>(contactmomenten, klantcontactmomentenResponse.getCount());
    }

    private RESTContactmoment convertToRESTContactmoment(final KlantContactMoment klantContactMoment) {
        final ContactMoment contactMoment = contactmomentenClientService.readContactmoment(
                uuidFromURI(klantContactMoment.getContactmoment()));
        return contactmomentConverter.convert(contactMoment);

    }
}
