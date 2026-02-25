/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.api

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import net.atos.zac.util.MediaTypes
import nl.info.client.bag.exception.BagResponseExceptionMapper
import nl.info.client.bag.model.generated.BronhouderHal
import nl.info.client.bag.model.generated.BronhouderHalCollection
import nl.info.client.bag.util.BagClientHeadersFactory
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
@Timeout(unit = ChronoUnit.SECONDS, value = 5)
@Path("/bronhouders")
interface BronhouderApi {

    @GET
    @Path("/{identificatie}")
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun bevragenBronhouder(
        @PathParam("identificatie") identificatie: String,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?
    ): BronhouderHal

    @GET
    @Path("/{identificatie}/{versie}/{timestampRegistratieLv}")
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun bronhouderIdentificatieVoorkomen(
        @PathParam("identificatie") identificatie: String,
        @PathParam("versie") versie: Int,
        @PathParam("timestampRegistratieLv") timestampRegistratieLv: String
    ): BronhouderHal

    @GET
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun zoekBronhouder(
        @QueryParam("woonplaatsIdentificatie") woonplaatsIdentificatie: String?,
        @QueryParam("objectIdentificatie") objectIdentificatie: String?,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?
    ): BronhouderHalCollection
}
