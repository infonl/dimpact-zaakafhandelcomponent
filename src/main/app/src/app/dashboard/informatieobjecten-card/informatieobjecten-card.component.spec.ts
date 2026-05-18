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

const buildInformatieobject = (
  fields: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">> = {},
) =>
  fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
    titel: "Test document",
    auteur: "Test auteur",
    ...fields,
  });

const buildDashboardCard = (signaleringType?: GeneratedType<"Type">) =>
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
    component.data = buildDashboardCard("ZAAK_DOCUMENT_TOEGEVOEGD");
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
      buildInformatieobject({ titel: "Doc A" }),
      buildInformatieobject({ titel: "Doc B" }),
    ];
    jest
      .spyOn(signaleringenService, "listInformatieobjectenSignalering")
      .mockReturnValue(of(docs));

    component["onLoad"]();

    expect(component.dataSource.data).toEqual(docs);
  });

  it("coalesces a null service response to an empty array", () => {
    jest
      .spyOn(signaleringenService, "listInformatieobjectenSignalering")
      .mockReturnValue(of(null as never));

    component["onLoad"]();

    expect(component.dataSource.data).toEqual([]);
  });

  it("skips the service call and clears dataSource when signaleringType is missing", () => {
    const spy = jest.spyOn(
      signaleringenService,
      "listInformatieobjectenSignalering",
    );
    spy.mockClear();
    component.data = buildDashboardCard(undefined);
    component.dataSource.data = [buildInformatieobject()];

    component["onLoad"]();

    expect(spy).not.toHaveBeenCalled();
    expect(component.dataSource.data).toEqual([]);
  });

  it("wires up sort and paginator on the dataSource after view init", () => {
    expect(component.dataSource.sort).toBe(component.sort);
    expect(component.dataSource.paginator).toBe(component.paginator);
  });

  it("reorders rows ascending then descending when the titel sort header is clicked", async () => {
    component.dataSource.data = [
      buildInformatieobject({ titel: "Charlie" }),
      buildInformatieobject({ titel: "Alpha" }),
      buildInformatieobject({ titel: "Bravo" }),
    ];
    fixture.detectChanges();

    const sortHeader = await loader.getHarness(
      MatSortHeaderHarness.with({ label: "documenttitel" }),
    );
    const table = await loader.getHarness(MatTableHarness);

    await sortHeader.click();
    const ascending = await Promise.all(
      (await table.getRows()).map(
        async (row) => (await row.getCellTextByColumnName()).titel,
      ),
    );
    expect(ascending).toEqual(["Alpha", "Bravo", "Charlie"]);

    await sortHeader.click();
    const descending = await Promise.all(
      (await table.getRows()).map(
        async (row) => (await row.getCellTextByColumnName()).titel,
      ),
    );
    expect(descending).toEqual(["Charlie", "Bravo", "Alpha"]);
  });

  it("exposes the expected column definitions", () => {
    expect(component.columns).toEqual([
      "titel",
      "registratiedatumTijd",
      "informatieobjectTypeOmschrijving",
      "auteur",
      "url",
    ]);
  });

  it("renders a table row for each informatieobject in dataSource", async () => {
    component.dataSource.data = [
      buildInformatieobject({ titel: "X" }),
      buildInformatieobject({ titel: "Y" }),
      buildInformatieobject({ titel: "Z" }),
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

  it("exposes mat-sort-header on every data column so client-side sorting stays clickable", async () => {
    const headers = await loader.getAllHarnesses(MatSortHeaderHarness);
    const labels = await Promise.all(
      headers.map((header) => header.getLabel()),
    );

    expect(labels).toEqual([
      "documenttitel",
      "registratiedatumTijd",
      "informatieobjectTypeOmschrijving",
      "auteur",
    ]);
  });

  it("sorts on the documenttype column end-to-end so the renamed field reorders rows", async () => {
    component.dataSource.data = [
      buildInformatieobject({
        titel: "Charlie",
        informatieobjectTypeOmschrijving: "Brief",
      }),
      buildInformatieobject({
        titel: "Alpha",
        informatieobjectTypeOmschrijving: "Aanvraag",
      }),
      buildInformatieobject({
        titel: "Bravo",
        informatieobjectTypeOmschrijving: "Contract",
      }),
    ];
    fixture.detectChanges();

    const informatieobjectTypeOmschrijvingHeader = await loader.getHarness(
      MatSortHeaderHarness.with({ label: "informatieobjectTypeOmschrijving" }),
    );
    const table = await loader.getHarness(MatTableHarness);

    await informatieobjectTypeOmschrijvingHeader.click();
    const titelsSortedByInformatieobjectTypeOmschrijvingAsc = await Promise.all(
      (await table.getRows()).map(
        async (row) => (await row.getCellTextByColumnName()).titel,
      ),
    );
    expect(titelsSortedByInformatieobjectTypeOmschrijvingAsc).toEqual([
      "Alpha",
      "Charlie",
      "Bravo",
    ]);
  });

  it("sorts on a date column (registratiedatumTijd) end-to-end", async () => {
    component.dataSource.data = [
      buildInformatieobject({
        titel: "Mid",
        registratiedatumTijd: "2025-06-15T10:00:00Z",
      }),
      buildInformatieobject({
        titel: "Oud",
        registratiedatumTijd: "2024-01-01T10:00:00Z",
      }),
      buildInformatieobject({
        titel: "Nieuw",
        registratiedatumTijd: "2026-03-20T10:00:00Z",
      }),
    ];
    fixture.detectChanges();

    const registratiedatumTijdHeader = await loader.getHarness(
      MatSortHeaderHarness.with({ label: "registratiedatumTijd" }),
    );
    const table = await loader.getHarness(MatTableHarness);

    await registratiedatumTijdHeader.click();
    const titelsSortedByRegistratiedatumTijdAsc = await Promise.all(
      (await table.getRows()).map(
        async (row) => (await row.getCellTextByColumnName()).titel,
      ),
    );
    expect(titelsSortedByRegistratiedatumTijdAsc).toEqual([
      "Oud",
      "Mid",
      "Nieuw",
    ]);

    await registratiedatumTijdHeader.click();
    const titelsSortedByRegistratiedatumTijdDesc = await Promise.all(
      (await table.getRows()).map(
        async (row) => (await row.getCellTextByColumnName()).titel,
      ),
    );
    expect(titelsSortedByRegistratiedatumTijdDesc).toEqual([
      "Nieuw",
      "Mid",
      "Oud",
    ]);
  });
});
