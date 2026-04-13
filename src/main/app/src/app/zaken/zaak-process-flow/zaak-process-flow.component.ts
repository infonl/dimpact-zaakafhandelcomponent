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
import { GeneratedType } from "../../shared/utils/generated-types";

@Component({
  selector: "zac-zaak-process-flow",
  templateUrl: "./zaak-process-flow.component.html",
  standalone: true,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .diagram-container {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
      padding: 16px;
    }

    .diagram-image {
      max-width: 100%;
      max-height: 100%;
      object-fit: contain;
    }
  `,
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
  protected readonly bpmnProcessDefinition =
    input.required<GeneratedType<"RestZaakBpmnProcessDefinition">>();
  protected readonly cacheBuster = Date.now();
}
