/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenSettingsComponent } from "./signaleringen-settings.component";

const INSTELLINGEN_URL = "/rest/signaleringen/instellingen";

const zaakOpNaam = fromPartial<GeneratedType<"RestSignaleringInstellingen">>({
  id: 1,
  type: "ZAAK_OP_NAAM",
  subjecttype: "ZAAK",
  dashboard: false,
  mail: false,
});

describe(SignaleringenSettingsComponent.name, () => {
  let fixture: ComponentFixture<SignaleringenSettingsComponent>;
  let httpTestingController: HttpTestingController;

  const utilService = fromPartial<UtilService>({
    setTitle: jest.fn(),
    setLoading: jest.fn(),
  });

  const user = userEvent.setup();

  async function renderComponent() {
    const rendered = await render(SignaleringenSettingsComponent, {
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        { provide: UtilService, useValue: utilService },
      ],
    });

    fixture = rendered.fixture;
    httpTestingController = TestBed.inject(HttpTestingController);
  }

  async function respondWith(
    instellingen: GeneratedType<"RestSignaleringInstellingen">[],
  ) {
    httpTestingController.expectOne(INSTELLINGEN_URL).flush(instellingen);
    await sleep();
    fixture.detectChanges();
  }

  function tableWrapper() {
    return fixture.nativeElement.querySelector(".table-wrapper") as HTMLElement;
  }

  function checkbox(name: string) {
    return screen.getByRole("checkbox", { name });
  }

  it("sets the page title", async () => {
    await renderComponent();
    await respondWith([]);

    expect(utilService.setTitle).toHaveBeenCalledWith(
      "title.signaleringen.settings",
    );
  });

  it("shades the table while the settings are still loading", async () => {
    await renderComponent();

    expect(tableWrapper()).toHaveClass("table-loading-shade");

    await respondWith([]);

    expect(tableWrapper()).not.toHaveClass("table-loading-shade");
  });

  it("shows no settings when there are none", async () => {
    await renderComponent();
    await respondWith([]);

    expect(screen.queryAllByRole("checkbox")).toHaveLength(0);
    expect(
      screen.queryByText("signalering.type.ZAAK_OP_NAAM"),
    ).not.toBeInTheDocument();
  });

  it("names every column of the settings table", async () => {
    await renderComponent();
    await respondWith([zaakOpNaam]);

    expect(
      screen.getByRole("columnheader", { name: "signalering.subjecttype" }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: "signalering.type" }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: "signalering.dashboard" }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: "signalering.mail" }),
    ).toBeVisible();
  });

  it("shows the translated subjecttype and type of a setting", async () => {
    await renderComponent();
    await respondWith([zaakOpNaam]);

    const row = screen.getByRole("row", {
      name: /signalering.type.ZAAK_OP_NAAM/,
    });

    expect(within(row).getByText("signalering.subjecttype.ZAAK")).toBeVisible();
    expect(
      within(row).getByText("signalering.type.ZAAK_OP_NAAM"),
    ).toBeVisible();
  });

  it("offers a checkbox for the dashboard and the mail notification", async () => {
    await renderComponent();
    await respondWith([zaakOpNaam]);

    expect(checkbox("actie.signalering.dashboard")).not.toBeChecked();
    expect(checkbox("actie.signalering.mail")).not.toBeChecked();
    expect(screen.getAllByRole("checkbox")).toHaveLength(2);
  });

  it("checks the checkbox of an enabled notification", async () => {
    await renderComponent();
    await respondWith([
      fromPartial<GeneratedType<"RestSignaleringInstellingen">>({
        ...zaakOpNaam,
        dashboard: true,
      }),
    ]);

    expect(checkbox("actie.signalering.dashboard")).toBeChecked();
  });

  it("offers no checkbox for a notification that cannot be configured", async () => {
    await renderComponent();
    await respondWith([
      fromPartial<GeneratedType<"RestSignaleringInstellingen">>({
        ...zaakOpNaam,
        dashboard: null,
      }),
    ]);

    expect(
      screen.queryByRole("checkbox", { name: "actie.signalering.dashboard" }),
    ).not.toBeInTheDocument();
    expect(checkbox("actie.signalering.mail")).toBeInTheDocument();
  });

  it("saves the setting when a checkbox is toggled", async () => {
    await renderComponent();
    await respondWith([zaakOpNaam]);

    await user.click(checkbox("actie.signalering.dashboard"));
    await sleep();

    const request = httpTestingController.expectOne(INSTELLINGEN_URL);

    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual({ ...zaakOpNaam, dashboard: true });

    request.flush({ ...zaakOpNaam, dashboard: true });
    await sleep();
  });

  it("shows the application as loading until the setting is saved", async () => {
    await renderComponent();
    await respondWith([zaakOpNaam]);

    await user.click(checkbox("actie.signalering.mail"));
    await sleep();

    expect(utilService.setLoading).toHaveBeenCalledWith(true);

    httpTestingController
      .expectOne(INSTELLINGEN_URL)
      .flush({ ...zaakOpNaam, mail: true });
    await sleep();

    expect(utilService.setLoading).toHaveBeenCalledWith(false);
  });
});
