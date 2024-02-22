/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.vrl;

import java.net.URI;
import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.vrl.exception.RuntimeExceptionMapper;
import net.atos.client.vrl.model.CommunicatiekanaalList200Response;
import net.atos.client.vrl.model.Resultaat;

/**
 * Referentielijsten & Selectielijst API
 *
 * <p> Een API om referentielijstwaarden en de gemeentelijke selectielijst te benaderen.  ## Selectielijst  De [Gemeentelijke Selectielijst](https://vng.nl/selectielijst) is relevant in het kader van archivering.  **Zaakgericht werken**  Bij het configureren van zaaktypes (en resultaattypes) in de catalogus API refereren een aantal resources naar resources binnen de Selectielijst API. Het gaat dan om de `ProcesType` en `Resultaat` resources.  ## Referentielijsten  Referentielijsten bevat een standaardset aan waarden. Deze waarden zijn net té dynamisch om in een enum opgenomen te worden, maar er is wel behoefte om deze landelijk te standaardiseren. Een voorbeeld hiervan is de set aan mogelijke communicatiekanalen.  ## Autorisatie  Deze APIs zijn alleen-lezen, en behoeven geen autorisatie.  ## Inhoud  De inhoud wordt beheerd door VNG Realisatie. Om de inhoud van referentielijsten bij te werken, contacteer dan VNG Realisatie via e-mail of op Github.  De inhoud van de Gemeentelijke Selectielijst wordt geïmporteerd vanuit de gepubliceerde Excel-bestanden.
 *
 */
@RegisterRestClient(configKey = "VRL-API-Client")
@RegisterProvider(RuntimeExceptionMapper.class)
@Produces({"application/json", "application/problem+json"})
@Path("api/v1/resultaten")
public interface ResultatenApi {

  /**
   * Ontsluit de selectielijst resultaten.
   * <p>
   * Bij een procestype horen meerdere mogelijke resultaten, al dan niet generiek/specifiek. Bij het configureren van een resultaattype in het ZTC wordt aangegeven welke selectielijstklasse van toepassing is, wat een referentie is naar een item van deze resource.  Zie https://vng.nl/files/vng/20170706-selectielijst-gemeenten-intergemeentelijke-organen-2017.pdf voor de bron van de inhoud.
   */
  @GET
  CommunicatiekanaalList200Response resultaatList(
      @QueryParam("procesType") URI procesType, @QueryParam("page") Integer page);

  /**
   * Ontsluit de selectielijst resultaten.
   *
   * Bij een procestype horen meerdere mogelijke resultaten, al dan niet generiek/specifiek. Bij het configureren van een resultaattype in het ZTC wordt aangegeven welke selectielijstklasse van toepassing is, wat een referentie is naar een item van deze resource.  Zie https://vng.nl/files/vng/20170706-selectielijst-gemeenten-intergemeentelijke-organen-2017.pdf voor de bron van de inhoud.
   */
  @GET
  @Path("/{uuid}")
  Resultaat resultaatRead(@PathParam("uuid") UUID uuid);
}
