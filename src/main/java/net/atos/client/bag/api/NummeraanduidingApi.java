/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.bag.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

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

import net.atos.client.bag.exception.BagResponseExceptionMapper;
import net.atos.client.bag.model.generated.NummeraanduidingIOHal;
import net.atos.client.bag.model.generated.NummeraanduidingIOHalCollection;
import net.atos.client.bag.model.generated.NummeraanduidingIOLvcHalCollection;
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
@Path("/nummeraanduidingen")
public interface NummeraanduidingApi {

    /**
     * Bevragen van een nummeraanduiding op basis van de identificatie van een nummeraanduiding
     * <p>
     * Bevragen/raadplegen van één nummeraanduiding met de identificatie van een nummeraanduiding. Parameter huidig kan worden toegepast,
     * zie [functionele specificatie huidig](https://github.com/lvbag/BAG-API/blob/master/Features/huidig.feature). De geldigOp en
     * beschikbaarOp parameters kunnen gebruikt worden voor tijdreis vragen, zie [functionele specificatie
     * tijdreizen](https://github.com/lvbag/BAG-API/blob/master/Features/tijdreizen.feature). Als expand&#x3D;ligtInWoonplaats,
     * ligtAanOpenbareRuimte of als expand&#x3D;true dan worden de gevraagde of alle gerelateerde objecten als geneste resources geleverd,
     * zie [functionele specificatie expand](https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature).
     */
    @GET
    @Path("/{nummeraanduidingIdentificatie}")
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    NummeraanduidingIOHal nummeraanduidingIdentificatie(
            @PathParam("nummeraanduidingIdentificatie") String nummeraanduidingIdentificatie,
            @QueryParam("geldigOp") LocalDate geldigOp,
            @QueryParam("beschikbaarOp") OffsetDateTime beschikbaarOp,
            @QueryParam("expand") String expand,
            @QueryParam("huidig") @DefaultValue("false") Boolean huidig
    ) throws ProcessingException;

    /**
     * Bevragen van een voorkomen van een nummeraanduiding, op basis van de identificatie van een nummeraanduiding en de identificatie van
     * een voorkomen, bestaande uit een versie en een timestamp van het tijdstip van registratie in de LV BAG.
     * <p>
     * Bevragen/raadplegen van een voorkomen van een nummeraanduiding met de identificatie van een nummeraanduiding en de identificatie van
     * een voorkomen, bestaande uit een versie en een timestamp van het tijdstip van registratie in de LV BAG.
     */
    @GET
    @Path("/{nummeraanduidingIdentificatie}/{versie}/{timestampRegistratieLv}")
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    NummeraanduidingIOHal nummeraanduidingIdentificatieVoorkomen(
            @PathParam("nummeraanduidingIdentificatie") String nummeraanduidingIdentificatie,
            @PathParam("versie") Integer versie,
            @PathParam("timestampRegistratieLv") String timestampRegistratieLv
    ) throws ProcessingException;

    /**
     * Bevragen levenscyclus van een nummeraanduiding met de identificatie van een nummeraanduiding.
     * <p>
     * Bevragen/raadplegen van de levenscyclus van één nummeraanduiding met de identificatie van een nummeraanduiding.
     */
    @GET
    @Path("/{nummeraanduidingIdentificatie}/lvc")
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    NummeraanduidingIOLvcHalCollection nummeraanduidingLvcIdentificatie(
            @PathParam("nummeraanduidingIdentificatie") String nummeraanduidingIdentificatie,
            @QueryParam("geheleLvc") @DefaultValue("false") Boolean geheleLvc
    ) throws ProcessingException;

    /**
     * Bevragen nummeraanduiding(en) op basis van verschillende combinaties van parameters.
     * <p>
     * De volgende (combinaties van) parameters worden ondersteund: 1. Bevragen/raadplegen van een (collectie van) nummeraanduiding(en) met
     * postcode en huisnummer (evt. met huisletter en huisnummertoevoeging). 2. Bevragen/raadplegen van een (collectie van)
     * nummeraanduiding(en) met woonplaats naam, openbare ruimte naam, huisnummer en optioneel huisnummertoevoeging en huisletter. 3.
     * Bevragen/zoeken van alle aan een openbare ruimte gerelateerde nummeraanduidingen met een openbare ruimte identificatie. Expand wordt
     * niet ondersteund. 4. Bevragen/zoeken van nummeraanduidingen met een pand identificatie. Expand wordt niet ondersteund. Bij de
     * bovenstaande bevragingen kunnen (tenzij anders vermeld) de volgende parameters worden gebruikt: geldigOp, beschikbaarOp, huidig, page
     * en pageSize. Voor paginering, zie: [functionele specificatie
     * paginering](https://github.com/lvbag/BAG-API/blob/master/Features/paginering.feature). De geldigOp en beschikbaarOp parameters kunnen
     * gebruikt worden voor tijdreis vragen, zie [functionele specificatie
     * tijdreizen](https://github.com/lvbag/BAG-API/blob/master/Features/tijdreizen.feature). Parameter huidig kan worden toegepast, zie
     * [functionele specificatie huidig](https://github.com/lvbag/BAG-API/blob/master/Features/huidig.feature). Als
     * expand&#x3D;ligtInWoonplaats, ligtAanOpenbareRuimte of als expand&#x3D;true dan worden de gevraagde of alle gerelateerde objecten als
     * geneste resources geleverd, zie [functionele specificatie
     * expand](https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature). Met de exacteMatch parameter kan worden aangegeven dat
     * alleen object(en) die exact overeenkomen met de opgegeven parameters, geretourneerd moeten worden, zie [functionele specificatie
     * exacte match](https://github.com/lvbag/BAG-API/blob/master/Features/exacte_matchnd.feature).
     */
    @GET
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    NummeraanduidingIOHalCollection zoekNummeraanduiding(
            @QueryParam("postcode") String postcode,
            @QueryParam("huisnummer") Integer huisnummer,
            @QueryParam("huisnummertoevoeging") String huisnummertoevoeging,
            @QueryParam("huisletter") String huisletter,
            @QueryParam("exacteMatch") @DefaultValue("false") Boolean exacteMatch,
            @QueryParam("woonplaatsNaam") String woonplaatsNaam,
            @QueryParam("openbareRuimteNaam") String openbareRuimteNaam,
            @QueryParam("openbareRuimteIdentificatie") String openbareRuimteIdentificatie,
            @QueryParam("huidig") @DefaultValue("false") Boolean huidig,
            @QueryParam("geldigOp") LocalDate geldigOp,
            @QueryParam("beschikbaarOp") OffsetDateTime beschikbaarOp,
            @QueryParam("page") @DefaultValue("1") Integer page,
            @QueryParam("pageSize") @DefaultValue("20") Integer pageSize,
            @QueryParam("expand") String expand,
            @QueryParam("pandIdentificatie") String pandIdentificatie
    ) throws ProcessingException;
}
