/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatSortHeaderHarness } from "@angular/material/sort/testing";
import { MatTableHarness } from "@angular/material/table/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { SharedModule } from "../../shared/shared.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
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
    rechten: { lezen: true },
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
  let zakenService: ZakenService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ZaakWaarschuwingenCardComponent],
      imports: [SharedModule, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideTanStackQuery(testQueryClient),
        // useValue: real WebsocketService opens a websocket connection in its
        // constructor; the cards only need addListener as a no-op in tests.
        { provide: WebsocketService, useValue: { addListener: jest.fn() } },
      ],
    }).compileComponents();

    zakenService = TestBed.inject(ZakenService);
    jest.spyOn(zakenService, "listZaakWaarschuwingen").mockReturnValue(of([]));

    const identityService = TestBed.inject(IdentityService);
    jest.spyOn(identityService, "readLoggedInUser").mockReturnValue(
      fromPartial<ReturnType<IdentityService["readLoggedInUser"]>>({
        queryKey: ["user"],
        queryFn: async () => fromPartial<GeneratedType<"RestUser">>({}),
      }),
    );

    fixture = TestBed.createComponent(ZaakWaarschuwingenCardComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    component.data = makeDashboardCard();
    fixture.detectChanges();
    // Stop the timed reload interval so harnesses (which await whenStable)
    // don't hang on a never-completing observable.
    component["reloader"]?.unsubscribe();
  });

  it("calls listZaakWaarschuwingen on load", () => {
    expect(zakenService.listZaakWaarschuwingen).toHaveBeenCalled();
  });

  it("populates dataSource with zaken returned by the service", () => {
    const zaken = [
      makeZaak({ identificatie: "ZAAK-A" }),
      makeZaak({ identificatie: "ZAAK-B" }),
    ];
    jest
      .spyOn(zakenService, "listZaakWaarschuwingen")
      .mockReturnValue(of(zaken));

    component["onLoad"]();

    expect(component.dataSource.data).toEqual(zaken);
  });

  it("wires up sort and paginator on the dataSource after view init", () => {
    expect(component.dataSource.sort).toBe(component.sort);
    expect(component.dataSource.paginator).toBe(component.paginator);
  });

  it("reorders rows ascending then descending when the identificatie sort header is clicked", async () => {
    component.dataSource.data = [
      makeZaak({ identificatie: "ZAAK-C" }),
      makeZaak({ identificatie: "ZAAK-A" }),
      makeZaak({ identificatie: "ZAAK-B" }),
    ];
    fixture.detectChanges();

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
    component.dataSource.data = [
      makeZaak({ identificatie: "ZAAK-X" }),
      makeZaak({ identificatie: "ZAAK-Y" }),
    ];
    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows();
    expect(rows.length).toBe(2);
  });

  it("renders empty state row when dataSource is empty", async () => {
    component.dataSource.data = [];
    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows();
    expect(rows.length).toBe(0);
  });

  it("renders a view button when zaak.rechten.lezen is true", async () => {
    component.dataSource.data = [
      makeZaak({
        rechten: fromPartial<GeneratedType<"RestZaakOverzicht">["rechten"]>({
          lezen: true,
        }),
      }),
    ];
    fixture.detectChanges();

    const buttons = await loader.getAllHarnesses(
      MatButtonHarness.with({ selector: '[id^="zaakBekijken_"]' }),
    );
    expect(buttons.length).toBe(1);
  });

  it("does not render a view button when zaak.rechten.lezen is false", async () => {
    component.dataSource.data = [
      makeZaak({
        rechten: fromPartial<GeneratedType<"RestZaakOverzicht">["rechten"]>({
          lezen: false,
        }),
      }),
    ];
    fixture.detectChanges();

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
