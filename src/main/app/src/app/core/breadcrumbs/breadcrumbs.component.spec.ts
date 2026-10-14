/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Location } from "@angular/common";
import { provideLocationMocks } from "@angular/common/testing";
import { Component } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { Title } from "@angular/platform-browser";
import { provideRouter, Router, TitleStrategy } from "@angular/router";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { render, screen, waitFor, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { BreadcrumbsComponent } from "./breadcrumbs.component";
import {
  BREADCRUMB_DETAIL,
  BREADCRUMB_ORIGIN,
  translatedTitle,
} from "./route-title";
import { ZacTitleStrategy } from "./zac-title-strategy";

@Component({ template: "" })
class PageStubComponent {}

describe(BreadcrumbsComponent.name, () => {
  async function navigateTo(...urls: string[]) {
    const { fixture } = await render(BreadcrumbsComponent, {
      imports: [TranslateModule.forRoot()],
      providers: [
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
                data: BREADCRUMB_DETAIL,
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
      ],
    });
    TestBed.inject(TranslateService).setTranslation("nl", {
      title: "Zaakafhandelcomponent - {{title}}",
      breadcrumbs: "Kruimelpad",
      "actie.admin": "Beheer-instellingen",
      "title.parameters": "Zaakafhandelparameters",
    });
    TestBed.inject(TranslateService).use("nl");
    for (const url of urls) {
      await TestBed.inject(Router).navigateByUrl(url);
    }
    fixture.detectChanges();
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

  it("shows the breadcrumbs of its own route when a detail page is opened directly", async () => {
    await navigateTo("/taken/1");

    expect(crumbs()).toEqual(["Taken", "Taak 1"]);
  });

  it("shows the breadcrumbs of its own route when a detail page is opened from a page that is no origin", async () => {
    await navigateTo("/admin/parameters", "/zaken/ZAAK-1");

    expect(crumbs()).toEqual(["Zaken", "ZAAK-1"]);
  });

  it("cuts the breadcrumbs back to a page that is already in them", async () => {
    await navigateTo("/zaken/mijn", "/zaken/ZAAK-1", "/taken/1");

    await userEvent.setup().click(screen.getByRole("link", { name: "ZAAK-1" }));

    expect(crumbs()).toEqual(["Zaken", "Mijn zaken", "ZAAK-1"]);
  });

  it("restores the breadcrumbs of a detail page that is reached through the browser history", async () => {
    await navigateTo("/zaken/mijn", "/zaken/ZAAK-1", "/admin/parameters");

    TestBed.inject(Location).back();

    await waitFor(() =>
      expect(crumbs()).toEqual(["Zaken", "Mijn zaken", "ZAAK-1"]),
    );
  });
});
