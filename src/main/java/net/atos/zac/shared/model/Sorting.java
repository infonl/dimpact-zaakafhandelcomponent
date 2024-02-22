/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.shared.model;

public class Sorting {
  private final String field;

  private final SorteerRichting direction;

  public Sorting(final String field) {
    this.field = field;
    this.direction = SorteerRichting.DESCENDING;
  }

  public Sorting(final String field, final SorteerRichting direction) {
    this.field = field;
    this.direction = direction;
  }

  public String getField() {
    return field;
  }

  public SorteerRichting getDirection() {
    return direction;
  }
}
