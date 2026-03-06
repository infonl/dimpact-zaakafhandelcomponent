/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.api

import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import net.atos.zac.util.MediaTypes
import nl.info.client.bag.exception.BagResponseExceptionMapper
import nl.info.client.bag.model.generated.NummeraanduidingIOHal
import nl.info.client.bag.model.generated.NummeraanduidingIOHalCollection
import nl.info.client.bag.model.generated.NummeraanduidingIOLvcHalCollection
import nl.info.client.bag.util.BagClientHeadersFactory
import nl.info.client.bag.util.JsonbConfiguration
import org.eclipse.microprofile.faulttolerance.Timeout
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@RegisterRestClient(configKey = "BAG-API-Client")
@RegisterClientHeaders(BagClientHeadersFactory::class)
@RegisterProvider(BagResponseExceptionMapper::class)
@RegisterProvider(JsonbConfiguration::class)
@Timeout(unit = ChronoUnit.SECONDS, value = 5)
@Path("/nummeraanduidingen")
@Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
interface NummeraanduidingApi {

    @GET
    @Path("/{nummeraanduidingIdentificatie}")
    @Throws(ProcessingException::class)
    fun nummeraanduidingIdentificatie(
        @PathParam("nummeraanduidingIdentificatie") nummeraanduidingIdentificatie: String,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("expand") expand: String?,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?
    ): NummeraanduidingIOHal

    @GET
    @Path("/{nummeraanduidingIdentificatie}/{versie}/{timestampRegistratieLv}")
    @Throws(ProcessingException::class)
    fun nummeraanduidingIdentificatieVoorkomen(
        @PathParam("nummeraanduidingIdentificatie") nummeraanduidingIdentificatie: String,
        @PathParam("versie") versie: Int,
        @PathParam("timestampRegistratieLv") timestampRegistratieLv: String
    ): NummeraanduidingIOHal

    @GET
    @Path("/{nummeraanduidingIdentificatie}/lvc")
    @Throws(ProcessingException::class)
    fun nummeraanduidingLvcIdentificatie(
        @PathParam("nummeraanduidingIdentificatie") nummeraanduidingIdentificatie: String,
        @QueryParam("geheleLvc") @DefaultValue("false") geheleLvc: Boolean?
    ): NummeraanduidingIOLvcHalCollection

    @GET
    @Throws(ProcessingException::class)
    @Suppress("LongParameterList")
    fun zoekNummeraanduiding(
        @QueryParam("postcode") postcode: String?,
        @QueryParam("huisnummer") huisnummer: Int?,
        @QueryParam("huisnummertoevoeging") huisnummertoevoeging: String?,
        @QueryParam("huisletter") huisletter: String?,
        @QueryParam("exacteMatch") @DefaultValue("false") exacteMatch: Boolean?,
        @QueryParam("woonplaatsNaam") woonplaatsNaam: String?,
        @QueryParam("openbareRuimteNaam") openbareRuimteNaam: String?,
        @QueryParam("openbareRuimteIdentificatie") openbareRuimteIdentificatie: String?,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("page") @DefaultValue("1") page: Int?,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int?,
        @QueryParam("expand") expand: String?,
        @QueryParam("pandIdentificatie") pandIdentificatie: String?
    ): NummeraanduidingIOHalCollection
}
