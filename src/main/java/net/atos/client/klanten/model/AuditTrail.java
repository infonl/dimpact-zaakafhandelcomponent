/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.klanten.model;

import java.lang.reflect.Type;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

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

public class AuditTrail {

  /**
   * Unieke identificatie van de audit regel.
   **/
  @JsonbProperty("uuid")
  private UUID uuid;

  @JsonbTypeSerializer(BronEnum.Serializer.class)
  @JsonbTypeDeserializer(BronEnum.Deserializer.class)
  public enum BronEnum {
    AC(String.valueOf("ac")),
    NRC(String.valueOf("nrc")),
    ZRC(String.valueOf("zrc")),
    ZTC(String.valueOf("ztc")),
    DRC(String.valueOf("drc")),
    BRC(String.valueOf("brc")),
    CMC(String.valueOf("cmc")),
    KC(String.valueOf("kc"));

    String value;

    BronEnum(String v) {
      value = v;
    }

    public String value() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static final class Deserializer implements JsonbDeserializer<BronEnum> {
      @Override
      public BronEnum deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
        for (BronEnum b : BronEnum.values()) {
          if (String.valueOf(b.value).equals(parser.getString())) {
            return b;
          }
        }
        throw new IllegalArgumentException("Unexpected value '" + parser.getString() + "'");
      }
    }

