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

export function breadcrumbTitles(root: ActivatedRouteSnapshot) {
  const titles: string[] = [];
  for (
    let route: ActivatedRouteSnapshot | null = root;
    route;
    route = route.firstChild
  ) {
    // A route without a title of its own inherits the title of its parent.
    if (route.routeConfig?.title !== undefined && route.title) {
      titles.push(route.title);
    }
  }
  return titles;
}
