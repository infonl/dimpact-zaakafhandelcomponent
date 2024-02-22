/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.kvk;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.kvk.basisprofiel.model.Basisprofiel;
import net.atos.client.kvk.basisprofiel.model.Eigenaar;
import net.atos.client.kvk.basisprofiel.model.Vestiging;
import net.atos.client.kvk.basisprofiel.model.VestigingList;
import net.atos.client.kvk.exception.RuntimeExceptionMapper;
import net.atos.client.kvk.util.KVKClientHeadersFactory;

/**
 * API Basisprofiel
 *
 * <p>Documentatie voor API Basisprofiel.
 */
@RegisterRestClient(configKey = "KVK-API-Client")
@RegisterClientHeaders(KVKClientHeadersFactory.class)
@RegisterProvider(RuntimeExceptionMapper.class)
@Produces({"application/hal+json"})
@Path("api/v1/basisprofielen/{kvkNummer}")
public interface BasisprofielClient {

  /**
   * Voor een specifiek bedrijf basisinformatie opvragen.
   */
  @GET
  Basisprofiel getBasisprofielByKvkNummer(
      @PathParam("kvkNummer") String kvkNummer,
      @QueryParam("geoData") @DefaultValue("false") Boolean geoData);

  /**
   * Voor een specifiek bedrijf eigenaar informatie opvragen.
   */
  @GET
  @Path("/eigenaar")
  Eigenaar getEigenaar(
      @PathParam("kvkNummer") String kvkNummer,
      @QueryParam("geoData") @DefaultValue("false") Boolean geoData);

  /**
   * Voor een specifiek bedrijf hoofdvestigingsinformatie opvragen.
   */
  @GET
  @Path("/hoofdvestiging")
  Vestiging getHoofdvestiging(
      @PathParam("kvkNummer") String kvkNummer,
      @QueryParam("geoData") @DefaultValue("false") Boolean geoData);

  /**
   * Voor een specifiek bedrijf een lijst met vestigingen opvragen.
   */
  @GET
  @Path("/vestigingen")
  VestigingList getVestigingen(@PathParam("kvkNummer") String kvkNummer);
}
