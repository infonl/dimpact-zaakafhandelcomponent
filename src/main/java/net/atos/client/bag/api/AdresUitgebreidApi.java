/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.bag.api;

import static net.atos.client.bag.BagClientService.DEFAULT_CRS;

import java.time.temporal.ChronoUnit;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.bag.exception.BagResponseExceptionMapper;
import net.atos.client.bag.model.generated.AdresUitgebreidHal;
import net.atos.client.bag.model.generated.AdresUitgebreidHalCollection;
import net.atos.client.bag.util.BagClientHeadersFactory;
import net.atos.client.bag.util.JsonbConfiguration;
import net.atos.zac.util.MediaTypes;

/**
 * IMBAG API - van de LVBAG
 *
 * <p>Dit is de [BAG API](https://zakelijk.kadaster.nl/-/bag-api) Individuele Bevragingen van de Landelijke Voorziening Basisregistratie
 * Adressen en Gebouwen (LVBAG). Meer informatie over de Basisregistratie Adressen en Gebouwen is te vinden op de website van het
 * [Ministerie van Binnenlandse Zaken en Koninkrijksrelaties](https://www.geobasisregistraties.nl/basisregistraties/adressen-en-gebouwen) en
 * [Kadaster](https://zakelijk.kadaster.nl/bag). De BAG API levert informatie conform de [BAG Catalogus
 * 2018](https://www.geobasisregistraties.nl/documenten/publicatie/2018/03/12/catalogus-2018) en het informatiemodel IMBAG 2.0. De API
 * specificatie volgt de [Nederlandse API-Strategie](https://docs.geostandaarden.nl/api/API-Strategie) specificatie versie van 20200204 en
 * is opgesteld in [OpenAPI Specificatie](https://www.forumstandaardisatie.nl/standaard/openapi-specification) (OAS) v3. Het standaard
 * mediatype HAL (`application/hal+json`) wordt gebruikt. Dit is een mediatype voor het weergeven van resources en hun relaties via
 * hyperlinks. Deze API is vooral gericht op individuele bevragingen (op basis van de identificerende gegevens van een object). Om gebruik
 * te kunnen maken van de BAG API is een API key nodig, deze kan verkregen worden door het
 * [aanvraagformulier](https://formulieren.kadaster.nl/aanvraag_bag_api_individuele_bevragingen_productie) in te vullen. Voor vragen, neem
 * contact op met de LVBAG beheerder o.v.v. BAG API 2.0. We zijn aan het kijken naar een geschikt medium hiervoor, mede ook om de API
 * iteratief te kunnen opstellen of doorontwikkelen samen met de community. Als de API iets (nog) niet kan, wat u wel graag wilt, neem dan
 * contact op.
 */
@RegisterRestClient(configKey = "BAG-API-Client")
@RegisterClientHeaders(BagClientHeadersFactory.class)
@RegisterProvider(BagResponseExceptionMapper.class)
@RegisterProvider(JsonbConfiguration.class)
@Timeout(unit = ChronoUnit.SECONDS, value = 5)
@Path("/adressenuitgebreid")
public interface AdresUitgebreidApi {

    /**
     * Bevragen van de uitgebreide informatie van één huidig adres met de identificatie van een nummeraanduiding.
     * <p>
     * Bevragen van de uitgebreide informatie van één huidig adres met de identificatie van een nummeraanduiding. Als
     * inclusiefEindStatus&#x3D;true, dan worden ook actuele adressen met een eind status geleverd, zie [functionele specificatie
     * inclusiefEindstatus](https://github.com/lvbag/BAG-API/blob/master/Features/inclusief-eind-status.feature).
     */
    @GET
    @Path("/{nummeraanduidingIdentificatie}")
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    AdresUitgebreidHal bevraagAdresUitgebreidMetNumId(
            @PathParam("nummeraanduidingIdentificatie") String nummeraanduidingIdentificatie,
            @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs,
            @QueryParam("inclusiefEindStatus") @DefaultValue("false") Boolean inclusiefEindStatus
    ) throws ProcessingException;

    /**
     * Bevragen van de uitgebreide informatie van één of meer huidige adressen op basis van verschillende combinaties van parameters.
     * <p>
     * De volgende (combinaties van) parameters worden ondersteund: 1. Bevragen van de uitgebreide informatie van één of meer huidige
     * adressen met een postcode, huisnummer en optioneel huisnummertoevoeging en huisletter. Het opgeven van een combinatie van parameters
     * levert niet altijd een exacte match met één adres, bv. in geval van meerdere objecten met huisnummertoevoegingen en/of huisletters.
     * Met de exacteMatch parameter kan worden aangegeven dat alleen object(en) die exact overeenkomen met de opgegeven parameters,
     * geretourneerd moeten worden. 2. Bevragen van de uitgebreide informatie van één of meer huidige adressen met de identificatie van een
     * adresseerbaar object. 3. Bevragen van de uitgebreide informatie van één of meer huidige adressen met woonplaats naam, openbare ruimte
     * naam, huisnummer en optioneel huisnummertoevoeging en huisletter. Het opgeven van een combinatie van parameters levert niet altijd
     * een exacte match met één adres, bv. in geval van meerdere objecten met huisnummertoevoegingen en/of huisletters. Met de exacteMatch
     * parameter kan worden aangegeven dat alleen object(en) die exact overeenkomen met de opgegeven parameters, geretourneerd moeten
     * worden. 4. Zoek uitgebreide adres informatie van huidige adressen met een zoekterm. Voor paginering, zie: [functionele specificatie
     * paginering](https://github.com/lvbag/BAG-API/blob/master/Features/paginering.feature). Als inclusiefEindStatus&#x3D;true, dan worden
     * ook actuele adressen met een eind status geleverd, zie [functionele specificatie
     * inclusiefEindstatus](https://github.com/lvbag/BAG-API/blob/master/Features/inclusief-eind-status.feature).
     */
    @GET
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    AdresUitgebreidHalCollection zoekAdresUitgebreid(
            @QueryParam("postcode") String postcode,
            @QueryParam("huisnummer") Integer huisnummer,
            @QueryParam("huisnummertoevoeging") String huisnummertoevoeging,
            @QueryParam("huisletter") String huisletter,
            @QueryParam("exacteMatch") @DefaultValue("false") Boolean exacteMatch,
            @QueryParam("adresseerbaarObjectIdentificatie") String adresseerbaarObjectIdentificatie,
            @QueryParam("woonplaatsNaam") String woonplaatsNaam,
            @QueryParam("openbareRuimteNaam") String openbareRuimteNaam,
            @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs,
            @QueryParam("page") @DefaultValue("1") Integer page,
            @QueryParam("pageSize") @DefaultValue("20") Integer pageSize,
            @QueryParam("q") String q,
            @QueryParam("inclusiefEindStatus") @DefaultValue("false") Boolean inclusiefEindStatus
    ) throws ProcessingException;
}
