/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Location } from "@angular/common";
import { Component, inject } from "@angular/core";
import { toSignal } from "@angular/core/rxjs-interop";
import { NavigationEnd, Router, RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { filter, map } from "rxjs";
import { Breadcrumb, breadcrumbKind, breadcrumbs } from "./route-title";

type HistoryState = { breadcrumbs?: Breadcrumb[] } | null;

@Component({
  selector: "zac-breadcrumbs",
  templateUrl: "./breadcrumbs.component.html",
  styleUrls: ["./breadcrumbs.component.less"],
  standalone: true,
  imports: [TranslatePipe, RouterLink],
})
export class BreadcrumbsComponent {
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private origin: Breadcrumb[] | null = null;

  protected readonly breadcrumbs = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.trail()),
    ),
    { initialValue: [] },
  );

  private trail() {
    const root = this.router.routerState.snapshot.root;
    const kind = breadcrumbKind(root);
    const routeBreadcrumbs = breadcrumbs(root);
    const trail =
      kind === "detail" ? this.detailTrail(routeBreadcrumbs) : routeBreadcrumbs;
    this.origin = kind ? trail : null;
    return trail;
  }

  private detailTrail(routeBreadcrumbs: Breadcrumb[]) {
    const state = this.location.getState() as HistoryState;
    if (state?.breadcrumbs) return state.breadcrumbs;

    const page = routeBreadcrumbs.at(-1);
    if (!page || !this.origin) return routeBreadcrumbs;

    const trail = [...upTo(this.origin, page), page];
    // Keeps the trail when the page is reloaded or reached through the browser history.
    this.location.replaceState(this.location.path(), "", {
      ...state,
      breadcrumbs: trail,
    });
    return trail;
  }
}

function upTo(trail: Breadcrumb[], page: Breadcrumb) {
  const index = trail.findIndex(
    ({ url }) => url.join("/") === page.url.join("/"),
  );
  return index === -1 ? trail : trail.slice(0, index);
}
