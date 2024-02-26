/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.contactmomenten;

import java.net.URI;
import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.contactmomenten.exception.NotFoundExceptionMapper;
import net.atos.client.contactmomenten.exception.RuntimeExceptionMapper;
import net.atos.client.contactmomenten.model.ObjectContactMoment;
import net.atos.client.contactmomenten.model.ObjectcontactmomentList200Response;
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
@Path("api/v1/objectcontactmomenten")
public interface ObjectcontactmomentenClient {

    /**
     * Alle OBJECT-CONTACTMOMENT relaties opvragen.
     */
    @GET
    @Produces({"application/json", "application/problem+json"})
    public ObjectcontactmomentList200Response objectcontactmomentList(@QueryParam("object") URI _object,
                                                                      @QueryParam("contactmoment") URI contactmoment,
                                                                      @QueryParam("objectType") String objectType,
                                                                      @QueryParam("page") Integer page) throws ProcessingException;

    /**
     * Een specifiek OBJECT-CONTACTMOMENT relatie opvragen.
     */
    @GET
    @Path("/{uuid}")
    @Produces({"application/json", "application/problem+json"})
    public ObjectContactMoment objectcontactmomentRead(@PathParam("uuid") UUID uuid,
                                                       @HeaderParam("If-None-Match") String ifNoneMatch) throws ProcessingException;
}
