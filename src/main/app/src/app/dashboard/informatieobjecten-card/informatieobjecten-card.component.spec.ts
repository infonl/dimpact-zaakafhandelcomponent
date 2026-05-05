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
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { SharedModule } from "../../shared/shared.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenService } from "../../signaleringen.service";
import { DashboardCard } from "../model/dashboard-card";
import { DashboardCardId } from "../model/dashboard-card-id";
import { DashboardCardType } from "../model/dashboard-card-type";
import { InformatieobjectenCardComponent } from "./informatieobjecten-card.component";

const makeInformatieobject = (
  fields: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">> = {},
) =>
  fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
    titel: "Test document",
    auteur: "Test auteur",
    ...fields,
  });

const makeDashboardCard = (signaleringType?: GeneratedType<"Type">) =>
  new DashboardCard(
    DashboardCardId.MIJN_DOCUMENTEN_NIEUW,
    DashboardCardType.ZAKEN,
    signaleringType,
  );

describe(InformatieobjectenCardComponent.name, () => {
  let fixture: ComponentFixture<InformatieobjectenCardComponent>;
  let component: InformatieobjectenCardComponent;
  let loader: HarnessLoader;
  let signaleringenService: SignaleringenService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [InformatieobjectenCardComponent],
      imports: [SharedModule, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideTanStackQuery(testQueryClient),
        { provide: WebsocketService, useValue: { addListener: jest.fn() } },
      ],
    }).compileComponents();

    signaleringenService = TestBed.inject(SignaleringenService);
    jest
      .spyOn(signaleringenService, "listInformatieobjectenSignalering")
      .mockReturnValue(of([]));

    const identityService = TestBed.inject(IdentityService);
    jest.spyOn(identityService, "readLoggedInUser").mockReturnValue(
      fromPartial<ReturnType<IdentityService["readLoggedInUser"]>>({
        queryKey: ["user"],
        queryFn: async () => fromPartial<GeneratedType<"RestUser">>({}),
      }),
    );

    fixture = TestBed.createComponent(InformatieobjectenCardComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    component.data = makeDashboardCard("ZAAK_DOCUMENT_TOEGEVOEGD");
    fixture.detectChanges();
    component["reloader"]?.unsubscribe();
  });

  it("calls listInformatieobjectenSignalering with the card's signaleringType on load", () => {
    expect(
      signaleringenService.listInformatieobjectenSignalering,
    ).toHaveBeenCalledWith("ZAAK_DOCUMENT_TOEGEVOEGD");
  });

  it("populates dataSource with informatieobjecten returned by the service", () => {
    const docs = [
      makeInformatieobject({ titel: "Doc A" }),
      makeInformatieobject({ titel: "Doc B" }),
    ];
    jest
      .spyOn(signaleringenService, "listInformatieobjectenSignalering")
      .mockReturnValue(of(docs));

    component["onLoad"](() => {});

    expect(component.dataSource.data).toEqual(docs);
  });

  it("coalesces a null service response to an empty array", () => {
    jest
      .spyOn(signaleringenService, "listInformatieobjectenSignalering")
      .mockReturnValue(of(null as never));

    component["onLoad"](() => {});

    expect(component.dataSource.data).toEqual([]);
  });

  it("skips the service call and clears dataSource when signaleringType is missing", () => {
    const spy = jest.spyOn(
      signaleringenService,
      "listInformatieobjectenSignalering",
    );
    spy.mockClear();
    component.data = makeDashboardCard(undefined);
    component.dataSource.data = [makeInformatieobject()];

    const afterLoad = jest.fn();
    component["onLoad"](afterLoad);

    expect(spy).not.toHaveBeenCalled();
    expect(component.dataSource.data).toEqual([]);
    expect(afterLoad).toHaveBeenCalled();
  });

  it("exposes the expected column definitions", () => {
    expect(component.columns).toEqual([
      "titel",
      "registratiedatumTijd",
      "informatieobjectType",
      "auteur",
      "url",
    ]);
  });

  it("renders a table row for each informatieobject in dataSource", async () => {
    component.dataSource.data = [
      makeInformatieobject({ titel: "X" }),
      makeInformatieobject({ titel: "Y" }),
      makeInformatieobject({ titel: "Z" }),
    ];
    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness);
    expect((await table.getRows()).length).toBe(3);
  });

  it("renders empty state row when dataSource is empty", async () => {
    component.dataSource.data = [];
    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness);
    expect((await table.getRows()).length).toBe(0);
  });
});
