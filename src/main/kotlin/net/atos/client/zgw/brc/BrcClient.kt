/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.brc

import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.atos.client.zgw.brc.exception.BrcResponseExceptionMapper
import net.atos.client.zgw.brc.model.BesluitenListParameters
import net.atos.client.zgw.brc.model.generated.Besluit
import net.atos.client.zgw.brc.model.generated.BesluitInformatieObject
import net.atos.client.zgw.shared.exception.ZgwErrorExceptionMapper
import net.atos.client.zgw.shared.exception.ZgwValidationErrorResponseExceptionMapper
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.shared.model.audit.AuditTrailRegel
import net.atos.client.zgw.shared.util.JsonbConfiguration
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParams
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.net.URI
import java.util.UUID

/**
 * BRC Client
 */
@RegisterRestClient(configKey = "ZGW-API-Client")
@RegisterClientHeaders(ZGWClientHeadersFactory::class)
@RegisterProvider(ZgwErrorExceptionMapper::class)
@RegisterProvider(ZgwValidationErrorResponseExceptionMapper::class)
@RegisterProvider(BrcResponseExceptionMapper::class)
@RegisterProvider(JsonbConfiguration::class)
@Path("besluiten/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Suppress("TooManyFunctions")
interface BrcClient {
    companion object {
        const val ACCEPT_CRS = "Accept-Crs"
        const val ACCEPT_CRS_VALUE = "EPSG:4326"
        const val CONTENT_CRS = "Content-Crs"
        const val CONTENT_CRS_VALUE = ACCEPT_CRS_VALUE
    }

    @GET
    @Path("besluiten")
    @ClientHeaderParams(
        ClientHeaderParam(name = ACCEPT_CRS, value = arrayOf(ACCEPT_CRS_VALUE)),
        ClientHeaderParam(name = CONTENT_CRS, value = arrayOf(CONTENT_CRS_VALUE))
    )
    fun besluitList(@BeanParam parameters: BesluitenListParameters): Results<Besluit>

    @POST
    @Path("besluiten")
    @ClientHeaderParams(
        ClientHeaderParam(name = ACCEPT_CRS, value = arrayOf(ACCEPT_CRS_VALUE)),
        ClientHeaderParam(name = CONTENT_CRS, value = arrayOf(CONTENT_CRS_VALUE))
    )
    fun besluitCreate(besluit: Besluit): Besluit

    @PUT
    @Path("besluiten/{uuid}")
    @ClientHeaderParams(
        ClientHeaderParam(name = ACCEPT_CRS, value = arrayOf(ACCEPT_CRS_VALUE)),
        ClientHeaderParam(name = CONTENT_CRS, value = arrayOf(CONTENT_CRS_VALUE))
    )
    fun besluitUpdate(@PathParam("uuid") uuid: UUID, besluit: Besluit): Besluit

    @GET
    @Path("besluiten/{besluit_uuid}/audittrail")
    fun listAuditTrail(@PathParam("besluit_uuid") besluitUUID: UUID): List<AuditTrailRegel>

    @GET
    @Path("besluiten/{uuid}")
    @ClientHeaderParams(
        ClientHeaderParam(name = ACCEPT_CRS, value = arrayOf(ACCEPT_CRS_VALUE)),
        ClientHeaderParam(name = CONTENT_CRS, value = arrayOf(CONTENT_CRS_VALUE))
    )
    fun besluitRead(@PathParam("uuid") uuid: UUID): Besluit

    @PATCH
    @Path("besluiten/{uuid}")
    @ClientHeaderParams(
        ClientHeaderParam(name = ACCEPT_CRS, value = arrayOf(ACCEPT_CRS_VALUE)),
        ClientHeaderParam(name = CONTENT_CRS, value = arrayOf(CONTENT_CRS_VALUE))
    )
    fun besluitPartialUpdate(@PathParam("uuid") uuid: UUID, besluit: Besluit): Besluit

    @DELETE
    @Path("besluiten/{uuid}")
    fun besluitDelete(@PathParam("uuid") uuid: UUID): Response

    @GET
    @Path("besluitinformatieobjecten")
    fun listBesluitInformatieobjectenByBesluit(@QueryParam("besluit") besluit: URI): List<BesluitInformatieObject>

    @GET
    @Path("besluitinformatieobjecten")
    fun listBesluitInformatieobjectenByInformatieObject(
        @QueryParam("informatieobject") informatieobject: URI
    ): List<BesluitInformatieObject>

    @POST
    @Path("besluitinformatieobjecten")
    @ClientHeaderParams(
        ClientHeaderParam(name = ACCEPT_CRS, value = arrayOf(ACCEPT_CRS_VALUE)),
        ClientHeaderParam(name = CONTENT_CRS, value = arrayOf(CONTENT_CRS_VALUE))
    )
    fun besluitinformatieobjectCreate(besluitInformatieobject: BesluitInformatieObject): BesluitInformatieObject

    @DELETE
    @Path("besluitinformatieobjecten/{uuid}")
    @ClientHeaderParams(
        ClientHeaderParam(name = ACCEPT_CRS, value = arrayOf(ACCEPT_CRS_VALUE)),
        ClientHeaderParam(name = CONTENT_CRS, value = arrayOf(CONTENT_CRS_VALUE))
    )
    fun besluitinformatieobjectDelete(@PathParam("uuid") uuid: UUID): BesluitInformatieObject
}
