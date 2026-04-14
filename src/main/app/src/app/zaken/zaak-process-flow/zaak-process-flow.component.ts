/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  ElementRef,
  HostListener,
  input,
  signal,
  viewChild,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";

const ZOOM_STEP = 0.25;
const ZOOM_MIN = 0.25;
const ZOOM_MAX = 4;
const SCROLL_STEP = 100;

@Component({
  selector: "zac-zaak-process-flow",
  templateUrl: "./zaak-process-flow.component.html",
  standalone: true,
  styleUrls: ["./zaak-process-flow.component.less"],
  imports: [
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatToolbarModule,
    MatTooltipModule,
    StaticTextComponent,
    TranslateModule,
  ],
})
export class ZaakProcessFlowComponent {
  protected readonly sideNav = input.required<MatDrawer>();
  protected readonly zaak = input.required<GeneratedType<"RestZaak">>();
  protected readonly cacheBuster = Date.now();

  private readonly containerRef =
    viewChild<ElementRef<HTMLDivElement>>("diagramContainer");

  protected readonly zoomLevel = signal(1);

  @HostListener("document:keydown.ArrowUp", ["$event"])
  protected onArrowUp(event: KeyboardEvent) {
    if (this.isInteractiveElementFocused()) return;
    event.preventDefault();
    this.zoomIn();
  }

  @HostListener("document:keydown.ArrowDown", ["$event"])
  protected onArrowDown(event: KeyboardEvent) {
    if (this.isInteractiveElementFocused()) return;
    event.preventDefault();
    this.zoomOut();
  }

  @HostListener("document:keydown.ArrowLeft", ["$event"])
  protected onArrowLeft(event: KeyboardEvent) {
    if (this.isInteractiveElementFocused()) return;
    event.preventDefault();
    this.containerRef()?.nativeElement.scrollBy({
      left: -SCROLL_STEP,
      behavior: "smooth",
    });
  }

  @HostListener("document:keydown.ArrowRight", ["$event"])
  protected onArrowRight(event: KeyboardEvent) {
    if (this.isInteractiveElementFocused()) return;
    event.preventDefault();
    this.containerRef()?.nativeElement.scrollBy({
      left: SCROLL_STEP,
      behavior: "smooth",
    });
  }

  private isInteractiveElementFocused(): boolean {
    const element = document.activeElement;
    if (!element) return false;
    const tag = element.tagName.toLowerCase();
    return (
      tag === "input" ||
      tag === "textarea" ||
      tag === "select" ||
      (element as HTMLElement).isContentEditable
    );
  }

  protected zoomIn() {
    this.zoomLevel.update((z) => Math.min(ZOOM_MAX, z + ZOOM_STEP));
  }

  protected zoomOut() {
    this.zoomLevel.update((z) => Math.max(ZOOM_MIN, z - ZOOM_STEP));
  }

  protected resetZoom() {
    this.zoomLevel.set(1);
  }
}
