/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatSortHeaderHarness } from "@angular/material/sort/testing";
import { MatTableHarness } from "@angular/material/table/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { of, Subject } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenService } from "../../signaleringen.service";
import { DashboardCard } from "../model/dashboard-card";
import { DashboardCardId } from "../model/dashboard-card-id";
import { DashboardCardType } from "../model/dashboard-card-type";
import { TakenCardComponent } from "./taken-card.component";

const makeTaak = (
  fields: Partial<GeneratedType<"RestSignaleringTaskSummary">> = {},
): GeneratedType<"RestSignaleringTaskSummary"> =>
  ({
    naam: "Test taak",
    zaakIdentificatie: "ZAAK-001",
    zaaktypeOmschrijving: "Testtype",
    ...fields,
  }) as Partial<
    GeneratedType<"RestSignaleringTaskSummary">
  > as unknown as GeneratedType<"RestSignaleringTaskSummary">;

const makeDashboardCard = (): DashboardCard =>
  new DashboardCard(
    DashboardCardId.MIJN_TAKEN,
    DashboardCardType.TAKEN,
    "TAAK_OP_NAAM" as GeneratedType<"Type">,
  );

describe(TakenCardComponent.name, () => {
  let fixture: ComponentFixture<TakenCardComponent>;
  let component: TakenCardComponent;
  let loader: HarnessLoader;
  let signaleringenService: SignaleringenService;

  const defaultParameters = {
    signaleringType: "TAAK_OP_NAAM" as GeneratedType<"Type">,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        TranslateModule.forRoot(),
        TakenCardComponent,
      ],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideTanStackQuery(testQueryClient),
        { provide: WebsocketService, useValue: { addListener: jest.fn() } },
      ],
    }).compileComponents();

    signaleringenService = TestBed.inject(SignaleringenService);
    jest
      .spyOn(signaleringenService, "listTakenSignalering")
      .mockReturnValue(of([]));

    const identityService = TestBed.inject(IdentityService);
    testQueryClient.setQueryData(
      identityService.readLoggedInUser().queryKey,
      fromPartial<GeneratedType<"RestUser">>({ id: "user", naam: "Test" }),
    );

    fixture = TestBed.createComponent(TakenCardComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    component.data = makeDashboardCard();
    fixture.detectChanges();
  });

  it("calls listTakenSignalering with the card's signaleringType on load", async () => {
    component["onLoad"]();
    await sleep();
    expect(signaleringenService.listTakenSignalering).toHaveBeenCalledWith(
      component.data.signaleringType,
    );
  });

  it("populates dataSource with tasks returned by the service", async () => {
    const taken = [makeTaak({ naam: "Taak A" }), makeTaak({ naam: "Taak B" })];
    testQueryClient.setQueryData(
      ["taken signaleringen dashboard", defaultParameters],
      taken,
    );
    jest
      .spyOn(signaleringenService, "listTakenSignalering")
      .mockReturnValue(of(taken));

    component["onLoad"]();
    await sleep();
    fixture.detectChanges();

    expect(component.dataSource.data).toEqual(taken);
  });

  it("skips the service call and clears dataSource when signaleringType is missing", async () => {
    const spy = jest.spyOn(signaleringenService, "listTakenSignalering");
    spy.mockClear();
    component.data = new DashboardCard(
      DashboardCardId.MIJN_TAKEN,
      DashboardCardType.TAKEN,
    );
    testQueryClient.removeQueries({
      queryKey: ["taken signaleringen dashboard"],
    });
    fixture.detectChanges();

    component["onLoad"]();
    await sleep();
    fixture.detectChanges();

    expect(spy).not.toHaveBeenCalled();
    expect(component.dataSource.data).toEqual([]);
  });

  it("wires up sort and paginator on the dataSource after view init", () => {
    expect(component.dataSource.sort).toBe(component.sort);
    expect(component.dataSource.paginator).toBe(component.paginator);
  });

  it("re-runs onLoad when the reload observable emits", async () => {
    const spy = jest.spyOn(signaleringenService, "listTakenSignalering");
    spy.mockClear();
    (component["reload"] as Subject<void>).next();
    await sleep();
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it("reorders rows ascending then descending when the naam sort header is clicked", async () => {
    const taken = [
      makeTaak({ naam: "Charlie" }),
      makeTaak({ naam: "Alpha" }),
      makeTaak({ naam: "Bravo" }),
    ];
    testQueryClient.setQueryData(
      ["taken signaleringen dashboard", defaultParameters],
      taken,
    );
    fixture.detectChanges();
    await sleep();
    fixture.detectChanges();

    const sortHeader = await loader.getHarness(
      MatSortHeaderHarness.with({ label: "naam" }),
    );
    const table = await loader.getHarness(MatTableHarness);

    await sortHeader.click();
    const ascending = await Promise.all(
      (await table.getRows()).map(
        async (row) => (await row.getCellTextByColumnName()).naam,
      ),
    );
    expect(ascending).toEqual(["Alpha", "Bravo", "Charlie"]);

    await sortHeader.click();
    const descending = await Promise.all(
      (await table.getRows()).map(
        async (row) => (await row.getCellTextByColumnName()).naam,
      ),
    );
    expect(descending).toEqual(["Charlie", "Bravo", "Alpha"]);
  });

  it("exposes the expected column definitions", () => {
    expect(component.columns).toEqual([
      "naam",
      "creatiedatumTijd",
      "zaakIdentificatie",
      "zaaktypeOmschrijving",
      "url",
    ]);
  });

  it("renders a table row for each task in dataSource", async () => {
    const taken = [makeTaak({ naam: "Taak X" }), makeTaak({ naam: "Taak Y" })];
    testQueryClient.setQueryData(
      ["taken signaleringen dashboard", defaultParameters],
      taken,
    );
    fixture.detectChanges();
    await sleep();
    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows();
    expect(rows.length).toBe(2);
  });

  it("renders empty state row when dataSource is empty", async () => {
    testQueryClient.setQueryData(
      ["taken signaleringen dashboard", defaultParameters],
      [],
    );
    fixture.detectChanges();
    await sleep();
    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows();
    expect(rows.length).toBe(0);
  });
});
