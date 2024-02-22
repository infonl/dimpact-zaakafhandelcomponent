/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.klanten.model;

import jakarta.json.bind.annotation.JsonbProperty;

public class VerblijfsAdres {

  /**
   * De unieke identificatie van het OBJECT
   **/
  @JsonbProperty("aoaIdentificatie")
  private String aoaIdentificatie;

  @JsonbProperty("wplWoonplaatsNaam")
  private String wplWoonplaatsNaam;

  /**
   * Een door het bevoegde gemeentelijke orgaan aan een OPENBARE RUIMTE toegekende benaming
   **/
  @JsonbProperty("gorOpenbareRuimteNaam")
  private String gorOpenbareRuimteNaam;

  @JsonbProperty("aoaPostcode")
  private String aoaPostcode;

  @JsonbProperty("aoaHuisnummer")
  private Integer aoaHuisnummer;

  @JsonbProperty("aoaHuisletter")
  private String aoaHuisletter;

  @JsonbProperty("aoaHuisnummertoevoeging")
  private String aoaHuisnummertoevoeging;

  @JsonbProperty("inpLocatiebeschrijving")
  private String inpLocatiebeschrijving;

  /**
   * De unieke identificatie van het OBJECT
   * @return aoaIdentificatie
   **/
  public String getAoaIdentificatie() {
    return aoaIdentificatie;
  }

  /**
   * Set aoaIdentificatie
   **/
  public void setAoaIdentificatie(String aoaIdentificatie) {
    this.aoaIdentificatie = aoaIdentificatie;
  }

  public VerblijfsAdres aoaIdentificatie(String aoaIdentificatie) {
    this.aoaIdentificatie = aoaIdentificatie;
    return this;
  }

  /**
   * Get wplWoonplaatsNaam
   * @return wplWoonplaatsNaam
   **/
  public String getWplWoonplaatsNaam() {
    return wplWoonplaatsNaam;
  }

  /**
   * Set wplWoonplaatsNaam
   **/
  public void setWplWoonplaatsNaam(String wplWoonplaatsNaam) {
    this.wplWoonplaatsNaam = wplWoonplaatsNaam;
  }

  public VerblijfsAdres wplWoonplaatsNaam(String wplWoonplaatsNaam) {
    this.wplWoonplaatsNaam = wplWoonplaatsNaam;
    return this;
  }

  /**
   * Een door het bevoegde gemeentelijke orgaan aan een OPENBARE RUIMTE toegekende benaming
   * @return gorOpenbareRuimteNaam
   **/
  public String getGorOpenbareRuimteNaam() {
    return gorOpenbareRuimteNaam;
  }

  /**
   * Set gorOpenbareRuimteNaam
   **/
  public void setGorOpenbareRuimteNaam(String gorOpenbareRuimteNaam) {
    this.gorOpenbareRuimteNaam = gorOpenbareRuimteNaam;
  }

  public VerblijfsAdres gorOpenbareRuimteNaam(String gorOpenbareRuimteNaam) {
    this.gorOpenbareRuimteNaam = gorOpenbareRuimteNaam;
    return this;
  }

  /**
   * Get aoaPostcode
   * @return aoaPostcode
   **/
  public String getAoaPostcode() {
    return aoaPostcode;
  }

  /**
   * Set aoaPostcode
   **/
  public void setAoaPostcode(String aoaPostcode) {
    this.aoaPostcode = aoaPostcode;
  }

  public VerblijfsAdres aoaPostcode(String aoaPostcode) {
    this.aoaPostcode = aoaPostcode;
    return this;
  }

  /**
   * Get aoaHuisnummer
   * minimum: 0
   * maximum: 99999
   * @return aoaHuisnummer
   **/
  public Integer getAoaHuisnummer() {
    return aoaHuisnummer;
  }

  /**
   * Set aoaHuisnummer
   **/
  public void setAoaHuisnummer(Integer aoaHuisnummer) {
    this.aoaHuisnummer = aoaHuisnummer;
  }

  public VerblijfsAdres aoaHuisnummer(Integer aoaHuisnummer) {
    this.aoaHuisnummer = aoaHuisnummer;
    return this;
  }

  /**
   * Get aoaHuisletter
   * @return aoaHuisletter
   **/
  public String getAoaHuisletter() {
    return aoaHuisletter;
  }

  /**
   * Set aoaHuisletter
   **/
  public void setAoaHuisletter(String aoaHuisletter) {
    this.aoaHuisletter = aoaHuisletter;
  }

  public VerblijfsAdres aoaHuisletter(String aoaHuisletter) {
    this.aoaHuisletter = aoaHuisletter;
    return this;
  }

  /**
   * Get aoaHuisnummertoevoeging
   * @return aoaHuisnummertoevoeging
   **/
  public String getAoaHuisnummertoevoeging() {
    return aoaHuisnummertoevoeging;
  }

  /**
   * Set aoaHuisnummertoevoeging
   **/
  public void setAoaHuisnummertoevoeging(String aoaHuisnummertoevoeging) {
    this.aoaHuisnummertoevoeging = aoaHuisnummertoevoeging;
  }

  public VerblijfsAdres aoaHuisnummertoevoeging(String aoaHuisnummertoevoeging) {
    this.aoaHuisnummertoevoeging = aoaHuisnummertoevoeging;
    return this;
  }

  /**
   * Get inpLocatiebeschrijving
   * @return inpLocatiebeschrijving
   **/
  public String getInpLocatiebeschrijving() {
    return inpLocatiebeschrijving;
  }

  /**
   * Set inpLocatiebeschrijving
   **/
  public void setInpLocatiebeschrijving(String inpLocatiebeschrijving) {
    this.inpLocatiebeschrijving = inpLocatiebeschrijving;
  }

  public VerblijfsAdres inpLocatiebeschrijving(String inpLocatiebeschrijving) {
    this.inpLocatiebeschrijving = inpLocatiebeschrijving;
    return this;
  }

  /**
   * Create a string representation of this pojo.
   **/
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VerblijfsAdres {\n");

    sb.append("    aoaIdentificatie: ").append(toIndentedString(aoaIdentificatie)).append("\n");
    sb.append("    wplWoonplaatsNaam: ").append(toIndentedString(wplWoonplaatsNaam)).append("\n");
    sb.append("    gorOpenbareRuimteNaam: ")
        .append(toIndentedString(gorOpenbareRuimteNaam))
        .append("\n");
    sb.append("    aoaPostcode: ").append(toIndentedString(aoaPostcode)).append("\n");
    sb.append("    aoaHuisnummer: ").append(toIndentedString(aoaHuisnummer)).append("\n");
    sb.append("    aoaHuisletter: ").append(toIndentedString(aoaHuisletter)).append("\n");
    sb.append("    aoaHuisnummertoevoeging: ")
        .append(toIndentedString(aoaHuisnummertoevoeging))
        .append("\n");
    sb.append("    inpLocatiebeschrijving: ")
        .append(toIndentedString(inpLocatiebeschrijving))
        .append("\n");
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
