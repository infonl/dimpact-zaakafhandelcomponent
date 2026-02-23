/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { CanMatch, Route, UrlSegment } from "@angular/router";

@Injectable({
  providedIn: "root",
})
export class PersoonResolverGuard implements CanMatch {
  private readonly uuidRegex =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

  canMatch(_route: Route, segments: UrlSegment[]): boolean {
    const temporaryPersonId = segments[0]?.path;
    return !!temporaryPersonId && this.isValidUuid(temporaryPersonId);
  }

  private isValidUuid(value: string): boolean {
    return this.uuidRegex.test(value);
  }
}
