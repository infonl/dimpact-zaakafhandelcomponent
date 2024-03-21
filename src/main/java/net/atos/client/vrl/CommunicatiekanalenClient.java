/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.vrl;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.vrl.exception.CommunicatiekanaalNotFoundException;
import net.atos.client.vrl.exception.CommunicatiekanaalNotFoundExceptionMapping;
import net.atos.client.vrl.exception.RuntimeExceptionMapper;
import net.atos.client.vrl.model.generated.CommunicatieKanaal;
import net.atos.client.vrl.model.generated.CommunicatiekanaalList200Response;

/**
 * Referentielijsten & Selectielijst API
 * <p>
 * Een API om referentielijstwaarden en de gemeentelijke selectielijst te benaderen.
 * <p>
 * ## Selectielijst
 * <p>
 * De [Gemeentelijke Selectielijst](<a href="https://vng.nl/selectielijst">...</a>) is relevant in het kader van archivering.
 * **Zaakgericht werken** Bij het configureren van zaaktypes (en resultaattypes) in de catalogus API refereren een aantal resources naar
 * resources binnen de Selectielijst API.
 * Het gaat dan om de `ProcesType` en `Resultaat` resources.
 * <p>
 * ## Referentielijsten Referentielijsten bevat een standaardset aan waarden.
 * <p>
 * Deze waarden zijn net té dynamisch om in een enum opgenomen te worden, maar er is wel behoefte om deze landelijk te standaardiseren.
 * Een voorbeeld hiervan is de set aan mogelijke communicatiekanalen.
 * <p>
 * ## Autorisatie Deze APIs zijn alleen-lezen, en behoeven geen autorisatie.
 * <p>
 * ## Inhoud De inhoud wordt beheerd door VNG Realisatie. Om de inhoud van referentielijsten bij te werken, contacteer dan VNG Realisatie
 * via e-mail of op Github.
 * <p>
 * De inhoud van de Gemeentelijke Selectielijst wordt geïmporteerd vanuit de gepubliceerde Excel-bestanden.
 */
@RegisterRestClient(configKey = "VRL-API-Client")
@RegisterProvider(RuntimeExceptionMapper.class)
@RegisterProvider(CommunicatiekanaalNotFoundExceptionMapping.class)
@Produces({"application/json", "application/problem+json"})
@Path("api/v1/communicatiekanalen")
public interface CommunicatiekanalenClient {

    @GET
    CommunicatiekanaalList200Response communicatiekanaalList(@QueryParam("page") Integer page);

    @GET
    @Path("/{uuid}")
    CommunicatieKanaal communicatiekanaalRead(@PathParam("uuid") UUID uuid) throws CommunicatiekanaalNotFoundException;
}
