/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatPaginator } from "@angular/material/paginator";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideQueryClient,
  provideTanStackQuery,
} from "@tanstack/angular-query-experimental";

import { createQueryOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenService } from "../../signaleringen.service";
import { DashboardCard } from "../model/dashboard-card";
import { DashboardCardId } from "../model/dashboard-card-id";
import { DashboardCardType } from "../model/dashboard-card-type";
import { ZakenCardComponent } from "./zaken-card.component";

const buildResultaat = (totaal: number, count = totaal) =>
  fromPartial<{
    resultaten: GeneratedType<"RestZaakOverzicht">[];
    totaal: number;
  }>({
    resultaten: Array.from({ length: count }, (_, i) =>
      fromPartial<GeneratedType<"RestZaakOverzicht">>({
        identificatie: `ZAAK-${i}`,
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
  let signaleringenService: SignaleringenService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZakenCardComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideQueryClient(testQueryClient),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTanStackQuery(testQueryClient),
        {
          provide: WebsocketService,
          useValue: { addListener: jest.fn() },
        },
      ],
    }).compileComponents();

    signaleringenService = TestBed.inject(SignaleringenService);
    const identityService = TestBed.inject(IdentityService);
    testQueryClient.setQueryData(
      identityService.readLoggedInUser().queryKey,
      fromPartial<GeneratedType<"RestUser">>({ id: "user", naam: "Test" }),
    );
  });

  function getPaginator(): MatPaginator {
    return fixture.debugElement.query(
      (el) => el.componentInstance instanceof MatPaginator,
    )!.componentInstance as MatPaginator;
  }

  it("renders paginator length from the query result total even when one page of rows is loaded", async () => {
    const zakenQuery = createQueryOptions(buildResultaat(25, 5));
    testQueryClient.setQueryData(zakenQuery.queryKey, buildResultaat(25, 5));
    jest
      .spyOn(signaleringenService, "listZakenSignalering")
      .mockReturnValue(zakenQuery as never);

    fixture = TestBed.createComponent(ZakenCardComponent);
    fixture.componentInstance.data = cardData;
    fixture.detectChanges();
    await sleep();
    fixture.detectChanges();

    expect(getPaginator().length).toBe(25);
  });

  it("does not bind the paginator to the dataSource so MatTableDataSource cannot overwrite paginator.length", () => {
    jest
      .spyOn(signaleringenService, "listZakenSignalering")
      .mockReturnValue(createQueryOptions(buildResultaat(0)) as never);

    fixture = TestBed.createComponent(ZakenCardComponent);
    fixture.componentInstance.data = cardData;
    fixture.detectChanges();

    expect(fixture.componentInstance.dataSource.paginator).toBeFalsy();
  });

  it("populates the data source with rows from the query result", async () => {
    const zakenQuery = createQueryOptions(buildResultaat(8));
    testQueryClient.setQueryData(zakenQuery.queryKey, buildResultaat(8));
    jest
      .spyOn(signaleringenService, "listZakenSignalering")
      .mockReturnValue(zakenQuery as never);

    fixture = TestBed.createComponent(ZakenCardComponent);
    fixture.componentInstance.data = cardData;
    fixture.detectChanges();
    await sleep();
    fixture.detectChanges();

    expect(fixture.componentInstance.dataSource.data).toHaveLength(8);
  });

  it("updates pageNumber when onPageChange is called", () => {
    fixture = TestBed.createComponent(ZakenCardComponent);
    fixture.componentInstance.data = cardData;

    fixture.componentInstance.onPageChange({ pageIndex: 4 });

    expect(fixture.componentInstance.pageNumber()).toBe(4);
  });

  it("starts with the default tijdstip-desc sort so the request always carries an explicit ordering", () => {
    jest
      .spyOn(signaleringenService, "listZakenSignalering")
      .mockReturnValue(createQueryOptions(buildResultaat(0)) as never);

    fixture = TestBed.createComponent(ZakenCardComponent);
    fixture.componentInstance.data = cardData;
    fixture.detectChanges();

    expect(fixture.componentInstance.sortField()).toBe("SIGNALERING_TIJDSTIP");
    expect(fixture.componentInstance.sortOrder()).toBe("DESC");
    expect(fixture.componentInstance.parameters()).toMatchObject({
      sortField: "SIGNALERING_TIJDSTIP",
      sortOrder: "DESC",
    });
  });

  it("propagates sort changes into the request and resets pagination back to the first page", () => {
    jest
      .spyOn(signaleringenService, "listZakenSignalering")
      .mockReturnValue(createQueryOptions(buildResultaat(0)) as never);

    fixture = TestBed.createComponent(ZakenCardComponent);
    fixture.componentInstance.data = cardData;
    fixture.detectChanges();

    fixture.componentInstance.onPageChange({ pageIndex: 3 });
    expect(fixture.componentInstance.pageNumber()).toBe(3);

    fixture.componentInstance.sort!.sortChange.emit({
      active: "ZAAK_STARTDATUM",
      direction: "desc",
    });

    expect(fixture.componentInstance.sortField()).toBe("ZAAK_STARTDATUM");
    expect(fixture.componentInstance.sortOrder()).toBe("DESC");
    expect(fixture.componentInstance.pageNumber()).toBe(0);
    expect(fixture.componentInstance.parameters()).toMatchObject({
      sortField: "ZAAK_STARTDATUM",
      sortOrder: "DESC",
      page: 0,
    });
  });

  it("reverts to the default sort order when the user toggles back to no direction", () => {
    jest
      .spyOn(signaleringenService, "listZakenSignalering")
      .mockReturnValue(createQueryOptions(buildResultaat(0)) as never);

    fixture = TestBed.createComponent(ZakenCardComponent);
    fixture.componentInstance.data = cardData;
    fixture.detectChanges();

    fixture.componentInstance.sort!.sortChange.emit({
      active: "ZAAK_IDENTIFICATIE",
      direction: "asc",
    });
    expect(fixture.componentInstance.sortField()).toBe("ZAAK_IDENTIFICATIE");

    fixture.componentInstance.sort!.sortChange.emit({
      active: "ZAAK_IDENTIFICATIE",
      direction: "",
    });

    expect(fixture.componentInstance.sortField()).toBe("SIGNALERING_TIJDSTIP");
    expect(fixture.componentInstance.sortOrder()).toBe("DESC");
    expect(fixture.componentInstance.parameters()).toMatchObject({
      sortField: "SIGNALERING_TIJDSTIP",
      sortOrder: "DESC",
    });
  });

  it("forwards sort fields to the signaleringen service when the query runs", async () => {
    const spy = jest
      .spyOn(signaleringenService, "listZakenSignalering")
      .mockReturnValue(createQueryOptions(buildResultaat(0)) as never);

    fixture = TestBed.createComponent(ZakenCardComponent);
    fixture.componentInstance.data = cardData;
    fixture.detectChanges();
    await sleep();

    fixture.componentInstance.sort!.sortChange.emit({
      active: "ZAAK_IDENTIFICATIE",
      direction: "asc",
    });
    fixture.detectChanges();
    await sleep();

    expect(spy).toHaveBeenCalledWith("ZAAK_OP_NAAM", {
      page: 0,
      rows: 5,
      sortField: "ZAAK_IDENTIFICATIE",
      sortOrder: "ASC",
    });
  });
});
