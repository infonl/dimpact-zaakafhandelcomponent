/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.api

import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import net.atos.zac.util.MediaTypes
import nl.info.client.bag.exception.BagResponseExceptionMapper
import nl.info.client.bag.model.BevraagAdressenParameters
import nl.info.client.bag.model.generated.AdresIOHal
import nl.info.client.bag.model.generated.AdresIOHalCollection
import nl.info.client.bag.model.generated.ZoekResultaatHalCollection
import nl.info.client.bag.util.BagClientHeadersFactory
import nl.info.client.bag.util.JsonbConfiguration
import org.eclipse.microprofile.faulttolerance.Timeout
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.time.temporal.ChronoUnit

@RegisterRestClient(configKey = "BAG-API-Client")
@RegisterClientHeaders(BagClientHeadersFactory::class)
@RegisterProvider(BagResponseExceptionMapper::class)
@RegisterProvider(JsonbConfiguration::class)
@Timeout(unit = ChronoUnit.SECONDS, value = 5)
@Path("/adressen")
interface AdresApi {

    @GET
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun bevraagAdressen(@BeanParam parameters: BevraagAdressenParameters): AdresIOHalCollection

    @GET
    @Path("/{nummeraanduidingIdentificatie}")
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun bevraagAdressenMetNumId(
        @PathParam("nummeraanduidingIdentificatie") nummeraanduidingIdentificatie: String,
        @QueryParam("expand") expand: String?,
        @QueryParam("inclusiefEindStatus") @DefaultValue("false") inclusiefEindStatus: Boolean?
    ): AdresIOHal

    @GET
    @Path("/zoek")
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun zoek(
        @QueryParam("zoek") zoek: String?,
        @QueryParam("page") @DefaultValue("1") page: Int?,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int?
    ): ZoekResultaatHalCollection
}
