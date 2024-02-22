/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.drc.model;

import java.net.URI;

import jakarta.ws.rs.QueryParam;

public class ObjectInformatieobjectListParameters {

  /**
   * URL-referentie naar het gerelateerde OBJECT (in deze of een andere API).
   */
  @QueryParam("object")
  private URI object;

  /**
   * URL-referentie naar het INFORMATIEOBJECT.
   */
  @QueryParam("informatieobject")
  private URI informatieobject;

  public ObjectInformatieobjectListParameters() {}

  public URI getObject() {
    return object;
  }

  public void setObject(URI object) {
    this.object = object;
  }

  public URI getInformatieobject() {
    return informatieobject;
  }

  public void setInformatieobject(URI informatieobject) {
    this.informatieobject = informatieobject;
  }
}
