/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { provideZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ReferentieTabelEditDialogComponent } from "./referentie-tabel-edit-dialog/referentie-tabel-edit-dialog.component";
import { ReferentieTabellenV2Component } from "./referentie-tabellen-v2.component";

const tabellen = fromPartial<GeneratedType<"RestReferenceTable">[]>([
  { id: 1, code: "TABEL_A", naam: "Tabel A", systeem: false, aantalWaarden: 2 },
  { id: 2, code: "TABEL_B", naam: "Tabel B", systeem: true, aantalWaarden: 1 },
]);

const geladenTabelA = fromPartial<GeneratedType<"RestReferenceTable">>({
  id: 1,
  code: "TABEL_A",
  naam: "Tabel A",
  systeem: false,
  waarden: [
    { id: 10, naam: "Waarde A1" },
    { id: 11, naam: "Waarde A2" },
  ],
});

describe(ReferentieTabellenV2Component.name, () => {
  let fixture: ComponentFixture<ReferentieTabellenV2Component>;
  let component: ReferentieTabellenV2Component;
  let service: ReferentieTabelService;
  let httpTestingController: HttpTestingController;
  let setTitle: jest.SpyInstance;
  let openSnackbar: jest.SpyInstance;
  let dialogOpen: jest.SpyInstance;

  async function flushRendering() {
    fixture.detectChanges();
    await fixture.whenStable();
    await sleep();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  async function setup({ seedDetail = false } = {}) {
    fixture = TestBed.createComponent(ReferentieTabellenV2Component);
    component = fixture.componentInstance;
    dialogOpen = jest
      .spyOn(TestBed.inject(MatDialog), "open")
      .mockReturnValue(
        fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(false) }),
      );

    // Seed the cache so the queries resolve without hitting the network.
    testQueryClient.setQueryData(
      service.listReferentieTabellenQuery().queryKey,
      tabellen,
    );
    if (seedDetail) {
      testQueryClient.setQueryData(
        service.readReferentieTabelQuery(1).queryKey,
        geladenTabelA,
      );
    }

    await flushRendering();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ReferentieTabellenV2Component,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
        provideRouter([]),
        // ConfiguratieService has a broad DI tree and is unused by this screen.
        { provide: ConfiguratieService, useValue: {} },
      ],
    }).compileComponents();

    service = TestBed.inject(ReferentieTabelService);
    httpTestingController = TestBed.inject(HttpTestingController);
    const utilService = TestBed.inject(UtilService);
    setTitle = jest
      .spyOn(utilService, "setTitle")
      .mockImplementation(() => undefined);
    openSnackbar = jest
      .spyOn(utilService, "openSnackbar")
      .mockImplementation(() => undefined);
  });

  afterEach(() => {
    testQueryClient.clear();
    httpTestingController.verify();
  });

  it("sets the title and renders a row per table on init", async () => {
    await setup();
    expect(setTitle).toHaveBeenCalledWith(
      "title.referentietabellen.v2",
      undefined,
    );
    expect(fixture.nativeElement.querySelectorAll(".tabel-row")).toHaveLength(
      2,
    );
    expect(fixture.nativeElement.textContent).toContain("TABEL_A");
    expect(fixture.nativeElement.textContent).toContain("TABEL_B");
  });

  it("lazily loads and renders the values when a row is expanded", async () => {
    await setup({ seedDetail: true });

    component["toggle"](tabellen[0]);
    await flushRendering();

    expect(component["expandedId"]()).toBe(1);
    expect(fixture.nativeElement.textContent).toContain("Waarde A1");
  });

  it("creates a table, shows a snackbar and closes the create form", async () => {
    await setup();

    component["openCreateForm"]();
    component["form"].setValue({ code: "NEW_CODE", naam: "New name" });
    component["addReferentieTabel"]();
    await sleep();

    const request = httpTestingController.expectOne(
      (candidate) =>
        candidate.method === "POST" &&
        candidate.url === "/rest/referentietabellen",
    );
    expect(request.request.body).toEqual({
      code: "NEW_CODE",
      naam: "New name",
      systeem: false,
      waarden: [],
    });
    request.flush({});
    await sleep();

    // The service's onSuccess invalidates the list, which refetches; that promise
    // is awaited before the per-call onSuccess (snackbar + close), so flush it first.
    httpTestingController
      .expectOne(
        (candidate) =>
          candidate.method === "GET" &&
          candidate.url === "/rest/referentietabellen",
      )
      .flush(tabellen);
    await sleep();

    expect(openSnackbar).toHaveBeenCalledWith(
      "msg.referentietabel.toegevoegd",
      {
        tabel: "NEW_CODE",
      },
    );
    expect(component["showCreateForm"]()).toBe(false);
  });

  it("opens the delete confirmation and shows a snackbar when confirmed", async () => {
    await setup();
    dialogOpen.mockReturnValue(
      fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(true) }),
    );

    component["verwijderReferentieTabel"](tabellen[0]);

    const dialogData = dialogOpen.mock.calls[0][1].data;
    expect(dialogData._melding.key).toBe("msg.tabel.verwijderen.bevestigen");
    expect(dialogData._melding.args).toEqual({ tabel: "TABEL_A" });
    expect(openSnackbar).toHaveBeenCalledWith(
      "msg.tabel.verwijderen.uitgevoerd",
      { tabel: "TABEL_A" },
    );
  });

  it("loads the full table then opens the rename dialog", async () => {
    await setup({ seedDetail: true });

    component["editReferentieTabel"](tabellen[0]);
    await sleep();

    expect(dialogOpen).toHaveBeenCalledWith(
      ReferentieTabelEditDialogComponent,
      expect.objectContaining({ data: geladenTabelA }),
    );
  });
});
