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

import net.atos.client.kvk.exception.RuntimeExceptionMapper;
import net.atos.client.kvk.util.KVKClientHeadersFactory;
import net.atos.client.kvk.vestigingsprofiel.model.Vestiging;

@RegisterRestClient(configKey = "KVK-API-Client")
@RegisterClientHeaders(KVKClientHeadersFactory.class)
@RegisterProvider(RuntimeExceptionMapper.class)
@Produces({"application/hal+json"})
@Path("api/v1/vestigingsprofielen")
public interface VestigingsprofielClient {

    /**
     * Voor een specifieke vestiging informatie opvragen.
     */
    @GET
    @Path("{vestigingsnummer}")
    Vestiging getVestigingByVestigingsnummer(
            @PathParam("vestigingsnummer") String vestigingsnummer,
            @QueryParam("geoData") @DefaultValue("false") Boolean geoData);
}
