/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { EventEmitter } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import { MatSidenav } from "@angular/material/sidenav";
import { MatSort } from "@angular/material/sort";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import { SessionStorageUtil } from "src/app/shared/storage/session-storage.util";
import { UtilService } from "src/app/core/service/util.service";
import { InboxDocumentenService } from "../inbox-documenten.service";
import { InboxDocumentenListComponent } from "./inbox-documenten-list.component";

const makeInboxDocument = (
  fields: Partial<{ id: string; titel: string; enkelvoudiginformatieobjectUUID: string; enkelvoudiginformatieobjectID: string; creatiedatum: string }> = {},
) =>
  ({
    id: "doc-1",
    titel: "Test document",
    enkelvoudiginformatieobjectUUID: "uuid-1",
    enkelvoudiginformatieobjectID: "ID-001",
    creatiedatum: "2026-01-01",
    ...fields,
  }) as unknown as Parameters<typeof InboxDocumentenListComponent.prototype["openDrawer"]>[0];

describe(InboxDocumentenListComponent.name, () => {
  let fixture: ComponentFixture<InboxDocumentenListComponent>;
  let component: InboxDocumentenListComponent;
  let inboxDocumentenService: InboxDocumentenService;
  let infoService: InformatieObjectenService;
  let utilService: UtilService;
  let dialog: MatDialog;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, TranslateModule.forRoot(), InboxDocumentenListComponent],
      providers: [
        provideHttpClient(),
        {
          provide: ActivatedRoute,
          useValue: { data: of({ tabelGegevens: { aantalPerPagina: 10 } }) },
        },
      ],
    }).compileComponents();

    inboxDocumentenService = TestBed.inject(InboxDocumentenService);
    infoService = TestBed.inject(InformatieObjectenService);
    utilService = TestBed.inject(UtilService);
    dialog = TestBed.inject(MatDialog);

    jest.spyOn(inboxDocumentenService, "list").mockReturnValue(
      of({ totaal: 0, resultaten: [] }),
    );
    jest.spyOn(Storage.prototype, "setItem").mockImplementation(() => {});
    jest.spyOn(Storage.prototype, "getItem").mockReturnValue(null);

    fixture = TestBed.createComponent(InboxDocumentenListComponent);
    component = fixture.componentInstance;

    component.sort = new MatSort();
    component.paginator = {
      pageSize: 10,
      pageIndex: 0,
      length: 0,
      page: new EventEmitter<PageEvent>(),
    } as unknown as MatPaginator;

    component.ngAfterViewInit();
  });

  it("should remember user data in SessionStorageUtil when updating list parameters", () => {
    const setItemSpy = jest.spyOn(SessionStorageUtil, "setItem");

    component.sort.active = "titel";
    component.sort.direction = "asc";
    component.paginator.pageSize = 25;

    component["updateListParameters"]();

    expect(setItemSpy).toHaveBeenCalledWith("INBOX_DOCUMENTEN_ZOEKPARAMETERS", {
      maxResults: 25,
      order: "asc",
      page: 0,
      sort: "titel",
    });
  });

  it("should use remembered user data when reloading (ngOnInit)", () => {
    const rememberedParams = {
      sort: "MockedTitle",
      order: "MockedOrder",
      maxResults: 99999,
      filtersType: "MockedInboxDocumentListParameters",
    };

    jest
      .spyOn(SessionStorageUtil, "getItem")
      .mockImplementation((key: string) => {
        if (key === "INBOX_DOCUMENTEN_ZOEKPARAMETERS") {
          return rememberedParams;
        }
        return null;
      });

    component.ngOnInit();

    expect(component["listParameters"]).toEqual(rememberedParams);
  });

  it("should return null from getDownloadURL when UUID is absent", () => {
    const doc = makeInboxDocument({ enkelvoudiginformatieobjectUUID: undefined });
    expect(component["getDownloadURL"](doc)).toBeNull();
  });

  it("should return download URL from getDownloadURL when UUID is present", () => {
    jest.spyOn(infoService, "getDownloadURL").mockReturnValue("/download/uuid-1");
    const doc = makeInboxDocument({ enkelvoudiginformatieobjectUUID: "uuid-1" });
    const url = component["getDownloadURL"](doc);
    expect(url).toBe("/download/uuid-1");
    expect(infoService.getDownloadURL).toHaveBeenCalledWith("uuid-1");
  });

  it("should return INBOX_DOCUMENTEN from getWerklijst", () => {
    expect(component["getWerklijst"]()).toBe("INBOX_DOCUMENTEN");
  });

  it("should reset pageIndex and emit filterChange and clearZoekopdracht on filtersChanged", () => {
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    const clearSpy = jest.spyOn(component["clearZoekopdracht"], "emit");
    component.paginator.pageIndex = 3;

    component["filtersChanged"]();

    expect(component.paginator.pageIndex).toBe(0);
    expect(clearSpy).toHaveBeenCalled();
    expect(filterChangeSpy).toHaveBeenCalled();
  });

  it("should emit filterChange on retriggerSearch", () => {
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    component["retriggerSearch"]();
    expect(filterChangeSpy).toHaveBeenCalled();
  });

  it("should reset to default parameters and emit filterChange on resetSearch", () => {
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    component["listParameters"] = { sort: "titel", order: "asc" };
    component.sort.active = "titel";
    component.sort.direction = "asc";
    component.paginator.pageIndex = 5;

    component["resetSearch"]();

    expect(component["listParameters"]).toMatchObject({
      sort: "creatiedatum",
      order: "desc",
    });
    expect(component.sort.active).toBe("creatiedatum");
    expect(component.sort.direction).toBe("desc");
    expect(component.paginator.pageIndex).toBe(0);
    expect(filterChangeSpy).toHaveBeenCalled();
  });

  it("should parse JSON and apply parameters on zoekopdrachtChanged with json", () => {
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    const params = { sort: "titel", order: "asc", filtersType: "InboxDocumentListParameters" };
    component["zoekopdrachtChanged"]({ json: JSON.stringify(params) } as never);

    // updateListParameters() fires synchronously via filterChange → only check fields from JSON
    expect(component["listParameters"]).toMatchObject({ filtersType: "InboxDocumentListParameters" });
    expect(component.paginator.pageIndex).toBe(0);
    expect(filterChangeSpy).toHaveBeenCalled();
  });

  it("should call resetSearch on zoekopdrachtChanged with null", () => {
    const resetSpy = jest.spyOn(component as never, "resetSearch");
    component["zoekopdrachtChanged"](null as never);
    expect(resetSpy).toHaveBeenCalled();
  });

  it("should only emit filterChange on zoekopdrachtChanged with defined but no json", () => {
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    component["zoekopdrachtChanged"]({} as never);
    expect(filterChangeSpy).toHaveBeenCalled();
  });

  it("should set selectedInformationObject and open sidenav on openDrawer", () => {
    const openSpy = jest.fn().mockResolvedValue(undefined);
    component.actionsSidenav = { open: openSpy } as unknown as MatSidenav;
    const doc = makeInboxDocument();

    component["openDrawer"](doc);

    expect(component["selectedInformationObject"]).toBe(doc);
    expect(openSpy).toHaveBeenCalled();
  });

  it("should reset page to 0 and persist to storage on ngOnDestroy", () => {
    const setItemSpy = jest.spyOn(SessionStorageUtil, "setItem");
    component["listParameters"].page = 5;

    component.ngOnDestroy();

    expect(setItemSpy).toHaveBeenCalledWith(
      "INBOX_DOCUMENTEN_ZOEKPARAMETERS",
      expect.objectContaining({ page: 0 }),
    );
  });

  it("should open confirm dialog and emit filterChange with snackbar on documentVerwijderen when confirmed", () => {
    const afterClosedSubject = new EventEmitter<boolean>();
    const dialogSpy = jest.spyOn(component["dialog"], "open").mockReturnValue({
      afterClosed: () => of(true),
    } as never);
    const snackbarSpy = jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    jest.spyOn(inboxDocumentenService, "delete").mockReturnValue(of(undefined) as never);

    const doc = makeInboxDocument({ id: "doc-42", titel: "My doc" });
    component["documentVerwijderen"](doc);

    expect(dialogSpy).toHaveBeenCalled();
    expect(snackbarSpy).toHaveBeenCalledWith("msg.document.verwijderen.uitgevoerd", {
      document: "My doc",
    });
    expect(filterChangeSpy).toHaveBeenCalled();
    void afterClosedSubject;
  });

  it("should not emit filterChange when confirm dialog is cancelled on documentVerwijderen", () => {
    jest.spyOn(component["dialog"], "open").mockReturnValue({
      afterClosed: () => of(false),
    } as never);
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    jest.spyOn(inboxDocumentenService, "delete").mockReturnValue(of(undefined) as never);

    component["documentVerwijderen"](makeInboxDocument());

    expect(filterChangeSpy).not.toHaveBeenCalled();
  });

  it("should populate dataSource from list response in ngAfterViewInit", () => {
    const docs = [makeInboxDocument({ id: "d1" }), makeInboxDocument({ id: "d2" })];
    jest.spyOn(inboxDocumentenService, "list").mockReturnValue(
      of({ totaal: 2, resultaten: docs }),
    );

    // Re-trigger by emitting filterChange
    component["filterChange"].emit();

    expect(component["dataSource"].data).toEqual(docs);
    expect(component.paginator.length).toBe(2);
  });
});
