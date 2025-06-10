/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Indicatie } from "./indicatie";

export class IndicatieItem {
  naam: Indicatie;
  icon: string;
  outlined = false;
  primary = false;
  toelichting: string;

  constructor(naam: Indicatie, icon: string, toelichting?: string) {
    this.naam = naam;
    this.icon = icon;
    this.toelichting = toelichting ?? "";
  }

  temporary(): IndicatieItem {
    this.primary = true;
    return this;
  }

  alternate(): IndicatieItem {
    this.outlined = true;
    return this;
  }
}
