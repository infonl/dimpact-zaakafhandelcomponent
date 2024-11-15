/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.bag.api;

import static net.atos.client.bag.BagClientService.DEFAULT_CRS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
import net.atos.client.bag.model.generated.Gebruiksdoel;
import net.atos.client.bag.model.generated.OppervlakteFilter;
import net.atos.client.bag.model.generated.VerblijfsobjectIOHal;
import net.atos.client.bag.model.generated.VerblijfsobjectIOHalCollection;
import net.atos.client.bag.model.generated.VerblijfsobjectIOLvcHalCollection;
import net.atos.client.bag.util.BagClientHeadersFactory;
import net.atos.zac.util.MediaTypes;

/**
 * IMBAG API - van de LVBAG
 *
 * <p>Dit is de [BAG API](<a href="https://zakelijk.kadaster.nl/-/bag-api">...</a>) Individuele Bevragingen van de Landelijke Voorziening
 * Basisregistratie
 * Adressen en Gebouwen (LVBAG). Meer informatie over de Basisregistratie Adressen en Gebouwen is te vinden op de website van het
 * [Ministerie van Binnenlandse Zaken en Koninkrijksrelaties](https://www.geobasisregistraties.nl/basisregistraties/adressen-en-gebouwen) en
 * [Kadaster](<a href="https://zakelijk.kadaster.nl/bag">...</a>). De BAG API levert informatie conform de [BAG Catalogus
 * 2018](<a href="https://www.geobasisregistraties.nl/documenten/publicatie/2018/03/12/catalogus-2018">...</a>) en het informatiemodel IMBAG
 * 2.0. De API
 * specificatie volgt de [Nederlandse API-Strategie](https://docs.geostandaarden.nl/api/API-Strategie) specificatie versie van 20200204 en
 * is opgesteld in [OpenAPI Specificatie](<a href="https://www.forumstandaardisatie.nl/standaard/openapi-specification">...</a>) (OAS) v3.
 * Het standaard
 * mediatype HAL (`application/hal+json`) wordt gebruikt. Dit is een mediatype voor het weergeven van resources en hun relaties via
 * hyperlinks. Deze API is vooral gericht op individuele bevragingen (op basis van de identificerende gegevens van een object). Om gebruik
 * te kunnen maken van de BAG API is een API key nodig, deze kan verkregen worden door het
 * [aanvraagformulier](<a href="https://formulieren.kadaster.nl/aanvraag_bag_api_individuele_bevragingen_productie">...</a>) in te vullen.
 * Voor vragen, neem
 * contact op met de LVBAG beheerder o.v.v. BAG API 2.0. We zijn aan het kijken naar een geschikt medium hiervoor, mede ook om de API
 * iteratief te kunnen opstellen of doorontwikkelen samen met de community. Als de API iets (nog) niet kan, wat u wel graag wilt, neem dan
 * contact op.
 */
@RegisterRestClient(configKey = "BAG-API-Client")
@RegisterClientHeaders(BagClientHeadersFactory.class)
@RegisterProvider(BagResponseExceptionMapper.class)
@Timeout(unit = ChronoUnit.SECONDS, value = 5)
@Path("/verblijfsobjecten")
public interface VerblijfsobjectApi {

    /**
     * bevragen 1 verblijfsobject met de identificatie van een verblijfsobject.
     * <p>
     * Bevragen/raadplegen van één voorkomen van een Verblijfsobject met de identificatie van een verblijfsobject. Parameter huidig kan
     * worden toegepast, zie [functionele specificatie huidig](<a
     * href="https://github.com/lvbag/BAG-API/blob/master/Features/huidig.feature">...</a>). De
     * geldigOp en beschikbaarOp parameters kunnen gebruikt worden voor tijdreis vragen, zie [functionele specificatie
     * tijdreizen](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/tijdreizen.feature">...</a>). Als
     * expand&#x3D;heeftAlsHoofdAdres,
     * heeftAlsNevenAdres, maaktDeelUitVan of true, dan worden de gevraagde of alle gerelateerde objecten als geneste resource geleverd, zie
     * [functionele specificatie expand](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature">...</a>).
     */
    @GET
    @Path("/{identificatie}")
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    VerblijfsobjectIOHal verblijfsobjectIdentificatie(
            @PathParam("identificatie") String identificatie,
            @QueryParam("geldigOp") LocalDate geldigOp,
            @QueryParam("beschikbaarOp") OffsetDateTime beschikbaarOp,
            @QueryParam("expand") String expand,
            @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs,
            @QueryParam("huidig") @DefaultValue("false") Boolean huidig
    ) throws ProcessingException;