    public static final class Serializer implements JsonbSerializer<BronEnum> {
      @Override
      public void serialize(BronEnum obj, JsonGenerator generator, SerializationContext ctx) {
        generator.write(obj.value);
      }
    }
  }

  /**
   * De naam van het component waar de wijziging in is gedaan.  Uitleg bij mogelijke waarden:  * `ac` - Autorisaties API * `nrc` - Notificaties API * `zrc` - Zaken API * `ztc` - Catalogi API * `drc` - Documenten API * `brc` - Besluiten API * `cmc` - Contactmomenten API * `kc` - Klanten API
   **/
  @JsonbProperty("bron")
  private BronEnum bron;

  /**
   * Unieke identificatie van de applicatie, binnen de organisatie.
   **/
  @JsonbProperty("applicatieId")
  private String applicatieId;

  /**
   * Vriendelijke naam van de applicatie.
   **/
  @JsonbProperty("applicatieWeergave")
  private String applicatieWeergave;

  /**
   * Unieke identificatie van de gebruiker die binnen de organisatie herleid kan worden naar een persoon.
   **/
  @JsonbProperty("gebruikersId")
  private String gebruikersId;

  /**
   * Vriendelijke naam van de gebruiker.
   **/
  @JsonbProperty("gebruikersWeergave")
  private String gebruikersWeergave;

  /**
   * De uitgevoerde handeling.  De bekende waardes voor dit veld zijn hieronder aangegeven,                         maar andere waardes zijn ook toegestaan  Uitleg bij mogelijke waarden:  * `create` - Object aangemaakt * `list` - Lijst van objecten opgehaald * `retrieve` - Object opgehaald * `destroy` - Object verwijderd * `update` - Object bijgewerkt * `partial_update` - Object deels bijgewerkt
   **/
  @JsonbProperty("actie")
  private String actie;

  /**
   * Vriendelijke naam van de actie.
   **/
  @JsonbProperty("actieWeergave")
  private String actieWeergave;

  /**
   * HTTP status code van de API response van de uitgevoerde handeling.
   **/
  @JsonbProperty("resultaat")
  private Integer resultaat;

  /**
   * De URL naar het hoofdobject van een component.
   **/
  @JsonbProperty("hoofdObject")
  private URI hoofdObject;

  /**
   * Het type resource waarop de actie gebeurde.
   **/
  @JsonbProperty("resource")
  private String resource;

  /**
   * De URL naar het object.
   **/
  @JsonbProperty("resourceUrl")
  private URI resourceUrl;

  /**
   * Toelichting waarom de handeling is uitgevoerd.
   **/
  @JsonbProperty("toelichting")
  private String toelichting;

  /**
   * Vriendelijke identificatie van het object.
   **/
  @JsonbProperty("resourceWeergave")
  private String resourceWeergave;

  /**
   * De datum waarop de handeling is gedaan.
   **/
  @JsonbProperty("aanmaakdatum")
  private OffsetDateTime aanmaakdatum;

  @JsonbProperty("wijzigingen")
  private Wijzigingen wijzigingen;

  public AuditTrail() {}

  @JsonbCreator
  public AuditTrail(
      @JsonbProperty(value = "aanmaakdatum", nillable = true) OffsetDateTime aanmaakdatum) {
    this.aanmaakdatum = aanmaakdatum;
  }

  /**
   * Unieke identificatie van de audit regel.
   * @return uuid
   **/
  public UUID getUuid() {
    return uuid;
  }

  /**
   * Set uuid
   **/
  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public AuditTrail uuid(UUID uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * De naam van het component waar de wijziging in is gedaan.  Uitleg bij mogelijke waarden:  * &#x60;ac&#x60; - Autorisaties API * &#x60;nrc&#x60; - Notificaties API * &#x60;zrc&#x60; - Zaken API * &#x60;ztc&#x60; - Catalogi API * &#x60;drc&#x60; - Documenten API * &#x60;brc&#x60; - Besluiten API * &#x60;cmc&#x60; - Contactmomenten API * &#x60;kc&#x60; - Klanten API
   * @return bron
   **/
  public BronEnum getBron() {
    return bron;
  }

  /**
   * Set bron
   **/
  public void setBron(BronEnum bron) {
    this.bron = bron;
  }

  public AuditTrail bron(BronEnum bron) {
    this.bron = bron;
    return this;
  }

  /**
   * Unieke identificatie van de applicatie, binnen de organisatie.
   * @return applicatieId
   **/
  public String getApplicatieId() {
    return applicatieId;
  }

  /**
   * Set applicatieId
   **/
  public void setApplicatieId(String applicatieId) {
    this.applicatieId = applicatieId;
  }

  public AuditTrail applicatieId(String applicatieId) {
    this.applicatieId = applicatieId;
    return this;
  }

  /**
   * Vriendelijke naam van de applicatie.
   * @return applicatieWeergave
   **/
  public String getApplicatieWeergave() {
    return applicatieWeergave;
  }

  /**
   * Set applicatieWeergave
   **/
  public void setApplicatieWeergave(String applicatieWeergave) {
    this.applicatieWeergave = applicatieWeergave;
  }

  public AuditTrail applicatieWeergave(String applicatieWeergave) {
    this.applicatieWeergave = applicatieWeergave;
    return this;
  }

  /**
   * Unieke identificatie van de gebruiker die binnen de organisatie herleid kan worden naar een persoon.
   * @return gebruikersId
   **/
  public String getGebruikersId() {
    return gebruikersId;
  }

  /**
   * Set gebruikersId
   **/
  public void setGebruikersId(String gebruikersId) {
    this.gebruikersId = gebruikersId;
  }

  public AuditTrail gebruikersId(String gebruikersId) {
    this.gebruikersId = gebruikersId;
    return this;
  }

  /**
   * Vriendelijke naam van de gebruiker.
   * @return gebruikersWeergave
   **/
  public String getGebruikersWeergave() {
    return gebruikersWeergave;
  }

  /**
   * Set gebruikersWeergave
   **/
  public void setGebruikersWeergave(String gebruikersWeergave) {
    this.gebruikersWeergave = gebruikersWeergave;
  }

  public AuditTrail gebruikersWeergave(String gebruikersWeergave) {
    this.gebruikersWeergave = gebruikersWeergave;
    return this;
  }

  /**
   * De uitgevoerde handeling.  De bekende waardes voor dit veld zijn hieronder aangegeven,                         maar andere waardes zijn ook toegestaan  Uitleg bij mogelijke waarden:  * &#x60;create&#x60; - Object aangemaakt * &#x60;list&#x60; - Lijst van objecten opgehaald * &#x60;retrieve&#x60; - Object opgehaald * &#x60;destroy&#x60; - Object verwijderd * &#x60;update&#x60; - Object bijgewerkt * &#x60;partial_update&#x60; - Object deels bijgewerkt
   * @return actie
   **/
  public String getActie() {
    return actie;
  }

  /**
   * Set actie
   **/
  public void setActie(String actie) {
    this.actie = actie;
  }

  public AuditTrail actie(String actie) {
    this.actie = actie;
    return this;
  }

  /**
   * Vriendelijke naam van de actie.
   * @return actieWeergave
   **/
  public String getActieWeergave() {
    return actieWeergave;
  }

  /**
   * Set actieWeergave
   **/
  public void setActieWeergave(String actieWeergave) {
    this.actieWeergave = actieWeergave;
  }

  public AuditTrail actieWeergave(String actieWeergave) {
    this.actieWeergave = actieWeergave;
    return this;
  }

  /**
   * HTTP status code van de API response van de uitgevoerde handeling.
   * minimum: 100
   * maximum: 599
   * @return resultaat
   **/
  public Integer getResultaat() {
    return resultaat;
  }

  /**
   * Set resultaat
   **/
  public void setResultaat(Integer resultaat) {
    this.resultaat = resultaat;
  }

  public AuditTrail resultaat(Integer resultaat) {
    this.resultaat = resultaat;
    return this;
  }

  /**
   * De URL naar het hoofdobject van een component.
   * @return hoofdObject
   **/
  public URI getHoofdObject() {
    return hoofdObject;
  }

  /**
   * Set hoofdObject
   **/
  public void setHoofdObject(URI hoofdObject) {
    this.hoofdObject = hoofdObject;
  }

  public AuditTrail hoofdObject(URI hoofdObject) {
    this.hoofdObject = hoofdObject;
    return this;
  }

  /**
   * Het type resource waarop de actie gebeurde.
   * @return resource
   **/
  public String getResource() {
    return resource;
  }

  /**
   * Set resource
   **/
  public void setResource(String resource) {
    this.resource = resource;
  }

  public AuditTrail resource(String resource) {
    this.resource = resource;
    return this;
  }

  /**
   * De URL naar het object.
   * @return resourceUrl
   **/
  public URI getResourceUrl() {
    return resourceUrl;
  }

  /**
   * Set resourceUrl
   **/
  public void setResourceUrl(URI resourceUrl) {
    this.resourceUrl = resourceUrl;
  }

  public AuditTrail resourceUrl(URI resourceUrl) {
    this.resourceUrl = resourceUrl;
    return this;
  }

  /**
   * Toelichting waarom de handeling is uitgevoerd.
   * @return toelichting
   **/
  public String getToelichting() {
    return toelichting;
  }

  /**
   * Set toelichting
   **/
  public void setToelichting(String toelichting) {
    this.toelichting = toelichting;
  }

  public AuditTrail toelichting(String toelichting) {
    this.toelichting = toelichting;
    return this;
  }

  /**
   * Vriendelijke identificatie van het object.
   * @return resourceWeergave
   **/
  public String getResourceWeergave() {
    return resourceWeergave;
  }

  /**
   * Set resourceWeergave
   **/
  public void setResourceWeergave(String resourceWeergave) {
    this.resourceWeergave = resourceWeergave;
  }

  public AuditTrail resourceWeergave(String resourceWeergave) {
    this.resourceWeergave = resourceWeergave;
    return this;
  }

  /**
   * De datum waarop de handeling is gedaan.
   * @return aanmaakdatum
   **/
  public OffsetDateTime getAanmaakdatum() {
    return aanmaakdatum;
  }

  /**
   * Get wijzigingen
   * @return wijzigingen
   **/
  public Wijzigingen getWijzigingen() {
    return wijzigingen;
  }

  /**
   * Set wijzigingen
   **/
  public void setWijzigingen(Wijzigingen wijzigingen) {
    this.wijzigingen = wijzigingen;
  }

  public AuditTrail wijzigingen(Wijzigingen wijzigingen) {
    this.wijzigingen = wijzigingen;
    return this;
  }

  /**
   * Create a string representation of this pojo.
   **/
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuditTrail {\n");

    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    bron: ").append(toIndentedString(bron)).append("\n");
    sb.append("    applicatieId: ").append(toIndentedString(applicatieId)).append("\n");
    sb.append("    applicatieWeergave: ").append(toIndentedString(applicatieWeergave)).append("\n");
    sb.append("    gebruikersId: ").append(toIndentedString(gebruikersId)).append("\n");
    sb.append("    gebruikersWeergave: ").append(toIndentedString(gebruikersWeergave)).append("\n");
    sb.append("    actie: ").append(toIndentedString(actie)).append("\n");
    sb.append("    actieWeergave: ").append(toIndentedString(actieWeergave)).append("\n");
    sb.append("    resultaat: ").append(toIndentedString(resultaat)).append("\n");
    sb.append("    hoofdObject: ").append(toIndentedString(hoofdObject)).append("\n");
    sb.append("    resource: ").append(toIndentedString(resource)).append("\n");
    sb.append("    resourceUrl: ").append(toIndentedString(resourceUrl)).append("\n");
    sb.append("    toelichting: ").append(toIndentedString(toelichting)).append("\n");
    sb.append("    resourceWeergave: ").append(toIndentedString(resourceWeergave)).append("\n");
    sb.append("    aanmaakdatum: ").append(toIndentedString(aanmaakdatum)).append("\n");
    sb.append("    wijzigingen: ").append(toIndentedString(wijzigingen)).append("\n");
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
