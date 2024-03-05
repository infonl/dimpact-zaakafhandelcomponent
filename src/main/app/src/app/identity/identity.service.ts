/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";

import { Observable, of } from "rxjs";
import { catchError, tap } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { SessionStorageUtil } from "../shared/storage/session-storage.util";
import { Group } from "./model/group";
import { LoggedInUser } from "./model/logged-in-user";
import { User } from "./model/user";

@Injectable({
  providedIn: "root",
})
export class IdentityService {
  public static LOGGED_IN_USER_KEY = "loggedInUser";

  constructor(
    private zacHttp: ZacHttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  listGroups(): Observable<Group[]> {
    return this.zacHttp
      .GET("/rest/identity/groups")
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listUsersInGroup(groupId: string): Observable<User[]> {
    return this.zacHttp
      .GET("/rest/identity/groups/{groupId}/users", {
        pathParams: { path: { groupId } },
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listUsers(): Observable<User[]> {
    return this.zacHttp
      .GET(`/rest/identity/users`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readLoggedInUser(): Observable<LoggedInUser> {
    const loggedInUser = SessionStorageUtil.getItem(
      IdentityService.LOGGED_IN_USER_KEY,
    ) as LoggedInUser;
    if (loggedInUser) {
      return of(loggedInUser);
    }
    return this.zacHttp.GET("/rest/identity/loggedInUser").pipe(
      tap((user) => {
        console.log(user);
        SessionStorageUtil.setItem(IdentityService.LOGGED_IN_USER_KEY, user);
      }),
      catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
    );
  }
}