    /**
     * bevragen voorkomen van een verblijfsobject, op basis van de identificatie van een verblijfsobject en de identificatie van een
     * voorkomen
     * <p>
     * Bevragen/raadplegen van een voorkomen van een verblijfsobject, met de identificatie van een verblijfsobject en de identificatie van
     * een voorkomen, bestaande uit een versie en een timestamp van het tijdstip van registratie in de LV BAG.
     */
    @GET
    @Path("/{identificatie}/{versie}/{timestampRegistratieLv}")
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    VerblijfsobjectIOHal verblijfsobjectIdentificatieVoorkomen(
            @PathParam("identificatie") String identificatie,
            @PathParam("versie") Integer versie,
            @PathParam("timestampRegistratieLv") String timestampRegistratieLv,
            @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs
    ) throws ProcessingException;

    /**
     * bevragen levenscyclus van een verblijfsobject met de identificatie van een verblijfsobject.
     * <p>
     * Bevragen/raadplegen van de levenscyclus van een Verblijfsobject met de identificatie van de verblijfsobject.
     */
    @GET
    @Path("/{identificatie}/lvc")
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    VerblijfsobjectIOLvcHalCollection verblijfsobjectLvcIdentificatie(
            @PathParam("identificatie") String identificatie,
            @QueryParam("geheleLvc") @DefaultValue("false") Boolean geheleLvc,
            @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs
    ) throws ProcessingException;

    /**
     * Zoeken van alle aan een pand gerelateerde verblijfsobjecten of binnen een bounding box (met paginering).
     * <p>
     * Zoek verblijfsobjecten: 1. gerelateerd aan een pand identificatie. 2. binnen een geometrische contour (rechthoek) in combinatie met
     * status geconstateerd, oppervlakte, gebruiksdoel. Parameter huidig kan worden toegepast, zie [functionele specificatie
     * huidig](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/huidig.feature">...</a>). De geldigOp en beschikbaarOp
     * parameters kunnen
     * gebruikt worden voor tijdreis vragen, zie [functionele specificatie
     * tijdreizen](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/tijdreizen.feature">...</a>). Als
     * expand&#x3D;heeftAlsHoofdAdres,
     * heeftAlsNevenAdres of true, dan worden de gevraagde of alle gerelateerde objecten als geneste resource geleverd, zie [functionele
     * specificatie expand](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature">...</a>). Voor paginering, zie:
     * [functionele
     * specificatie paginering](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/paginering.feature">...</a>).
     */
    @GET
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    VerblijfsobjectIOHalCollection zoekVerblijfsobjecten(
            @QueryParam("pandIdentificatie") String pandIdentificatie,
            @QueryParam("huidig") @DefaultValue("false") Boolean huidig,
            @QueryParam("geldigOp") LocalDate geldigOp,
            @QueryParam("beschikbaarOp") OffsetDateTime beschikbaarOp,
            @QueryParam("expand") String expand,
            @QueryParam("page") @DefaultValue("1") Integer page,
            @QueryParam("pageSize") @DefaultValue("20") Integer pageSize,
            @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs,
            @HeaderParam("Content-Crs") String contentCrs,
            @QueryParam("bbox") List<BigDecimal> bbox,
            @QueryParam("geconstateerd") Boolean geconstateerd,
            @QueryParam("oppervlakte") OppervlakteFilter oppervlakte,
            @QueryParam("gebruiksdoelen") List<Gebruiksdoel> gebruiksdoelen
    ) throws ProcessingException;
}
