/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
} from "@angular/core";
import { MatIcon } from "@angular/material/icon";
import { TranslateService } from "@ngx-translate/core";
import { FileIcon } from "../../informatie-objecten/model/file-icon";

@Component({
  selector: "zac-document-icon",
  templateUrl: "./document-icon.component.html",
  styleUrls: ["./document-icon.component.less"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [MatIcon],
})
export class DocumentIconComponent {
  private readonly translate = inject(TranslateService);

  readonly bestandsnaam = input<string | null>();

  getFileIcon(filename?: string | null) {
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
