/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input, Signal } from "@angular/core";
import { IndicatieItem } from "../model/indicatie-item";

export enum IndicatiesLayout {
  SEARCH = "SEARCH",
  COMPACT = "COMPACT",
  EXTENDED = "EXTENDED",
}

@Component({
  template: "",
  standalone: true,
})
export abstract class IndicatiesComponent {
  protected Layout = IndicatiesLayout;
  readonly layout = input.required<IndicatiesLayout>();
  protected abstract readonly indicaties: Signal<IndicatieItem[]>;
}
