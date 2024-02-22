/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.klanten.model;

import jakarta.json.bind.annotation.JsonbProperty;

public class Wijzigingen {

  /**
   * Volledige JSON body van het object zoals dat bestond voordat de actie heeft plaatsgevonden.
   **/
  @JsonbProperty("oud")
  private Object oud;

  /**
   * Volledige JSON body van het object na de actie.
   **/
  @JsonbProperty("nieuw")
  private Object nieuw;

  /**
   * Volledige JSON body van het object zoals dat bestond voordat de actie heeft plaatsgevonden.
   * @return oud
   **/
  public Object getOud() {
    return oud;
  }

  /**
   * Set oud
   **/
  public void setOud(Object oud) {
    this.oud = oud;
  }

  public Wijzigingen oud(Object oud) {
    this.oud = oud;
    return this;
  }

  /**
   * Volledige JSON body van het object na de actie.
   * @return nieuw
   **/
  public Object getNieuw() {
    return nieuw;
  }

  /**
   * Set nieuw
   **/
  public void setNieuw(Object nieuw) {
    this.nieuw = nieuw;
  }

  public Wijzigingen nieuw(Object nieuw) {
    this.nieuw = nieuw;
    return this;
  }

  /**
   * Create a string representation of this pojo.
   **/
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Wijzigingen {\n");

    sb.append("    oud: ").append(toIndentedString(oud)).append("\n");
    sb.append("    nieuw: ").append(toIndentedString(nieuw)).append("\n");
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
