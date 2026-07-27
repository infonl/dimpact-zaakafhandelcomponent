/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { MailtemplateBeheerService } from "./mailtemplate-beheer.service";

@Injectable({
  providedIn: "root",
})
export class MailtemplateResolver {
  private readonly mailtemplateBeheerService = inject(
    MailtemplateBeheerService,
  );
  private readonly queryClient = inject(QueryClient);
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);

  resolve(route: ActivatedRouteSnapshot) {
    const id = route.paramMap.get("id");

    if (!id) {
      throw new Error(
        `${MailtemplateResolver.name}: no 'id' parameter found in route`,
      );
    }

    return this.queryClient.ensureQueryData({
      ...this.mailtemplateBeheerService.readMailtemplateQuery(Number(id)),
      retry: (_count, error) => {
        this.foutAfhandelingService.httpErrorAfhandelen(error);
        return false;
      },
    });
  }
}
