/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, inject, input } from "@angular/core";
import { MatIconButton } from "@angular/material/button";
import { MatIcon } from "@angular/material/icon";
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
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../../../core/service/util.service";
import { runMutation } from "../../../../shared/http/run-mutation";
import { EmptyPipe } from "../../../../shared/pipes/empty.pipe";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ZaakDialogService } from "../../../zaak-dialog.service";
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
    MatIcon,
    MatIconButton,
    EmptyPipe,
    TranslatePipe,
  ],
})
export class ZaakDetailsMedewerkersTabComponent {
  private readonly zakenService = inject(ZakenService);
  private readonly zaakDialogService = inject(ZaakDialogService);
  private readonly utilService = inject(UtilService);
  private readonly queryClient = inject(QueryClient);

  readonly zaakUuid = input.required<string>();
  readonly canOntkoppelen = input(false);

  protected readonly medewerkersQuery = injectQuery(() =>
    this.zakenService.listGeautoriseerdeMedewerkersQuery(this.zaakUuid()),
  );

  protected readonly medewerkers = computed(
    () => this.medewerkersQuery.data() ?? [],
  );

  protected readonly columns = computed(() =>
    this.canOntkoppelen()
      ? ["naam", "roltype", "actions"]
      : ["naam", "roltype"],
  );

  protected ontkoppelMedewerker(
    medewerker: GeneratedType<"RestGeautoriseerdeMedewerker">,
  ) {
    const naam = medewerker.naam ?? medewerker.id ?? "";
    this.zaakDialogService
      .openOntkoppelZaakspecifiekGeautoriseerdeMedewerker(naam, (reden) =>
        runMutation(
          this.queryClient,
          this.zakenService.deleteZaakspecifiekGeautoriseerdeMedewerker(
            this.zaakUuid(),
          ),
          { medewerkerId: medewerker.id!, reden },
        ),
      )
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar(
            "msg.zaakspecifiek-geautoriseerde-medewerker.ontkoppeld",
            { medewerker: naam },
          );
        }
      });
  }
}
