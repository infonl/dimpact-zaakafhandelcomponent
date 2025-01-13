/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.or.object;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParams;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.or.objects.model.generated.ModelObject;
import net.atos.client.or.shared.exception.FoutExceptionMapper;
import net.atos.client.or.shared.exception.ORRuntimeResponseExceptionMapper;
import net.atos.client.or.shared.exception.ValidatieFoutExceptionMapper;

/**
 *
 */
@RegisterRestClient(configKey = "Objects-API-Client")
@RegisterClientHeaders(ObjectsClientHeadersFactory.class)
@RegisterProvider(FoutExceptionMapper.class)
@RegisterProvider(ValidatieFoutExceptionMapper.class)
@RegisterProvider(ORRuntimeResponseExceptionMapper.class)
@Produces(APPLICATION_JSON)
@Path("api/v2")
public interface ObjectsClient {

    String ACCEPT_CRS = "Accept-Crs";

    String ACCEPT_CRS_VALUE = "EPSG:4326";

    String CONTENT_CRS = "Content-Crs";

    String CONTENT_CRS_VALUE = ACCEPT_CRS_VALUE;

    @POST
    @Path("objects")
    @ClientHeaderParams({
                         @ClientHeaderParam(name = ACCEPT_CRS, value = ACCEPT_CRS_VALUE),
                         @ClientHeaderParam(name = CONTENT_CRS, value = CONTENT_CRS_VALUE)})
    ModelObject objectCreate(final ModelObject object);

    @GET
    @Path("objects/{object-uuid}")
    ModelObject objectRead(@PathParam("object-uuid") final UUID objectUUID);

    @PUT
    @Path("objects/{object-uuid}")
    @ClientHeaderParams({
                         @ClientHeaderParam(name = ACCEPT_CRS, value = ACCEPT_CRS_VALUE),
                         @ClientHeaderParam(name = CONTENT_CRS, value = CONTENT_CRS_VALUE)})
    ModelObject objectUpdate(@PathParam("object-uuid") final UUID objectUUID, final ModelObject object);
}
