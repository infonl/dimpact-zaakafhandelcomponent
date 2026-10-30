/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable, inject } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { InformatieObjectenService } from "./informatie-objecten.service";

@Injectable({
  providedIn: "root",
})
export class InformatieObjectResolver {
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly queryClient = inject(QueryClient);

  resolve(route: ActivatedRouteSnapshot) {
    const informatieObjectUUID = route.paramMap.get("uuid");
    const informatieObjectVersie = route.paramMap.get("versie");

    if (!informatieObjectUUID) {
      throw new Error(
        `${InformatieObjectResolver.name}: 'uuid' is not defined in route`,
      );
    }

    // The view only hears of changes to the document while it is open.
    void this.informatieObjectenService.invalidateInformatieobject(
      informatieObjectUUID,
    );

    if (informatieObjectVersie) {
      return this.queryClient.fetchQuery(
        this.informatieObjectenService.readEnkelvoudigInformatieobjectVersie(
          informatieObjectUUID,
          Number(informatieObjectVersie),
        ),
      );
    }
    return this.queryClient.fetchQuery(
      this.informatieObjectenService.readEnkelvoudigInformatieobject(
        informatieObjectUUID,
      ),
    );
  }
}
