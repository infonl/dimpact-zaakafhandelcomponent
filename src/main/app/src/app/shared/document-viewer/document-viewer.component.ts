/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass, NgIf } from "@angular/common";
import { Component, computed, inject, input } from "@angular/core";
import { DomSanitizer } from "@angular/platform-browser";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import {
  FileFormat,
  FileFormatUtil,
} from "../../informatie-objecten/model/file-format";
import { GeneratedType } from "../utils/generated-types";

@Component({
  selector: "zac-document-viewer",
  templateUrl: "./document-viewer.component.html",
  styleUrls: ["./document-viewer.component.less"],
  standalone: true,
  imports: [NgClass, NgIf],
})
export class DocumentViewerComponent {
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly sanitizer = inject(DomSanitizer);

  readonly document =
    input.required<GeneratedType<"RestEnkelvoudigInformatieobject">>();

  protected readonly previewSrc = computed(() => {
    const document = this.document();
    if (!FileFormatUtil.isPreviewAvailable(document.formaat as FileFormat)) {
      return null;
    }

    const url = this.informatieObjectenService.getPreviewUrl(
      document.uuid!,
      document.versie,
    );
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  });

  isImage() {
    return FileFormatUtil.isImage(this.document().formaat as FileFormat);
  }

  isPDF() {
    return this.document().formaat === FileFormat.PDF;
  }
}
