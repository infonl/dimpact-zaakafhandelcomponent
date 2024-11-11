/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, RouterStateSnapshot } from "@angular/router";
import { Observable } from "rxjs";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantenService } from "../klanten.service";

@Injectable({
  providedIn: "root",
})
export class PersoonResolverService {
  constructor(private klantenService: KlantenService) {}

  resolve(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot,
  ): Observable<GeneratedType<"RestPersoon">> {
    const bsn: string = route.paramMap.get("bsn");
    return this.klantenService.readPersoon(bsn);
  }
}
