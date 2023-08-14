/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

/**
 * API Vestigingsprofiel
 * Documentatie voor API Vestigingsprofiel.
 * <p>
 * The version of the OpenAPI document: 1.3
 */
package net.atos.client.kvk;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.annotation.RegisterProviders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.kvk.exception.RuntimeExceptionMapper;
import net.atos.client.kvk.util.KVKClientHeadersFactory;
import net.atos.client.kvk.vestigingsprofiel.model.Vestiging;

@RegisterRestClient(configKey = "KVK-API-Client")
@RegisterClientHeaders(KVKClientHeadersFactory.class)
@RegisterProviders({
        @RegisterProvider(RuntimeExceptionMapper.class)
})
@Produces({"application/hal+json"})
@Path("api/v1/vestigingsprofielen")
public interface VestigingsprofielClient {

    /**
     * Voor een specifieke vestiging informatie opvragen.
     */
    @GET
    @Path("{vestigingsnummer}")
    Vestiging getVestigingByVestigingsnummer(@PathParam("vestigingsnummer") String vestigingsnummer,
            @QueryParam("geoData") @DefaultValue("false") Boolean geoData);

}
