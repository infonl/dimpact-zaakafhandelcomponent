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

import net.atos.client.bag.model.AdresseerbaarObjectIOHal;
import net.atos.client.bag.model.AdresseerbaarObjectLvcIOHalCollection;
import net.atos.client.bag.model.AdresseerbareObjectenIOHalCollection;
import net.atos.client.bag.model.Gebruiksdoel;
import net.atos.client.bag.model.OppervlakteFilter;
import net.atos.client.bag.model.TypeAdresseerbaarObject;
import net.atos.client.bag.util.BAGClientHeadersFactory;
import net.atos.client.bag.util.JsonbConfiguration;
import net.atos.client.brp.exception.RuntimeExceptionMapper;

/**
 * IMBAG API - van de LVBAG
 *
 * <p>Dit is de [BAG API](https://zakelijk.kadaster.nl/-/bag-api) Individuele Bevragingen van de Landelijke Voorziening Basisregistratie Adressen en Gebouwen (LVBAG).  Meer informatie over de Basisregistratie Adressen en Gebouwen is te vinden op de website van het [Ministerie van Binnenlandse Zaken en Koninkrijksrelaties](https://www.geobasisregistraties.nl/basisregistraties/adressen-en-gebouwen) en [Kadaster](https://zakelijk.kadaster.nl/bag).  De BAG API levert informatie conform de [BAG Catalogus 2018](https://www.geobasisregistraties.nl/documenten/publicatie/2018/03/12/catalogus-2018) en het informatiemodel IMBAG 2.0. De API specificatie volgt de [Nederlandse API-Strategie](https://docs.geostandaarden.nl/api/API-Strategie) specificatie versie van 20200204 en is opgesteld in [OpenAPI Specificatie](https://www.forumstandaardisatie.nl/standaard/openapi-specification) (OAS) v3.  Het standaard mediatype HAL (`application/hal+json`) wordt gebruikt. Dit is een mediatype voor het weergeven van resources en hun relaties via hyperlinks.  Deze API is vooral gericht op individuele bevragingen (op basis van de identificerende gegevens van een object). Om gebruik te kunnen maken van de BAG API is een API key nodig, deze kan verkregen worden door het [aanvraagformulier](https://formulieren.kadaster.nl/aanvraag_bag_api_individuele_bevragingen_productie) in te vullen.  Voor vragen, neem contact op met de LVBAG beheerder o.v.v. BAG API 2.0. We zijn aan het kijken naar een geschikt medium hiervoor, mede ook om de API iteratief te kunnen opstellen of doorontwikkelen samen met de community. Als de API iets (nog) niet kan, wat u wel graag wilt, neem dan contact op.
 */
@RegisterRestClient(configKey = "BAG-API-Client")
@RegisterClientHeaders(BAGClientHeadersFactory.class)
@RegisterProvider(RuntimeExceptionMapper.class)
@RegisterProvider(JsonbConfiguration.class)
@Timeout(unit = ChronoUnit.SECONDS, value = 5)
@Path("/adresseerbareobjecten")
public interface AdresseerbaarObjectApi {

  /**
   * Bevragen van één adresseerbaar object met een adresseerbaar  object identificatie.
   * <p>
   * Bevragen van één adresseerbaar object (ligplaats, standplaats of  verblijfsobject) met de identificatie van een adresseerbaar object.  Parameter huidig kan worden toegepast, zie [functionele specificatie huidig](https://github.com/lvbag/BAG-API/blob/master/Features/huidig.feature).  De geldigOp en beschikbaarOp parameters kunnen gebruikt worden voor  tijdreis vragen, zie  [functionele specificatie tijdreizen](https://github.com/lvbag/BAG-API/blob/master/Features/tijdreizen.feature).   Als expand&#x3D;heeftAlsHoofdAdres, heeftAlsNevenAdres, maaktDeelUitVan  (of een combinatie daarvan) of als expand&#x3D;true, dan worden de  gevraagde of alle gerelateerde resources als geneste resource geleverd, zie [functionele specificatie expand](https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature).
   */
  @GET
  @Path("/{adresseerbaarObjectIdentificatie}")
  @Produces({"application/hal+json", "application/problem+json"})
  public AdresseerbaarObjectIOHal bevragenAdresseerbaarObject(
      @PathParam("adresseerbaarObjectIdentificatie") String adresseerbaarObjectIdentificatie,
      @QueryParam("geldigOp") LocalDate geldigOp,
      @QueryParam("beschikbaarOp") OffsetDateTime beschikbaarOp,
      @QueryParam("expand") String expand,
      @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs,
      @QueryParam("huidig") @DefaultValue("false") Boolean huidig)
      throws ProcessingException;

