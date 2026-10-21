/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { inject } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideQueryClient,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { screen } from "@testing-library/angular";
import { sleep, testQueryClient } from "../../../setupJest";
import { ReferentieTabelService } from "../admin/referentie-tabel.service";
import { QUERY_CLIENT } from "../shared/http/query-client";
import { FoutAfhandelingComponent } from "./fout-afhandeling.component";
import { FoutAfhandelingService } from "./fout-afhandeling.service";

const SERVER_ERROR_TEXTS_URL = "/rest/referentietabellen/server-error-text";

describe(FoutAfhandelingComponent.name, () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        FoutAfhandelingComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();
  });

  const setup = (
    foutmelding: string,
    bericht: string,
    serverErrorTexts: string[],
  ) => {
    const foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
    const referentieTabelService = TestBed.inject(ReferentieTabelService);
    foutAfhandelingService.foutmelding = foutmelding;
    foutAfhandelingService.bericht = bericht;
    testQueryClient.setQueryData(
      referentieTabelService.listServerErrorTextsQuery().queryKey,
      serverErrorTexts,
    );

    const fixture = TestBed.createComponent(FoutAfhandelingComponent);
    fixture.detectChanges();
  };

  it("displays foutmelding in heading when set", () => {
    setup("Er is een fout opgetreden", "", []);

    expect(
      screen.getByRole("heading", { name: "Er is een fout opgetreden" }),
    ).toBeInTheDocument();
  });

  it("falls back to default translation key in heading when foutmelding is empty", () => {
    setup("", "", []);

    expect(
      screen.getByRole("heading", { name: "error-card.title.default" }),
    ).toBeInTheDocument();
  });

  it("shows bericht when bericht is non-empty", () => {
    setup("", "Neem contact op met de beheerder", []);

    expect(
      screen.getByText("Neem contact op met de beheerder"),
    ).toBeInTheDocument();
  });

  it("shows no paragraphs when there is neither bericht nor server error texts", () => {
    setup("Fout", "", []);

    expect(screen.queryAllByRole("paragraph")).toHaveLength(0);
  });

  it("shows the server error texts", () => {
    setup("", "", ["Server onbeschikbaar", "Probeer het later"]);

    expect(screen.getByText("Server onbeschikbaar")).toBeInTheDocument();
    expect(screen.getByText("Probeer het later")).toBeInTheDocument();
  });

  it("neither retries nor opens an error dialog when the server error texts fail to load", async () => {
    TestBed.overrideProvider(QueryClient, {
      useFactory: () => inject(QUERY_CLIENT),
    });
    const foutAfhandelen = jest.spyOn(
      TestBed.inject(FoutAfhandelingService),
      "foutAfhandelen",
    );
    const httpTestingController = TestBed.inject(HttpTestingController);

    TestBed.createComponent(FoutAfhandelingComponent).detectChanges();
    await sleep();
    httpTestingController
      .expectOne(SERVER_ERROR_TEXTS_URL)
      .flush(null, { status: 500, statusText: "Internal Server Error" });
    await sleep();

    expect(
      TestBed.inject(QUERY_CLIENT).getQueryState([SERVER_ERROR_TEXTS_URL]),
    ).toMatchObject({ status: "error", fetchStatus: "idle" });
    httpTestingController.expectNone(SERVER_ERROR_TEXTS_URL);
    expect(foutAfhandelen).not.toHaveBeenCalled();
  });
});
