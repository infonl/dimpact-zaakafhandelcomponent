/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Location } from "@angular/common";
import { Router } from "@angular/router";
import { Breadcrumb } from "../../core/breadcrumbs/route-title";

export const openedFromSearchState = { origin: "zoeken" } as const;

export type NavigationState = {
  origin?: string;
  navigationTrail?: Breadcrumb[];
} | null;

export function navigationTrail(location: Location) {
  return (location.getState() as NavigationState)?.navigationTrail ?? [];
}

/** The breadcrumb before the current page: where "back" goes after an action */
export function findOrigin(location: Location) {
  return navigationTrail(location).at(-2) ?? null;
}

export function navigateToBreadcrumb(breadcrumb: Breadcrumb, router: Router) {
  const { key, url } = breadcrumb;
  if (key && window.navigation.entries().some((entry) => entry.key === key))
    return Promise.resolve(window.navigation.traverseTo(key).finished).then(
      () => true,
    );

  return router.navigateByUrl(url);
}

/** The deepest trail: the origin page and two pages below it */
export const MAXIMUM_TRAIL_LENGTH = 3;

export function detailPageType(url: string) {
  return segments(url)[0];
}

export function continueTrail(trail: Breadcrumb[], page: Breadcrumb) {
  const index = trail.findIndex(
    ({ url, type }) =>
      path(url) === path(page.url) || (!!page.type && type === page.type),
  );
  const continued = [...(index === -1 ? trail : trail.slice(0, index)), page];
  return continued.length > MAXIMUM_TRAIL_LENGTH
    ? [continued[0], ...continued.slice(1 - MAXIMUM_TRAIL_LENGTH)]
    : continued;
}

function path(url: string) {
  return url.split(/[?#]/)[0];
}

function segments(url: string) {
  return path(url).split("/").filter(Boolean);
}
