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
import { ReferentieTabelCreateDialogComponent } from "./referentie-tabel-create-dialog/referentie-tabel-create-dialog.component";
import { ReferentieTabelEditDialogComponent } from "./referentie-tabel-edit-dialog/referentie-tabel-edit-dialog.component";
import { ReferentieTabellenComponent } from "./referentie-tabellen.component";

const tabellen = fromPartial<GeneratedType<"RestReferenceTable">[]>([
  {
    id: 1,
    code: "TABEL_A",
    name: "Tabel A",
    systemTable: false,
    valuesCount: 2,
  },
  {
    id: 2,
    code: "TABEL_B",
    name: "Tabel B",
    systemTable: true,
    valuesCount: 1,
  },
]);

const geladenTabelA = fromPartial<GeneratedType<"RestReferenceTable">>({
  id: 1,
  code: "TABEL_A",
  name: "Tabel A",
  systemTable: false,
  values: [
    { id: 10, name: "Waarde A1" },
    { id: 11, name: "Waarde A2" },
  ],
});

describe(ReferentieTabellenComponent.name, () => {
  let fixture: ComponentFixture<ReferentieTabellenComponent>;
  let component: ReferentieTabellenComponent;
  let service: ReferentieTabelService;
  let httpTestingController: HttpTestingController;
  let setTitle: jest.SpyInstance;
  let openSnackbar: jest.SpyInstance;
  let dialogOpen: jest.SpyInstance;

  // jsdom has no scrollIntoView; stub it per test and restore to avoid leaking.
  const originalScrollIntoView = Element.prototype.scrollIntoView;

  async function flushRendering() {
    fixture.detectChanges();
    await fixture.whenStable();
    await sleep();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  async function setup({ seedDetail = false } = {}) {
    fixture = TestBed.createComponent(ReferentieTabellenComponent);
    component = fixture.componentInstance;
    dialogOpen = jest
      .spyOn(TestBed.inject(MatDialog), "open")
      .mockReturnValue(
        fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(false) }),
      );

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
    Element.prototype.scrollIntoView = jest.fn();

    await TestBed.configureTestingModule({
      imports: [
        ReferentieTabellenComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
        provideRouter([]),
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
    Element.prototype.scrollIntoView = originalScrollIntoView;
    testQueryClient.clear();
    httpTestingController.verify();
  });

  it("sets the title and renders a row per table on init", async () => {
    await setup();
    expect(setTitle).toHaveBeenCalledWith(
      "title.referentietabellen",
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

  it("opens the create dialog", async () => {
    await setup();
    component["openCreateDialog"]();
    expect(dialogOpen).toHaveBeenCalledWith(
      ReferentieTabelCreateDialogComponent,
      expect.objectContaining({ width: "500px" }),
    );
  });

  it("expands and scrolls to the table returned by the create dialog", async () => {
    await setup({ seedDetail: true });
    dialogOpen.mockReturnValue(
      fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(1) }),
    );

    component["openCreateDialog"]();
    await flushRendering();

    expect(component["expandedId"]()).toBe(1);
    expect(Element.prototype.scrollIntoView).toHaveBeenCalled();
  });

  it("opens the delete confirmation and shows a snackbar when confirmed", async () => {
    await setup();
    dialogOpen.mockReturnValue(
      fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(true) }),
    );

    component["verwijderReferentieTabel"](tabellen[0]);

    const dialogData = dialogOpen.mock.calls[0][1].data;
    expect(dialogData._melding.key).toBe("msg.tabel.verwijderen-bevestigen");
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
