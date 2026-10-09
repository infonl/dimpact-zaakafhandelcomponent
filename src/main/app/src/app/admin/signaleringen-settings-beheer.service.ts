/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class SignaleringenSettingsBeheerService {
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly queryClient = inject(QueryClient);

  list(groupId: string) {
    return this.zacQueryClient.GET(
      "/rest/signaleringen/group/{groupId}/instellingen",
      { path: { groupId } },
    );
  }

  put(groupId: string) {
    return mergeMutationOptions(
      this.zacQueryClient.PUT(
        "/rest/signaleringen/group/{groupId}/instellingen",
        { path: { groupId } },
      ),
      {
        onSuccess: () =>
          this.queryClient.invalidateQueries({
            queryKey: this.list(groupId).queryKey,
          }),
      },
    );
  }
}
