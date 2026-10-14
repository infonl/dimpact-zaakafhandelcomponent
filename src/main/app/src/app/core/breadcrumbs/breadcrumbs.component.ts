/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Location } from "@angular/common";
import { Component, inject } from "@angular/core";
import { toSignal } from "@angular/core/rxjs-interop";
import { ActivatedRouteSnapshot, NavigationEnd, Router } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { filter, map } from "rxjs";
import {
  continueTrail,
  navigateToBreadcrumb,
  NavigationState,
  openedFromSearchState,
} from "../../shared/navigation/navigation-origin";
import { Breadcrumb, breadcrumbKind, breadcrumbs } from "./route-title";

@Component({
  selector: "zac-breadcrumbs",
  templateUrl: "./breadcrumbs.component.html",
  styleUrls: ["./breadcrumbs.component.less"],
  standalone: true,
  imports: [TranslatePipe],
})
export class BreadcrumbsComponent {
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private originTrail: Breadcrumb[] | null = null;

  protected readonly breadcrumbs = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.trail()),
    ),
    { initialValue: [] },
  );

  protected navigate(event: MouseEvent, breadcrumb: Breadcrumb) {
    if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey)
      return;
    event.preventDefault();
    void navigateToBreadcrumb(breadcrumb, this.router);
  }

  private trail() {
    const root = this.router.routerState.snapshot.root;
    const kind = breadcrumbKind(root);
    const state = this.location.getState() as NavigationState;
    const trail = state?.navigationTrail ?? this.startTrail(root, kind, state);
    this.originTrail = kind ? trail : null;
    return trail;
  }

  private startTrail(
    root: ActivatedRouteSnapshot,
    kind: ReturnType<typeof breadcrumbKind>,
    state: NavigationState,
  ) {
    const routeTrail = breadcrumbs(root);
    const routePage = routeTrail.pop();
    if (!routePage) return [];

    const page = {
      ...routePage,
      url: this.router.url,
      key: window.navigation.currentEntry?.key,
    };
    const trail =
      kind === "detail" &&
      this.originTrail &&
      state?.origin !== openedFromSearchState.origin
        ? continueTrail(this.originTrail, page)
        : [...routeTrail, page];
    // The browser history keeps the trail with its page, for a reload and for back and forward.
    this.location.replaceState(this.location.path(), "", {
      ...state,
      navigationTrail: trail,
    });
    return trail;
  }
}
