/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient, withXhr } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatSortHeaderHarness } from "@angular/material/sort/testing";
import { MatTableHarness } from "@angular/material/table/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import {
  notifyManager,
  provideTanStackQuery,
} from "@tanstack/angular-query-experimental";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { DashboardCard } from "../model/dashboard-card";
import { DashboardCardId } from "../model/dashboard-card-id";
import { DashboardCardType } from "../model/dashboard-card-type";
import { ZaakWaarschuwingenCardComponent } from "./zaak-waarschuwingen-card.component";

const makeZaak = (
  fields: Partial<GeneratedType<"RestZaakOverzicht">> = {},
): GeneratedType<"RestZaakOverzicht"> =>
  ({
    identificatie: "ZAAK-001",
    uuid: "uuid-1",
    einddatumGepland: null,
    uiterlijkeEinddatumAfdoening: null,
    einddatum: null,
    rechten: { canLezen: true },
    ...fields,
  }) as Partial<
    GeneratedType<"RestZaakOverzicht">
  > as unknown as GeneratedType<"RestZaakOverzicht">;

const makeDashboardCard = (): DashboardCard =>
  new DashboardCard(
    DashboardCardId.MIJN_ZAKEN_WAARSCHUWING,
    DashboardCardType.ZAAK_WAARSCHUWINGEN,
  );

describe(ZaakWaarschuwingenCardComponent.name, () => {
  let fixture: ComponentFixture<ZaakWaarschuwingenCardComponent>;
  let component: ZaakWaarschuwingenCardComponent;
  let loader: HarnessLoader;

  beforeEach(async () => {
    notifyManager.setScheduler((callback) => callback());
    await TestBed.configureTestingModule({
      imports: [ZaakWaarschuwingenCardComponent, NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTanStackQuery(testQueryClient),
        // useValue: real WebsocketService opens a websocket connection in its
        // constructor; the cards only need addListener as a no-op in tests.
        { provide: WebsocketService, useValue: { addListener: jest.fn() } },
      ],
    }).compileComponents();

    const identityService = TestBed.inject(IdentityService);
    testQueryClient.setQueryData(
      identityService.readLoggedInUser().queryKey,
      fromPartial<GeneratedType<"RestUser">>({
        username: "user",
        naam: "Test",
      }),
    );

    fixture = TestBed.createComponent(ZaakWaarschuwingenCardComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.componentRef.setInput("data", makeDashboardCard());
    fixture.detectChanges();
  });

  async function answerWaarschuwingen(
    zaken: GeneratedType<"RestZaakOverzicht">[],
  ) {
    await sleep();
    TestBed.inject(HttpTestingController)
      .match("/rest/zaken/waarschuwing")
      .forEach((request) => request.flush(zaken));
    await sleep();
    TestBed.tick();
    fixture.detectChanges();
  }

  afterEach(async () => {
    await answerWaarschuwingen([]);
    notifyManager.setScheduler((callback) => setTimeout(callback, 0));
  });

  it("reads the zaak waarschuwingen again on load, so that a cached list is refreshed", async () => {
    await answerWaarschuwingen([]);

    component["onLoad"]();
    await sleep();

    TestBed.inject(HttpTestingController).expectOne("/rest/zaken/waarschuwing");
  });

  it("populates dataSource with zaken returned by the service", async () => {
    const zaken = [
      makeZaak({ identificatie: "ZAAK-A" }),
      makeZaak({ identificatie: "ZAAK-B" }),
    ];

    await answerWaarschuwingen(zaken);

    expect(component.dataSource.data).toEqual(zaken);
  });

  it("shows the cached zaken at once on a second mount, so that the dashboard does not wait for the server", async () => {
    const zaken = [makeZaak({ identificatie: "ZAAK-CACHED" })];
    await answerWaarschuwingen(zaken);

    const secondFixture = TestBed.createComponent(
      ZaakWaarschuwingenCardComponent,
    );
    secondFixture.componentRef.setInput("data", makeDashboardCard());
    secondFixture.detectChanges();

    expect(secondFixture.componentInstance.dataSource.data).toEqual(zaken);
  });

  it("wires up sort and paginator on the dataSource after view init", () => {
    expect(component.dataSource.sort).toBe(component.sort());
    expect(component.dataSource.paginator).toBe(component.paginator());
  });

  it("reorders rows ascending then descending when the identificatie sort header is clicked", async () => {
    const zaken = [
      makeZaak({ identificatie: "ZAAK-C" }),
      makeZaak({ identificatie: "ZAAK-A" }),
      makeZaak({ identificatie: "ZAAK-B" }),
    ];
    await answerWaarschuwingen(zaken);

    const sortHeader = await loader.getHarness(
      MatSortHeaderHarness.with({ label: "zaak.identificatie" }),
    );
    const table = await loader.getHarness(MatTableHarness);

    await sortHeader.click();
    const ascending = await Promise.all(
      (await table.getRows()).map(
        async (row) => (await row.getCellTextByColumnName()).identificatie,
      ),
    );
    expect(ascending).toEqual(["ZAAK-A", "ZAAK-B", "ZAAK-C"]);

    await sortHeader.click();
    const descending = await Promise.all(
      (await table.getRows()).map(
        async (row) => (await row.getCellTextByColumnName()).identificatie,
      ),
    );
    expect(descending).toEqual(["ZAAK-C", "ZAAK-B", "ZAAK-A"]);
  });

  it("exposes the expected column definitions", () => {
    expect(component.columns).toEqual([
      "identificatie",
      "streefdatum",
      "dagenTotStreefdatum",
      "fataledatum",
      "dagenTotFataledatum",
      "url",
    ]);
  });

  it("renders a table row for each zaak in dataSource", async () => {
    const zaken = [
      makeZaak({ identificatie: "ZAAK-X" }),
      makeZaak({ identificatie: "ZAAK-Y" }),
    ];
    await answerWaarschuwingen(zaken);

    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows();
    expect(rows.length).toBe(2);
  });

  it("renders empty state row when dataSource is empty", async () => {
    await answerWaarschuwingen([]);

    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows();
    expect(rows.length).toBe(0);
  });

  it("renders a view button when zaak.rechten.lezen is true", async () => {
    const zaken = [
      makeZaak({
        rechten: fromPartial<GeneratedType<"RestZaakOverzicht">["rechten"]>({
          canLezen: true,
        }),
      }),
    ];
    await answerWaarschuwingen(zaken);

    const buttons = await loader.getAllHarnesses(
      MatButtonHarness.with({ selector: '[id^="zaakBekijken_"]' }),
    );
    expect(buttons.length).toBe(1);
  });

  it("does not render a view button when zaak.rechten.lezen is false", async () => {
    const zaken = [
      makeZaak({
        rechten: fromPartial<GeneratedType<"RestZaakOverzicht">["rechten"]>({
          canLezen: false,
        }),
      }),
    ];
    await answerWaarschuwingen(zaken);

    const buttons = await loader.getAllHarnesses(
      MatButtonHarness.with({ selector: '[id^="zaakBekijken_"]' }),
    );
    expect(buttons.length).toBe(0);
  });

  describe("isAfterDate", () => {
    it("returns false when datum is null", () => {
      expect(component.isAfterDate(null, null)).toBe(false);
    });

    it("returns true when datum is before the actual date", () => {
      expect(component.isAfterDate("2020-01-01", "2024-01-01")).toBe(true);
    });

    it("returns false when datum is after the actual date", () => {
      expect(component.isAfterDate("2030-01-01", "2024-01-01")).toBe(false);
    });
  });
});
