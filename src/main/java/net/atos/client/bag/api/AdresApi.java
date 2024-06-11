/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.bag.api;

import java.time.temporal.ChronoUnit;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.bag.exception.BagRuntimeExceptionMapper;
import net.atos.client.bag.model.BevraagAdressenParameters;
import net.atos.client.bag.model.generated.AdresIOHal;
import net.atos.client.bag.model.generated.AdresIOHalCollection;
import net.atos.client.bag.model.generated.ZoekResultaatHalCollection;
import net.atos.client.bag.util.BagClientHeadersFactory;
import net.atos.client.bag.util.JsonbConfiguration;

/**
 * IMBAG API - van de LVBAG
 * Dit is de [BAG API](<a href="https://zakelijk.kadaster.nl/-/bag-api">...</a>) Individuele Bevragingen van de Landelijke Voorziening Basisregistratie
 * Adressen en Gebouwen (LVBAG). Meer informatie over de Basisregistratie Adressen en Gebouwen is te vinden op de website van het
 * [Ministerie van Binnenlandse Zaken en Koninkrijksrelaties](https://www.geobasisregistraties.nl/basisregistraties/adressen-en-gebouwen) en
 * [Kadaster](https://zakelijk.kadaster.nl/bag). De BAG API levert informatie conform de [BAG Catalogus
 * 2018](https://www.geobasisregistraties.nl/documenten/publicatie/2018/03/12/catalogus-2018) en het informatiemodel IMBAG 2.0. De API
 * specificatie volgt de [Nederlandse API-Strategie](<a href="https://docs.geostandaarden.nl/api/API-Strategie">...</a>) specificatie versie van 20200204 en
 * is opgesteld in [OpenAPI Specificatie](https://www.forumstandaardisatie.nl/standaard/openapi-specification) (OAS) v3. Het standaard
 * mediatype HAL (`application/hal+json`) wordt gebruikt. Dit is een mediatype voor het weergeven van resources en hun relaties via
 * hyperlinks. Deze API is vooral gericht op individuele bevragingen (op basis van de identificerende gegevens van een object). Om gebruik
 * te kunnen maken van de BAG API is een API key nodig, deze kan verkregen worden door het
 * [aanvraagformulier](<a href="https://formulieren.kadaster.nl/aanvraag_bag_api_individuele_bevragingen_productie">...</a>) in te vullen. Voor vragen, neem
 * contact op met de LVBAG beheerder o.v.v. BAG API 2.0. We zijn aan het kijken naar een geschikt medium hiervoor, mede ook om de API
 * iteratief te kunnen opstellen of doorontwikkelen samen met de community. Als de API iets (nog) niet kan, wat u wel graag wilt, neem dan
 * contact op.
 */
@RegisterRestClient(configKey = "BAG-API-Client")
@RegisterClientHeaders(BagClientHeadersFactory.class)
@RegisterProvider(BagRuntimeExceptionMapper.class)
@RegisterProvider(JsonbConfiguration.class)
@Timeout(unit = ChronoUnit.SECONDS, value = 5)
@Path("/adressen")
public interface AdresApi {

    /**
     * Bevragen van huidige adressen met een (combinatie van) zoek parameters.
     * <p>
     * De volgende bevragingen worden ondersteund:
     * 1. Bevragen van één of meer huidige adressen met postcode, huisnummer en optioneel huisnummertoevoeging en huisletter.
     * Parameter exacteMatch kan worden toegepast.
     * 2. Bevragen van één of meer huidige adressen met de identificatie van een adresseerbaar object.
     * 3. Bevragen van één of meer huidige adressen met woonplaats naam, openbare ruimte naam, huisnummer en optioneel huisnummertoevoeging
     * en/of huisletter. Parameter exacteMatch kan worden toegepast.
     * 4. Bevragen van één of meer huidige adressen met de identificatie van een pand.
     * Expand wordt niet ondersteund.
     * 5. Zoek huidige adressen met een zoekterm.
     * Bij de bovenstaande bevragingen
     * kunnen eveneens de volgende parameters worden gebruikt (tenzij anders vermeld): Als expand&#x3D;nummeraanduiding, openbareRuimte,
     * woonplaats, adresseerbaarObject, panden (of een combinatie daarvan) of als expand&#x3D;true, dan worden de gevraagde of alle
     * gerelateerde resources als geneste resource geleverd, zie [functionele specificatie
     * expand](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature">...</a>). Voor paginering, zie: [functionele specificatie
     * paginering](https://github.com/lvbag/BAG-API/blob/master/Features/paginering.feature). Als inclusiefEindStatus&#x3D;true, dan worden
     * ook actuele adressen met een eind status geleverd, zie [functionele specificatie
     * inclusiefEindstatus](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/inclusief-eind-status.feature">...</a>).
     */
    @GET
    @Produces({"application/hal+json", "application/problem+json"})
    AdresIOHalCollection bevraagAdressen(
            @BeanParam BevraagAdressenParameters parameters
    ) throws ProcessingException;

    /**
     * Bevragen van een huidig adres met de identificatie van een nummeraanduiding.
     * <p>
     * Bevragen van een huidig adres met de identificatie van een nummeraanduiding.
     * Als expand&#x3D;nummeraanduiding, openbareRuimte, woonplaats, adresseerbaarObject, panden (of een combinatie daarvan) of als
     * expand&#x3D;true, dan worden de gevraagde of alle gerelateerde resources als geneste resource geleverd, zie [functionele specificatie
     * expand](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature">...</a>). Als inclusiefEindStatus&#x3D;true, dan worden ook
     * actuele adressen met een eind status geleverd, zie [functionele specificatie
     * inclusiefEindstatus](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/inclusief-eind-status.feature">...</a>).
     */
    @GET
    @Path("/{nummeraanduidingIdentificatie}")
    @Produces({"application/hal+json", "application/problem+json"})
    AdresIOHal bevraagAdressenMetNumId(
            @PathParam("nummeraanduidingIdentificatie") String nummeraanduidingIdentificatie,
            @QueryParam("expand") String expand,
            @QueryParam("inclusiefEindStatus") @DefaultValue("false") Boolean inclusiefEindStatus
    ) throws ProcessingException;

    /**
     * Zoeken van huidige adressen
     * <p>
     * Zoeken van huidige adressen met postcode, woonplaats, straatnaam, huisnummer, huisletter, huisnummertoevoeging.
     * Een adres kan worden gevonden door de zoekresultaatidentificatie uit het antwoord als parameter mee te geven in get /adressen.
     * Voor paginering, zie: [functionele specificatie
     * paginering](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/paginering.feature">...</a>).
     *
     * @deprecated
     */
    @Deprecated
    @GET
    @Path("/zoek")
    @Produces({"application/hal+json", "application/problem+json"})
    ZoekResultaatHalCollection zoek(
            @QueryParam("zoek") String zoek,
            @QueryParam("page") @DefaultValue("1") Integer page,
            @QueryParam("pageSize") @DefaultValue("20") Integer pageSize
    ) throws ProcessingException;
}