  /**
   * Bevragen van de levenscyclus van een adresseerbaar object met een adresseerbaar object identificatie.
   * <p>
   * Bevragen van de levenscyclus van een adresseerbaar object (ligplaats, standplaats of verblijfsobject) met de identificatie van een adresseerbaar object. Met de geheleLvc parameter kan worden aangegeven of de gehele levenscyclus (inclusief voorkomens die uit de geldige levenscyclus zijn verwijderd) of alleen de actieve levenscyclus (alleen de voorkomens die onderdeel zijn van de geldige levenscyclus) gewenst zijn.
   */
  @GET
  @Path("/{adresseerbaarObjectIdentificatie}/lvc")
  @Produces({"application/hal+json", "application/problem+json"})
  public AdresseerbaarObjectLvcIOHalCollection bevragenAdresseerbaarObjectLvc(
      @PathParam("adresseerbaarObjectIdentificatie") String adresseerbaarObjectIdentificatie,
      @QueryParam("geheleLvc") @DefaultValue("false") Boolean geheleLvc,
      @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs)
      throws ProcessingException;

  /**
   * Bevragen van één of meer adresseerbare objecten op basis van een nummeraanduiding identificatie.
   * <p>
   * Zoek actuele adresseerbare objecten (verblijfsobjecten, standplaatsen of ligplaatsen):  1. met een nummeraanduiding identificatie.  2. binnen een geometrische contour (rechthoek) in combinatie met status geconstateerd, oppervlakte, gebruiksdoel en/of type.  3. met een lijst van pandidentificaties.   Parameter huidig kan worden toegepast, zie [functionele specificatie huidig](https://github.com/lvbag/BAG-API/blob/master/Features/huidig.feature).  De geldigOp en beschikbaarOp parameters kunnen gebruikt worden voor  tijdreis vragen, zie  [functionele specificatie tijdreizen](https://github.com/lvbag/BAG-API/blob/master/Features/tijdreizen.feature).   Als expand&#x3D;heeftAlsHoofdAdres, heeftAlsNevenAdres, maaktDeelUitVan (of een combinatie daarvan) of als expand&#x3D;true, dan worden de gevraagde of alle gerelateerde resources als geneste resource geleverd, zie [functionele specificatie expand](https://github.com/lvbag/BAG-API/blob/master/Features/expand.feature).  Voor paginering, zie: [functionele specificatie paginering](https://github.com/lvbag/BAG-API/blob/master/Features/paginering.feature).
   */
  @GET
  @Produces({"application/hal+json", "application/problem+json"})
  public AdresseerbareObjectenIOHalCollection zoekAdresseerbareObjecten(
      @QueryParam("nummeraanduidingIdentificatie") String nummeraanduidingIdentificatie,
      @QueryParam("huidig") @DefaultValue("false") Boolean huidig,
      @QueryParam("geldigOp") LocalDate geldigOp,
      @QueryParam("beschikbaarOp") OffsetDateTime beschikbaarOp,
      @QueryParam("expand") String expand,
      @HeaderParam("Accept-Crs") @DefaultValue(DEFAULT_CRS) String acceptCrs,
      @HeaderParam("Content-Crs") String contentCrs,
      @QueryParam("page") @DefaultValue("1") Integer page,
      @QueryParam("pageSize") @DefaultValue("20") Integer pageSize,
      @QueryParam("bbox") List<BigDecimal> bbox,
      @QueryParam("geconstateerd") Boolean geconstateerd,
      @QueryParam("oppervlakte") OppervlakteFilter oppervlakte,
      @QueryParam("gebruiksdoelen") List<Gebruiksdoel> gebruiksdoelen,
      @QueryParam("type") TypeAdresseerbaarObject type,
      @QueryParam("pandIdentificaties") List<String> pandIdentificaties)
      throws ProcessingException;
}
