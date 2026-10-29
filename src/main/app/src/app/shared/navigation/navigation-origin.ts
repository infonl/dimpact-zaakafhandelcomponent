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

export function continueTrail(trail: Breadcrumb[], page: Breadcrumb) {
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
