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
import {
  injectMutation,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { UtilService } from "../../../core/service/util.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../../shared/confirm-dialog/confirm-dialog.component";
import { EmptyPipe } from "../../../shared/pipes/empty.pipe";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ReferentieTabelService } from "../../referentie-tabel.service";
import { ReferentieTabelValueDialogComponent } from "../referentie-tabel-value-dialog/referentie-tabel-value-dialog.component";

/**
 * Expanded content of a single reference table row: a table of its values,
 * with add / edit / delete. It owns the value mutations and refreshes the
 * shared reference-table queries on success (mirroring the BPMN item).
 */
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
  private readonly queryClient = inject(QueryClient);

  private readonly updateMutation = injectMutation(() => ({
    mutationFn: (body: GeneratedType<"RestReferenceTableUpdate">) =>
      this.service.updateReferentieTabelAsync(this.tabel().id!, body),
    onSuccess: () => {
      void this.queryClient.invalidateQueries({
        queryKey: this.service.listReferentieTabellenQuery().queryKey,
      });
      void this.queryClient.invalidateQueries({
        queryKey: this.service.readReferentieTabelQuery(this.tabel().id!)
          .queryKey,
      });
    },
  }));

  protected addWaarde() {
    this.dialog
      .open(ReferentieTabelValueDialogComponent, {
        data: {
          naam: "",
          titel: "referentietabel.waarde.toevoegen.titel",
          icoon: "add_circle",
        },
        width: "500px",
      })
      .afterClosed()
      .subscribe((naam?: string) => {
        if (!naam) {
          return;
        }
        this.updateMutation.mutate(
          this.buildBody([...(this.tabel().waarden ?? []), { naam }]),
          {
            onSuccess: () =>
              this.utilService.openSnackbar(
                "msg.referentietabel.waarde.toegevoegd",
                { waarde: naam },
              ),
          },
        );
      });
  }

  protected editWaarde(waarde: GeneratedType<"RestReferenceTableValue">) {
    this.dialog
      .open(ReferentieTabelValueDialogComponent, {
        data: {
          naam: waarde.naam,
          titel: "referentietabel.waarde.wijzigen.titel",
          icoon: "edit",
        },
        width: "500px",
      })
      .afterClosed()
      .subscribe((naam?: string) => {
        if (!naam) {
          return;
        }
        const waarden = (this.tabel().waarden ?? []).map((current) =>
          current.id === waarde.id ? { ...current, naam } : current,
        );
        this.updateMutation.mutate(this.buildBody(waarden), {
          onSuccess: () =>
            this.utilService.openSnackbar(
              "msg.referentietabel.waarde.gewijzigd",
              { waarde: naam },
            ),
        });
      });
  }

  protected deleteWaarde(waarde: GeneratedType<"RestReferenceTableValue">) {
    const waarden = (this.tabel().waarden ?? []).filter(
      (current) => current.id !== waarde.id,
    );
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData({
          key: "msg.referentietabel.waarde.verwijderen.bevestigen",
          args: { waarde: waarde.naam },
        }),
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.updateMutation.mutate(this.buildBody(waarden), {
          onSuccess: () =>
            this.utilService.openSnackbar(
              "msg.referentietabel.waarde.verwijderd",
              { waarde: waarde.naam },
            ),
        });
      });
  }

  private buildBody(
    waarden: GeneratedType<"RestReferenceTableValue">[],
  ): GeneratedType<"RestReferenceTableUpdate"> {
    const tabel = this.tabel();
    return { code: tabel.code, naam: tabel.naam, waarden };
  }
}
