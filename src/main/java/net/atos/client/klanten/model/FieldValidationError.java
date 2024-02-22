/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.klanten.model;

import jakarta.json.bind.annotation.JsonbProperty;

public class FieldValidationError {

  /**
   * Naam van het veld met ongeldige gegevens
   **/
  @JsonbProperty("name")
  private String name;

  /**
   * Systeemcode die het type fout aangeeft
   **/
  @JsonbProperty("code")
  private String code;

  /**
   * Uitleg wat er precies fout is met de gegevens
   **/
  @JsonbProperty("reason")
  private String reason;

  /**
   * Naam van het veld met ongeldige gegevens
   * @return name
   **/
  public String getName() {
    return name;
  }

  /**
   * Set name
   **/
  public void setName(String name) {
    this.name = name;
  }

  public FieldValidationError name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Systeemcode die het type fout aangeeft
   * @return code
   **/
  public String getCode() {
    return code;
  }

  /**
   * Set code
   **/
  public void setCode(String code) {
    this.code = code;
  }

  public FieldValidationError code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Uitleg wat er precies fout is met de gegevens
   * @return reason
   **/
  public String getReason() {
    return reason;
  }

  /**
   * Set reason
   **/
  public void setReason(String reason) {
    this.reason = reason;
  }

  public FieldValidationError reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Create a string representation of this pojo.
   **/
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FieldValidationError {\n");

    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
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
