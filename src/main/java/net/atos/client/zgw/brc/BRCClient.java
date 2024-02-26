/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.brc;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParams;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.zgw.brc.model.BesluitenListParameters;
import net.atos.client.zgw.brc.model.generated.Besluit;
import net.atos.client.zgw.brc.model.generated.BesluitInformatieObject;
import net.atos.client.zgw.shared.exception.FoutExceptionMapper;
import net.atos.client.zgw.shared.exception.RuntimeExceptionMapper;
import net.atos.client.zgw.shared.exception.ValidatieFoutExceptionMapper;
import net.atos.client.zgw.shared.model.Results;
import net.atos.client.zgw.shared.model.audit.AuditTrailRegel;
import net.atos.client.zgw.shared.util.JsonbConfiguration;
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory;

/**
 * BRC Client
 */
@RegisterRestClient(configKey = "ZGW-API-Client")
@RegisterClientHeaders(ZGWClientHeadersFactory.class)
@RegisterProvider(FoutExceptionMapper.class)
@RegisterProvider(ValidatieFoutExceptionMapper.class)
@RegisterProvider(RuntimeExceptionMapper.class)
@RegisterProvider(JsonbConfiguration.class)
@Path("besluiten/api/v1")
@Produces(APPLICATION_JSON)
public interface BRCClient {

    String ACCEPT_CRS = "Accept-Crs";

    String ACCEPT_CRS_VALUE = "EPSG:4326";

    String CONTENT_CRS = "Content-Crs";

    String CONTENT_CRS_VALUE = ACCEPT_CRS_VALUE;

    @GET
    @Path("besluiten")
    @ClientHeaderParams({
                         @ClientHeaderParam(name = ACCEPT_CRS, value = ACCEPT_CRS_VALUE),
                         @ClientHeaderParam(name = CONTENT_CRS, value = CONTENT_CRS_VALUE)})
    Results<Besluit> besluitList(@BeanParam final BesluitenListParameters parameters);

    @POST
    @Path("besluiten")
    @ClientHeaderParams({
                         @ClientHeaderParam(name = ACCEPT_CRS, value = ACCEPT_CRS_VALUE),
                         @ClientHeaderParam(name = CONTENT_CRS, value = CONTENT_CRS_VALUE)})
    Besluit besluitCreate(final Besluit besluit);

    @PUT
    @Path("besluiten/{uuid}")
    @ClientHeaderParams({
                         @ClientHeaderParam(name = ACCEPT_CRS, value = ACCEPT_CRS_VALUE),
                         @ClientHeaderParam(name = CONTENT_CRS, value = CONTENT_CRS_VALUE)})
    Besluit besluitUpdate(@PathParam("uuid") final UUID uuid, final Besluit besluit);

    @GET
    @Path("besluiten/{besluit_uuid}/audittrail")
    List<AuditTrailRegel> listAuditTrail(@PathParam("besluit_uuid") final UUID besluitUUID);

    @GET
    @Path("besluiten/{uuid}")
    @ClientHeaderParams({
                         @ClientHeaderParam(name = ACCEPT_CRS, value = ACCEPT_CRS_VALUE),
                         @ClientHeaderParam(name = CONTENT_CRS, value = CONTENT_CRS_VALUE)})
    Besluit besluitRead(@PathParam("uuid") final UUID uuid);

    @PATCH
    @Path("besluiten/{uuid}")
    @ClientHeaderParams({
                         @ClientHeaderParam(name = ACCEPT_CRS, value = ACCEPT_CRS_VALUE),
                         @ClientHeaderParam(name = CONTENT_CRS, value = CONTENT_CRS_VALUE)})
    Besluit besluitPartialUpdate(@PathParam("uuid") final UUID uuid, final Besluit besluit);

    @DELETE
    @Path("besluiten/{uuid}")
    Response besluitDelete(@PathParam("uuid") final UUID uuid);

    @GET
    @Path("besluitinformatieobjecten")
    List<BesluitInformatieObject> listBesluitInformatieobjectenByBesluit(@QueryParam("besluit") final URI besluit);

    @GET
    @Path("besluitinformatieobjecten")
    List<BesluitInformatieObject> listBesluitInformatieobjectenByInformatieObject(@QueryParam(
        "informatieobject") final URI informatieobject);

    @POST
    @Path("besluitinformatieobjecten")
    @ClientHeaderParams({
                         @ClientHeaderParam(name = ACCEPT_CRS, value = ACCEPT_CRS_VALUE),
                         @ClientHeaderParam(name = CONTENT_CRS, value = CONTENT_CRS_VALUE)})
    BesluitInformatieObject besluitinformatieobjectCreate(final BesluitInformatieObject besluitInformatieobject);

    @DELETE
    @Path("besluitinformatieobjecten/{uuid}")
    @ClientHeaderParams({
                         @ClientHeaderParam(name = ACCEPT_CRS, value = ACCEPT_CRS_VALUE),
                         @ClientHeaderParam(name = CONTENT_CRS, value = CONTENT_CRS_VALUE)})
    BesluitInformatieObject besluitinformatieobjectDelete(@PathParam("uuid") final UUID uuid);
}
