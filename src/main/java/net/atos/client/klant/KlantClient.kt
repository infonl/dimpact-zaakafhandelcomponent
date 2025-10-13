/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.klant

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import net.atos.client.klant.exception.KlantRuntimeResponseExceptionMapper
import net.atos.client.klant.model.PaginatedExpandPartijList
import net.atos.client.klant.util.KlantClientHeadersFactory
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.util.UUID

/**
 * Klanten API
 *
 *
 * Een API om zowel klanten te registreren als op te vragen.
 * Een klant is een natuurlijk persoon, niet-natuurlijk persoon (bedrijf) of vestiging waarbij het gaat om niet geverifieerde gegevens.
 */
@RegisterRestClient(configKey = "Klantinteracties-API-Client")
@RegisterClientHeaders(KlantClientHeadersFactory::class)
@RegisterProvider(KlantRuntimeResponseExceptionMapper::class)
@Path("/klantinteracties/api/v1")
@Suppress("LongParameterList")
interface KlantClient {
    @GET
    @Path("/partijen")
    @Produces(MediaType.APPLICATION_JSON)
    @Throws(ProcessingException::class)
    fun partijenList(
        @QueryParam("bezoekadresAdresregel1") bezoekadresAdresregel1: String?,
        @QueryParam("bezoekadresAdresregel2") bezoekadresAdresregel2: String?,
        @QueryParam("bezoekadresAdresregel3") bezoekadresAdresregel3: String?,
        @QueryParam("bezoekadresLand") bezoekadresLand: String?,
        @QueryParam("bezoekadresNummeraanduidingId") bezoekadresNummeraanduidingId: String?,
        @QueryParam("categorierelatie__categorie__naam") categorierelatieCategorieNaam: String?,
        @QueryParam("correspondentieadresAdresregel1") correspondentieadresAdresregel1: String?,
        @QueryParam("correspondentieadresAdresregel2") correspondentieadresAdresregel2: String?,
        @QueryParam("correspondentieadresAdresregel3") correspondentieadresAdresregel3: String?,
        @QueryParam("correspondentieadresLand") correspondentieadresLand: String?,
        @QueryParam("correspondentieadresNummeraanduidingId") correspondentieadresNummeraanduidingId: String?,
        @QueryParam("expand") expand: String?,
        @QueryParam("indicatieActief") indicatieActief: Boolean?,
        @QueryParam("indicatieGeheimhouding") indicatieGeheimhouding: Boolean?,
        @QueryParam("nummer") nummer: String?,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?,
        @QueryParam("partijIdentificator__codeObjecttype") partijIdentificatorCodeObjecttype: String?,
        @QueryParam("partijIdentificator__codeRegister") partijIdentificatorCodeRegister: String?,
        @QueryParam("partijIdentificator__codeSoortObjectId") partijIdentificatorCodeSoortObjectId: String?,
        @QueryParam("partijIdentificator__objectId") partijIdentificatorObjectId: String?,
        @QueryParam("soortPartij") soortPartij: String?,
        @QueryParam("vertegenwoordigdePartij__url") vertegenwoordigdePartijUrl: String?,
        @QueryParam("vertegenwoordigdePartij__uuid") vertegenwoordigdePartijUuid: UUID?
    ): PaginatedExpandPartijList
}
