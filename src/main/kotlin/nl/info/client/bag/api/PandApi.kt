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
import nl.info.client.bag.model.generated.BouwjaarFilter
import nl.info.client.bag.model.generated.PandIOHal
import nl.info.client.bag.model.generated.PandIOHalCollection
import nl.info.client.bag.model.generated.PandIOLvcHalCollection
import nl.info.client.bag.model.generated.PointGeoJSON
import nl.info.client.bag.model.generated.StatusPand
import nl.info.client.bag.util.BagClientHeadersFactory
import nl.info.client.bag.util.JsonbConfiguration
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
@RegisterProvider(JsonbConfiguration::class)
@Timeout(unit = ChronoUnit.SECONDS, value = 5)
@Path("/panden")
@Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
interface PandApi {

    @POST
    @Consumes(APPLICATION_JSON)
    @Throws(ProcessingException::class)
    @Suppress("LongParameterList")
    fun pandGeometrie(
        pointGeoJSON: PointGeoJSON,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?,
        @HeaderParam("Content-Crs") contentCrs: String?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String
    ): PandIOHalCollection

    @GET
    @Path("/{identificatie}")
    @Throws(ProcessingException::class)
    fun pandIdentificatie(
        @PathParam("identificatie") identificatie: String,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @HeaderParam("Accept-Crs") @DefaultValue("epsg:28992") acceptCrs: String,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?
    ): PandIOHal

    @GET
    @Path("/{identificatie}/{versie}/{timestampRegistratieLv}")
    @Throws(ProcessingException::class)
    fun pandIdentificatieVoorkomen(
        @PathParam("identificatie") identificatie: String,
        @PathParam("versie") versie: Int,
        @PathParam("timestampRegistratieLv") timestampRegistratieLv: String,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String
    ): PandIOHal

    @GET
    @Path("/{identificatie}/lvc")
    @Throws(ProcessingException::class)
    fun pandLvcIdentificatie(
        @PathParam("identificatie") identificatie: String,
        @QueryParam("geheleLvc") @DefaultValue("false") geheleLvc: Boolean?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String
    ): PandIOLvcHalCollection

    @GET
    @Throws(ProcessingException::class)
    @Suppress("LongParameterList")
    fun zoekPanden(
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?,
        @HeaderParam("Content-Crs") contentCrs: String?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String,
        @QueryParam("page") @DefaultValue("1") page: Int?,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int?,
        @QueryParam("point") point: PointGeoJSON?,
        @QueryParam("bbox") bbox: List<BigDecimal>?,
        @QueryParam("statusPand") statusPand: List<StatusPand>?,
        @QueryParam("geconstateerd") geconstateerd: Boolean?,
        @QueryParam("bouwjaar") bouwjaar: BouwjaarFilter?,
        @QueryParam("adresseerbaarObjectIdentificatie") adresseerbaarObjectIdentificatie: String?,
        @QueryParam("nummeraanduidingIdentificatie") nummeraanduidingIdentificatie: String?
    ): PandIOHalCollection
}
