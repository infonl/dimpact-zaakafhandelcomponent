/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Location } from "@angular/common";
import { provideLocationMocks } from "@angular/common/testing";
import { Component } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { Title } from "@angular/platform-browser";
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  TitleStrategy,
} from "@angular/router";
import { TranslateService, provideTranslateService } from "@ngx-translate/core";
import { render, screen, waitFor, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { navigationTrail } from "../../shared/navigation/navigation-origin";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { BreadcrumbsComponent } from "./breadcrumbs.component";
import {
  BREADCRUMB_DETAIL,
  BREADCRUMB_ORIGIN,
  breadcrumbDetails,
  translatedTitle,
} from "./route-title";
import { ZacTitleStrategy } from "./zac-title-strategy";

@Component({ template: "" })
class PageStubComponent {}

describe(BreadcrumbsComponent.name, () => {
  let zaakspecifiekGeautoriseerdeZaken: string[];

  beforeEach(() => {
    zaakspecifiekGeautoriseerdeZaken = ["ZAAK-ZAAKSPECIFIEK"];
  });

  function zaak(identificatie: string, isZaakspecifiekGeautoriseerd: boolean) {
    return fromPartial<GeneratedType<"RestZaak">>({
      uuid: `uuid-${identificatie}`,
      identificatie,
      isZaakspecifiekGeautoriseerd,
    });
  }

  async function navigateTo(...urls: string[]) {
    const { fixture } = await render(BreadcrumbsComponent, {
      providers: [
        provideTranslateService(),
        provideRouter([
          { path: "untitled", component: PageStubComponent },
          {
            path: "zaken",
            title: "Zaken",
            children: [
              {
                path: "mijn",
                title: "Mijn zaken",
                data: BREADCRUMB_ORIGIN,
                component: PageStubComponent,
              },
              {
                path: ":id",
                title: (route) => route.paramMap.get("id") ?? "",
                resolve: {
                  zaak: (route: ActivatedRouteSnapshot) => {
                    const identificatie = route.paramMap.get("id") ?? "";
                    return zaak(
                      identificatie,
                      zaakspecifiekGeautoriseerdeZaken.includes(identificatie),
                    );
                  },
                },
                data: {
                  ...BREADCRUMB_DETAIL,
                  ...breadcrumbDetails((route) => {
                    const { uuid, isZaakspecifiekGeautoriseerd } = route.data[
                      "zaak"
                    ] as GeneratedType<"RestZaak">;
                    return { zaak: { uuid, isZaakspecifiekGeautoriseerd } };
                  }),
                },
                component: PageStubComponent,
              },
            ],
          },
          {
            path: "taken",
            title: "Taken",
            children: [
              {
                path: "mijn",
                title: "Mijn taken",
                data: BREADCRUMB_ORIGIN,
                component: PageStubComponent,
              },
              {
                path: ":id",
                title: (route) => `Taak ${route.paramMap.get("id")}`,
                data: BREADCRUMB_DETAIL,
                component: PageStubComponent,
              },
            ],
          },
          {
            path: "admin",
            title: translatedTitle("actie.admin"),
            children: [
              {
                path: "parameters",
                title: translatedTitle("title.parameters"),
                children: [
                  { path: "", component: PageStubComponent },
                  {
                    path: ":uuid",
                    resolve: { zaaktype: () => "Intern zaaktype" },
                    children: [
                      {
                        path: "",
                        title: (route) => route.data["zaaktype"],
                        component: PageStubComponent,
                      },
                    ],
                  },
                ],
              },
            ],
          },
        ]),
        { provide: TitleStrategy, useClass: ZacTitleStrategy },
        provideLocationMocks(),
        provideNoopAnimations(),
      ],
    });
    TestBed.inject(TranslateService).setTranslation("nl", {
      title: "Zaakafhandelcomponent - {{title}}",
      breadcrumbs: "Kruimelpad",
      "actie.admin": "Beheer-instellingen",
      "title.parameters": "Zaakafhandelparameters",
      "zaak.is-zaakspecifiek-geautoriseerd": "Zaakspecifiek geautoriseerd",
    });
    TestBed.inject(TranslateService).use("nl");
    for (const url of urls) {
      await TestBed.inject(Router).navigateByUrl(url);
    }
    fixture.detectChanges();
    return fixture;
  }

  function crumbs() {
    return within(screen.getByRole("navigation", { name: "Kruimelpad" }))
      .getAllByRole("listitem")
      .map((item) => item.textContent?.trim());
  }

  it("shows the title of every route from the root down to the page", async () => {
    await navigateTo("/admin/parameters/some-uuid");

    expect(crumbs()).toEqual([
      "Beheer-instellingen",
      "Zaakafhandelparameters",
      "Intern zaaktype",
    ]);
  });

  it("links every crumb but the last to the page of its route", async () => {
    await navigateTo("/admin/parameters/some-uuid");

    expect(
      screen.getByRole("link", { name: "Beheer-instellingen" }),
    ).toHaveAttribute("href", "/admin");
    expect(
      screen.getByRole("link", { name: "Zaakafhandelparameters" }),
    ).toHaveAttribute("href", "/admin/parameters");
    expect(
      screen.queryByRole("link", { name: "Intern zaaktype" }),
    ).not.toBeInTheDocument();
  });

  it("navigates to the page of a crumb when it is clicked", async () => {
    await navigateTo("/admin/parameters/some-uuid");

    await userEvent
      .setup()
      .click(screen.getByRole("link", { name: "Zaakafhandelparameters" }));

    expect(TestBed.inject(Router).url).toBe("/admin/parameters");
    expect(crumbs()).toEqual(["Beheer-instellingen", "Zaakafhandelparameters"]);
  });

  it("marks the last crumb as the current page", async () => {
    await navigateTo("/admin/parameters/some-uuid");

    expect(screen.getByText("Intern zaaktype")).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  it("shows a parent title once, although the empty-path child inherits it", async () => {
    await navigateTo("/admin/parameters");

    expect(crumbs()).toEqual(["Beheer-instellingen", "Zaakafhandelparameters"]);
  });

  it("titles the browser tab after the breadcrumbs", async () => {
    await navigateTo("/admin/parameters/some-uuid");

    expect(TestBed.inject(Title).getTitle()).toBe(
      "Zaakafhandelcomponent - Beheer-instellingen | Zaakafhandelparameters | Intern zaaktype",
    );
  });

  it("shows no breadcrumbs on a route without a title", async () => {
    await navigateTo("/untitled");

    expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
    expect(TestBed.inject(Title).getTitle()).toBe("Zaakafhandelcomponent");
  });

  it("continues the breadcrumbs of the origin page a detail page is opened from", async () => {
    await navigateTo("/taken/mijn", "/zaken/ZAAK-1", "/taken/1");

    expect(crumbs()).toEqual(["Taken", "Mijn taken", "ZAAK-1", "Taak 1"]);
  });

  it("links the inherited breadcrumbs to the pages they came from", async () => {
    await navigateTo("/zaken/mijn", "/zaken/ZAAK-1", "/taken/1");

    expect(screen.getByRole("link", { name: "ZAAK-1" })).toHaveAttribute(
      "href",
      "/zaken/ZAAK-1",
    );
  });

  it("cuts the breadcrumbs back to a page that is already in them", async () => {
    await navigateTo("/zaken/mijn", "/zaken/ZAAK-1", "/taken/1");

    await userEvent.setup().click(screen.getByRole("link", { name: "ZAAK-1" }));

    expect(crumbs()).toEqual(["Zaken", "Mijn zaken", "ZAAK-1"]);
  });

  it("shows a lock that screen readers announce as zaakspecifiek geautoriseerd beside the zaaknummer of a zaakspecifiek geautoriseerde zaak", async () => {
    await navigateTo("/zaken/mijn", "/zaken/ZAAK-ZAAKSPECIFIEK");

    const currentPage = screen.getByRole("listitem", { current: "page" });
    expect(currentPage).toHaveTextContent("ZAAK-ZAAKSPECIFIEK");
    expect(
      within(currentPage).getByRole("img", {
        name: "Zaakspecifiek geautoriseerd",
      }),
    ).toBeInTheDocument();
  });

  it("keeps the lock on the zaak crumb when a taak is opened from a zaakspecifiek geautoriseerde zaak", async () => {
    await navigateTo("/zaken/mijn", "/zaken/ZAAK-ZAAKSPECIFIEK", "/taken/1");

    const zaakLink = screen.getByRole("link", { name: /ZAAK-ZAAKSPECIFIEK/ });
    expect(zaakLink).toHaveAttribute("href", "/zaken/ZAAK-ZAAKSPECIFIEK");
    expect(
      within(zaakLink).getByRole("img", {
        name: "Zaakspecifiek geautoriseerd",
      }),
    ).toBeInTheDocument();
  });

  it("shows no lock beside the zaaknummer of a zaak that is not zaakspecifiek geautoriseerd", async () => {
    await navigateTo("/zaken/mijn", "/zaken/ZAAK-1");

    expect(screen.getByText("ZAAK-1")).toBeInTheDocument();
    expect(
      screen.queryByRole("img", { name: "Zaakspecifiek geautoriseerd" }),
    ).not.toBeInTheDocument();
  });

  it("shows the lock as soon as the zaak is saved as zaakspecifiek geautoriseerd, without a navigation", async () => {
    const fixture = await navigateTo("/zaken/mijn", "/zaken/ZAAK-1");

    TestBed.inject(ZakenService).cacheZaak(zaak("ZAAK-1", true));
    fixture.detectChanges();

    expect(
      screen.getByRole("img", { name: "Zaakspecifiek geautoriseerd" }),
    ).toBeInTheDocument();
  });

  it("removes the lock as soon as the zaakspecifieke autorisatie of the zaak is lifted, without a navigation", async () => {
    const fixture = await navigateTo(
      "/zaken/mijn",
      "/zaken/ZAAK-ZAAKSPECIFIEK",
    );

    TestBed.inject(ZakenService).cacheZaak(zaak("ZAAK-ZAAKSPECIFIEK", false));
    fixture.detectChanges();

    expect(
      screen.queryByRole("img", { name: "Zaakspecifiek geautoriseerd" }),
    ).not.toBeInTheDocument();
  });

  it("saves the changed zaakspecifieke autorisatie in the trail of the page, for a reload", async () => {
    await navigateTo("/zaken/mijn", "/zaken/ZAAK-1");

    TestBed.inject(ZakenService).cacheZaak(zaak("ZAAK-1", true));

    expect(navigationTrail(TestBed.inject(Location)).at(-1)).toEqual(
      expect.objectContaining({
        title: "ZAAK-1",
        details: {
          zaak: { uuid: "uuid-ZAAK-1", isZaakspecifiekGeautoriseerd: true },
        },
      }),
    );
  });

  it("keeps the changed lock on the zaak crumb when a taak is opened after the save", async () => {
    const fixture = await navigateTo("/zaken/mijn", "/zaken/ZAAK-1");
    TestBed.inject(ZakenService).cacheZaak(zaak("ZAAK-1", true));

    await TestBed.inject(Router).navigateByUrl("/taken/1");
    fixture.detectChanges();

    expect(
      within(screen.getByRole("link", { name: /ZAAK-1/ })).getByRole("img", {
        name: "Zaakspecifiek geautoriseerd",
      }),
    ).toBeInTheDocument();
  });

  it("shows the current zaakspecifieke autorisatie of the zaak when its page is restored from the history", async () => {
    const fixture = await navigateTo(
      "/zaken/mijn",
      "/zaken/ZAAK-ZAAKSPECIFIEK",
      "/taken/1",
    );
    zaakspecifiekGeautoriseerdeZaken = [];

    TestBed.inject(Location).back();

    await waitFor(() =>
      expect(TestBed.inject(Router).url).toBe("/zaken/ZAAK-ZAAKSPECIFIEK"),
    );
    fixture.detectChanges();
    expect(
      screen.queryByRole("img", { name: "Zaakspecifiek geautoriseerd" }),
    ).not.toBeInTheDocument();
  });

  it("shows the lock on a page restored from the history whose saved trail has no details of its zaak", async () => {
    const fixture = await navigateTo(
      "/zaken/mijn",
      "/zaken/ZAAK-ZAAKSPECIFIEK",
    );
    const location = TestBed.inject(Location);
    location.replaceState(location.path(), "", {
      navigationTrail: navigationTrail(location).map(({ title, url, key }) => ({
        title,
        url,
        key,
      })),
    });
    await TestBed.inject(Router).navigateByUrl("/taken/1");

    location.back();

    await waitFor(() =>
      expect(TestBed.inject(Router).url).toBe("/zaken/ZAAK-ZAAKSPECIFIEK"),
    );
    fixture.detectChanges();
    expect(
      within(screen.getByRole("listitem", { current: "page" })).getByRole(
        "img",
        { name: "Zaakspecifiek geautoriseerd" },
      ),
    ).toBeInTheDocument();
  });
});
