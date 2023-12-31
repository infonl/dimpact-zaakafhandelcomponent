/**
 * Contactmomenten API
 * Een API om contactmomenten met klanten te registreren of op te vragen.  **Afhankelijkheden**  Deze API is afhankelijk van:  * Autorisaties API * Notificaties API * Klanten API * Zaken API *(optioneel)* * Verzoeken API *(optioneel)* * Documenten API *(optioneel)*  **Autorisatie**  Deze API vereist autorisatie. Je kan de [token-tool](https://zaken-auth.vng.cloud/) gebruiken om JWT-tokens te genereren.  ** Notificaties  Deze API publiceert notificaties op het kanaal `contactmomenten`.  **Main resource**  `contactmoment`    **Kenmerken**  * `bronorganisatie`: Het RSIN van de Niet-natuurlijk persoon zijnde de organisatie die de klantinteractie heeft gecreeerd. Dit moet een geldig RSIN zijn van 9 nummers en voldoen aan https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef * `kanaal`: Het communicatiekanaal waarlangs het CONTACTMOMENT gevoerd wordt  **Resources en acties**   **Handige links**  * [Documentatie](https://zaakgerichtwerken.vng.cloud/standaard) * [Zaakgericht werken](https://zaakgerichtwerken.vng.cloud)
 *
 * The version of the OpenAPI document: 1.0.0
 * Contact: standaarden.ondersteuning@vng.nl
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package net.atos.client.contactmomenten.model;

import java.lang.reflect.Type;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;


public class ContactMoment  {

 /**
   * URL-referentie naar dit object. Dit is de unieke identificatie en locatie van dit object.
  **/
  @JsonbProperty("url")
  private URI url;

 /**
   * URL-referentie naar het vorige CONTACTMOMENT.
  **/
  @JsonbProperty("vorigContactmoment")
  private URI vorigContactmoment;

 /**
   * URL-referentie naar het volgende CONTACTMOMENT.
  **/
  @JsonbProperty("volgendContactmoment")
  private URI volgendContactmoment;

 /**
   * Het RSIN van de Niet-natuurlijk persoon zijnde de organisatie die de klantinteractie heeft gecreeerd. Dit moet een geldig RSIN zijn van 9 nummers en voldoen aan https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef
  **/
  @JsonbProperty("bronorganisatie")
  private String bronorganisatie;

 /**
   * De datum en het tijdstip waarop het CONTACTMOMENT is geregistreerd.
  **/
  @JsonbProperty("registratiedatum")
  private OffsetDateTime registratiedatum;

 /**
   * Het communicatiekanaal waarlangs het CONTACTMOMENT gevoerd wordt
  **/
  @JsonbProperty("kanaal")
  private String kanaal;

 /**
   * Het communicatiekanaal dat voor opvolging van de klantinteractie de voorkeur heeft van de KLANT.
  **/
  @JsonbProperty("voorkeurskanaal")
  private String voorkeurskanaal;

 /**
   * Een ISO 639-2/B taalcode waarin de inhoud van het INFORMATIEOBJECT is vastgelegd. Voorbeeld: `nld`. Zie: https://www.iso.org/standard/4767.html
  **/
  @JsonbProperty("voorkeurstaal")
  private String voorkeurstaal;

 /**
   * Een toelichting die inhoudelijk de klantinteractie van de klant beschrijft.
  **/
  @JsonbProperty("tekst")
  private String tekst;

 /**
   * Eén of meerdere links naar een product, webpagina of andere entiteit zodat contactmomenten gegroepeerd kunnen worden op onderwerp.
  **/
  @JsonbProperty("onderwerpLinks")
  private List<URI> onderwerpLinks = null;

  @JsonbTypeSerializer(InitiatiefnemerEnum.Serializer.class)
  @JsonbTypeDeserializer(InitiatiefnemerEnum.Deserializer.class)
  public enum InitiatiefnemerEnum {

    GEMEENTE(String.valueOf("gemeente")), KLANT(String.valueOf("klant"));


    String value;

    InitiatiefnemerEnum (String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public static final class Deserializer implements JsonbDeserializer<InitiatiefnemerEnum> {
        @Override
        public InitiatiefnemerEnum deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            for (InitiatiefnemerEnum b : InitiatiefnemerEnum.values()) {
                if (String.valueOf(b.value).equals(parser.getString())) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + parser.getString() + "'");
        }
    }

    public static final class Serializer implements JsonbSerializer<InitiatiefnemerEnum> {
        @Override
        public void serialize(InitiatiefnemerEnum obj, JsonGenerator generator, SerializationContext ctx) {
            generator.write(obj.value);
        }
    }
  }

 /**
   * De partij die het contact heeft geïnitieerd.
  **/
  @JsonbProperty("initiatiefnemer")
  private InitiatiefnemerEnum initiatiefnemer;

 /**
   * URL-referentie naar een medewerker
  **/
  @JsonbProperty("medewerker")
  private URI medewerker;

  @JsonbProperty("medewerkerIdentificatie")
  private Medewerker medewerkerIdentificatie;

  public ContactMoment() {
  }

 @JsonbCreator
  public ContactMoment(
    @JsonbProperty(value = "url", nillable = true) URI url,
    @JsonbProperty(value = "volgendContactmoment", nillable = true) URI volgendContactmoment
  ) {
    this.url = url;
    this.volgendContactmoment = volgendContactmoment;
  }

 /**
   * URL-referentie naar dit object. Dit is de unieke identificatie en locatie van dit object.
   * @return url
  **/
  public URI getUrl() {
    return url;
  }


 /**
   * URL-referentie naar het vorige CONTACTMOMENT.
   * @return vorigContactmoment
  **/
  public URI getVorigContactmoment() {
    return vorigContactmoment;
  }

  /**
    * Set vorigContactmoment
  **/
  public void setVorigContactmoment(URI vorigContactmoment) {
    this.vorigContactmoment = vorigContactmoment;
  }

  public ContactMoment vorigContactmoment(URI vorigContactmoment) {
    this.vorigContactmoment = vorigContactmoment;
    return this;
  }

 /**
   * URL-referentie naar het volgende CONTACTMOMENT.
   * @return volgendContactmoment
  **/
  public URI getVolgendContactmoment() {
    return volgendContactmoment;
  }


 /**
   * Het RSIN van de Niet-natuurlijk persoon zijnde de organisatie die de klantinteractie heeft gecreeerd. Dit moet een geldig RSIN zijn van 9 nummers en voldoen aan https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef
   * @return bronorganisatie
  **/
  public String getBronorganisatie() {
    return bronorganisatie;
  }

  /**
    * Set bronorganisatie
  **/
  public void setBronorganisatie(String bronorganisatie) {
    this.bronorganisatie = bronorganisatie;
  }

  public ContactMoment bronorganisatie(String bronorganisatie) {
    this.bronorganisatie = bronorganisatie;
    return this;
  }

 /**
   * De datum en het tijdstip waarop het CONTACTMOMENT is geregistreerd.
   * @return registratiedatum
  **/
  public OffsetDateTime getRegistratiedatum() {
    return registratiedatum;
  }

  /**
    * Set registratiedatum
  **/
  public void setRegistratiedatum(OffsetDateTime registratiedatum) {
    this.registratiedatum = registratiedatum;
  }

  public ContactMoment registratiedatum(OffsetDateTime registratiedatum) {
    this.registratiedatum = registratiedatum;
    return this;
  }

 /**
   * Het communicatiekanaal waarlangs het CONTACTMOMENT gevoerd wordt
   * @return kanaal
  **/
  public String getKanaal() {
    return kanaal;
  }

  /**
    * Set kanaal
  **/
  public void setKanaal(String kanaal) {
    this.kanaal = kanaal;
  }

  public ContactMoment kanaal(String kanaal) {
    this.kanaal = kanaal;
    return this;
  }

 /**
   * Het communicatiekanaal dat voor opvolging van de klantinteractie de voorkeur heeft van de KLANT.
   * @return voorkeurskanaal
  **/
  public String getVoorkeurskanaal() {
    return voorkeurskanaal;
  }

  /**
    * Set voorkeurskanaal
  **/
  public void setVoorkeurskanaal(String voorkeurskanaal) {
    this.voorkeurskanaal = voorkeurskanaal;
  }

  public ContactMoment voorkeurskanaal(String voorkeurskanaal) {
    this.voorkeurskanaal = voorkeurskanaal;
    return this;
  }

 /**
   * Een ISO 639-2/B taalcode waarin de inhoud van het INFORMATIEOBJECT is vastgelegd. Voorbeeld: &#x60;nld&#x60;. Zie: https://www.iso.org/standard/4767.html
   * @return voorkeurstaal
  **/
  public String getVoorkeurstaal() {
    return voorkeurstaal;
  }

  /**
    * Set voorkeurstaal
  **/
  public void setVoorkeurstaal(String voorkeurstaal) {
    this.voorkeurstaal = voorkeurstaal;
  }

  public ContactMoment voorkeurstaal(String voorkeurstaal) {
    this.voorkeurstaal = voorkeurstaal;
    return this;
  }

 /**
   * Een toelichting die inhoudelijk de klantinteractie van de klant beschrijft.
   * @return tekst
  **/
  public String getTekst() {
    return tekst;
  }

  /**
    * Set tekst
  **/
  public void setTekst(String tekst) {
    this.tekst = tekst;
  }

  public ContactMoment tekst(String tekst) {
    this.tekst = tekst;
    return this;
  }

 /**
   * Eén of meerdere links naar een product, webpagina of andere entiteit zodat contactmomenten gegroepeerd kunnen worden op onderwerp.
   * @return onderwerpLinks
  **/
  public List<URI> getOnderwerpLinks() {
    return onderwerpLinks;
  }

  /**
    * Set onderwerpLinks
  **/
  public void setOnderwerpLinks(List<URI> onderwerpLinks) {
    this.onderwerpLinks = onderwerpLinks;
  }

  public ContactMoment onderwerpLinks(List<URI> onderwerpLinks) {
    this.onderwerpLinks = onderwerpLinks;
    return this;
  }

  public ContactMoment addOnderwerpLinksItem(URI onderwerpLinksItem) {
    if (this.onderwerpLinks == null) {
      this.onderwerpLinks = new ArrayList<>();
    }
    this.onderwerpLinks.add(onderwerpLinksItem);
    return this;
  }

 /**
   * De partij die het contact heeft geïnitieerd.
   * @return initiatiefnemer
  **/
  public InitiatiefnemerEnum getInitiatiefnemer() {
    return initiatiefnemer;
  }

  /**
    * Set initiatiefnemer
  **/
  public void setInitiatiefnemer(InitiatiefnemerEnum initiatiefnemer) {
    this.initiatiefnemer = initiatiefnemer;
  }

  public ContactMoment initiatiefnemer(InitiatiefnemerEnum initiatiefnemer) {
    this.initiatiefnemer = initiatiefnemer;
    return this;
  }

 /**
   * URL-referentie naar een medewerker
   * @return medewerker
  **/
  public URI getMedewerker() {
    return medewerker;
  }

  /**
    * Set medewerker
  **/
  public void setMedewerker(URI medewerker) {
    this.medewerker = medewerker;
  }

  public ContactMoment medewerker(URI medewerker) {
    this.medewerker = medewerker;
    return this;
  }

 /**
   * Get medewerkerIdentificatie
   * @return medewerkerIdentificatie
  **/
  public Medewerker getMedewerkerIdentificatie() {
    return medewerkerIdentificatie;
  }

  /**
    * Set medewerkerIdentificatie
  **/
  public void setMedewerkerIdentificatie(Medewerker medewerkerIdentificatie) {
    this.medewerkerIdentificatie = medewerkerIdentificatie;
  }

  public ContactMoment medewerkerIdentificatie(Medewerker medewerkerIdentificatie) {
    this.medewerkerIdentificatie = medewerkerIdentificatie;
    return this;
  }


  /**
    * Create a string representation of this pojo.
  **/
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ContactMoment {\n");

    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    vorigContactmoment: ").append(toIndentedString(vorigContactmoment)).append("\n");
    sb.append("    volgendContactmoment: ").append(toIndentedString(volgendContactmoment)).append("\n");
    sb.append("    bronorganisatie: ").append(toIndentedString(bronorganisatie)).append("\n");
    sb.append("    registratiedatum: ").append(toIndentedString(registratiedatum)).append("\n");
    sb.append("    kanaal: ").append(toIndentedString(kanaal)).append("\n");
    sb.append("    voorkeurskanaal: ").append(toIndentedString(voorkeurskanaal)).append("\n");
    sb.append("    voorkeurstaal: ").append(toIndentedString(voorkeurstaal)).append("\n");
    sb.append("    tekst: ").append(toIndentedString(tekst)).append("\n");
    sb.append("    onderwerpLinks: ").append(toIndentedString(onderwerpLinks)).append("\n");
    sb.append("    initiatiefnemer: ").append(toIndentedString(initiatiefnemer)).append("\n");
    sb.append("    medewerker: ").append(toIndentedString(medewerker)).append("\n");
    sb.append("    medewerkerIdentificatie: ").append(toIndentedString(medewerkerIdentificatie)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private static String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
