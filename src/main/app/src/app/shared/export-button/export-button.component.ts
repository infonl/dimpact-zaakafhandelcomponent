/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { TranslateModule } from "@ngx-translate/core";
import { UtilService } from "../../core/service/util.service";
import { CsvService } from "../../csv/csv.service";
import { GeneratedType } from "../utils/generated-types";

@Component({
  selector: "zac-export-button[zoekParameters][filename]",
  templateUrl: "./export-button.component.html",
  styleUrls: ["./export-button.component.less"],
  imports: [MatButtonModule, MatIconModule, TranslateModule],
})
export class ExportButtonComponent {
  @Input() zoekParameters!: GeneratedType<"RestZoekParameters">;
  @Input() filename!: string;

  constructor(
    private csvService: CsvService,
    private utilService: UtilService,
  ) {}

  downloadExport() {
    this.csvService.exportToCSV(this.zoekParameters).subscribe((response) => {
      this.utilService.downloadBlobResponse(response, this.filename);
    });
  }
}
