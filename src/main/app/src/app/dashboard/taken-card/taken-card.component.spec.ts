/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatTableHarness } from "@angular/material/table/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { testQueryClient } from "../../../../setupJest";
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
        {
          provide: WebsocketService,
          useValue: { addListener: jest.fn() },
        },
      ],
    }).compileComponents();

    signaleringenService = TestBed.inject(SignaleringenService);
    jest
      .spyOn(signaleringenService, "listTakenSignalering")
      .mockReturnValue(of([]));

    const identityService = TestBed.inject(IdentityService);
    jest.spyOn(identityService, "readLoggedInUser").mockReturnValue({
      queryKey: ["user"],
      queryFn: async () => null,
    } as never);

    fixture = TestBed.createComponent(TakenCardComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    component.data = makeDashboardCard();
    fixture.detectChanges();
  });

  it("calls listTakenSignalering with the card's signaleringType on load", () => {
    expect(signaleringenService.listTakenSignalering).toHaveBeenCalledWith(
      component.data.signaleringType,
    );
  });

  it("populates dataSource with tasks returned by the service", () => {
    const taken = [makeTaak({ naam: "Taak A" }), makeTaak({ naam: "Taak B" })];
    jest
      .spyOn(signaleringenService, "listTakenSignalering")
      .mockReturnValue(of(taken));

    component["onLoad"](() => {});

    expect(component.dataSource.data).toEqual(taken);
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
    component.dataSource.data = taken;
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
});
