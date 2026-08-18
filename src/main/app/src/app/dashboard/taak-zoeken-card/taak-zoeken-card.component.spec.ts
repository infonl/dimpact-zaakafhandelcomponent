/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";

import { sleep, testQueryClient } from "../../../../setupJest";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { DashboardCard } from "../model/dashboard-card";
import { DashboardCardId } from "../model/dashboard-card-id";
import { DashboardCardType } from "../model/dashboard-card-type";
import { TaakZoekenCardComponent } from "./taak-zoeken-card.component";

const makeResultaat = (totaal: number, count = totaal) => ({
  resultaten: Array.from({ length: count }, (_, index) => ({
    naam: `TAAK-FOUND-${index}`,
  })),
  totaal,
});

const cardData = new DashboardCard(
  DashboardCardId.MIJN_TAKEN,
  DashboardCardType.TAAK_ZOEKEN,
);

describe(TaakZoekenCardComponent.name, () => {
  let fixture: ComponentFixture<TaakZoekenCardComponent>;
  let httpTestingController: HttpTestingController;

  const user = userEvent.setup();

  beforeEach(() => {
    notifyManager.setScheduler((fn) => fn());
  });

  afterEach(() => {
    notifyManager.setScheduler((fn) => Promise.resolve().then(fn));
  });

  async function setup() {
    const rendered = await render(TaakZoekenCardComponent, {
      inputs: { data: cardData },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTanStackQuery(testQueryClient),
        { provide: WebsocketService, useValue: { addListener: jest.fn() } },
      ],
    });

    fixture = rendered.fixture;
    httpTestingController = TestBed.inject(HttpTestingController);
    await sleep();
  }

  async function respondWith(resultaat: ReturnType<typeof makeResultaat>) {
    const request = httpTestingController.expectOne("/rest/zoeken/list");
    request.flush(resultaat);
    await sleep();
    // the table creates the row views in one pass and binds their cells in the next
    fixture.detectChanges();
    fixture.detectChanges();
    return request;
  }

  function sortHeader(name: string) {
    return screen.getByRole("button", { name });
  }

  it("renders a row for each search result", async () => {
    await setup();

    await respondWith(makeResultaat(3));

    expect(screen.getAllByRole("row", { name: /TAAK-FOUND/ })).toHaveLength(3);
  });

  it("renders every row the search returned, so the paginator never slices the page client side", async () => {
    await setup();

    await respondWith(makeResultaat(8));

    expect(screen.getAllByRole("row", { name: /TAAK-FOUND/ })).toHaveLength(8);
  });

  it("reports the total number of results in the paginator instead of the number of loaded rows", async () => {
    await setup();

    await respondWith(makeResultaat(25, 5));

    expect(screen.getByText(/of 25/)).toBeVisible();
  });

  it("renders the columns in display order", async () => {
    await setup();
    await respondWith(makeResultaat(0));

    const headers = screen.getAllByRole("columnheader");

    expect(headers).toHaveLength(5);
    expect(headers[0]).toHaveTextContent("naam");
    expect(headers[1]).toHaveTextContent("creatiedatumTijd");
    expect(headers[2]).toHaveTextContent("zaakIdentificatie");
    expect(headers[3]).toHaveTextContent("zaaktype");
    expect(headers[4].textContent?.trim()).toBe("");
  });

  it("asks for the first page of the newest taken", async () => {
    await setup();

    const request = await respondWith(makeResultaat(0));

    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toMatchObject({
      sorteerVeld: "TAAK_CREATIEDATUM",
      sorteerRichting: "desc",
      rows: 5,
      page: 0,
    });
  });

  it("loads the next page when the user pages forward", async () => {
    await setup();
    await respondWith(makeResultaat(25, 5));

    await user.click(screen.getByRole("button", { name: "Next page" }));
    await sleep();
    const request = await respondWith(makeResultaat(25, 5));

    expect(request.request.body).toMatchObject({ page: 1 });
    expect(screen.getByText(/10 of 25/)).toBeVisible();
  });

  it("sorts by the column the user clicks and returns to the first page", async () => {
    await setup();
    await respondWith(makeResultaat(25, 5));

    await user.click(screen.getByRole("button", { name: "Next page" }));
    await sleep();
    await respondWith(makeResultaat(25, 5));

    await user.click(sortHeader("naam"));
    await sleep();
    const request = await respondWith(makeResultaat(25, 5));

    expect(request.request.body).toMatchObject({
      sorteerVeld: "TAAK_NAAM",
      sorteerRichting: "asc",
      page: 0,
    });
  });
});
