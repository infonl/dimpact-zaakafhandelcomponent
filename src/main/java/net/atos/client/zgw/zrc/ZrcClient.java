/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

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

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParams;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.zgw.shared.exception.ZgwErrorExceptionMapper;
import net.atos.client.zgw.shared.exception.ZgwValidationErrorResponseExceptionMapper;
import net.atos.client.zgw.shared.model.Results;
import net.atos.client.zgw.shared.model.audit.ZRCAuditTrailRegel;
import net.atos.client.zgw.shared.util.JsonbConfiguration;
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory;
import net.atos.client.zgw.zrc.exception.ZrcResponseExceptionMapper;
import net.atos.client.zgw.zrc.model.Rol;
import net.atos.client.zgw.zrc.model.RolListParameters;
import net.atos.client.zgw.zrc.model.Status;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.client.zgw.zrc.model.ZaakInformatieobjectListParameters;
import net.atos.client.zgw.zrc.model.ZaakListParameters;
import net.atos.client.zgw.zrc.model.ZaakUuid;
import net.atos.client.zgw.zrc.model.generated.Resultaat;
import net.atos.client.zgw.zrc.model.generated.ZaakEigenschap;
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectListParameters;

@RegisterRestClient(configKey = "ZGW-API-Client")
@RegisterClientHeaders(ZGWClientHeadersFactory.class)
@RegisterProvider(ZgwErrorExceptionMapper.class)
@RegisterProvider(ZgwValidationErrorResponseExceptionMapper.class)
@RegisterProvider(ZrcResponseExceptionMapper.class)
@RegisterProvider(JsonbConfiguration.class)
@Path("zaken/api/v1")
@Produces(APPLICATION_JSON)
public interface ZrcClient {

    String ACCEPT_CRS = "Accept-Crs";

    String ACCEPT_CRS_VALUE = "EPSG:4326";

    String CONTENT_CRS = "Content-Crs";

    String CONTENT_CRS_VALUE = ACCEPT_CRS_VALUE;

    @GET
    @Path("zaken")
    @ClientHeaderParams({
                         @ClientHeaderParam(name = ACCEPT_CRS, value = ACCEPT_CRS_VALUE),
                         @ClientHeaderParam(name = CONTENT_CRS, value = CONTENT_CRS_VALUE)})
    Results<Zaak> zaakList(@BeanParam final ZaakListParameters parameters);

    @GET
    @Path("zaken")
    @ClientHeaderParams({
                         @ClientHeaderParam(name = ACCEPT_CRS, value = ACCEPT_CRS_VALUE),
                         @ClientHeaderParam(name = CONTENT_CRS, value = CONTENT_CRS_VALUE)})
    Results<ZaakUuid> zaakListUuids(@BeanParam final ZaakListParameters parameters);

    @POST
    @Path("zaken")
    @ClientHeaderParams({
                         @ClientHeaderParam(name = ACCEPT_CRS, value = ACCEPT_CRS_VALUE),
                         @ClientHeaderParam(name = CONTENT_CRS, value = CONTENT_CRS_VALUE)})
    Zaak zaakCreate(final Zaak zaak);

    @PATCH
    @Path("zaken/{uuid}")
    @ClientHeaderParams({
                         @ClientHeaderParam(name = ACCEPT_CRS, value = ACCEPT_CRS_VALUE),
                         @ClientHeaderParam(name = CONTENT_CRS, value = CONTENT_CRS_VALUE)})
    Zaak zaakPartialUpdate(@PathParam("uuid") final UUID uuid, final Zaak zaak);

    @GET
    @Path("zaken/{uuid}")
    @ClientHeaderParams({
                         @ClientHeaderParam(name = ACCEPT_CRS, value = ACCEPT_CRS_VALUE),
                         @ClientHeaderParam(name = CONTENT_CRS, value = CONTENT_CRS_VALUE)})
    Zaak zaakRead(@PathParam("uuid") final UUID uuid);

    @GET
    @Path("rollen")
    Results<Rol<?>> rolList(@BeanParam final RolListParameters parameters);

    @POST
    @Path("rollen")
    Rol<?> rolCreate(final Rol<?> rol);

    @DELETE
    @Path("rollen/{uuid}")
    void rolDelete(@PathParam("uuid") final UUID uuid);

    @GET
    @Path("rollen/{uuid}")
    Rol<?> rolRead(@PathParam("uuid") final UUID uuid);

    @GET
    @Path("zaakinformatieobjecten")
    List<ZaakInformatieobject> zaakinformatieobjectList(@BeanParam final ZaakInformatieobjectListParameters parameters);

    @POST
    @Path("zaakinformatieobjecten")
    ZaakInformatieobject zaakinformatieobjectCreate(final ZaakInformatieobject zaakInformatieObject);

    @DELETE
    @Path("zaakinformatieobjecten/{uuid}")
    void zaakinformatieobjectDelete(@PathParam("uuid") final UUID uuid);

    @POST
    @Path("statussen")
    Status statusCreate(final Status status);

    @POST
    @Path("resultaten")
    Resultaat resultaatCreate(final Resultaat resultaat);

    @PUT
    @Path("resultaten/{uuid}")
    Resultaat resultaatUpdate(@PathParam("uuid") final UUID resultaatUUID, final Resultaat resultaat);

    @DELETE
    @Path("resultaten/{uuid}")
    void resultaatDelete(@PathParam("uuid") final UUID uuid);

    @GET
    @Path("zaken/{zaak_uuid}/zaakeigenschappen")
    List<ZaakEigenschap> zaakeigenschapList(@PathParam("zaak_uuid") final UUID zaakUUID);

    @POST
    @Path("zaken/{zaak_uuid}/zaakeigenschappen")
    ZaakEigenschap zaakeigenschapCreate(@PathParam("zaak_uuid") final UUID zaakUUID, final ZaakEigenschap zaakeigenschap);

    @GET
    @Path("zaakobjecten")
    Results<Zaakobject> zaakobjectList(@BeanParam final ZaakobjectListParameters zaakobjectListParameters);

    @POST
    @Path("zaakobjecten")
    Zaakobject zaakobjectCreate(final Zaakobject zaakobject);

    @DELETE
    @Path("zaakobjecten/{uuid}")
    void zaakobjectDelete(@PathParam("uuid") final UUID uuid);

    @GET
    @Path("zaakobjecten/{uuid}")
    Zaakobject zaakobjectRead(@PathParam("uuid") final UUID uuid);

    @GET
    @Path("zaken/{zaak_uuid}/audittrail")
    List<ZRCAuditTrailRegel> listAuditTrail(@PathParam("zaak_uuid") final UUID zaakUUID);

    @GET
    @Path("zaakinformatieobjecten/{uuid}")
    ZaakInformatieobject zaakinformatieobjectRead(@PathParam("uuid") UUID zaakinformatieobjectUUID);

}
