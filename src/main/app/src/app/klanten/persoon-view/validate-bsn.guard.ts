/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";

export const validateBsnGuard: CanActivateFn = () => {
  const router = inject(Router);
  const bsn = router.getCurrentNavigation()?.extras.state?.bsn;

  if (!bsn) {
    return router.createUrlTree(["/persoon/fout"]);
  }

  return true;
};
