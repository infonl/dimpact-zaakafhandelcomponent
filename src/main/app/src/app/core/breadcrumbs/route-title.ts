/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject } from "@angular/core";
import { ActivatedRouteSnapshot, ResolveFn } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { GeneratedType } from "../../shared/utils/generated-types";

export function translatedTitle(key: string): ResolveFn<string> {
  return () => inject(TranslateService).get(key);
}

export type BreadcrumbDetails = {
  zaak: Pick<
    GeneratedType<"RestZaak">,
    "uuid" | "isZaakspecifiekGeautoriseerd"
  >;
};

export type Breadcrumb = {
  title: string;
  url: string;
  key?: string;
  /** Set on a detail page: a detail page of the same type takes the place of this one in the trail */
  type?: string;
  details?: Partial<BreadcrumbDetails>;
};

type BreadcrumbDetailsFn = (
  route: ActivatedRouteSnapshot,
) => Partial<BreadcrumbDetails>;

export const BREADCRUMB_ORIGIN = { breadcrumbs: "origin" } as const;
export const BREADCRUMB_DETAIL = { breadcrumbs: "detail" } as const;
// A section such as `/zaken` is no page of its own. It names the page it redirects to,
// and it shows as the parent of a detail page that was opened without an origin page.
export const BREADCRUMB_SECTION = { breadcrumbSection: true } as const;

// The details end up in the browser history with the trail, so they must be plain data.
export function breadcrumbDetails(details: BreadcrumbDetailsFn) {
  return { breadcrumbDetails: details };
}

export function breadcrumbKind(root: ActivatedRouteSnapshot) {
  let route = root;
  while (route.firstChild) route = route.firstChild;
  const kind: unknown = route.data["breadcrumbs"];
  return kind === "origin" || kind === "detail" ? kind : null;
}

export function breadcrumbs(root: ActivatedRouteSnapshot) {
  const showsSections = breadcrumbKind(root) === "detail";
  const crumbs: Breadcrumb[] = [];
  for (
    let route: ActivatedRouteSnapshot | null = root;
    route;
    route = route.firstChild
  ) {
    // A route without a title of its own inherits the title of its parent.
    const isSection = route.routeConfig?.data?.["breadcrumbSection"] === true;
    if (
      route.routeConfig?.title !== undefined &&
      route.title &&
      (showsSections || !isSection)
    ) {
      const details = route.data["breadcrumbDetails"] as
        | BreadcrumbDetailsFn
        | undefined;
      crumbs.push({
        title: route.title,
        url:
          "/" +
          route.pathFromRoot
            .flatMap(({ url }) => url.map(({ path }) => path))
            .join("/"),
        details: details?.(route),
      });
    }
  }
  return crumbs;
}
