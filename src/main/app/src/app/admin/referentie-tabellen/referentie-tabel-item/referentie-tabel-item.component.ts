/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, input } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog } from "@angular/material/dialog";
import { MatIconModule } from "@angular/material/icon";
import { MatTableModule } from "@angular/material/table";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../../shared/confirm-dialog/confirm-dialog.component";
import { EmptyPipe } from "../../../shared/pipes/empty.pipe";
import { runMutation } from "../../../shared/http/run-mutation";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ReferentieTabelService } from "../../referentie-tabel.service";
import { ReferentieTabelValueDialogComponent } from "./referentie-tabel-value-dialog/referentie-tabel-value-dialog.component";

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

  protected readonly columns = ["index", "name", "actions"] as const;

  private readonly service = inject(ReferentieTabelService);
  private readonly dialog = inject(MatDialog);
  private readonly queryClient = inject(QueryClient);

  protected addValue() {
    this.dialog.open(ReferentieTabelValueDialogComponent, {
      data: { tabel: this.tabel() },
      width: "500px",
      autoFocus: "input:not([disabled])",
    });
  }

  protected editValue(value: GeneratedType<"RestReferenceTableValue">) {
    this.dialog.open(ReferentieTabelValueDialogComponent, {
      data: { tabel: this.tabel(), value },
      width: "500px",
      autoFocus: "input:not([disabled])",
    });
  }

  protected deleteValue(value: GeneratedType<"RestReferenceTableValue">) {
    const { mutationOptions, body } = this.service.deleteReferentieTabelValue(
      this.tabel(),
      value,
    );

    this.dialog.open(ConfirmDialogComponent, {
      data: new ConfirmDialogData(
        {
          key: "msg.referentietabel.waarde-verwijderen-bevestigen",
          args: { value: value.name },
        },
        runMutation(this.queryClient, mutationOptions, body),
      ),
    });
  }
}
