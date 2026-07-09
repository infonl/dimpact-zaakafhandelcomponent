/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { KlantenService } from "../klanten.service";

@Injectable({
  providedIn: "root",
})
export class PersoonResolverService {
  constructor(private klantenService: KlantenService) {}

  resolve(route: ActivatedRouteSnapshot) {
    const bsn = route.paramMap.get("bsn");

    if (!bsn) {
      throw new Error(
        `${PersoonResolverService.name}: no 'bsn' found in route`,
      );
    }

    return this.klantenService.readPersoon(bsn);
  }
}
