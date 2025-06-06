/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Directive,
  EventEmitter,
  HostBinding,
  HostListener,
  Output,
} from "@angular/core";

@Directive({
  selector: "[DropZone]",
  standalone: true,
})
export class FileDragAndDropDirective {
  @Output() fileDropped = new EventEmitter<FileList>();
  @HostBinding("style.border") private border = "solid transparent";

  @HostListener("dragover", ["$event"]) public onDragOver(evt: DragEvent) {
    evt.preventDefault();
    evt.stopPropagation();
    this.border = "dotted #FF4D2A";
  }

  @HostListener("dragleave", ["$event"]) public onDragLeave(evt: DragEvent) {
    evt.preventDefault();
    evt.stopPropagation();
    this.border = "solid transparent";
  }

  @HostListener("drop", ["$event"]) public ondrop(evt: DragEvent) {
    evt.preventDefault();
    evt.stopPropagation();
    this.border = "solid transparent";
    const files = evt.dataTransfer?.files;
    if (!files?.length) return;

    this.fileDropped.emit(files);
  }
}
