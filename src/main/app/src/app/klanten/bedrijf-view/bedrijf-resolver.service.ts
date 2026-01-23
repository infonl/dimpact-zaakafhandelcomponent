/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { KlantenService } from "../klanten.service";

@Injectable({
  providedIn: "root",
})
export class BedrijfResolverService {
  constructor(private klantenService: KlantenService) {}

  resolve(route: ActivatedRouteSnapshot) {
    const id = route.paramMap.get("vesOrRSIN");

    if (!id) {
      throw new Error(
        `${BedrijfResolverService.name}: no 'vesOrRSIN' found in route`,
      );
    }

    return this.klantenService.readBedrijf(id);
  }
}
