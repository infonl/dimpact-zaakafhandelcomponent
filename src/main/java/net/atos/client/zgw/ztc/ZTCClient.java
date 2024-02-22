/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.UUID;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.zgw.shared.exception.FoutExceptionMapper;
import net.atos.client.zgw.shared.exception.RuntimeExceptionMapper;
import net.atos.client.zgw.shared.exception.ValidatieFoutExceptionMapper;
import net.atos.client.zgw.shared.model.Results;
import net.atos.client.zgw.shared.util.JsonbConfiguration;
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory;
import net.atos.client.zgw.ztc.model.BesluittypeListParameters;
import net.atos.client.zgw.ztc.model.CatalogusListParameters;
import net.atos.client.zgw.ztc.model.EigenschapListParameters;
import net.atos.client.zgw.ztc.model.ResultaattypeListParameters;
import net.atos.client.zgw.ztc.model.RoltypeListParameters;
import net.atos.client.zgw.ztc.model.StatustypeListParameters;
import net.atos.client.zgw.ztc.model.ZaaktypeInformatieobjecttypeListParameters;
import net.atos.client.zgw.ztc.model.ZaaktypeListParameters;
import net.atos.client.zgw.ztc.model.generated.BesluitType;
import net.atos.client.zgw.ztc.model.generated.Catalogus;
import net.atos.client.zgw.ztc.model.generated.Eigenschap;
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType;
import net.atos.client.zgw.ztc.model.generated.ResultaatType;
import net.atos.client.zgw.ztc.model.generated.RolType;
import net.atos.client.zgw.ztc.model.generated.StatusType;
import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.client.zgw.ztc.model.generated.ZaakTypeInformatieObjectType;

/**
 * Note that this client should normally only be used by {@link ZTCClientService} and not directly
 * because of caching purposes.
 */
@RegisterRestClient(configKey = "ZGW-API-Client")
@RegisterClientHeaders(ZGWClientHeadersFactory.class)
@RegisterProvider(FoutExceptionMapper.class)
@RegisterProvider(ValidatieFoutExceptionMapper.class)
@RegisterProvider(RuntimeExceptionMapper.class)
@RegisterProvider(JsonbConfiguration.class)
@Path("catalogi/api/v1")
@Produces(APPLICATION_JSON)
public interface ZTCClient {

    @GET
    @Path("catalogussen")
    Results<Catalogus> catalogusList(@BeanParam final CatalogusListParameters parameters);

    @GET
    @Path("eigenschappen")
    Results<Eigenschap> eigenschapList(@BeanParam final EigenschapListParameters parameters);

    @GET
    @Path("informatieobjecttypen")
    Results<InformatieObjectType> informatieobjecttypeList();

    @GET
    @Path("resultaattypen/{uuid}")
    ResultaatType resultaattypeRead(@PathParam("uuid") final UUID uuid);

    @GET
    @Path("resultaattypen")
    Results<ResultaatType> resultaattypeList(
            @BeanParam final ResultaattypeListParameters parameters);

    @GET
    @Path("roltypen/{uuid}")
    RolType roltypeRead(@PathParam("uuid") final UUID uuid);

    @GET
    @Path("roltypen")
    Results<RolType> roltypeList(@BeanParam final RoltypeListParameters parameters);

    @GET
    @Path("statustypen")
    Results<StatusType> statustypeList(@BeanParam final StatustypeListParameters parameters);

    @GET
    @Path("zaaktype-informatieobjecttypen")
    Results<ZaakTypeInformatieObjectType> zaaktypeinformatieobjecttypeList(
            @BeanParam final ZaaktypeInformatieobjecttypeListParameters parameters);

    @GET
    @Path("statustypen/{uuid}")
    StatusType statustypeRead(@PathParam("uuid") final UUID uuid);

    @GET
    @Path("zaaktypen")
    Results<ZaakType> zaaktypeList(@BeanParam final ZaaktypeListParameters parameters);

    @GET
    @Path("zaaktypen/{uuid}")
    ZaakType zaaktypeRead(@PathParam("uuid") final UUID uuid);

    @GET
    @Path("informatieobjecttypen/{uuid}")
    InformatieObjectType informatieObjectTypeRead(@PathParam("uuid") UUID informatieObjectTypeUUID);

    @GET
    @Path("besluittypen")
    Results<BesluitType> besluittypeList(@BeanParam final BesluittypeListParameters parameters);

    @GET
    @Path("besluittypen/{uuid}")
    BesluitType besluittypeRead(@PathParam("uuid") final UUID uuid);
}
