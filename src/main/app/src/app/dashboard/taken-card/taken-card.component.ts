/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { MatAnchor } from "@angular/material/button";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator } from "@angular/material/paginator";
import { MatSort, MatSortHeader } from "@angular/material/sort";
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatNoDataRow,
  MatRow,
  MatRowDef,
  MatTable,
} from "@angular/material/table";
import { RouterLink } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenService } from "../../signaleringen.service";
import { DashboardCardComponent } from "../dashboard-card/dashboard-card.component";

@Component({
  selector: "zac-taken-card",
  templateUrl: "./taken-card.component.html",
  styleUrls: [
    "../dashboard-card/dashboard-card.component.less",
    "./taken-card.component.less",
  ],
  standalone: true,
  imports: [
    MatTable,
    MatSort,
    MatColumnDef,
    MatHeaderCellDef,
    MatHeaderCell,
    MatSortHeader,
    MatCellDef,
    MatCell,
    MatHeaderRowDef,
    MatHeaderRow,
    MatRowDef,
    MatRow,
    MatNoDataRow,
    MatPaginator,
    MatAnchor,
    MatIcon,
    RouterLink,
    TranslateModule,
    DatumPipe,
    EmptyPipe,
  ],
})
export class TakenCardComponent extends DashboardCardComponent<
  GeneratedType<"RestSignaleringTaskSummary">
> {
  readonly columns: string[] = [
    "naam",
    "creatiedatumTijd",
    "zaakIdentificatie",
    "zaaktypeOmschrijving",
    "url",
  ];

  constructor(
    private readonly signaleringenService: SignaleringenService,
    protected readonly identityService: IdentityService,
    protected readonly websocketService: WebsocketService,
  ) {
    super(identityService, websocketService);
  }

  protected onLoad(afterLoad: () => void): void {
    this.signaleringenService
      .listTakenSignalering(this.data.signaleringType!)
      .subscribe((taken) => {
        this.dataSource.data = taken;
        afterLoad();
      });
  }
}
