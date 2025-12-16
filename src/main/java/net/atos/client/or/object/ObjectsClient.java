/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.or.object;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.or.shared.exception.FoutExceptionMapper;
import net.atos.client.or.shared.exception.ORRuntimeResponseExceptionMapper;
import net.atos.client.or.shared.exception.ValidatieFoutExceptionMapper;
import nl.info.client.or.objects.model.generated.ModelObject;

@RegisterRestClient(configKey = "Objects-API-Client")
@RegisterClientHeaders(ObjectsClientHeadersFactory.class)
@RegisterProvider(FoutExceptionMapper.class)
@RegisterProvider(ValidatieFoutExceptionMapper.class)
@RegisterProvider(ORRuntimeResponseExceptionMapper.class)
@Produces(APPLICATION_JSON)
@Path("api/v2")
public interface ObjectsClient {
    String ACCEPT_CRS_VALUE = "EPSG:4326";

    @GET
    @Path("objects/{object-uuid}")
    ModelObject objectRead(@PathParam("object-uuid") final UUID objectUUID);
}
