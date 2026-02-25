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
import nl.info.client.bag.model.generated.AdresUitgebreidHal
import nl.info.client.bag.model.generated.AdresUitgebreidHalCollection
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
@Path("/adressenuitgebreid")
interface AdresUitgebreidApi {

    @GET
    @Path("/{nummeraanduidingIdentificatie}")
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    fun bevraagAdresUitgebreidMetNumId(
        @PathParam("nummeraanduidingIdentificatie") nummeraanduidingIdentificatie: String,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String,
        @QueryParam("inclusiefEindStatus") @DefaultValue("false") inclusiefEindStatus: Boolean?
    ): AdresUitgebreidHal

    @GET
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Throws(ProcessingException::class)
    @Suppress("LongParameterList")
    fun zoekAdresUitgebreid(
        @QueryParam("postcode") postcode: String?,
        @QueryParam("huisnummer") huisnummer: Int?,
        @QueryParam("huisnummertoevoeging") huisnummertoevoeging: String?,
        @QueryParam("huisletter") huisletter: String?,
        @QueryParam("exacteMatch") @DefaultValue("false") exacteMatch: Boolean?,
        @QueryParam("adresseerbaarObjectIdentificatie") adresseerbaarObjectIdentificatie: String?,
        @QueryParam("woonplaatsNaam") woonplaatsNaam: String?,
        @QueryParam("openbareRuimteNaam") openbareRuimteNaam: String?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String,
        @QueryParam("page") @DefaultValue("1") page: Int?,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int?,
        @QueryParam("q") q: String?,
        @QueryParam("inclusiefEindStatus") @DefaultValue("false") inclusiefEindStatus: Boolean?
    ): AdresUitgebreidHalCollection
}
