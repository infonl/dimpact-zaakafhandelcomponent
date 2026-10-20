/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import { sleep } from "../../../../setupJest";
import { FoutDetailedDialogComponent } from "./fout-detailed-dialog.component";

const SERVER_ERROR_TEXTS_URL = "/rest/referentietabellen/server-error-text";

function setup(showServerErrorTexts: boolean) {
  TestBed.configureTestingModule({
    imports: [
      FoutDetailedDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      { provide: MatDialogRef, useValue: { close: jest.fn() } },
      {
        provide: MAT_DIALOG_DATA,
        useValue: {
          error: "Er is een fout opgetreden",
          details: "",
          showServerErrorTexts,
        },
      },
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
});
