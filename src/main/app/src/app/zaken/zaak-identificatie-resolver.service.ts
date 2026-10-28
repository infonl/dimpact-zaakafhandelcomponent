/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable, inject } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { ZakenService } from "./zaken.service";

@Injectable({
  providedIn: "root",
})
export class ZaakIdentificatieResolver {
  private readonly zakenService = inject(ZakenService);

  resolve(route: ActivatedRouteSnapshot) {
    const zaakID = route.paramMap.get("zaakIdentificatie");
    if (!zaakID) {
      throw new Error(
        `${ZaakIdentificatieResolver.name}: No 'zaakID' found in route`,
      );
    }

    return this.zakenService.readZaakByID(zaakID);
  }
}
