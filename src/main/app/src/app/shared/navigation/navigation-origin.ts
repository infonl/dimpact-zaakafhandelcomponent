/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Router } from "@angular/router";

export const openedFromSearchState = { origin: "zoeken" } as const;

/**
 * The page the current page was opened from. The search panel overlays whatever page is
 * open, so that page is not where the user came from: a page opened from search has no
 * origin.
 */
export function findOrigin(router: Router) {
  const currentNavigation = router.lastSuccessfulNavigation;
  if (
    currentNavigation?.extras.state?.["origin"] === openedFromSearchState.origin
  )
    return null;

  return currentNavigation?.previousNavigation?.finalUrl ?? null;
}
