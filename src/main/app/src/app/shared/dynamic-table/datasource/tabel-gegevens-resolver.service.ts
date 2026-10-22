/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable, inject } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { GebruikersvoorkeurenService } from "../../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";

@Injectable({
  providedIn: "root",
})
export class TabelGegevensResolver {
  private gebruikersvoorkeurenService = inject(GebruikersvoorkeurenService);

  resolve(route: ActivatedRouteSnapshot) {
    return this.gebruikersvoorkeurenService.readTabelGegevens(
      route.data.werklijst,
    );
  }
}
