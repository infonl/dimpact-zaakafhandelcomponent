/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";

import { Observable, of } from "rxjs";
import { catchError, tap } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { SessionStorageUtil } from "../shared/storage/session-storage.util";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class IdentityService {
  public static LOGGED_IN_USER_KEY = "loggedInUser";

  constructor(
    private zacHttp: ZacHttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  listGroups(zaaktypeUuid?: string): Observable<GeneratedType<"RestGroup">[]> {
    if (!zaaktypeUuid) {
      return this.zacHttp
        .GET("/rest/identity/groups")
        .pipe(
          catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
        );
    }

    return this.zacHttp
      .GET("/rest/identity/groups/zaaktype/{zaaktypeUuid}", {
        pathParams: { path: { zaaktypeUuid } },
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listUsersInGroup(groupId: string) {
    return this.zacHttp
      .GET("/rest/identity/groups/{groupId}/users", {
        pathParams: { path: { groupId } },
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listUsers() {
    return this.zacHttp
      .GET(`/rest/identity/users`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readLoggedInUser() {
    const loggedInUser = SessionStorageUtil.getItem<
      GeneratedType<"RestLoggedInUser">
    >(IdentityService.LOGGED_IN_USER_KEY);
    if (loggedInUser) {
      return of(loggedInUser);
    }
    return this.zacHttp.GET("/rest/identity/loggedInUser").pipe(
      tap((user) => {
        SessionStorageUtil.setItem(IdentityService.LOGGED_IN_USER_KEY, user);
      }),
      catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
    );
  }
}
