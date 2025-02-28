/*
 * SPDX-FileCopyrightText: 2022 Atos
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
export class ZaakUuidResolver {
  constructor(private zakenService: ZakenService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<Zaak> {
    const zaakUuid = route.paramMap.get("zaakUuid");
    if (!zaakUuid) {
      throw new Error("'zaakUuid' is missing in the route parameters");
    }
    return this.zakenService.readZaak(zaakUuid);
  }
}
