/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.klanten.model;

import jakarta.json.bind.annotation.JsonbProperty;

public class Fout {

  /**
   * URI referentie naar het type fout, bedoeld voor developers
   **/
  @JsonbProperty("type")
  private String type;

  /**
   * Systeemcode die het type fout aangeeft
   **/
  @JsonbProperty("code")
  private String code;

  /**
   * Generieke titel voor het type fout
   **/
  @JsonbProperty("title")
  private String title;

  /**
   * De HTTP status code
   **/
  @JsonbProperty("status")
  private Integer status;

  /**
   * Extra informatie bij de fout, indien beschikbaar
   **/
  @JsonbProperty("detail")
  private String detail;

  /**
   * URI met referentie naar dit specifiek voorkomen van de fout. Deze kan gebruikt worden in combinatie met server logs, bijvoorbeeld.
   **/
  @JsonbProperty("instance")
  private String instance;

  /**
   * URI referentie naar het type fout, bedoeld voor developers
   * @return type
   **/
  public String getType() {
    return type;
  }

  /**
   * Set type
   **/
  public void setType(String type) {
    this.type = type;
  }

  public Fout type(String type) {
    this.type = type;
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

  public Fout code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Generieke titel voor het type fout
   * @return title
   **/
  public String getTitle() {
    return title;
  }

  /**
   * Set title
   **/
  public void setTitle(String title) {
    this.title = title;
  }

  public Fout title(String title) {
    this.title = title;
    return this;
  }

  /**
   * De HTTP status code
   * @return status
   **/
  public Integer getStatus() {
    return status;
  }

  /**
   * Set status
   **/
  public void setStatus(Integer status) {
    this.status = status;
  }

  public Fout status(Integer status) {
    this.status = status;
    return this;
  }

  /**
   * Extra informatie bij de fout, indien beschikbaar
   * @return detail
   **/
  public String getDetail() {
    return detail;
  }

  /**
   * Set detail
   **/
  public void setDetail(String detail) {
    this.detail = detail;
  }

  public Fout detail(String detail) {
    this.detail = detail;
    return this;
  }

  /**
   * URI met referentie naar dit specifiek voorkomen van de fout. Deze kan gebruikt worden in combinatie met server logs, bijvoorbeeld.
   * @return instance
   **/
  public String getInstance() {
    return instance;
  }

  /**
   * Set instance
   **/
  public void setInstance(String instance) {
    this.instance = instance;
  }

  public Fout instance(String instance) {
    this.instance = instance;
    return this;
  }

  /**
   * Create a string representation of this pojo.
   **/
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Fout {\n");

    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
    sb.append("    instance: ").append(toIndentedString(instance)).append("\n");
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
