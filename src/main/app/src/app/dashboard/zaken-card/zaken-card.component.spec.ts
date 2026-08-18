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

import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { DashboardCard } from "../model/dashboard-card";
import { DashboardCardId } from "../model/dashboard-card-id";
import { DashboardCardType } from "../model/dashboard-card-type";
import { ZakenCardComponent } from "./zaken-card.component";

const LOGGED_IN_USER_QUERY_KEY = ["/rest/identity/loggedInUser"];

const makeResultaat = (totaal: number, count = totaal, prefix = "ZAAK-FOUND") =>
  fromPartial<{
    resultaten: GeneratedType<"RestZaakOverzicht">[];
    totaal: number;
  }>({
    resultaten: Array.from({ length: count }, (_, index) =>
      fromPartial<GeneratedType<"RestZaakOverzicht">>({
        identificatie: `${prefix}-${index}`,
      }),
    ),
    totaal,
  });

const cardData = new DashboardCard(
  DashboardCardId.MIJN_ZAKEN_NIEUW,
  DashboardCardType.ZAKEN,
  "ZAAK_OP_NAAM",
);

describe(ZakenCardComponent.name, () => {
  let fixture: ComponentFixture<ZakenCardComponent>;
  let httpTestingController: HttpTestingController;

  const user = userEvent.setup();

  beforeEach(() => {
    notifyManager.setScheduler((fn) => fn());
  });

  afterEach(() => {
    notifyManager.setScheduler((fn) => Promise.resolve().then(fn));
  });

  async function setup() {
    testQueryClient.setQueryData(
      LOGGED_IN_USER_QUERY_KEY,
      fromPartial<GeneratedType<"RestUser">>({
        id: "fakeUserId",
        naam: "fakeUserName",
      }),
    );

    const rendered = await render(ZakenCardComponent, {
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
    const request = httpTestingController.expectOne(
      "/rest/signaleringen/zaken/ZAAK_OP_NAAM",
    );
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

  it("renders a row for each zaak in the signalering", async () => {
    await setup();

    await respondWith(makeResultaat(4));

    expect(screen.getAllByRole("row", { name: /ZAAK-FOUND/ })).toHaveLength(4);
  });

  it("renders every row the signalering returned, so the paginator never slices the page client side", async () => {
    await setup();

    await respondWith(makeResultaat(8));

    expect(screen.getAllByRole("row", { name: /ZAAK-FOUND/ })).toHaveLength(8);
  });

  it("reports the total number of zaken in the paginator instead of the number of loaded rows", async () => {
    await setup();

    await respondWith(makeResultaat(25, 5));

    expect(screen.getByText(/of 25/)).toBeVisible();
  });

  it("renders the columns in display order", async () => {
    await setup();
    await respondWith(makeResultaat(0));

    const headers = screen.getAllByRole("columnheader");

    expect(headers).toHaveLength(5);
    expect(headers[0]).toHaveTextContent("zaak.identificatie");
    expect(headers[1]).toHaveTextContent("startdatum");
    expect(headers[2]).toHaveTextContent("zaaktype");
    expect(headers[3]).toHaveTextContent("omschrijving");
    expect(headers[4].textContent?.trim()).toBe("");
  });

  it("asks for the first page of the most recent signaleringen", async () => {
    await setup();

    const request = await respondWith(makeResultaat(0));

    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual({
      page: 0,
      rows: 5,
      sortField: "SIGNALERING_TIJDSTIP",
      sortOrder: "DESC",
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

    await user.click(sortHeader("startdatum"));
    await sleep();
    const request = await respondWith(makeResultaat(25, 5));

    expect(request.request.body).toMatchObject({
      sortField: "ZAAK_STARTDATUM",
      sortOrder: "ASC",
      page: 0,
    });
  });

  it("shows the signaleringen in their default order again when the user clears the sort", async () => {
    await setup();
    await respondWith(makeResultaat(1, 1, "ZAAK-DEFAULT"));

    await user.click(sortHeader("zaak.identificatie"));
    await sleep();
    await respondWith(makeResultaat(1, 1, "ZAAK-ASCENDING"));
    expect(screen.getByRole("row", { name: /ZAAK-ASCENDING/ })).toBeVisible();

    await user.click(sortHeader("zaak.identificatie"));
    await sleep();
    await respondWith(makeResultaat(1, 1, "ZAAK-DESCENDING"));
    expect(screen.getByRole("row", { name: /ZAAK-DESCENDING/ })).toBeVisible();

    await user.click(sortHeader("zaak.identificatie"));
    await sleep();
    fixture.detectChanges();
    fixture.detectChanges();

    httpTestingController.expectNone(() => true);
    expect(screen.getByRole("row", { name: /ZAAK-DEFAULT/ })).toBeVisible();
  });
});
