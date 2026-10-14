/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { Title } from "@angular/platform-browser";
import { provideRouter, Router, TitleStrategy } from "@angular/router";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { render, screen, within } from "@testing-library/angular";
import { BreadcrumbsComponent } from "./breadcrumbs.component";
import { translatedTitle } from "./route-title";
import { ZacTitleStrategy } from "./zac-title-strategy";

@Component({ template: "" })
class PageStubComponent {}

describe(BreadcrumbsComponent.name, () => {
  async function navigateTo(url: string) {
    const { fixture } = await render(BreadcrumbsComponent, {
      imports: [TranslateModule.forRoot()],
      providers: [
        provideRouter([
          { path: "untitled", component: PageStubComponent },
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
      ],
    });
    TestBed.inject(TranslateService).setTranslation("nl", {
      title: "Zaakafhandelcomponent - {{title}}",
      breadcrumbs: "Kruimelpad",
      "actie.admin": "Beheer-instellingen",
      "title.parameters": "Zaakafhandelparameters",
    });
    TestBed.inject(TranslateService).use("nl");
    await TestBed.inject(Router).navigateByUrl(url);
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
});
