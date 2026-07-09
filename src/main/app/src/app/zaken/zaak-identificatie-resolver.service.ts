/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { Observable } from "rxjs";
import { Zaak } from "./model/zaak";
import { ZakenService } from "./zaken.service";

@Injectable({
  providedIn: "root",
})
export class ZaakIdentificatieResolver {
  constructor(private zakenService: ZakenService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<Zaak> {
    const zaakID = route.paramMap.get("zaakIdentificatie");
    if (!zaakID) {
      throw new Error(
        `${ZaakIdentificatieResolver.name}: No 'zaakID' found in route`,
      );
    }

    return this.zakenService.readZaakByID(zaakID);
  }
}
