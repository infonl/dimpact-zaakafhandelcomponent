/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, input } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { TranslatePipe } from "@ngx-translate/core";
import { UtilService } from "../../core/service/util.service";
import { CsvService } from "../../csv/csv.service";
import { GeneratedType } from "../utils/generated-types";

@Component({
  selector: "zac-export-button[zoekParameters][filename]",
  templateUrl: "./export-button.component.html",
  styleUrls: ["./export-button.component.less"],
  imports: [MatButtonModule, MatIconModule, TranslatePipe],
})
export class ExportButtonComponent {
  private csvService = inject(CsvService);
  private utilService = inject(UtilService);

  readonly zoekParameters =
    input.required<GeneratedType<"RestZoekParameters">>();
  readonly filename = input.required<string>();

  downloadExport() {
    this.csvService.exportToCSV(this.zoekParameters()).subscribe((response) => {
      this.utilService.downloadBlobResponse(response, this.filename());
    });
  }
}
