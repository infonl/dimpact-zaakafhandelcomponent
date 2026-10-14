/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Location } from "@angular/common";
import { provideLocationMocks } from "@angular/common/testing";
import { Component } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { provideRouter, Router } from "@angular/router";
import { waitFor } from "@testing-library/angular";
import { BreadcrumbsComponent } from "../../core/breadcrumbs/breadcrumbs.component";
import {
  BREADCRUMB_DETAIL,
  BREADCRUMB_ORIGIN,
} from "../../core/breadcrumbs/route-title";
import {
  findOrigin,
  navigateToBreadcrumb,
  navigationTrail,
  openedFromSearchState,
} from "./navigation-origin";

@Component({ template: "" })
class EmptyComponent {}

describe(findOrigin.name, () => {
  let router: Router;
  let location: Location;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: "",
            title: "Dashboard",
            data: BREADCRUMB_ORIGIN,
            component: EmptyComponent,
          },
          { path: "admin", title: "Beheer", component: EmptyComponent },
          {
            path: "zaken",
            title: "Zaken",
            children: [
              {
                path: "mijn",
                title: "Mijn zaken",
                data: BREADCRUMB_ORIGIN,
                component: EmptyComponent,
              },
              {
                path: ":id",
                title: (route) => route.paramMap.get("id") ?? "",
                data: BREADCRUMB_DETAIL,
                component: EmptyComponent,
              },
            ],
          },
          {
            path: "taken",
            title: "Taken",
            children: [
              {
                path: ":id",
                title: (route) => `Taak ${route.paramMap.get("id")}`,
                data: BREADCRUMB_DETAIL,
                component: EmptyComponent,
              },
            ],
          },
          {
            path: "informatie-objecten",
            title: "Documenten",
            children: [
              {
                path: ":uuid",
                title: "Document",
                data: BREADCRUMB_DETAIL,
                component: EmptyComponent,
              },
              {
                path: ":uuid/:versie",
                title: "Document",
                data: BREADCRUMB_DETAIL,
                component: EmptyComponent,
              },
            ],
          },
        ]),
        provideLocationMocks(),
      ],
    });
    TestBed.createComponent(BreadcrumbsComponent);
    router = TestBed.inject(Router);
    location = TestBed.inject(Location);
  });

  async function navigate(...urls: string[]) {
    for (const url of urls) await router.navigateByUrl(url);
  }

  it("is the page the current page was opened from", async () => {
    await navigate("/zaken/mijn", "/zaken/ZAAK-1", "/taken/1");

    expect(findOrigin(location)?.url).toBe("/zaken/ZAAK-1");
    expect(navigationTrail(location).map(({ title }) => title)).toEqual([
      "Zaken",
      "Mijn zaken",
      "ZAAK-1",
      "Taak 1",
    ]);
  });

  it("keeps the query string of the page the current page was opened from", async () => {
    await navigate("/zaken/mijn?sort=startdatum", "/zaken/ZAAK-1");

    expect(findOrigin(location)?.url).toBe("/zaken/mijn?sort=startdatum");
  });

  it("is the parent page when the current page was opened directly", async () => {
    await navigate("/taken/1");

    expect(findOrigin(location)?.url).toBe("/taken");
  });

  it("is the parent page when the current page was opened from search, because search overlays an unrelated page", async () => {
    await navigate("/zaken/ZAAK-1");
    await router.navigateByUrl("/taken/1", { state: openedFromSearchState });

    expect(findOrigin(location)?.url).toBe("/taken");
  });

  it("is the parent page when the current page was opened from a page that is no origin", async () => {
    await navigate("/admin", "/zaken/ZAAK-1");

    expect(findOrigin(location)?.url).toBe("/zaken");
  });

  it("is the page before the current page when the current page was already in the trail", async () => {
    await navigate("/zaken/mijn", "/zaken/ZAAK-1", "/taken/1", "/zaken/ZAAK-1");

    expect(findOrigin(location)?.url).toBe("/zaken/mijn");
  });

  it("skips other versions of the current page, so it never returns to the page itself", async () => {
    await navigate(
      "/zaken/ZAAK-1",
      "/informatie-objecten/document-1",
      "/informatie-objecten/document-1/2",
    );

    expect(findOrigin(location)?.url).toBe("/zaken/ZAAK-1");
  });

  it("is absent on a page without a parent page", async () => {
    await navigate("/");

    expect(findOrigin(location)).toBeNull();
  });

  it("is restored with the page when the page is reached through the browser history", async () => {
    await navigate("/zaken/mijn", "/zaken/ZAAK-1", "/taken/1", "/admin");

    location.back();

    await waitFor(() =>
      expect(findOrigin(location)?.url).toBe("/zaken/ZAAK-1"),
    );
  });
});

describe(navigateToBreadcrumb.name, () => {
  const breadcrumb = { title: "ZAAK-1", url: "/zaken/ZAAK-1", key: "zaak" };
  let router: Router;

  beforeEach(() => {
    router = TestBed.inject(Router);
    jest.spyOn(router, "navigateByUrl").mockResolvedValue(true);
  });

  it("goes back through the browser history when the page of the breadcrumb is still in it, so the page returns as it was left", async () => {
    Object.assign(window.navigation, {
      entries: () => [{ key: "zaak" }, { key: "taak" }],
      traverseTo: jest.fn(() => ({ finished: Promise.resolve() })),
    });

    await navigateToBreadcrumb(breadcrumb, router);

    expect(window.navigation.traverseTo).toHaveBeenCalledWith("zaak");
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it("opens the url of the breadcrumb when its page is no longer in the browser history", async () => {
    Object.assign(window.navigation, { entries: () => [{ key: "taak" }] });

    await navigateToBreadcrumb(breadcrumb, router);

    expect(window.navigation.traverseTo).not.toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith("/zaken/ZAAK-1");
  });
});
