/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import {
  ActivatedRouteSnapshot,
  CanActivate,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from "@angular/router";

@Injectable({
  providedIn: "root",
})
export class PersoonResolverGuard implements CanActivate {
  private readonly uuidRegex =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

  constructor(private router: Router) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    _state: RouterStateSnapshot,
  ): UrlTree | boolean {
    const temporaryPersonId = route.paramMap.get("temporaryPersonId");

    if (!temporaryPersonId || !this.isValidUuid(temporaryPersonId)) {
      return this.router.createUrlTree(["/persoon"]);
    }

    return true;
  }

  private isValidUuid(value: string): boolean {
    return this.uuidRegex.test(value);
  }
}
