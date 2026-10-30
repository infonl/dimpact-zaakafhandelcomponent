/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { InjectionToken, Provider } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatSortHeaderHarness } from "@angular/material/sort/testing";
import { MatTableHarness } from "@angular/material/table/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import {
  provideTanStackQuery,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { QUERY_CLIENT } from "../../shared/http/query-client";
import { GeneratedType } from "../../shared/utils/generated-types";
import { DashboardCard } from "../model/dashboard-card";
import { DashboardCardId } from "../model/dashboard-card-id";
import { DashboardCardType } from "../model/dashboard-card-type";
import { ZaakWaarschuwingenCardComponent } from "./zaak-waarschuwingen-card.component";

const WAARSCHUWINGEN_URL = "/rest/zaken/waarschuwing";
const NOT_FOUND = { status: 404, statusText: "Not Found" };

const makeZaak = (
  fields: Partial<GeneratedType<"RestZaakOverzicht">> = {},
): GeneratedType<"RestZaakOverzicht"> =>
  fromPartial<GeneratedType<"RestZaakOverzicht">>({
    identificatie: "ZAAK-001",
    uuid: "uuid-1",
    einddatumGepland: null,
    uiterlijkeEinddatumAfdoening: null,
    einddatum: null,
    rechten: fromPartial<GeneratedType<"RestZaakOverzicht">["rechten"]>({
      canLezen: true,
    }),
    ...fields,
  });

const zaakVerlopenEvent = fromPartial<ScreenEvent>({
  objectId: { detail: "ZAAK_VERLOPEND" },
});

