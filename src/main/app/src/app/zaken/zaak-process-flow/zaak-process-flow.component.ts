/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input, signal } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import { GeneratedType } from "../../shared/utils/generated-types";

const ZOOM_STEP = 0.25;
const ZOOM_MIN = 0.25;
const ZOOM_MAX = 4;

@Component({
  selector: "zac-zaak-process-flow",
  templateUrl: "./zaak-process-flow.component.html",
  standalone: true,
  styleUrls: ["./zaak-process-flow.component.less"],
  imports: [
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatToolbarModule,
    MatTooltipModule,
    TranslateModule,
  ],
})
export class ZaakProcessFlowComponent {
  protected readonly sideNav = input.required<MatDrawer>();
  protected readonly zaakUuid = input.required<string>();
  protected readonly bpmnProcessDefinition =
    input.required<GeneratedType<"RestZaakBpmnProcessDefinition">>();
  protected readonly cacheBuster = Date.now();

  protected readonly zoomLevel = signal(1);

  protected zoomIn() {
    this.zoomLevel.update((z) => Math.min(ZOOM_MAX, z + ZOOM_STEP));
  }

  protected zoomOut() {
    this.zoomLevel.update((z) => Math.max(ZOOM_MIN, z - ZOOM_STEP));
  }

  protected resetZoom() {
    this.zoomLevel.set(1);
  }
}
