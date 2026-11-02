/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { Provider } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { screen } from "@testing-library/angular";
import { sleep } from "../../../../setupJest";
import { QUERY_CLIENT } from "../../shared/http/query-client";
import { FoutAfhandelingService } from "../fout-afhandeling.service";
import { FoutDetailedDialogComponent } from "./fout-detailed-dialog.component";

const SERVER_ERROR_TEXTS_URL = "/rest/referentietabellen/server-error-text";

function setup(
  showServerErrorTexts: boolean,
  providers: Provider[] = [],
  waarschuwing?: string,
  titel?: string,
) {
  TestBed.configureTestingModule({
    imports: [FoutDetailedDialogComponent, NoopAnimationsModule],
    providers: [
      provideTranslateService(),
      { provide: MatDialogRef, useValue: { close: jest.fn() } },
      {
        provide: MAT_DIALOG_DATA,
        useValue: {
          error: "Er is een fout opgetreden",
          details: "",
          showServerErrorTexts,
          waarschuwing,
          titel,
        },
      },
      ...providers,
    ],
  });

  const fixture = TestBed.createComponent(FoutDetailedDialogComponent);
  fixture.detectChanges();
  return {
    fixture,
    httpTestingController: TestBed.inject(HttpTestingController),
  };
}

describe(FoutDetailedDialogComponent.name, () => {
  it("shows the waarschuwing it is given above the error", async () => {
    setup(false, [], "fakeWaarschuwing");

    expect(screen.getByText("fakeWaarschuwing")).toBeInTheDocument();
    expect(screen.getByText("warning")).toBeInTheDocument();
  });

  it("shows the general title when it is given no title of its own", async () => {
    setup(false);

    expect(
      screen.getByText("dialoog.error.titel.algemeen"),
    ).toBeInTheDocument();
  });

  it("shows the title it is given instead of the general one", async () => {
    setup(false, [], "fakeWaarschuwing", "fakeTitel");

    expect(screen.getByText("fakeTitel")).toBeInTheDocument();
    expect(
      screen.queryByText("dialoog.error.titel.algemeen"),
    ).not.toBeInTheDocument();
  });

  it("does not load the server error texts when it does not show them", async () => {
    const { httpTestingController } = setup(false);
    await sleep();

    httpTestingController.expectNone(SERVER_ERROR_TEXTS_URL);
  });

  it("loads and shows the server error texts when it shows them", async () => {
    const { fixture, httpTestingController } = setup(true);
    await sleep();

    httpTestingController
      .expectOne(SERVER_ERROR_TEXTS_URL)
      .flush(["Neem contact op met de beheerder"]);
    // TanStack notifies its observers on one timer tick and injectQuery updates its signal on the next.
    await sleep();
    await sleep();
    fixture.detectChanges();

    expect(
      screen.getByText("Neem contact op met de beheerder"),
    ).toBeInTheDocument();
  });

  it("neither retries nor opens another error dialog when the server error texts fail to load", async () => {
    const { httpTestingController } = setup(true, [
      provideTanStackQuery(QUERY_CLIENT),
    ]);
    const foutAfhandelen = jest.spyOn(
      TestBed.inject(FoutAfhandelingService),
      "foutAfhandelen",
    );
    await sleep();

    httpTestingController
      .expectOne(SERVER_ERROR_TEXTS_URL)
      .flush(null, { status: 500, statusText: "Internal Server Error" });
    await sleep();
    expect(
      TestBed.inject(QUERY_CLIENT).getQueryState([SERVER_ERROR_TEXTS_URL]),
    ).toMatchObject({ status: "error", fetchStatus: "idle" });

    expect(
      TestBed.inject(QUERY_CLIENT).getQueryState([SERVER_ERROR_TEXTS_URL]),
    ).toMatchObject({ status: "error", fetchStatus: "idle" });
    httpTestingController.expectNone(SERVER_ERROR_TEXTS_URL);
    expect(foutAfhandelen).not.toHaveBeenCalled();
  });
});
