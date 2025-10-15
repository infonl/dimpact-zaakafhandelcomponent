/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.klant

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import nl.info.client.klant.exception.KlantRuntimeResponseExceptionMapper
import nl.info.client.klant.model.PaginatedExpandPartijList
import nl.info.client.klant.util.KlantClientHeadersFactory
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
        @QueryParam("bezoekadresAdresregel1") bezoekadresAdresregel1: String? = null,
        @QueryParam("bezoekadresAdresregel2") bezoekadresAdresregel2: String? = null,
        @QueryParam("bezoekadresAdresregel3") bezoekadresAdresregel3: String? = null,
        @QueryParam("bezoekadresLand") bezoekadresLand: String? = null,
        @QueryParam("bezoekadresNummeraanduidingId") bezoekadresNummeraanduidingId: String? = null,
        @QueryParam("categorierelatie__categorie__naam") categorierelatieCategorieNaam: String? = null,
        @QueryParam("correspondentieadresAdresregel1") correspondentieadresAdresregel1: String? = null,
        @QueryParam("correspondentieadresAdresregel2") correspondentieadresAdresregel2: String? = null,
        @QueryParam("correspondentieadresAdresregel3") correspondentieadresAdresregel3: String? = null,
        @QueryParam("correspondentieadresLand") correspondentieadresLand: String? = null,
        @QueryParam("correspondentieadresNummeraanduidingId") correspondentieadresNummeraanduidingId: String? = null,
        @QueryParam("expand") expand: String? = null,
        @QueryParam("indicatieActief") indicatieActief: Boolean? = null,
        @QueryParam("indicatieGeheimhouding") indicatieGeheimhouding: Boolean? = null,
        @QueryParam("nummer") nummer: String? = null,
        @QueryParam("page") page: Int? = null,
        @QueryParam("pageSize") pageSize: Int? = null,
        @QueryParam("partijIdentificator__codeObjecttype") partijIdentificatorCodeObjecttype: String? = null,
        @QueryParam("partijIdentificator__codeRegister") partijIdentificatorCodeRegister: String? = null,
        @QueryParam("partijIdentificator__codeSoortObjectId") partijIdentificatorCodeSoortObjectId: String? = null,
        @QueryParam("partijIdentificator__objectId") partijIdentificatorObjectId: String? = null,
        @QueryParam("soortPartij") soortPartij: String? = null,
        @QueryParam("vertegenwoordigdePartij__url") vertegenwoordigdePartijUrl: String? = null,
        @QueryParam("vertegenwoordigdePartij__uuid") vertegenwoordigdePartijUuid: UUID? = null
    ): PaginatedExpandPartijList
}
