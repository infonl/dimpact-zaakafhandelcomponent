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
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import net.atos.zac.util.MediaTypes
import nl.info.client.bag.BagClientService.Companion.DEFAULT_CRS
import nl.info.client.bag.exception.BagResponseExceptionMapper
import nl.info.client.bag.model.generated.AdresseerbaarObjectIOHal
import nl.info.client.bag.model.generated.AdresseerbaarObjectLvcIOHalCollection
import nl.info.client.bag.model.generated.AdresseerbareObjectenIOHalCollection
import nl.info.client.bag.model.generated.Gebruiksdoel
import nl.info.client.bag.model.generated.OppervlakteFilter
import nl.info.client.bag.model.generated.TypeAdresseerbaarObject
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
@Path("/adresseerbareobjecten")
interface AdresseerbaarObjectApi {

    @GET
    @Path("/{adresseerbaarObjectIdentificatie}")
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Suppress("LongParameterList")
    fun bevragenAdresseerbaarObject(
        @PathParam("adresseerbaarObjectIdentificatie") adresseerbaarObjectIdentificatie: String,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("expand") expand: String?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?
    ): AdresseerbaarObjectIOHal

    @GET
    @Path("/{adresseerbaarObjectIdentificatie}/lvc")
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    fun bevragenAdresseerbaarObjectLvc(
        @PathParam("adresseerbaarObjectIdentificatie") adresseerbaarObjectIdentificatie: String,
        @QueryParam("geheleLvc") @DefaultValue("false") geheleLvc: Boolean?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String
    ): AdresseerbaarObjectLvcIOHalCollection

    @GET
    @Produces(MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON)
    @Suppress("LongParameterList")
    fun zoekAdresseerbareObjecten(
        @QueryParam("nummeraanduidingIdentificatie") nummeraanduidingIdentificatie: String?,
        @QueryParam("huidig") @DefaultValue("false") huidig: Boolean?,
        @QueryParam("geldigOp") geldigOp: LocalDate?,
        @QueryParam("beschikbaarOp") beschikbaarOp: OffsetDateTime?,
        @QueryParam("expand") expand: String?,
        @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) acceptCrs: String,
        @HeaderParam("Content-Crs") contentCrs: String?,
        @QueryParam("page") @DefaultValue("1") page: Int?,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int?,
        @QueryParam("bbox") bbox: List<BigDecimal>?,
        @QueryParam("geconstateerd") geconstateerd: Boolean?,
        @QueryParam("oppervlakte") oppervlakte: OppervlakteFilter?,
        @QueryParam("gebruiksdoelen") gebruiksdoelen: List<Gebruiksdoel>?,
        @QueryParam("type") type: TypeAdresseerbaarObject?,
        @QueryParam("pandIdentificaties") pandIdentificaties: List<String>?
    ): AdresseerbareObjectenIOHalCollection
}
