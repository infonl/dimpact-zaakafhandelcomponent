/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, RouterStateSnapshot } from "@angular/router";
import { Observable } from "rxjs";
import { KlantenService } from "../klanten.service";
import { Bedrijf } from "../model/bedrijven/bedrijf";

@Injectable({
  providedIn: "root",
})
export class BedrijfResolverService {
  constructor(private klantenService: KlantenService) {}

  resolve(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot,
  ): Observable<Bedrijf> {
    const id: string = route.paramMap.get("vestigingsnummer");
    return this.klantenService.readVestiging(id);
  }
}
