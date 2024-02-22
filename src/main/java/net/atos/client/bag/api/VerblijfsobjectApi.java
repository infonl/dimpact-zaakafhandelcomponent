/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.bag.api;

import static net.atos.client.bag.BAGClientService.DEFAULT_CRS;

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

import net.atos.client.bag.model.Gebruiksdoel;
import net.atos.client.bag.model.OppervlakteFilter;
import net.atos.client.bag.model.VerblijfsobjectIOHal;
import net.atos.client.bag.model.VerblijfsobjectIOHalCollection;
import net.atos.client.bag.model.VerblijfsobjectIOLvcHalCollection;
import net.atos.client.bag.util.BAGClientHeadersFactory;
import net.atos.client.brp.exception.RuntimeExceptionMapper;

/**
 * IMBAG API - van de LVBAG
 *
 * <p>Dit is de [BAG API](https://zakelijk.kadaster.nl/-/bag-api) Individuele Bevragingen van de Landelijke Voorziening Basisregistratie Adressen en Gebouwen (LVBAG).  Meer informatie over de Basisregistratie Adressen en Gebouwen is te vinden op de website van het [Ministerie van Binnenlandse Zaken en Koninkrijksrelaties](https://www.geobasisregistraties.nl/basisregistraties/adressen-en-gebouwen) en [Kadaster](https://zakelijk.kadaster.nl/bag).  De BAG API levert informatie conform de [BAG Catalogus 2018](https://www.geobasisregistraties.nl/documenten/publicatie/2018/03/12/catalogus-2018) en het informatiemodel IMBAG 2.0. De API specificatie volgt de [Nederlandse API-Strategie](https://docs.geostandaarden.nl/api/API-Strategie) specificatie versie van 20200204 en is opgesteld in [OpenAPI Specificatie](https://www.forumstandaardisatie.nl/standaard/openapi-specification) (OAS) v3.  Het standaard mediatype HAL (`application/hal+json`) wordt gebruikt. Dit is een mediatype voor het weergeven van resources en hun relaties via hyperlinks.  Deze API is vooral gericht op individuele bevragingen (op basis van de identificerende gegevens van een object). Om gebruik te kunnen maken van de BAG API is een API key nodig, deze kan verkregen worden door het [aanvraagformulier](https://formulieren.kadaster.nl/aanvraag_bag_api_individuele_bevragingen_productie) in te vullen.  Voor vragen, neem contact op met de LVBAG beheerder o.v.v. BAG API 2.0. We zijn aan het kijken naar een geschikt medium hiervoor, mede ook om de API iteratief te kunnen opstellen of doorontwikkelen samen met de community. Als de API iets (nog) niet kan, wat u wel graag wilt, neem dan contact op.
 */
@RegisterRestClient(configKey = "BAG-API-Client")
@RegisterClientHeaders(BAGClientHeadersFactory.class)
@RegisterProvider(RuntimeExceptionMapper.class)
@Timeout(unit = ChronoUnit.SECONDS, value = 5)
@Path("/verblijfsobjecten")
public interface VerblijfsobjectApi {

    /**
     * bevragen 1 verblijfsobject met de identificatie van een verblijfsobject.
     * <p>
     * Bevragen/raadplegen van één voorkomen van een Verblijfsobject met de identificatie van een verblijfsobject. Parameter huidig kan worden toegepast, zie [functionele specificatie huidig](https://github.com/lvbag/BAG-API/blob/master/Features/huidig.feature). De geldigOp en beschikbaarOp parameters kunnen gebruikt worden voor  tijdreis vragen, zie  [functionele specificatie tijdreizen](https://github.com/lvbag/BAG-API/blob/master/Features/tijdreizen.feature).  Als expand&#x3D;heeftAlsHoofdAdres, heeftAlsNevenAdres, maaktDeelUitVan of true, dan worden de gevraagde of alle gerelateerde objecten als geneste resource geleverd, zie [functionele specificatie expand](https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature).
     */
    @GET
    @Path("/{identificatie}")
    @Produces({"application/hal+json", "application/problem+json"})
    public VerblijfsobjectIOHal verblijfsobjectIdentificatie(
            @PathParam("identificatie") String identificatie,
            @QueryParam("geldigOp") LocalDate geldigOp,
            @QueryParam("beschikbaarOp") OffsetDateTime beschikbaarOp,
            @QueryParam("expand") String expand,
            @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs,
            @QueryParam("huidig") @DefaultValue("false") Boolean huidig)
            throws ProcessingException;

    /**
     * bevragen voorkomen van een verblijfsobject, op basis van de identificatie van een verblijfsobject en de identificatie van een voorkomen
     * <p>
     * Bevragen/raadplegen van een voorkomen van een verblijfsobject, met de identificatie van een verblijfsobject en de identificatie van een voorkomen, bestaande uit een versie en een timestamp van het tijdstip van registratie in de LV BAG.
     */
    @GET
    @Path("/{identificatie}/{versie}/{timestampRegistratieLv}")
    @Produces({"application/hal+json", "application/problem+json"})
    public VerblijfsobjectIOHal verblijfsobjectIdentificatieVoorkomen(
            @PathParam("identificatie") String identificatie,
            @PathParam("versie") Integer versie,
            @PathParam("timestampRegistratieLv") String timestampRegistratieLv,
            @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs)
            throws ProcessingException;

    /**
     * bevragen levenscyclus van een verblijfsobject met de identificatie van een verblijfsobject.
     * <p>
     * Bevragen/raadplegen van de levenscyclus van een Verblijfsobject met de identificatie van de verblijfsobject.
     */
    @GET
    @Path("/{identificatie}/lvc")
    @Produces({"application/hal+json", "application/problem+json"})
    public VerblijfsobjectIOLvcHalCollection verblijfsobjectLvcIdentificatie(
            @PathParam("identificatie") String identificatie,
            @QueryParam("geheleLvc") @DefaultValue("false") Boolean geheleLvc,
            @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs)
            throws ProcessingException;

    /**
     * Zoeken van alle aan een pand gerelateerde verblijfsobjecten of binnen een bounding box (met paginering).
     * <p>
     * Zoek verblijfsobjecten:  1. gerelateerd aan een pand identificatie.  2. binnen een geometrische contour (rechthoek) in combinatie met status geconstateerd, oppervlakte, gebruiksdoel.   Parameter huidig kan worden toegepast, zie [functionele specificatie huidig](https://github.com/lvbag/BAG-API/blob/master/Features/huidig.feature).  De geldigOp en beschikbaarOp parameters kunnen gebruikt worden voor  tijdreis vragen, zie  [functionele specificatie tijdreizen](https://github.com/lvbag/BAG-API/blob/master/Features/tijdreizen.feature).   Als expand&#x3D;heeftAlsHoofdAdres, heeftAlsNevenAdres of true, dan worden de gevraagde of alle gerelateerde objecten als geneste resource geleverd, zie [functionele specificatie expand](https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature).  Voor paginering, zie: [functionele specificatie paginering](https://github.com/lvbag/BAG-API/blob/master/Features/paginering.feature).
     */
    @GET
    @Produces({"application/hal+json", "application/problem+json"})
    public VerblijfsobjectIOHalCollection zoekVerblijfsobjecten(
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
            @QueryParam("gebruiksdoelen") List<Gebruiksdoel> gebruiksdoelen)
            throws ProcessingException;
}
