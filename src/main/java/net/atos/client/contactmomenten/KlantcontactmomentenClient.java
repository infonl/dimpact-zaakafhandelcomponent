/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.contactmomenten;

import java.util.UUID;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.contactmomenten.exception.NotFoundExceptionMapper;
import net.atos.client.contactmomenten.exception.RuntimeExceptionMapper;
import net.atos.client.contactmomenten.model.KlantContactMoment;
import net.atos.client.contactmomenten.model.KlantcontactmomentList200Response;
import net.atos.client.contactmomenten.model.KlantcontactmomentListParameters;
import net.atos.client.contactmomenten.util.ContactmomentenClientHeadersFactory;

/**
 * Contactmomenten API
 * <p>
 * Een API om contactmomenten met klanten te registreren of op te vragen.
 */
@RegisterRestClient(configKey = "Contactmomenten-API-Client")
@RegisterClientHeaders(ContactmomentenClientHeadersFactory.class)
@RegisterProvider(RuntimeExceptionMapper.class)
@RegisterProvider(NotFoundExceptionMapper.class)
@Path("api/v1/klantcontactmomenten")
public interface KlantcontactmomentenClient {

  /**
   * Maak een KLANT-CONTACTMOMENT relatie aan.
   * <p>
   * Registreer een CONTACTMOMENT bij een KLANT.
   * **Er wordt gevalideerd op**
   * * geldigheid &#x60;contactmoment&#x60; URL
   * * geldigheid &#x60;klant&#x60; URL
   * * de combinatie &#x60;contactmoment&#x60; en &#x60;klant&#x60; moet uniek zijn
   */
  @POST
  @Consumes({"application/json"})
  @Produces({"application/json", "application/problem+json"})
  public KlantContactMoment klantcontactmomentCreate(
      @HeaderParam("Content-Type") String contentType, KlantContactMoment klantContactMoment)
      throws ProcessingException;

  /**
   * Verwijder een KLANT-CONTACTMOMENT relatie.
   */
  @DELETE
  @Path("/{uuid}")
  @Produces({"application/problem+json"})
  public void klantcontactmomentDelete(@PathParam("uuid") UUID uuid) throws ProcessingException;

  /**
   * Alle KLANT-CONTACTMOMENT relaties opvragen.
   * <p>
   * Deze lijst kan gefilterd wordt met query-string parameters.
   */
  @GET
  @Produces({"application/json", "application/problem+json"})
  public KlantcontactmomentList200Response klantcontactmomentList(
      @BeanParam final KlantcontactmomentListParameters parameters) throws ProcessingException;

  /**
   * Een specifieke KLANT-CONTACTMOMENT relatie opvragen.
   */
  @GET
  @Path("/{uuid}")
  @Produces({"application/json", "application/problem+json"})
  public KlantContactMoment klantcontactmomentRead(
      @PathParam("uuid") UUID uuid, @HeaderParam("If-None-Match") String ifNoneMatch)
      throws ProcessingException;
}
