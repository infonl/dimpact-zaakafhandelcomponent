/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";

import { queryOptions } from "@tanstack/angular-query-experimental";
import { lastValueFrom, Observable } from "rxjs";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class IdentityService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  listGroups(): Observable<GeneratedType<"RestGroup">[]> {
    return this.zacHttpClient.GET("/rest/identity/groups");
  }

  listBehandelaarGroupsForZaaktypes(zaaktypeDescriptions: string[]) {
    return queryOptions({
      queryKey: ["/rest/identity/behandelaar-groups", zaaktypeDescriptions],
      queryFn: () =>
        lastValueFrom(
          this.zacHttpClient.POST("/rest/identity/behandelaar-groups", {
            zaaktypeDescriptions,
          }),
        ),
    });
  }

  listBehandelaarGroupsForZaaktype(
    zaaktypeDescription: string,
  ): Observable<GeneratedType<"RestGroup">[]> {
    return this.zacHttpClient.GET(
      "/rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups",
      {
        path: { zaaktypeDescription },
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
    return this.zacQueryClient.GET("/rest/identity/loggedInUser");
  }
}
