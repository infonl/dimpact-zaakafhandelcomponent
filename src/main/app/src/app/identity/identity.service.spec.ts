/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
  withXhr,
} from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { provideTranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../setupJest";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { IdentityService } from "./identity.service";

describe(IdentityService.name, () => {
  let service: IdentityService;
  let zacQueryClient: ZacQueryClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        { provide: FoutAfhandelingService, useValue: {} },
        provideHttpClient(withXhr(), withInterceptorsFromDi()),
        provideQueryClient(testQueryClient),
      ],
    });

    service = TestBed.inject(IdentityService);
    zacQueryClient = TestBed.inject(ZacQueryClient);
  });

  describe("listGroups", () => {
    it("builds query options for all active groups", () => {
      jest.spyOn(zacQueryClient, "GET");
      service.listGroups();
      expect(zacQueryClient.GET).toHaveBeenCalledWith("/rest/identity/groups");
    });
  });

  describe("listBehandelaarGroupsForZaaktype", () => {
    it("builds query options for the groups with the given zaaktype description as path parameter", () => {
      jest.spyOn(zacQueryClient, "GET");
      service.listBehandelaarGroupsForZaaktype("Test Zaaktype");
      expect(zacQueryClient.GET).toHaveBeenCalledWith(
        "/rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups",
        { path: { zaaktypeDescription: "Test Zaaktype" } },
      );
    });
  });

  describe("listUsersInGroup", () => {
    it("builds query options for the users with the given group id as path parameter", () => {
      jest.spyOn(zacQueryClient, "GET");
      service.listUsersInGroup("group-1");
      expect(zacQueryClient.GET).toHaveBeenCalledWith(
        "/rest/identity/groups/{groupId}/users",
        { path: { groupId: "group-1" } },
      );
    });
  });

  describe("listUsers", () => {
    it("builds query options for all users", () => {
      jest.spyOn(zacQueryClient, "GET");
      service.listUsers();
      expect(zacQueryClient.GET).toHaveBeenCalledWith("/rest/identity/users");
    });
  });

  describe("readLoggedInUser", () => {
    it("builds query options for the logged-in user endpoint", () => {
      jest.spyOn(zacQueryClient, "GET");
      service.readLoggedInUser();
      expect(zacQueryClient.GET).toHaveBeenCalledWith(
        "/rest/identity/loggedInUser",
      );
    });
  });
});
