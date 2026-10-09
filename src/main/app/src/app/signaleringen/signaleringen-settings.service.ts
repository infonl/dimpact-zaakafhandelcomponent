/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { SignaleringenService } from "../signaleringen.service";

@Injectable({
  providedIn: "root",
})
export class SignaleringenSettingsService {
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly queryClient = inject(QueryClient);
  private readonly signaleringenService = inject(SignaleringenService);

  list() {
    return this.zacQueryClient.GET("/rest/signaleringen/instellingen");
  }

  put() {
    return mergeMutationOptions(
      this.zacQueryClient.PUT("/rest/signaleringen/instellingen"),
      {
        onSuccess: () =>
          Promise.all([
            this.queryClient.invalidateQueries({
              queryKey: this.list().queryKey,
            }),
            this.signaleringenService.invalidateDashboardSignaleringTypen(),
          ]),
      },
    );
  }
}
