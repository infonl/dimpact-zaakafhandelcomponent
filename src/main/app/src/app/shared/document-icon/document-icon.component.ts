/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input } from "@angular/core";
import { MatIcon } from "@angular/material/icon";
import { TranslateService } from "@ngx-translate/core";
import { FileIcon } from "../../informatie-objecten/model/file-icon";

@Component({
  selector: "zac-document-icon",
  templateUrl: "./document-icon.component.html",
  styleUrls: ["./document-icon.component.less"],
  standalone: true,
  imports: [MatIcon],
})
export class DocumentIconComponent {
  @Input() bestandsnaam?: string;

  constructor(private translate: TranslateService) {}

  getFileIcon(filename?: string) {
    return FileIcon.getIconByBestandsnaam(filename);
  }

  getFileTooltip(filetype: string) {
    return filetype === "unknown"
      ? this.translate.instant("bestandstype.onbekend")
      : this.translate.instant("bestandstype", {
          type: filetype.toUpperCase(),
        });
  }
}
