/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

/**
 * Referentielijsten & Selectielijst API
 * Een API om referentielijstwaarden en de gemeentelijke selectielijst te benaderen. ## Selectielijst De [Gemeentelijke
 * Selectielijst](https://vng.nl/selectielijst) is relevant in het kader van archivering. **Zaakgericht werken** Bij het configureren van
 * zaaktypes (en resultaattypes) in de catalogus API refereren een aantal resources naar resources binnen de Selectielijst API. Het gaat dan
 * om de `ProcesType` en `Resultaat` resources. ## Referentielijsten Referentielijsten bevat een standaardset aan waarden. Deze waarden zijn
 * net té dynamisch om in een enum opgenomen te worden, maar er is wel behoefte om deze landelijk te standaardiseren. Een voorbeeld hiervan
 * is de set aan mogelijke communicatiekanalen. ## Autorisatie Deze APIs zijn alleen-lezen, en behoeven geen autorisatie. ## Inhoud De
 * inhoud wordt beheerd door VNG Realisatie. Om de inhoud van referentielijsten bij te werken, contacteer dan VNG Realisatie via e-mail of
 * op Github. De inhoud van de Gemeentelijke Selectielijst wordt geïmporteerd vanuit de gepubliceerde Excel-bestanden.
 * <p>
 * The version of the OpenAPI document: 1.0.0-alpha
 * Contact: standaarden.ondersteuning@vng.nl
 * <p>
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package net.atos.client.vrl;

import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.vrl.exception.RuntimeExceptionMapper;
import net.atos.client.vrl.model.generated.ResultaattypeOmschrijvingGeneriek;

/**
 * Referentielijsten & Selectielijst API
 *
 * <p> Een API om referentielijstwaarden en de gemeentelijke selectielijst te benaderen. ## Selectielijst De [Gemeentelijke
 * Selectielijst](https://vng.nl/selectielijst) is relevant in het kader van archivering. **Zaakgericht werken** Bij het configureren van
 * zaaktypes (en resultaattypes) in de catalogus API refereren een aantal resources naar resources binnen de Selectielijst API. Het gaat dan
 * om de `ProcesType` en `Resultaat` resources. ## Referentielijsten Referentielijsten bevat een standaardset aan waarden. Deze waarden zijn
 * net té dynamisch om in een enum opgenomen te worden, maar er is wel behoefte om deze landelijk te standaardiseren. Een voorbeeld hiervan
 * is de set aan mogelijke communicatiekanalen. ## Autorisatie Deze APIs zijn alleen-lezen, en behoeven geen autorisatie. ## Inhoud De
 * inhoud wordt beheerd door VNG Realisatie. Om de inhoud van referentielijsten bij te werken, contacteer dan VNG Realisatie via e-mail of
 * op Github. De inhoud van de Gemeentelijke Selectielijst wordt geïmporteerd vanuit de gepubliceerde Excel-bestanden.
 *
 */
@RegisterRestClient(configKey = "VRL-API-Client")
@RegisterProvider(RuntimeExceptionMapper.class)
@Produces({"application/json", "application/problem+json"})
@Path("api/v1/resultaattypeomschrijvingen")
public interface ResultaattypeomschrijvingenApi {

    @GET
    List<ResultaattypeOmschrijvingGeneriek> resultaattypeomschrijvinggeneriekList();

    @GET
    @Path("/{uuid}")
    ResultaattypeOmschrijvingGeneriek resultaattypeomschrijvinggeneriekRead(@PathParam("uuid") UUID uuid);
}
