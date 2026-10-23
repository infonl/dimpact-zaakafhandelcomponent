/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, effect, inject } from "@angular/core";
import { MatIconAnchor } from "@angular/material/button";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator } from "@angular/material/paginator";
import { MatProgressSpinner } from "@angular/material/progress-spinner";
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
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakIdentificatieComponent } from "../../shared/zaak-identificatie/zaak-identificatie.component";
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
    ZaakIdentificatieComponent,
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
    MatProgressSpinner,
    MatIconAnchor,
    MatIcon,
    RouterLink,
    TranslatePipe,
    DatumPipe,
    EmptyPipe,
  ],
})
export class TakenCardComponent extends DashboardCardComponent<
  GeneratedType<"RestSignaleringTaskSummary">
> {
  private readonly signaleringenService = inject(SignaleringenService);

  readonly columns: string[] = [
    "naam",
    "creatiedatumTijd",
    "zaakIdentificatie",
    "zaaktypeOmschrijving",
    "url",
  ];

  parameters = computed(() => ({
    signaleringType: this.data().signaleringType,
  }));

  takenQuery = injectQuery(() => ({
    ...this.signaleringenService.listTakenSignalering(
      this.parameters().signaleringType!,
    ),
    enabled: this.parameters().signaleringType != null,
  }));

  constructor() {
    super();

    effect(() => {
      this.dataSource.data = this.takenQuery.data() ?? [];
    });
  }

  protected onLoad(): void {
    if (!this.data().signaleringType) {
      this.dataSource.data = [];
      return;
    }
    this.takenQuery.refetch();
  }
}
