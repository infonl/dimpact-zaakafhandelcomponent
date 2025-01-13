/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.or.objecttype;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.or.objecttypes.model.generated.ObjectType;
import net.atos.client.or.objecttypes.model.generated.ObjectVersion;
import net.atos.client.or.shared.exception.FoutExceptionMapper;
import net.atos.client.or.shared.exception.ORRuntimeResponseExceptionMapper;
import net.atos.client.or.shared.exception.ValidatieFoutExceptionMapper;

@RegisterRestClient(configKey = "Objecttypes-API-Client")
@RegisterClientHeaders(ObjecttypesClientHeadersFactory.class)
@RegisterProvider(FoutExceptionMapper.class)
@RegisterProvider(ValidatieFoutExceptionMapper.class)
@RegisterProvider(ORRuntimeResponseExceptionMapper.class)

@Produces(APPLICATION_JSON)
@Path("api/v2")
public interface ObjecttypesClient {

    @GET
    @Path("objecttypes")
    List<ObjectType> objecttypeList();

    @GET
    @Path("objecttypes/{objecttype-uuid}")
    ObjectType objecttypeRead(@PathParam("objecttype-uuid") final UUID objecttypeUUID);

    @GET
    @Path("objecttypes/{objecttype-uuid}/versions")
    List<ObjectVersion> objectversionList(@PathParam("objecttype-uuid") final UUID objecttypeUUID);

    @GET
    @Path("objecttypes/{objecttype-uuid}/versions/{version}")
    ObjectVersion objectversionRead(
            @PathParam("objecttype-uuid") final UUID objecttypeUUID,
            @PathParam("version") final Integer version
    );
}
