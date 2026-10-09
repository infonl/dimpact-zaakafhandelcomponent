/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { provideRouter, Router } from "@angular/router";
import { UtilService } from "../../core/service/util.service";
import { navigationOriginState, NavigationService } from "./navigation.service";

@Component({ template: "" })
class EmptyComponent {}

describe(NavigationService.name, () => {
  let service: NavigationService;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        { provide: UtilService, useValue: { setLoading: jest.fn() } },
        provideRouter([{ path: "**", component: EmptyComponent }]),
      ],
    });
    service = TestBed.inject(NavigationService);
    router = TestBed.inject(Router);
  });

  describe(NavigationService.prototype.returnToOrigin.name, () => {
    const isTaak = (path: string) => path === "/taken/1";

    it("goes back to the page the user opened the left page from", async () => {
      await router.navigateByUrl("/zaken/ZAAK-1");
      await router.navigateByUrl("/taken/1");

      await service.returnToOrigin(isTaak, "/");

      expect(router.url).toBe("/zaken/ZAAK-1");
    });

    it("leaves every page that matches, so earlier versions of the same page are skipped", async () => {
      await router.navigateByUrl("/documenten/inbox");
      await router.navigateByUrl("/informatie-objecten/uuid-1");
      await router.navigateByUrl("/informatie-objecten/uuid-1/2");

      await service.returnToOrigin(
        (path) => path.startsWith("/informatie-objecten/uuid-1"),
        "/documenten/ontkoppelde",
      );

      expect(router.url).toBe("/documenten/inbox");
    });

    it("ignores query parameters when it matches the left page", async () => {
      await router.navigateByUrl("/taken/werkvoorraad");
      await router.navigateByUrl("/taken/1?bron=email");

      await service.returnToOrigin(isTaak, "/");

      expect(router.url).toBe("/taken/werkvoorraad");
    });

    it("goes to the fallback when the left page was opened from search, because search overlays an unrelated page", async () => {
      await router.navigateByUrl("/zaken/ZAAK-1");
      await router.navigateByUrl("/taken/1", {
        state: navigationOriginState("zoeken"),
      });

      await service.returnToOrigin(isTaak, "/dashboard");

      expect(router.url).toBe("/dashboard");
    });

    it("goes to the fallback when no page was opened before the left page", async () => {
      await router.navigateByUrl("/taken/1");

      await service.returnToOrigin(isTaak, "/dashboard");

      expect(router.url).toBe("/dashboard");
    });

    it("reads the history that an earlier version stored as plain URLs", async () => {
      sessionStorage.setItem(
        "navigationHistory",
        JSON.stringify(["/zaken/ZAAK-1", "/taken/1"]),
      );

      await service.returnToOrigin(isTaak, "/");

      expect(router.url).toBe("/zaken/ZAAK-1");
    });

    it("does not add the page it returns to a second time, so the back button still works", async () => {
      await router.navigateByUrl("/zaken/ZAAK-1");
      await router.navigateByUrl("/taken/1");

      await service.returnToOrigin(isTaak, "/");

      expect(JSON.parse(sessionStorage.getItem("navigationHistory")!)).toEqual([
        { url: "/zaken/ZAAK-1" },
      ]);
    });
  });
});
