/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.bag.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static net.atos.client.bag.BagClientService.DEFAULT_CRS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
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
import net.atos.client.bag.model.generated.PointGeoJSON;
import net.atos.client.bag.model.generated.WoonplaatsIOHal;
import net.atos.client.bag.model.generated.WoonplaatsIOHalCollection;
import net.atos.client.bag.model.generated.WoonplaatsIOLvcHalCollection;
import net.atos.client.bag.util.BagClientHeadersFactory;
import net.atos.client.bag.util.JsonbConfiguration;
import net.atos.zac.util.MediaTypes;

/**
 * IMBAG API - van de LVBAG
 *
 * <p>Dit is de [BAG API](<a href="https://zakelijk.kadaster.nl/-/bag-api">...</a>) Individuele Bevragingen van de Landelijke Voorziening
 * Basisregistratie
 * Adressen en Gebouwen (LVBAG). Meer informatie over de Basisregistratie Adressen en Gebouwen is te vinden op de website van het
 * [Ministerie van Binnenlandse Zaken en Koninkrijksrelaties](https://www.geobasisregistraties.nl/basisregistraties/adressen-en-gebouwen) en
 * [Kadaster](<a href="https://zakelijk.kadaster.nl/bag">...</a>). De BAG API levert informatie conform de [BAG Catalogus
 * 2018](https://www.geobasisregistraties.nl/documenten/publicatie/2018/03/12/catalogus-2018) en het informatiemodel IMBAG 2.0. De API
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
@RegisterProvider(JsonbConfiguration.class)
@Timeout(unit = ChronoUnit.SECONDS, value = 5)
@Path("/woonplaatsen")
public interface WoonplaatsApi {

    /**
     * bevragen van een woonplaats met een geometrische locatie.
     * <p>
     * Bevragen/raadplegen van een voorkomen van een Woonplaats met een geometrische locatie. Parameter huidig kan worden toegepast, zie
     * [functionele specificatie huidig](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/huidig.feature">...</a>). De
     * geldigOp en beschikbaarOp
     * parameters kunnen gebruikt worden voor tijdreis vragen, zie [functionele specificatie
     * tijdreizen](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/tijdreizen.feature">...</a>). Als expand&#x3D;bronhouders,
     * geometrie of true
     * dan worden de gevraagde of alle gerelateerde objecten als geneste resource geleverd, zie [functionele specificatie
     * expand](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature">...</a>).
     */
    @POST
    @Consumes({APPLICATION_JSON})
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    WoonplaatsIOHalCollection woonplaatsGeometrie(
            PointGeoJSON pointGeoJSON,
            @QueryParam("geldigOp") LocalDate geldigOp,
            @QueryParam("beschikbaarOp") OffsetDateTime beschikbaarOp,
            @QueryParam("huidig") @DefaultValue("false") Boolean huidig,
            @QueryParam("expand") String expand,
            @HeaderParam("Content-Crs") String contentCrs,
            @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs
    ) throws ProcessingException;

    /**
     * bevragen van een woonplaats met de identificatie van een woonplaats.
     * <p>
     * Bevragen/raadplegen van een voorkomen van een Woonplaats met de identificatie van de woonplaats. Parameter huidig kan worden
     * toegepast, zie [functionele specificatie huidig](<a
     * href="https://github.com/lvbag/BAG-API/blob/master/Features/huidig.feature">...</a>). De geldigOp
     * en beschikbaarOp parameters kunnen gebruikt worden voor tijdreis vragen, zie [functionele specificatie
     * tijdreizen](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/tijdreizen.feature">...</a>). Als expand&#x3D;bronhouders,
     * geometrie of true
     * dan worden de gevraagde of alle gerelateerde objecten als geneste resource geleverd, zie [functionele specificatie
     * expand](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature">...</a>).
     */
    @GET
    @Path("/{identificatie}")
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    WoonplaatsIOHal woonplaatsIdentificatie(
            @PathParam("identificatie") String identificatie,
            @QueryParam("geldigOp") LocalDate geldigOp,
            @QueryParam("beschikbaarOp") OffsetDateTime beschikbaarOp,
            @QueryParam("expand") String expand,
            @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs,
            @QueryParam("huidig") @DefaultValue("false") Boolean huidig
    ) throws ProcessingException;

    /**
     * bevragen van een voorkomen van een woonplaats met de identificatie van een woonplaats en de identificatie van een voorkomen,
     * bestaande uit een versie en een timestamp van het tijdstip van registratie in de LV BAG.
     * <p>
     * Bevragen/raadplegen van een voorkomen van een Woonplaats met de identificatie van een woonplaats en de identificatie van een
     * voorkomen, bestaande uit een versie en een timestamp van het tijdstip van registratie in de LV BAG. Als expand&#x3D;bronhouders,
     * geometrie of true dan worden de gevraagde of alle gerelateerde objecten als geneste resource geleverd, zie [functionele specificatie
     * expand](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature">...</a>).
     */
    @GET
    @Path("/{identificatie}/{versie}/{timestampRegistratieLv}")
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    WoonplaatsIOHal woonplaatsIdentificatieVoorkomen(
            @PathParam("identificatie") String identificatie,
            @PathParam("versie") Integer versie,
            @PathParam("timestampRegistratieLv") String timestampRegistratieLv,
            @QueryParam("expand") String expand,
            @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs
    ) throws ProcessingException;

    /**
     * bevragen van de levenscyclus van een woonplaats met de identificatie van een woonplaats.
     * <p>
     * Bevragen/raadplegen van de levenscyclus van een Woonplaats met de identificatie van de woonplaats. Als expand&#x3D;bronhouders,
     * geometrie of true dan worden de gevraagde of alle gerelateerde objecten als geneste resource geleverd, zie [functionele specificatie
     * expand](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature">...</a>).
     */
    @GET
    @Path("/{identificatie}/lvc")
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    WoonplaatsIOLvcHalCollection woonplaatsLvcIdentificatie(
            @PathParam("identificatie") String identificatie,
            @QueryParam("geheleLvc") @DefaultValue("false") Boolean geheleLvc,
            @QueryParam("expand") String expand,
            @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs
    ) throws ProcessingException;

    /**
     * Zoeken van één of meer woonplaatsen met een woonplaatsnaam, geometrische locatie of binnen een bounding box.
     * <p>
     * Zoeken van actuele woonplaatsen: 1. met een woonplaatsnaam. 2. met een geometrische locatie. 3. binnen een geometrische contour
     * (rechthoek). Parameter huidig kan worden toegepast, zie [functionele specificatie
     * huidig](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/huidig.feature">...</a>). De geldigOp en beschikbaarOp
     * parameters kunnen
     * gebruikt worden voor tijdreis vragen, zie [functionele specificatie
     * tijdreizen](https://github.com/lvbag/BAG-API/blob/master/Features/tijdreizen.feature). Als expand&#x3D;bronhouders, geometrie of true
     * dan worden de gevraagde of alle gerelateerde objecten als geneste resource geleverd, zie [functionele specificatie
     * expand](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature">...</a>). Voor paginering, zie: [functionele
     * specificatie
     * paginering](<a href="https://github.com/lvbag/BAG-API/blob/master/Features/paginering.feature">...</a>).
     */
    @GET
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    WoonplaatsIOHalCollection zoekWoonplaatsen(
            @QueryParam("naam") String naam,
            @QueryParam("geldigOp") LocalDate geldigOp,
            @QueryParam("beschikbaarOp") OffsetDateTime beschikbaarOp,
            @QueryParam("huidig") @DefaultValue("false") Boolean huidig,
            @QueryParam("expand") String expand,
            @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs,
            @HeaderParam("Content-Crs") String contentCrs,
            @QueryParam("page") @DefaultValue("1") Integer page,
            @QueryParam("pageSize") @DefaultValue("20") Integer pageSize,
            @QueryParam("point") PointGeoJSON point,
            @QueryParam("bbox") List<BigDecimal> bbox
    ) throws ProcessingException;
}
