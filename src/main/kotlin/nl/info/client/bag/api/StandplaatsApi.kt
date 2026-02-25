/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.api

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType.APPLICATION_JSON
import net.atos.zac.util.MediaTypes
import nl.info.client.bag.BagClientService.Companion.DEFAULT_CRS
import nl.info.client.bag.exception.BagResponseExceptionMapper
import nl.info.client.bag.model.generated.PointGeoJSON
import nl.info.client.bag.model.generated.StandplaatsIOHal
import nl.info.client.bag.model.generated.StandplaatsIOHalCollection
import nl.info.client.bag.model.generated.StandplaatsIOLvcHalCollection
import nl.info.client.bag.util.BagClientHeadersFactory
import org.eclipse.microprofile.faulttolerance.Timeout
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@RegisterRestClient(configKey = "BAG-API-Client")
@RegisterClientHeaders(BagClientHeadersFactory::class)
@RegisterProvider(BagResponseExceptionMapper::class)
@Timeout(unit = ChronoUnit.SECONDS, value = 5)
@Path("/standplaatsen")
interface StandplaatsApi {

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun standplaatsGeometrie(
        pointGeoJSON: PointGeoJSON,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?,
        @QueryParam("expand") expand: String?,
        @HeaderParam("Content-Crs") contentCrs: String?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String
    ): StandplaatsIOHalCollection

    @GET
    @Path("/{identificatie}")
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun standplaatsIdentificatie(
        @PathParam("identificatie") identificatie: String,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("expand") expand: String?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?
    ): StandplaatsIOHal

    @GET
    @Path("/{identificatie}/{versie}/{timestampRegistratieLv}")
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun standplaatsIdentificatieVoorkomen(
        @PathParam("identificatie") identificatie: String,
        @PathParam("versie") versie: Int,
        @PathParam("timestampRegistratieLv") timestampRegistratieLv: String,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String
    ): StandplaatsIOHal

    @GET
    @Path("/{identificatie}/lvc")
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun standplaatsLvcIdentificatie(
        @PathParam("identificatie") identificatie: String,
        @QueryParam("geheleLvc") @DefaultValue("false") geheleLvc: Boolean?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String
    ): StandplaatsIOLvcHalCollection

    @GET
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun zoekStandplaatsen(
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?,
        @QueryParam("expand") expand: String?,
        @HeaderParam("Content-Crs") contentCrs: String?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String,
        @QueryParam("page") @DefaultValue("1") page: Int?,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int?,
        @QueryParam("point") point: PointGeoJSON?,
        @QueryParam("bbox") bbox: List<BigDecimal>?
    ): StandplaatsIOHalCollection
}
