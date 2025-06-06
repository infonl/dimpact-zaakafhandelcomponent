/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { DOCUMENT, isPlatformBrowser } from "@angular/common";
import {
  Directive,
  ElementRef,
  EventEmitter,
  Inject,
  OnDestroy,
  OnInit,
  Optional,
  Output,
  PLATFORM_ID,
} from "@angular/core";
import { Subscription, fromEvent } from "rxjs";
import { HasEventTargetAddRemove } from "rxjs/internal/observable/fromEvent";
import { filter } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";

@Directive({
  selector: "[zacOutsideClick]",
})
export class OutsideClickDirective implements OnInit, OnDestroy {
  private static inclusions: string[] = [
    "mat-option-text",
    "mdc-list-item__primary-text",
  ];

  @Output("zacOutsideClick") outsideClick = new EventEmitter<MouseEvent>();

  private subscription: Subscription | null = null;

  constructor(
    private element: ElementRef,
    @Optional()
    @Inject(DOCUMENT)
    private document:
      | HasEventTargetAddRemove<MouseEvent>
      | ArrayLike<HasEventTargetAddRemove<MouseEvent>>,
    @Inject(PLATFORM_ID) private platformId: Record<string, unknown>,
    private utilService: UtilService,
  ) {}

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
