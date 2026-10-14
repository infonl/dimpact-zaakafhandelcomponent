/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Location } from "@angular/common";
import {
  DestroyRef,
  inject,
  provideEnvironmentInitializer,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { NavigationEnd, Router } from "@angular/router";
import { filter } from "rxjs";
import {
  Breadcrumb,
  breadcrumbKind,
  breadcrumbs,
} from "../../core/breadcrumbs/route-title";

export const openedFromSearchState = { origin: "zoeken" } as const;

type NavigationState = {
  origin?: string;
  navigationTrail?: Breadcrumb[];
} | null;

/**
 * Keeps the trail of pages that led to the current page in the browser history state, so the
 * trail survives a reload and moves along with the back and forward buttons.
 */
export function provideNavigationTrail() {
  return provideEnvironmentInitializer(() => {
    const router = inject(Router);
    const location = inject(Location);
    let originTrail: Breadcrumb[] | null = null;

    router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe(() => {
        const root = router.routerState.snapshot.root;
        const kind = breadcrumbKind(root);
        const state = location.getState() as NavigationState;
        let trail = state?.navigationTrail;
        if (!trail) {
          const routeTrail = breadcrumbs(root);
          const page = routeTrail.pop();
          trail = !page
            ? []
            : kind === "detail" &&
                originTrail &&
                state?.origin !== openedFromSearchState.origin
              ? continueTrail(originTrail, { ...page, url: router.url })
              : [...routeTrail, { ...page, url: router.url }];
          location.replaceState(location.path(), "", {
            ...state,
            navigationTrail: trail,
          });
        }
        originTrail = kind ? trail : null;
      });
  });
}

export function navigationTrail(location: Location) {
  return (location.getState() as NavigationState)?.navigationTrail ?? [];
}

/** The page that the automatic "back" after an action returns to */
export function findOrigin(location: Location) {
  return navigationTrail(location).at(-2)?.url ?? null;
}

function continueTrail(trail: Breadcrumb[], page: Breadcrumb) {
  const last = trail.at(-1);
  if (last && isVersionOf(last.url, page.url))
    return [...trail.slice(0, -1), page];

  const index = trail.findIndex(({ url }) => path(url) === path(page.url));
  return [...(index === -1 ? trail : trail.slice(0, index)), page];
}

// Another version of a page, like `/informatie-objecten/<uuid>/2` of `/informatie-objecten/<uuid>`,
// has the url of that page as its start. A one-segment url such as `/zaken` is a section, not a page.
function isVersionOf(url: string, otherUrl: string) {
  const [shorter, longer] = [segments(url), segments(otherUrl)].sort(
    (a, b) => a.length - b.length,
  );
  return (
    shorter.length > 1 &&
    shorter.every((segment, index) => segment === longer[index])
  );
}

function path(url: string) {
  return url.split(/[?#]/)[0];
}

function segments(url: string) {
  return path(url).split("/").filter(Boolean);
}
