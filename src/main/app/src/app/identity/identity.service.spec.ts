/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { testQueryClient } from "../../../setupJest";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { IdentityService } from "./identity.service";

describe(IdentityService.name, () => {
  let service: IdentityService;
  let zacHttpClient: ZacHttpClient;
  let zacQueryClient: ZacQueryClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        provideHttpClient(withInterceptorsFromDi()),
        provideQueryClient(testQueryClient),
      ],
    });

    service = TestBed.inject(IdentityService);
    zacHttpClient = TestBed.inject(ZacHttpClient);
    zacQueryClient = TestBed.inject(ZacQueryClient);
  });

  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  describe("listGroups", () => {
    it("fetches all active groups", () => {
      jest.spyOn(zacHttpClient, "GET").mockReturnValue(of([] as never));
      service.listGroups();
      expect(zacHttpClient.GET).toHaveBeenCalledWith("/rest/identity/groups");
    });
  });

  describe("listBehandelaarGroupsForZaaktype", () => {
    it("fetches groups with the given zaaktype description as path parameter", () => {
      jest.spyOn(zacHttpClient, "GET").mockReturnValue(of([] as never));
      service.listBehandelaarGroupsForZaaktype("Test Zaaktype");
      expect(zacHttpClient.GET).toHaveBeenCalledWith(
        "/rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups",
        { path: { zaaktypeDescription: "Test Zaaktype" } },
      );
    });
  });

  describe("listUsersInGroup", () => {
    it("fetches users with the given group id as path parameter", () => {
      jest.spyOn(zacHttpClient, "GET").mockReturnValue(of([] as never));
      service.listUsersInGroup("group-1");
      expect(zacHttpClient.GET).toHaveBeenCalledWith(
        "/rest/identity/groups/{groupId}/users",
        { path: { groupId: "group-1" } },
      );
    });
  });

  describe("listUsers", () => {
    it("fetches all users", () => {
      jest.spyOn(zacHttpClient, "GET").mockReturnValue(of([] as never));
      service.listUsers();
      expect(zacHttpClient.GET).toHaveBeenCalledWith("/rest/identity/users");
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
