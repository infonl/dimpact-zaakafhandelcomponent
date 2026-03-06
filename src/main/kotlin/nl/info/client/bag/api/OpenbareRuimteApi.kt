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
import nl.info.client.bag.model.generated.OpenbareRuimteIOHal
import nl.info.client.bag.model.generated.OpenbareRuimteIOHalCollection
import nl.info.client.bag.model.generated.OpenbareRuimteIOLvcHalCollection
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
@Path("/openbareruimten")
@Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
interface OpenbareRuimteApi {

    @GET
    @Path("/{openbareRuimteIdentificatie}")
    @Throws(ProcessingException::class)
    fun openbareruimteIdentificatie(
        @PathParam("openbareRuimteIdentificatie") openbareRuimteIdentificatie: String,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("expand") expand: String?,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?
    ): OpenbareRuimteIOHal

    @GET
    @Path("/{openbareRuimteIdentificatie}/{versie}/{timestampRegistratieLv}")
    @Throws(ProcessingException::class)
    fun openbareruimteIdentificatieVoorkomen(
        @PathParam("openbareRuimteIdentificatie") openbareRuimteIdentificatie: String,
        @PathParam("versie") versie: Int,
        @PathParam("timestampRegistratieLv") timestampRegistratieLv: String
    ): OpenbareRuimteIOHal

    @GET
    @Path("/{openbareRuimteIdentificatie}/lvc")
    @Throws(ProcessingException::class)
    fun openbareruimteLvcIdentificatie(
        @PathParam("openbareRuimteIdentificatie") openbareRuimteIdentificatie: String,
        @QueryParam("geheleLvc") @DefaultValue("false") geheleLvc: Boolean?
    ): OpenbareRuimteIOLvcHalCollection

    @GET
    @Throws(ProcessingException::class)
    @Suppress("LongParameterList")
    fun zoekOpenbareRuimten(
        @QueryParam("woonplaatsNaam") woonplaatsNaam: String?,
        @QueryParam("openbareRuimteNaam") openbareRuimteNaam: String?,
        @QueryParam("woonplaatsIdentificatie") woonplaatsIdentificatie: String?,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("page") @DefaultValue("1") page: Int?,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int?,
        @QueryParam("expand") expand: String?
    ): OpenbareRuimteIOHalCollection
}
