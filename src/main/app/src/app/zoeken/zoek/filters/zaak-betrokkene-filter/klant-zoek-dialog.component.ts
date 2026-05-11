/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject } from "@angular/core";
import { MatDialogContent, MatDialogRef } from "@angular/material/dialog";
import { KlantZoekComponent } from "../../../../klanten/zoek/klanten/klant-zoek.component";

@Component({
  selector: "zac-klant-zoek-dialog",
  templateUrl: "klant-zoek-dialog.component.html",
  styleUrls: ["./klant-zoek-dialog.component.less"],
  standalone: true,
  imports: [MatDialogContent, KlantZoekComponent],
})
export class KlantZoekDialog {
  protected readonly dialogRef = inject(MatDialogRef<KlantZoekDialog>);
}
