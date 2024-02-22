/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.model;

import jakarta.ws.rs.QueryParam;

/**
 *
 */
public abstract class AbstractZTCListParameters {

  /*
   * Filter objects depending on their concept status
   */
  private ObjectStatusFilter status;

  @QueryParam("status")
  public String getStatus() {
    return status != null ? status.toValue() : null;
  }

  public void setStatus(final ObjectStatusFilter status) {
    this.status = status;
  }
}
