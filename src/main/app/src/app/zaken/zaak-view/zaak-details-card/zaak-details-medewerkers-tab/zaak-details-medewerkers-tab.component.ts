/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, inject, input } from "@angular/core";
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
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { EmptyPipe } from "../../../../shared/pipes/empty.pipe";
import { ZakenService } from "../../../zaken.service";

@Component({
  selector: "zac-zaak-details-medewerkers-tab",
  templateUrl: "./zaak-details-medewerkers-tab.component.html",
  styles: [":host { display: block; }"],
  standalone: true,
  imports: [
    MatTable,
    MatColumnDef,
    MatHeaderCell,
    MatHeaderCellDef,
    MatCell,
    MatCellDef,
    MatHeaderRow,
    MatHeaderRowDef,
    MatNoDataRow,
    MatRow,
    MatRowDef,
    EmptyPipe,
    TranslatePipe,
  ],
})
export class ZaakDetailsMedewerkersTabComponent {
  private readonly zakenService = inject(ZakenService);

  readonly zaakUuid = input.required<string>();

  protected readonly medewerkersQuery = injectQuery(() =>
    this.zakenService.listGeautoriseerdeMedewerkersQuery(this.zaakUuid()),
  );

  protected readonly medewerkers = computed(
    () => this.medewerkersQuery.data() ?? [],
  );

  protected readonly columns = ["naam", "roltype"] as const;
}
