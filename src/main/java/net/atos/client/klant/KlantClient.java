/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.klant;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.klant.exception.KlantRuntimeResponseExceptionMapper;
import net.atos.client.klant.model.PaginatedExpandPartijList;
import net.atos.client.klant.util.KlantClientHeadersFactory;

/**
 * Klanten API
 * <p>
 * Een API om zowel klanten te registreren als op te vragen.
 * Een klant is een natuurlijk persoon, niet-natuurlijk persoon (bedrijf) of vestiging waarbij het gaat om niet geverifieerde gegevens.
 */
@RegisterRestClient(configKey = "Klantinteracties-API-Client")
@RegisterClientHeaders(KlantClientHeadersFactory.class)
@RegisterProvider(KlantRuntimeResponseExceptionMapper.class)
@Path("/klantinteracties/api/v1")
public interface KlantClient {
    @GET
    @Path("/partijen")
    @Produces({APPLICATION_JSON})
    PaginatedExpandPartijList partijenList(
            @QueryParam("bezoekadresAdresregel1") String bezoekadresAdresregel1,
            @QueryParam("bezoekadresAdresregel2") String bezoekadresAdresregel2,
            @QueryParam("bezoekadresAdresregel3") String bezoekadresAdresregel3,
            @QueryParam("bezoekadresLand") String bezoekadresLand,
            @QueryParam("bezoekadresNummeraanduidingId") String bezoekadresNummeraanduidingId,
            @QueryParam("categorierelatie__categorie__naam") String categorierelatieCategorieNaam,
            @QueryParam("correspondentieadresAdresregel1") String correspondentieadresAdresregel1,
            @QueryParam("correspondentieadresAdresregel2") String correspondentieadresAdresregel2,
            @QueryParam("correspondentieadresAdresregel3") String correspondentieadresAdresregel3,
            @QueryParam("correspondentieadresLand") String correspondentieadresLand,
            @QueryParam("correspondentieadresNummeraanduidingId") String correspondentieadresNummeraanduidingId,
            @QueryParam("expand") String expand,
            @QueryParam("indicatieActief") Boolean indicatieActief,
            @QueryParam("indicatieGeheimhouding") Boolean indicatieGeheimhouding,
            @QueryParam("nummer") String nummer,
            @QueryParam("page") Integer page,
            @QueryParam("partijIdentificator__codeObjecttype") String partijIdentificatorCodeObjecttype,
            @QueryParam("partijIdentificator__codeRegister") String partijIdentificatorCodeRegister,
            @QueryParam("partijIdentificator__codeSoortObjectId") String partijIdentificatorCodeSoortObjectId,
            @QueryParam("partijIdentificator__objectId") String partijIdentificatorObjectId,
            @QueryParam("soortPartij") String soortPartij,
            @QueryParam("vertegenwoordigdePartij__url") String vertegenwoordigdePartijUrl,
            @QueryParam("vertegenwoordigdePartij__uuid") UUID vertegenwoordigdePartijUuid
    ) throws ProcessingException;
}
