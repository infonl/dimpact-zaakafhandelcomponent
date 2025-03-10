/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { GebruikersvoorkeurenService } from "../../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";

@Injectable({
  providedIn: "root",
})
export class TabelGegevensResolver {
  constructor(
    private gebruikersvoorkeurenService: GebruikersvoorkeurenService,
  ) {}

  resolve(route: ActivatedRouteSnapshot) {
    return this.gebruikersvoorkeurenService.readTabelGegevens(
      route.data.werklijst,
    );
  }
}
