/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.api

import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import net.atos.zac.util.MediaTypes
import nl.info.client.bag.BagClientService.Companion.DEFAULT_CRS
import nl.info.client.bag.exception.BagResponseExceptionMapper
import nl.info.client.bag.model.generated.Gebruiksdoel
import nl.info.client.bag.model.generated.OppervlakteFilter
import nl.info.client.bag.model.generated.VerblijfsobjectIOHal
import nl.info.client.bag.model.generated.VerblijfsobjectIOHalCollection
import nl.info.client.bag.model.generated.VerblijfsobjectIOLvcHalCollection
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
@Path("/verblijfsobjecten")
interface VerblijfsobjectApi {

    @GET
    @Path("/{identificatie}")
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun verblijfsobjectIdentificatie(
        @PathParam("identificatie") identificatie: String,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("expand") expand: String?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?
    ): VerblijfsobjectIOHal

    @GET
    @Path("/{identificatie}/{versie}/{timestampRegistratieLv}")
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun verblijfsobjectIdentificatieVoorkomen(
        @PathParam("identificatie") identificatie: String,
        @PathParam("versie") versie: Int,
        @PathParam("timestampRegistratieLv") timestampRegistratieLv: String,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String
    ): VerblijfsobjectIOHal

    @GET
    @Path("/{identificatie}/lvc")
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun verblijfsobjectLvcIdentificatie(
        @PathParam("identificatie") identificatie: String,
        @QueryParam("geheleLvc") @DefaultValue("false") geheleLvc: Boolean?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String
    ): VerblijfsobjectIOLvcHalCollection

    @GET
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun zoekVerblijfsobjecten(
        @QueryParam("pandIdentificatie") pandIdentificatie: String?,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("expand") expand: String?,
        @QueryParam("page") @DefaultValue("1") page: Int?,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String,
        @HeaderParam("Content-Crs") contentCrs: String?,
        @QueryParam("bbox") bbox: List<BigDecimal>?,
        @QueryParam("geconstateerd") geconstateerd: Boolean?,
        @QueryParam("oppervlakte") oppervlakte: OppervlakteFilter?,
        @QueryParam("gebruiksdoelen") gebruiksdoelen: List<Gebruiksdoel>?
    ): VerblijfsobjectIOHalCollection
}
