/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { MatDialogModule, MatDialogRef } from "@angular/material/dialog";
import { KlantZoekComponent } from "../../../../klanten/zoek/klanten/klant-zoek.component";

@Component({
  templateUrl: "klant-zoek-dialog.component.html",
  styleUrls: ["./klant-zoek-dialog.component.less"],
  standalone: true,
  imports: [MatDialogModule, KlantZoekComponent],
})
export class KlantZoekDialog {
  constructor(public dialogRef: MatDialogRef<KlantZoekDialog>) {}
}
