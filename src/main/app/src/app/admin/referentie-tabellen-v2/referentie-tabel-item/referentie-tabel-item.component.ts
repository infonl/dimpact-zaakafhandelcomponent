/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, input } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog } from "@angular/material/dialog";
import { MatIconModule } from "@angular/material/icon";
import { MatTableModule } from "@angular/material/table";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import { UtilService } from "../../../core/service/util.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../../shared/confirm-dialog/confirm-dialog.component";
import { EmptyPipe } from "../../../shared/pipes/empty.pipe";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ReferentieTabelService } from "../../referentie-tabel.service";
import { ReferentieTabelValueDialogComponent } from "../referentie-tabel-value-dialog/referentie-tabel-value-dialog.component";

@Component({
  standalone: true,
  selector: "zac-referentie-tabel-item",
  templateUrl: "./referentie-tabel-item.component.html",
  styleUrls: ["./referentie-tabel-item.component.less"],
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    TranslateModule,
    EmptyPipe,
  ],
})
export class ReferentieTabelItemComponent {
  readonly tabel = input.required<GeneratedType<"RestReferenceTable">>();

  protected readonly columns = ["index", "naam", "actions"] as const;

  private readonly service = inject(ReferentieTabelService);
  private readonly dialog = inject(MatDialog);
  private readonly utilService = inject(UtilService);

  protected addWaarde() {
    this.dialog.open(ReferentieTabelValueDialogComponent, {
      data: { tabel: this.tabel() },
      width: "500px",
      autoFocus: "first-tabbable",
    });
  }

  protected editWaarde(waarde: GeneratedType<"RestReferenceTableValue">) {
    this.dialog.open(ReferentieTabelValueDialogComponent, {
      data: { tabel: this.tabel(), waarde },
      width: "500px",
      autoFocus: "first-tabbable",
    });
  }

  protected deleteWaarde(waarde: GeneratedType<"RestReferenceTableValue">) {
    const tabel = this.tabel();
    const waarden = (tabel.waarden ?? []).filter(
      (current) => current.id !== waarde.id,
    );
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.referentietabel.waarde-verwijderen-bevestigen",
            args: { waarde: waarde.naam },
          },
          this.service.updateReferentieTabelWithRefresh(tabel.id!, {
            code: tabel.code,
            naam: tabel.naam,
            waarden,
          }),
        ),
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.utilService.openSnackbar(
            "msg.referentietabel.waarde-verwijderd",
            { waarde: waarde.naam },
          );
        }
      });
  }
}