describe(ZaakWaarschuwingenCardComponent.name, () => {
  let fixture: ComponentFixture<ZaakWaarschuwingenCardComponent>;
  let component: ZaakWaarschuwingenCardComponent;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let signaleringenChanged: (event: ScreenEvent) => void;

  async function setup({
    queryClient = testQueryClient,
    card = new DashboardCard(
      DashboardCardId.MIJN_ZAKEN_WAARSCHUWING,
      DashboardCardType.ZAAK_WAARSCHUWINGEN,
      "ZAAK_VERLOPEND",
    ),
    providers = [],
  }: {
    queryClient?: QueryClient | InjectionToken<QueryClient>;
    card?: DashboardCard;
    providers?: Provider[];
  } = {}) {
    const websocketService = fromPartial<WebsocketService>({
      addListener: jest.fn((_opcode, _objectType, _objectId, callback) => {
        signaleringenChanged = callback as (event: ScreenEvent) => void;
        return fromPartial<WebsocketListener>({});
      }),
    });

    ({ fixture } = await render(ZaakWaarschuwingenCardComponent, {
      inputs: { data: card },
      imports: [NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTanStackQuery(queryClient),
        { provide: WebsocketService, useValue: websocketService },
        ...providers,
      ],
    }));

    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    httpTestingController
      .expectOne("/rest/identity/loggedInUser")
      .flush(
        fromPartial<GeneratedType<"RestLoggedInUser">>({ username: "user" }),
      );
    await sleep();
  }

  /**
   * The card reads on mount and reads again when it loads, so the first read
   * can have two requests open.
   */
  async function respondWith(
    response: GeneratedType<"RestZaakOverzicht">[] | typeof NOT_FOUND,
  ) {
    const requests = httpTestingController.match(WAARSCHUWINGEN_URL);
    expect(requests.length).toBeGreaterThan(0);
    requests.forEach((request) =>
      Array.isArray(response)
        ? request.flush(response)
        : request.flush(null, response),
    );
    await sleep();
    fixture.detectChanges();
    await sleep();
    // the table creates the row views in one pass and binds their cells in the next
    fixture.detectChanges();
    fixture.detectChanges();
  }

  async function readAgain() {
    signaleringenChanged(zaakVerlopenEvent);
    await sleep();
  }

  async function identificatiesInRowOrder() {
    const table = await loader.getHarness(MatTableHarness);
    return Promise.all(
      (await table.getRows()).map(
        async (row) => (await row.getCellTextByColumnName()).identificatie,
      ),
    );
  }

  it("reads the zaak waarschuwingen through a GET of the waarschuwing endpoint", async () => {
    await setup();

    const requests = httpTestingController.match(WAARSCHUWINGEN_URL);

    expect(requests.map(({ request }) => request.method)).toContain("GET");
    requests.forEach((request) => request.flush([]));
  });

  it("shows the zaken it read", async () => {
    await setup();

    await respondWith([
      makeZaak({ identificatie: "ZAAK-A" }),
      makeZaak({ identificatie: "ZAAK-B" }),
    ]);

    expect(screen.getByRole("row", { name: /ZAAK-A/ })).toBeVisible();
    expect(screen.getByRole("row", { name: /ZAAK-B/ })).toBeVisible();
  });

  it("reads the zaak waarschuwingen again when one of the user's zaken expires", async () => {
    await setup();
    await respondWith([makeZaak({ identificatie: "ZAAK-A" })]);

    await readAgain();
    await respondWith([makeZaak({ identificatie: "ZAAK-B" })]);

    expect(screen.getByRole("row", { name: /ZAAK-B/ })).toBeVisible();
  });

  it("does not read the zaak waarschuwingen again for a signalering of another type", async () => {
    await setup();
    await respondWith([]);

    signaleringenChanged(
      fromPartial<ScreenEvent>({ objectId: { detail: "TAAK_OP_NAAM" } }),
    );
    await sleep();

    httpTestingController.expectNone(WAARSCHUWINGEN_URL);
  });

  it("wires up sort and paginator on the dataSource after view init", async () => {
    await setup();
    await respondWith([]);

    expect(component.dataSource.sort).toBe(component.sort());
    expect(component.dataSource.paginator).toBe(component.paginator());
  });

  it("reorders rows ascending then descending when the identificatie sort header is clicked", async () => {
    await setup();
    await respondWith([
      makeZaak({ identificatie: "ZAAK-C" }),
      makeZaak({ identificatie: "ZAAK-A" }),
      makeZaak({ identificatie: "ZAAK-B" }),
    ]);
    const sortHeader = await loader.getHarness(
      MatSortHeaderHarness.with({ label: "zaak.identificatie" }),
    );

    await sortHeader.click();
    expect(await identificatiesInRowOrder()).toEqual([
      "ZAAK-A",
      "ZAAK-B",
      "ZAAK-C",
    ]);

    await sortHeader.click();
    expect(await identificatiesInRowOrder()).toEqual([
      "ZAAK-C",
      "ZAAK-B",
      "ZAAK-A",
    ]);
  });

  it("exposes the expected column definitions", async () => {
    await setup();
    await respondWith([]);

    expect(component.columns).toEqual([
      "identificatie",
      "streefdatum",
      "dagenTotStreefdatum",
      "fataledatum",
      "dagenTotFataledatum",
      "url",
    ]);
  });

  it("renders the empty state when there are no zaak waarschuwingen", async () => {
    await setup();

    await respondWith([]);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("renders a view button when zaak.rechten.lezen is true", async () => {
    await setup();
    await respondWith([makeZaak()]);

    const buttons = await loader.getAllHarnesses(
      MatButtonHarness.with({ selector: '[id^="zaakBekijken_"]' }),
    );
    expect(buttons.length).toBe(1);
  });

  it("does not render a view button when zaak.rechten.lezen is false", async () => {
    await setup();
    await respondWith([
      makeZaak({
        rechten: fromPartial<GeneratedType<"RestZaakOverzicht">["rechten"]>({
          canLezen: false,
        }),
      }),
    ]);

    const buttons = await loader.getAllHarnesses(
      MatButtonHarness.with({ selector: '[id^="zaakBekijken_"]' }),
    );
    expect(buttons.length).toBe(0);
  });

  describe("when the zaak waarschuwingen are read again in the background", () => {
    it("shows the refetch hint and keeps the zaken when the read fails", async () => {
      await setup();
      await respondWith([makeZaak({ identificatie: "ZAAK-OUD" })]);

      await readAgain();
      await respondWith(NOT_FOUND);

      expect(screen.getByRole("status")).toHaveTextContent(
        "msg.verversen.mislukt",
      );
      expect(screen.getByRole("row", { name: /ZAAK-OUD/ })).toBeVisible();
    });

    it("hides the refetch hint again after a read that succeeds", async () => {
      await setup();
      await respondWith([makeZaak({ identificatie: "ZAAK-OUD" })]);
      await readAgain();
      await respondWith(NOT_FOUND);

      await readAgain();
      await respondWith([makeZaak({ identificatie: "ZAAK-NIEUW" })]);

      expect(
        screen.queryByText("msg.verversen.mislukt"),
      ).not.toBeInTheDocument();
      expect(screen.getByRole("row", { name: /ZAAK-NIEUW/ })).toBeVisible();
    });

    it("does not show the refetch hint when the first read fails, because the error dialog reports that", async () => {
      await setup();

      await respondWith(NOT_FOUND);

      expect(
        screen.queryByText("msg.verversen.mislukt"),
      ).not.toBeInTheDocument();
    });
  });

  describe("with the error handling of the app", () => {
    const foutAfhandelen = jest.fn().mockReturnValue(of());

    const setupWithAppQueryClient = () =>
      setup({
        queryClient: QUERY_CLIENT,
        providers: [
          {
            provide: FoutAfhandelingService,
            useValue: fromPartial<FoutAfhandelingService>({ foutAfhandelen }),
          },
        ],
      });

    afterEach(() => {
      TestBed.inject(QueryClient).clear();
    });

    it("opens the error dialog once when the first read fails", async () => {
      await setupWithAppQueryClient();

      await respondWith(NOT_FOUND);

      expect(foutAfhandelen).toHaveBeenCalledTimes(1);
      httpTestingController.expectNone(WAARSCHUWINGEN_URL);
    });

    it("shows the refetch hint instead of the error dialog when a background read fails", async () => {
      await setupWithAppQueryClient();
      await respondWith([makeZaak({ identificatie: "ZAAK-OUD" })]);

      await readAgain();
      await respondWith(NOT_FOUND);

      expect(foutAfhandelen).not.toHaveBeenCalled();
      expect(screen.getByRole("status")).toHaveTextContent(
        "msg.verversen.mislukt",
      );
    });
  });

  describe("isAfterDate", () => {
    it("returns false when datum is null", async () => {
      await setup();
      await respondWith([]);

      expect(component.isAfterDate(null, null)).toBe(false);
    });

    it("returns true when datum is before the actual date", async () => {
      await setup();
      await respondWith([]);

      expect(component.isAfterDate("2020-01-01", "2024-01-01")).toBe(true);
    });

    it("returns false when datum is after the actual date", async () => {
      await setup();
      await respondWith([]);

      expect(component.isAfterDate("2030-01-01", "2024-01-01")).toBe(false);
    });
  });
});
