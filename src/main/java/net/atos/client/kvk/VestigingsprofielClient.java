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

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.kvk.exception.KvkRuntimeExceptionMapper;
import net.atos.client.kvk.util.KvkClientHeadersFactory;
import net.atos.client.kvk.vestigingsprofiel.model.generated.Vestiging;
import net.atos.zac.util.MediaTypes;

@RegisterRestClient(configKey = "KVK-API-Client")
@RegisterClientHeaders(KvkClientHeadersFactory.class)
@RegisterProvider(KvkRuntimeExceptionMapper.class)
@Produces({MediaTypes.MEDIA_TYPE_HAL_JSON})
@Path("api/v1/vestigingsprofielen")
public interface VestigingsprofielClient {

    /**
     * Voor een specifieke vestiging informatie opvragen.
     */
    @GET
    @Path("{vestigingsnummer}")
    Vestiging getVestigingByVestigingsnummer(
            @PathParam("vestigingsnummer") String vestigingsnummer,
            @QueryParam("geoData") @DefaultValue("false") Boolean geoData
    );

}
