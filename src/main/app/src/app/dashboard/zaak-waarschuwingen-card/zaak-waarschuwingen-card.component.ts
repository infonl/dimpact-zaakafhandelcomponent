/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, effect, inject } from "@angular/core";
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
import { DagenPipe } from "../../shared/pipes/dagen.pipe";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { RefetchErrorHintComponent } from "../../shared/refetch-error-hint/refetch-error-hint.component";
import { DateConditionals } from "../../shared/utils/date-conditionals";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakIdentificatieComponent } from "../../shared/zaak-identificatie/zaak-identificatie.component";
import { ZakenService } from "../../zaken/zaken.service";
import { DashboardCardComponent } from "../dashboard-card/dashboard-card.component";

@Component({
  selector: "zac-zaak-waarschuwingen-card",
  templateUrl: "./zaak-waarschuwingen-card.component.html",
  styleUrls: [
    "../dashboard-card/dashboard-card.component.less",
    "./zaak-waarschuwingen-card.component.less",
  ],
  standalone: true,
  imports: [
    RefetchErrorHintComponent,
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
    DagenPipe,
    EmptyPipe,
  ],
})
export class ZaakWaarschuwingenCardComponent extends DashboardCardComponent<
  GeneratedType<"RestZaakOverzicht">
> {
  private readonly zakenService = inject(ZakenService);

  columns = [
    "identificatie",
    "streefdatum",
    "dagenTotStreefdatum",
    "fataledatum",
    "dagenTotFataledatum",
    "url",
  ] as const;

  zakenQuery = injectQuery(() => this.zakenService.listZaakWaarschuwingen());

  constructor() {
    super();

    effect(() => {
      this.dataSource.data = this.zakenQuery.data() ?? [];
    });
  }

  isAfterDate(
    datum: GeneratedType<"RestZaakOverzicht">["einddatumGepland"],
    actual: GeneratedType<"RestZaakOverzicht">["einddatum"],
  ) {
    return DateConditionals.isExceeded(datum ?? null, actual);
  }

  protected onLoad() {
    this.zakenQuery.refetch();
  }
}
