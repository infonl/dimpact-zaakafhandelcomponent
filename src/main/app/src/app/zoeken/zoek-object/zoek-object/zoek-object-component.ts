/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ChangeDetectionStrategy, Component, InputSignal } from "@angular/core";
import { MatSidenav } from "@angular/material/sidenav";

@Component({
  template: "",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
})
export abstract class ZoekObjectComponent {
  abstract readonly sideNav: InputSignal<MatSidenav>;

  constructor() {}
}
