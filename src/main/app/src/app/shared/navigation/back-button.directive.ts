/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Directive, HostListener, inject } from "@angular/core";
import { toSignal } from "@angular/core/rxjs-interop";
import { Router } from "@angular/router";
import { fromEvent, map } from "rxjs";

/** Whether the browser history holds a ZAC page before the current one */
export function injectCanGoBack() {
  return toSignal(
    fromEvent(window.navigation, "currententrychange").pipe(
      map(() => window.navigation.canGoBack),
    ),
    { initialValue: window.navigation.canGoBack },
  );
}

@Directive({
  selector: "[zacBackButton]",
  standalone: true,
})
export class BackButtonDirective {
  private readonly router = inject(Router);

  @HostListener("click")
  onClick() {
    if (window.navigation.canGoBack) {
      history.back();
      return;
    }
    void this.router.navigateByUrl("/");
  }
}
