/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject } from "@angular/core";
import { ActivatedRouteSnapshot, ResolveFn } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";

export function translatedTitle(key: string): ResolveFn<string> {
  return () => inject(TranslateService).get(key);
}

export type Breadcrumb = { title: string; url: string[] };

export const BREADCRUMB_ORIGIN = { breadcrumbs: "origin" } as const;
export const BREADCRUMB_DETAIL = { breadcrumbs: "detail" } as const;

export function breadcrumbKind(root: ActivatedRouteSnapshot) {
  let route = root;
  while (route.firstChild) route = route.firstChild;
  const kind: unknown = route.data["breadcrumbs"];
  return kind === "origin" || kind === "detail" ? kind : null;
}

export function breadcrumbs(root: ActivatedRouteSnapshot) {
  const crumbs: Breadcrumb[] = [];
  for (
    let route: ActivatedRouteSnapshot | null = root;
    route;
    route = route.firstChild
  ) {
    // A route without a title of its own inherits the title of its parent.
    if (route.routeConfig?.title !== undefined && route.title) {
      crumbs.push({
        title: route.title,
        url: [
          "/",
          ...route.pathFromRoot.flatMap(({ url }) =>
            url.map(({ path }) => path),
          ),
        ],
      });
    }
  }
  return crumbs;
}
