/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

export class DatumRange {
  van: Date | null = null;
  tot: Date | null = null;

  constructor(van?: Date | null, tot?: Date | null) {
    this.van = van || null;
    this.tot = tot || null;
  }

  hasValue() {
    return this.van != null || this.tot != null;
  }
}
