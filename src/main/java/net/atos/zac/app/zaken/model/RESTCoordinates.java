/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model;

public class RESTCoordinates {

  public RESTCoordinates() {}

  public RESTCoordinates(final double x, final double y) {
    this.x = x;
    this.y = y;
  }

  public double x;

  public double y;
}
