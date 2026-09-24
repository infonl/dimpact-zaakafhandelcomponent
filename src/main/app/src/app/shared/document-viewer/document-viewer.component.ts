/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass, NgIf } from "@angular/common";
import {
  Component,
  inject,
  input,
  OnChanges,
  OnInit,
  SimpleChanges,
} from "@angular/core";
import { DomSanitizer, SafeUrl } from "@angular/platform-browser";
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
export class DocumentViewerComponent implements OnInit, OnChanges {
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly sanitizer = inject(DomSanitizer);

  readonly document =
    input.required<GeneratedType<"RestEnkelvoudigInformatieobject">>();

  previewSrc: SafeUrl | null = null;
  showPreview = false;

  ngOnChanges(changes: SimpleChanges) {
    if (!this.document()) return;
    if (changes.document.isFirstChange()) return;

    this.loadDocument();
  }

  ngOnInit() {
    this.loadDocument();
  }

  private loadDocument() {
    const document = this.document();
    if (!FileFormatUtil.isPreviewAvailable(document.formaat as FileFormat)) {
      this.showPreview = false;
      this.previewSrc = null;
      return;
    }

    this.showPreview = true;
    const url = this.informatieObjectenService.getPreviewUrl(
      document.uuid!,
      document.versie,
    );
    this.previewSrc = this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  isImage() {
    return FileFormatUtil.isImage(this.document().formaat as FileFormat);
  }

  isPDF() {
    return this.document().formaat === FileFormat.PDF;
  }
}
