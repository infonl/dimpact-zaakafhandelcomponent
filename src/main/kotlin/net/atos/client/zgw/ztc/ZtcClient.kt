/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc

import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.client.zgw.shared.exception.ZgwErrorExceptionMapper
import net.atos.client.zgw.shared.exception.ZgwValidationErrorResponseExceptionMapper
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.shared.util.JsonbConfiguration
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory
import net.atos.client.zgw.ztc.exception.ZtcResponseExceptionMapper
import net.atos.client.zgw.ztc.model.BesluittypeListParameters
import net.atos.client.zgw.ztc.model.CatalogusListParameters
import net.atos.client.zgw.ztc.model.EigenschapListParameters
import net.atos.client.zgw.ztc.model.ResultaattypeListParameters
import net.atos.client.zgw.ztc.model.RoltypeListGeneriekParameters
import net.atos.client.zgw.ztc.model.RoltypeListParameters
import net.atos.client.zgw.ztc.model.StatustypeListParameters
import net.atos.client.zgw.ztc.model.ZaaktypeInformatieobjecttypeListParameters
import net.atos.client.zgw.ztc.model.ZaaktypeListParameters
import net.atos.client.zgw.ztc.model.generated.BesluitType
import net.atos.client.zgw.ztc.model.generated.Catalogus
import net.atos.client.zgw.ztc.model.generated.Eigenschap
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType
import net.atos.client.zgw.ztc.model.generated.ResultaatType
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.client.zgw.ztc.model.generated.StatusType
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.client.zgw.ztc.model.generated.ZaakTypeInformatieObjectType
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.util.UUID

/**
 * Note that this client should normally only be used by [ZtcClientService] and not directly
 * because of caching purposes.
 */
@RegisterRestClient(configKey = "ZGW-API-Client")
@RegisterClientHeaders(ZGWClientHeadersFactory::class)
@RegisterProvider(ZgwErrorExceptionMapper::class)
@RegisterProvider(ZgwValidationErrorResponseExceptionMapper::class)
@RegisterProvider(ZtcResponseExceptionMapper::class)
@RegisterProvider(JsonbConfiguration::class)
@Path("catalogi/api/v1")
@Produces(
    MediaType.APPLICATION_JSON
)
@Suppress("TooManyFunctions")
interface ZtcClient {
    @GET
    @Path("catalogussen")
    fun catalogusList(@BeanParam parameters: CatalogusListParameters): Results<Catalogus>

    @GET
    @Path("eigenschappen")
    fun eigenschapList(@BeanParam parameters: EigenschapListParameters): Results<Eigenschap>

    @GET
    @Path("informatieobjecttypen")
    fun informatieobjecttypeList(): Results<InformatieObjectType>

    @GET
    @Path("resultaattypen/{uuid}")
    fun resultaattypeRead(@PathParam("uuid") uuid: UUID): ResultaatType

    @GET
    @Path("resultaattypen")
    fun resultaattypeList(@BeanParam parameters: ResultaattypeListParameters): Results<ResultaatType>

    @GET
    @Path("roltypen/{uuid}")
    fun roltypeRead(@PathParam("uuid") uuid: UUID): RolType

    @GET
    @Path("roltypen")
    fun roltypeList(@BeanParam parameters: RoltypeListParameters? = null): Results<RolType>

    @GET
    @Path("roltypen")
    fun roltypeListGeneriek(@BeanParam parameters: RoltypeListGeneriekParameters? = null): Results<RolType>

    @GET
    @Path("statustypen")
    fun statustypeList(@BeanParam parameters: StatustypeListParameters): Results<StatusType>

    @GET
    @Path("zaaktype-informatieobjecttypen")
    fun zaaktypeinformatieobjecttypeList(
        @BeanParam parameters: ZaaktypeInformatieobjecttypeListParameters
    ): Results<ZaakTypeInformatieObjectType>

    @GET
    @Path("statustypen/{uuid}")
    fun statustypeRead(@PathParam("uuid") uuid: UUID): StatusType

    @GET
    @Path("zaaktypen")
    fun zaaktypeList(@BeanParam parameters: ZaaktypeListParameters): Results<ZaakType>

    @GET
    @Path("zaaktypen/{uuid}")
    fun zaaktypeRead(@PathParam("uuid") uuid: UUID): ZaakType

    @GET
    @Path("informatieobjecttypen/{uuid}")
    fun informatieObjectTypeRead(@PathParam("uuid") informatieObjectTypeUUID: UUID): InformatieObjectType

    @GET
    @Path("besluittypen")
    fun besluittypeList(@BeanParam parameters: BesluittypeListParameters): Results<BesluitType>

    @GET
    @Path("besluittypen/{uuid}")
    fun besluittypeRead(@PathParam("uuid") uuid: UUID): BesluitType
}
