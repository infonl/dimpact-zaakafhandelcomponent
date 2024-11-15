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
import net.atos.client.bag.model.generated.OpenbareRuimteIOHal;
import net.atos.client.bag.model.generated.OpenbareRuimteIOHalCollection;
import net.atos.client.bag.model.generated.OpenbareRuimteIOLvcHalCollection;
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
@Path("/openbareruimten")
public interface OpenbareRuimteApi {

    /**
     * bevragen van een openbare ruimte met de identificatie van een openbare ruimte.
     * <p>
     * Bevragen/raadplegen van een openbare ruimte met de identificatie van een openbare ruimte. Parameter huidig kan worden toegepast, zie
     * [functionele specificatie huidig](https://github.com/lvbag/BAG-API/blob/master/Features/huidig.feature). De geldigOp en beschikbaarOp
     * parameters kunnen gebruikt worden voor tijdreis vragen, zie [functionele specificatie
     * tijdreizen](https://github.com/lvbag/BAG-API/blob/master/Features/tijdreizen.feature). Als expand&#x3D;ligtInWoonplaats of true dan
     * wordt de woonplaats als geneste resource geleverd, zie [functionele specificatie
     * expand](https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature).
     */
    @GET
    @Path("/{openbareRuimteIdentificatie}")
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    OpenbareRuimteIOHal openbareruimteIdentificatie(
            @PathParam("openbareRuimteIdentificatie") String openbareRuimteIdentificatie,
            @QueryParam("geldigOp") LocalDate geldigOp,
            @QueryParam("beschikbaarOp") OffsetDateTime beschikbaarOp,
            @QueryParam("expand") String expand,
            @QueryParam("huidig") @DefaultValue("false") Boolean huidig
    ) throws ProcessingException;

    /**
     * bevragen van een voorkomen van een openbare ruimte met de identificatie van een openbare ruimte en de identificatie van een
     * voorkomen, bestaande uit een versie en een timestamp van het tijdstip van registratie in de LV BAG.
     * <p>
     * Bevragen/raadplegen van een voorkomen van een openbare ruimte met de identificatie van een openbare ruimte en de identificatie van
     * een voorkomen, bestaande uit een versie en een timestamp van het tijdstip van registratie in de LV BAG.
     */
    @GET
    @Path("/{openbareRuimteIdentificatie}/{versie}/{timestampRegistratieLv}")
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    OpenbareRuimteIOHal openbareruimteIdentificatieVoorkomen(
            @PathParam("openbareRuimteIdentificatie") String openbareRuimteIdentificatie,
            @PathParam("versie") Integer versie,
            @PathParam("timestampRegistratieLv") String timestampRegistratieLv
    ) throws ProcessingException;

    /**
     * bevragen levenscyclus van een openbare ruimte met de identificatie van een openbare ruimte.
     * <p>
     * Bevragen/raadplegen van de levenscyclus van één openbare ruimte, via de identificatie van een openbare ruimte.
     */
    @GET
    @Path("/{openbareRuimteIdentificatie}/lvc")
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    OpenbareRuimteIOLvcHalCollection openbareruimteLvcIdentificatie(
            @PathParam("openbareRuimteIdentificatie") String openbareRuimteIdentificatie,
            @QueryParam("geheleLvc") @DefaultValue("false") Boolean geheleLvc
    ) throws ProcessingException;

    /**
     * bevragen openbare ruimte(n) op basis van de verschillende combinaties van parameters.
     * <p>
     * De volgende (combinaties van) parameters worden ondersteund: 1. Bevragen/raadplegen van een openbare ruimte met een woonplaats naam
     * en een openbare ruimte naam. Als expand&#x3D;ligtInWoonplaats of true dan wordt de woonplaats als geneste resource geleverd, zie
     * [functionele specificatie expand](https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature). 2. Bevragen/raadplegen van
     * een openbare ruimte met een woonplaats identificatie en een openbare ruimte naam. Als expand&#x3D;ligtInWoonplaats of true dan wordt
     * de woonplaats als geneste resource geleverd, zie [functionele specificatie
     * expand](https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature). 3. Bevragen/zoeken van alle aan een woonplaats
     * gerelateerde openbare ruimten met de woonplaats identificatie. Parameter huidig kan worden toegepast, zie [functionele specificatie
     * huidig](https://github.com/lvbag/BAG-API/blob/master/Features/huidig.feature). Expand wordt niet ondersteund. Bij alle bovenstaande
     * combinaties wordt paginering ondersteund en kunnen de parameters geldigOp en beschikbaarOp worden gebruikt. Voor paginering, zie:
     * [functionele specificatie paginering](https://github.com/lvbag/BAG-API/blob/master/Features/paginering.feature). De geldigOp en
     * beschikbaarOp parameters kunnen gebruikt worden voor tijdreis vragen, zie [functionele specificatie
     * tijdreizen](https://github.com/lvbag/BAG-API/blob/master/Features/tijdreizen.feature).
     */
    @GET
    @Produces({MediaTypes.MEDIA_TYPE_HAL_JSON, MediaTypes.MEDIA_TYPE_PROBLEM_JSON})
    OpenbareRuimteIOHalCollection zoekOpenbareRuimten(
            @QueryParam("woonplaatsNaam") String woonplaatsNaam,
            @QueryParam("openbareRuimteNaam") String openbareRuimteNaam,
            @QueryParam("woonplaatsIdentificatie") String woonplaatsIdentificatie,
            @QueryParam("huidig") @DefaultValue("false") Boolean huidig,
            @QueryParam("geldigOp") LocalDate geldigOp,
            @QueryParam("beschikbaarOp") OffsetDateTime beschikbaarOp,
            @QueryParam("page") @DefaultValue("1") Integer page,
            @QueryParam("pageSize") @DefaultValue("20") Integer pageSize,
            @QueryParam("expand") String expand
    ) throws ProcessingException;
}
