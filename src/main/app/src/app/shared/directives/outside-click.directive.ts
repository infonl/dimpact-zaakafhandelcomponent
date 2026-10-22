/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { isPlatformBrowser } from "@angular/common";
import {
  Directive,
  DOCUMENT,
  ElementRef,
  OnDestroy,
  OnInit,
  output,
  PLATFORM_ID,
  inject,
} from "@angular/core";
import { fromEvent, Subscription } from "rxjs";
import { filter } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";

@Directive({
  selector: "[zacOutsideClick]",
})
export class OutsideClickDirective implements OnInit, OnDestroy {
  private element = inject(ElementRef);
  private document = inject(DOCUMENT);
  private platformId = inject<Record<string, unknown>>(PLATFORM_ID);
  private utilService = inject(UtilService);

  private static inclusions: string[] = [
    "mat-option-text",
    "mdc-list-item__primary-text",
  ];

  readonly outsideClick = output<MouseEvent>({ alias: "zacOutsideClick" });

  private subscription: Subscription | null = null;

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    setTimeout(() => {
      this.subscription = fromEvent<MouseEvent>(this.document, "click")
        .pipe(
          filter((event) => {
            if (this.utilService.hasEditOverlay()) {
              return false;
            }
            const clickTarget = event.target as HTMLElement;
            return !OutsideClickDirective.isOrContainsClickTarget(
              this.element.nativeElement,
              clickTarget,
            );
          }),
        )
        .subscribe((event) => this.outsideClick.emit(event));
    }, 0);
  }

  ngOnDestroy() {
    this.subscription?.unsubscribe();
  }

  private static isOrContainsClickTarget(
    element: HTMLElement,
    clickTarget: HTMLElement,
  ) {
    const included: boolean = this.inclusions.some((value) =>
      clickTarget.classList.contains(value),
    );
    return element === clickTarget || element.contains(clickTarget) || included;
  }
}
