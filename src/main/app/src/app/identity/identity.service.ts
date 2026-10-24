/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class IdentityService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  listGroups() {
    return this.zacQueryClient.GET("/rest/identity/groups");
  }

  listBehandelaarGroupsForZaaktypesQuery() {
    return this.zacQueryClient.POST("/rest/identity/behandelaar-groups");
  }

  listBehandelaarGroupsForZaaktype(zaaktypeDescription: string) {
    return this.zacQueryClient.GET(
      "/rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups",
      { path: { zaaktypeDescription } },
    );
  }

  listUsersInGroup(groupId: string) {
    return this.zacQueryClient.GET("/rest/identity/groups/{groupId}/users", {
      path: { groupId },
    });
  }

  listUsers() {
    return this.zacQueryClient.GET("/rest/identity/users");
  }

  readLoggedInUser() {
    return this.zacQueryClient.GET("/rest/identity/loggedInUser");
  }
}
