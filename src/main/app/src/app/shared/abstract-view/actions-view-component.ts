/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Signal } from "@angular/core";
import { MatDrawer } from "@angular/material/sidenav";
import { ViewComponent } from "./view-component";

@Component({
  template: "",
  standalone: true,
})
export abstract class ActionsViewComponent extends ViewComponent {
  abstract readonly actionsSidenav: Signal<MatDrawer>;
}
