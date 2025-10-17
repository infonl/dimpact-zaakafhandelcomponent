/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";

import { Observable, of } from "rxjs";
import { tap } from "rxjs/operators";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { SessionStorageUtil } from "../shared/storage/session-storage.util";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class IdentityService {
  public static LOGGED_IN_USER_KEY = "loggedInUser";

  constructor(private readonly zacHttpClient: ZacHttpClient) {}

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
    const loggedInUser = SessionStorageUtil.getItem<
      GeneratedType<"RestLoggedInUser">
    >(IdentityService.LOGGED_IN_USER_KEY);
    if (loggedInUser) {
      return of(loggedInUser);
    }
    return this.zacHttpClient.GET("/rest/identity/loggedInUser").pipe(
      tap((user) => {
        SessionStorageUtil.setItem(IdentityService.LOGGED_IN_USER_KEY, user);
      }),
    );
  }
}
