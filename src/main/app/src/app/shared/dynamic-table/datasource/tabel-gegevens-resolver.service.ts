/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { GebruikersvoorkeurenService } from "../../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";

@Injectable({
  providedIn: "root",
})
export class TabelGegevensResolver {
  private readonly gebruikersvoorkeurenService = inject(
    GebruikersvoorkeurenService,
  );
  private readonly queryClient = inject(QueryClient);

  resolve(route: ActivatedRouteSnapshot) {
    return this.queryClient.fetchQuery(
      this.gebruikersvoorkeurenService.readTabelGegevens(route.data.werklijst),
    );
  }
}
