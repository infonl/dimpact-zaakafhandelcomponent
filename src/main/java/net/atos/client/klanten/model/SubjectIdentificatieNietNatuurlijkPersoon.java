/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.klanten.model;

import jakarta.json.bind.annotation.JsonbProperty;

public class SubjectIdentificatieNietNatuurlijkPersoon {

  @JsonbProperty("subjectIdentificatie")
  private NietNatuurlijkPersoon subjectIdentificatie;

  /**
   * Get subjectIdentificatie
   * @return subjectIdentificatie
   **/
  public NietNatuurlijkPersoon getSubjectIdentificatie() {
    return subjectIdentificatie;
  }

  /**
   * Set subjectIdentificatie
   **/
  public void setSubjectIdentificatie(NietNatuurlijkPersoon subjectIdentificatie) {
    this.subjectIdentificatie = subjectIdentificatie;
  }

  public SubjectIdentificatieNietNatuurlijkPersoon subjectIdentificatie(
      NietNatuurlijkPersoon subjectIdentificatie) {
    this.subjectIdentificatie = subjectIdentificatie;
    return this;
  }

  /**
   * Create a string representation of this pojo.
   **/
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SubjectIdentificatieNietNatuurlijkPersoon {\n");

    sb.append("    subjectIdentificatie: ")
        .append(toIndentedString(subjectIdentificatie))
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
