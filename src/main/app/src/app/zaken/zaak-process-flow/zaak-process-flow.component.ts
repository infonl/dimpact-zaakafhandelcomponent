/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";

@Component({
  selector: "zac-zaak-process-flow",
  templateUrl: "./zaak-process-flow.component.html",
  standalone: true,
  imports: [
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatToolbarModule,
    TranslateModule,
  ],
})
export class ZaakProcessFlowComponent {
  protected readonly sideNav = input.required<MatDrawer>();
  protected readonly zaakUuid = input.required<string>();
}
