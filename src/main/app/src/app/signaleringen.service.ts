/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { PutBody } from "./shared/http/http-client";
import { ZacQueryClient } from "./shared/http/zac-query-client";
import { GeneratedType } from "./shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class SignaleringenService {
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly queryClient = inject(QueryClient);

  readLatestSignalering() {
    return this.zacQueryClient.GET("/rest/signaleringen/latest");
  }

  invalidateLatestSignalering() {
    return this.queryClient.invalidateQueries({
      queryKey: this.readLatestSignalering().queryKey,
    });
  }

  listDashboardSignaleringTypen() {
    return this.zacQueryClient.GET("/rest/signaleringen/typen/dashboard");
  }

  invalidateDashboardSignaleringTypen() {
    return this.queryClient.invalidateQueries({
      queryKey: this.listDashboardSignaleringTypen().queryKey,
    });
  }

  listZakenSignalering(
    type: GeneratedType<"Type">,
    body: PutBody<"/rest/signaleringen/zaken/{type}">,
  ) {
    return this.zacQueryClient.PUT_QUERY(
      "/rest/signaleringen/zaken/{type}",
      body,
      { path: { type } },
    );
  }

  listTakenSignalering(
    signaleringType: GeneratedType<"RestSignaleringInstellingen">["type"],
  ) {
    return this.zacQueryClient.GET("/rest/signaleringen/taken/{type}", {
      path: { type: signaleringType },
    });
  }

  listInformatieobjectenSignalering(
    signaleringType: GeneratedType<"RestSignaleringInstellingen">["type"],
  ) {
    return this.zacQueryClient.GET(
      "/rest/signaleringen/informatieobjecten/{type}",
      { path: { type: signaleringType } },
    );
  }
}
