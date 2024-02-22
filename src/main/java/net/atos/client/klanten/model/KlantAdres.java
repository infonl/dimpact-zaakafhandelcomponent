/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.klanten.model;

import jakarta.json.bind.annotation.JsonbProperty;

/**
 * Adresgegevens zoals opgegeven door de klant (kan ook een buitenlandsadres zijn)
 **/
public class KlantAdres {

  @JsonbProperty("straatnaam")
  private String straatnaam;

  @JsonbProperty("huisnummer")
  private Integer huisnummer;

  @JsonbProperty("huisletter")
  private String huisletter;

  @JsonbProperty("huisnummertoevoeging")
  private String huisnummertoevoeging;

  @JsonbProperty("postcode")
  private String postcode;

  @JsonbProperty("woonplaatsnaam")
  private String woonplaatsnaam;

  /**
   * De code, behorende bij de landnaam, zoals opgenomen in de Land/Gebied-tabel van de BRP.
   **/
  @JsonbProperty("landcode")
  private String landcode;

  /**
   * Get straatnaam
   * @return straatnaam
   **/
  public String getStraatnaam() {
    return straatnaam;
  }

  /**
   * Set straatnaam
   **/
  public void setStraatnaam(String straatnaam) {
    this.straatnaam = straatnaam;
  }

  public KlantAdres straatnaam(String straatnaam) {
    this.straatnaam = straatnaam;
    return this;
  }

  /**
   * Get huisnummer
   * minimum: 0
   * maximum: 99999
   * @return huisnummer
   **/
  public Integer getHuisnummer() {
    return huisnummer;
  }

  /**
   * Set huisnummer
   **/
  public void setHuisnummer(Integer huisnummer) {
    this.huisnummer = huisnummer;
  }

  public KlantAdres huisnummer(Integer huisnummer) {
    this.huisnummer = huisnummer;
    return this;
  }

  /**
   * Get huisletter
   * @return huisletter
   **/
  public String getHuisletter() {
    return huisletter;
  }

  /**
   * Set huisletter
   **/
  public void setHuisletter(String huisletter) {
    this.huisletter = huisletter;
  }

  public KlantAdres huisletter(String huisletter) {
    this.huisletter = huisletter;
    return this;
  }

  /**
   * Get huisnummertoevoeging
   * @return huisnummertoevoeging
   **/
  public String getHuisnummertoevoeging() {
    return huisnummertoevoeging;
  }

  /**
   * Set huisnummertoevoeging
   **/
  public void setHuisnummertoevoeging(String huisnummertoevoeging) {
    this.huisnummertoevoeging = huisnummertoevoeging;
  }

  public KlantAdres huisnummertoevoeging(String huisnummertoevoeging) {
    this.huisnummertoevoeging = huisnummertoevoeging;
    return this;
  }

  /**
   * Get postcode
   * @return postcode
   **/
  public String getPostcode() {
    return postcode;
  }

  /**
   * Set postcode
   **/
  public void setPostcode(String postcode) {
    this.postcode = postcode;
  }

  public KlantAdres postcode(String postcode) {
    this.postcode = postcode;
    return this;
  }

  /**
   * Get woonplaatsnaam
   * @return woonplaatsnaam
   **/
  public String getWoonplaatsnaam() {
    return woonplaatsnaam;
  }

  /**
   * Set woonplaatsnaam
   **/
  public void setWoonplaatsnaam(String woonplaatsnaam) {
    this.woonplaatsnaam = woonplaatsnaam;
  }

  public KlantAdres woonplaatsnaam(String woonplaatsnaam) {
    this.woonplaatsnaam = woonplaatsnaam;
    return this;
  }

  /**
   * De code, behorende bij de landnaam, zoals opgenomen in de Land/Gebied-tabel van de BRP.
   * @return landcode
   **/
  public String getLandcode() {
    return landcode;
  }

  /**
   * Set landcode
   **/
  public void setLandcode(String landcode) {
    this.landcode = landcode;
  }

  public KlantAdres landcode(String landcode) {
    this.landcode = landcode;
    return this;
  }

  /**
   * Create a string representation of this pojo.
   **/
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class KlantAdres {\n");

    sb.append("    straatnaam: ").append(toIndentedString(straatnaam)).append("\n");
    sb.append("    huisnummer: ").append(toIndentedString(huisnummer)).append("\n");
    sb.append("    huisletter: ").append(toIndentedString(huisletter)).append("\n");
    sb.append("    huisnummertoevoeging: ")
        .append(toIndentedString(huisnummertoevoeging))
        .append("\n");
    sb.append("    postcode: ").append(toIndentedString(postcode)).append("\n");
    sb.append("    woonplaatsnaam: ").append(toIndentedString(woonplaatsnaam)).append("\n");
    sb.append("    landcode: ").append(toIndentedString(landcode)).append("\n");
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
