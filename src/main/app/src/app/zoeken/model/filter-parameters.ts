/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

export class FilterParameters {
  values: string[];
  inverse: string;

  constructor(values: string[], inverse: boolean) {
    this.values = values;
    this.inverse = String(inverse);
  }
}
