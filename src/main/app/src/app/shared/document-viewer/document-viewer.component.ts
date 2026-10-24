/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  Input,
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
})
export class DocumentViewerComponent implements OnInit, OnChanges {
  @Input() document: GeneratedType<"RestEnkelvoudigInformatieobject">;

  previewSrc: SafeUrl = null;
  showPreview = false;

  constructor(
    private informatieObjectenService: InformatieObjectenService,
    private sanitizer: DomSanitizer,
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    this.document = changes.document.currentValue;
    if (this.document && !changes.document.isFirstChange()) {
      this.loadDocument();
    }
  }

  ngOnInit(): void {
    this.loadDocument();
  }

  private loadDocument(): void {
    if (
      FileFormatUtil.isPreviewAvailable(this.document.formaat as FileFormat)
    ) {
      this.showPreview = true;
      const url = this.informatieObjectenService.getPreviewUrl(
        this.document.uuid,
        this.document.versie,
      );
      this.previewSrc = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    } else {
      this.showPreview = false;
      this.previewSrc = null;
    }
  }

  isImage(): boolean {
    return FileFormatUtil.isImage(this.document.formaat as FileFormat);
  }

  isPDF(): boolean {
    return this.document.formaat === FileFormat.PDF;
  }
}
