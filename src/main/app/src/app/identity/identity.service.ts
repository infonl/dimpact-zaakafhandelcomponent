/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";

import {from, Observable} from "rxjs";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";
import {QueryClient} from "@tanstack/angular-query-experimental";
import { persistQueryClient } from '@tanstack/query-persist-client-core'
import { createAsyncStoragePersister } from '@tanstack/query-async-storage-persister'

@Injectable({
  providedIn: "root",
})
export class IdentityService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  private readonly queryClient = new QueryClient()

  constructor() {
    persistQueryClient({
      queryClient: this.queryClient,
      persister: createAsyncStoragePersister({
        storage: window.sessionStorage,
        key: "zac:tanstack:query",
      })
    })
  }

  listGroups(zaaktypeUuid?: string): Observable<GeneratedType<"RestGroup">[]> {
    if (!zaaktypeUuid) {
      return this.zacHttpClient.GET("/rest/identity/groups");
    }

    return this.zacHttpClient.GET(
      "/rest/identity/groups/zaaktype/{zaaktypeUuid}",
      {
        path: { zaaktypeUuid },
      },
    );
  }

  listUsersInGroup(groupId: string) {
    return this.zacHttpClient.GET("/rest/identity/groups/{groupId}/users", {
      path: { groupId },
    });
  }

  listUsers() {
    return this.zacHttpClient.GET(`/rest/identity/users`);
  }

  readLoggedInUser() {
    const promise = this.queryClient.fetchQuery({
      ...this.zacQueryClient.GET("/rest/identity/loggedInUser"),
    })

    return from(promise);
  }

  readLoggedInUserAuthorization() {
    return this.zacQueryClient.GET("/rest/identity/loggedInUser");
  }
}
