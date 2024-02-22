/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.klanten.model;

import jakarta.json.bind.annotation.JsonbProperty;

public class SubVerblijfBuitenland {

  /**
   * De code, behorende bij de landnaam, zoals opgenomen in de Land/Gebied-tabel van de BRP.
   **/
  @JsonbProperty("lndLandcode")
  private String lndLandcode;

  /**
   * De naam van het land, zoals opgenomen in de Land/Gebied-tabel van de BRP.
   **/
  @JsonbProperty("lndLandnaam")
  private String lndLandnaam;

  @JsonbProperty("subAdresBuitenland1")
  private String subAdresBuitenland1;

  @JsonbProperty("subAdresBuitenland2")
  private String subAdresBuitenland2;

  @JsonbProperty("subAdresBuitenland3")
  private String subAdresBuitenland3;

  /**
   * De code, behorende bij de landnaam, zoals opgenomen in de Land/Gebied-tabel van de BRP.
   * @return lndLandcode
   **/
  public String getLndLandcode() {
    return lndLandcode;
  }

  /**
   * Set lndLandcode
   **/
  public void setLndLandcode(String lndLandcode) {
    this.lndLandcode = lndLandcode;
  }

  public SubVerblijfBuitenland lndLandcode(String lndLandcode) {
    this.lndLandcode = lndLandcode;
    return this;
  }

  /**
   * De naam van het land, zoals opgenomen in de Land/Gebied-tabel van de BRP.
   * @return lndLandnaam
   **/
  public String getLndLandnaam() {
    return lndLandnaam;
  }

  /**
   * Set lndLandnaam
   **/
  public void setLndLandnaam(String lndLandnaam) {
    this.lndLandnaam = lndLandnaam;
  }

  public SubVerblijfBuitenland lndLandnaam(String lndLandnaam) {
    this.lndLandnaam = lndLandnaam;
    return this;
  }

  /**
   * Get subAdresBuitenland1
   * @return subAdresBuitenland1
   **/
  public String getSubAdresBuitenland1() {
    return subAdresBuitenland1;
  }

  /**
   * Set subAdresBuitenland1
   **/
  public void setSubAdresBuitenland1(String subAdresBuitenland1) {
    this.subAdresBuitenland1 = subAdresBuitenland1;
  }

  public SubVerblijfBuitenland subAdresBuitenland1(String subAdresBuitenland1) {
    this.subAdresBuitenland1 = subAdresBuitenland1;
    return this;
  }

  /**
   * Get subAdresBuitenland2
   * @return subAdresBuitenland2
   **/
  public String getSubAdresBuitenland2() {
    return subAdresBuitenland2;
  }

  /**
   * Set subAdresBuitenland2
   **/
  public void setSubAdresBuitenland2(String subAdresBuitenland2) {
    this.subAdresBuitenland2 = subAdresBuitenland2;
  }

  public SubVerblijfBuitenland subAdresBuitenland2(String subAdresBuitenland2) {
    this.subAdresBuitenland2 = subAdresBuitenland2;
    return this;
  }

  /**
   * Get subAdresBuitenland3
   * @return subAdresBuitenland3
   **/
  public String getSubAdresBuitenland3() {
    return subAdresBuitenland3;
  }

  /**
   * Set subAdresBuitenland3
   **/
  public void setSubAdresBuitenland3(String subAdresBuitenland3) {
    this.subAdresBuitenland3 = subAdresBuitenland3;
  }

  public SubVerblijfBuitenland subAdresBuitenland3(String subAdresBuitenland3) {
    this.subAdresBuitenland3 = subAdresBuitenland3;
    return this;
  }

  /**
   * Create a string representation of this pojo.
   **/
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SubVerblijfBuitenland {\n");

    sb.append("    lndLandcode: ").append(toIndentedString(lndLandcode)).append("\n");
    sb.append("    lndLandnaam: ").append(toIndentedString(lndLandnaam)).append("\n");
    sb.append("    subAdresBuitenland1: ")
        .append(toIndentedString(subAdresBuitenland1))
        .append("\n");
    sb.append("    subAdresBuitenland2: ")
        .append(toIndentedString(subAdresBuitenland2))
        .append("\n");
    sb.append("    subAdresBuitenland3: ")
        .append(toIndentedString(subAdresBuitenland3))
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
