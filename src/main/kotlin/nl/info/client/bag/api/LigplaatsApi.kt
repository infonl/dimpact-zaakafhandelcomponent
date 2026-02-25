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
import nl.info.client.bag.model.generated.LigplaatsIOHal
import nl.info.client.bag.model.generated.LigplaatsIOHalCollection
import nl.info.client.bag.model.generated.LigplaatsIOLvcHalCollection
import nl.info.client.bag.model.generated.PointGeoJSON
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
@Path("/ligplaatsen")
@Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
interface LigplaatsApi {

    @POST
    @Consumes(APPLICATION_JSON)
    @Throws(ProcessingException::class)
    @Suppress("LongParameterList")
    fun ligplaatsGeometrie(
        pointGeoJSON: PointGeoJSON,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?,
        @QueryParam("expand") expand: String?,
        @HeaderParam("Content-Crs") contentCrs: String?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String
    ): LigplaatsIOHalCollection

    @GET
    @Path("/{identificatie}")
    @Throws(ProcessingException::class)
    @Suppress("LongParameterList")
    fun ligplaatsIdentificatie(
        @PathParam("identificatie") identificatie: String,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("expand") expand: String?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?
    ): LigplaatsIOHal

    @GET
    @Path("/{identificatie}/{versie}/{timestampRegistratieLv}")
    @Throws(ProcessingException::class)
    fun ligplaatsIdentificatieVoorkomen(
        @PathParam("identificatie") identificatie: String,
        @PathParam("versie") versie: Int,
        @PathParam("timestampRegistratieLv") timestampRegistratieLv: String,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String
    ): LigplaatsIOHal

    @GET
    @Path("/{identificatie}/lvc")
    @Throws(ProcessingException::class)
    fun ligplaatsLvcIdentificatie(
        @PathParam("identificatie") identificatie: String,
        @QueryParam("geheleLvc") @DefaultValue("false") geheleLvc: Boolean?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String
    ): LigplaatsIOLvcHalCollection

    @GET
    @Throws(ProcessingException::class)
    @Suppress("LongParameterList")
    fun zoekLigplaatsen(
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
    ): LigplaatsIOHalCollection
}
