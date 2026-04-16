/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { EventEmitter } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialogRef } from "@angular/material/dialog";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import { MatSidenav } from "@angular/material/sidenav";
import { MatSort } from "@angular/material/sort";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import { SessionStorageUtil } from "src/app/shared/storage/session-storage.util";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { OntkoppeldeDocumentenService } from "../ontkoppelde-documenten.service";
import { OntkoppeldeDocumentenListComponent } from "./ontkoppelde-documenten-list.component";

type DetachedDocument = GeneratedType<"RestDetachedDocument">;

const makeDetachedDocument = (
  fields: Partial<DetachedDocument> = {},
): DetachedDocument =>
  ({
    id: 1,
    titel: "Test document",
    documentUUID: "uuid-1",
    creatiedatum: "2026-01-01",
    zaakID: "ZAAK-001",
    ontkoppeldDoor: { id: "user-1", naam: "Jan" },
    ontkoppeldOp: "2026-01-02",
    reden: "Test reden",
    isVergrendeld: false,
    ...fields,
  }) as Partial<DetachedDocument> as unknown as DetachedDocument;

describe(OntkoppeldeDocumentenListComponent.name, () => {
  let fixture: ComponentFixture<OntkoppeldeDocumentenListComponent>;
  let component: OntkoppeldeDocumentenListComponent;
  let ontkoppeldeDocumentenService: OntkoppeldeDocumentenService;
  let infoService: InformatieObjectenService;
  let utilService: UtilService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        TranslateModule.forRoot(),
        OntkoppeldeDocumentenListComponent,
      ],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { data: of({ tabelGegevens: { aantalPerPagina: 10 } }) },
        },
      ],
    }).compileComponents();

    ontkoppeldeDocumentenService = TestBed.inject(OntkoppeldeDocumentenService);
    infoService = TestBed.inject(InformatieObjectenService);
    utilService = TestBed.inject(UtilService);

    jest
      .spyOn(ontkoppeldeDocumentenService, "list")
      .mockReturnValue(of({ totaal: 0, resultaten: [] }));
    jest.spyOn(Storage.prototype, "setItem").mockImplementation(() => {});
    jest.spyOn(Storage.prototype, "getItem").mockReturnValue(null);

    fixture = TestBed.createComponent(OntkoppeldeDocumentenListComponent);
    component = fixture.componentInstance;

    component.sort = new MatSort();
    component.paginator = {
      pageSize: 10,
      pageIndex: 0,
      length: 0,
      page: new EventEmitter<PageEvent>(),
    } as Partial<MatPaginator> as unknown as MatPaginator;

    component.ngAfterViewInit();
  });

  it("should remember user data in SessionStorageUtil when updating list parameters", () => {
    const setItemSpy = jest.spyOn(SessionStorageUtil, "setItem");

    component.sort.active = "titel";
    component.sort.direction = "asc";
    component.paginator.pageSize = 25;

    component["updateListParameters"]();

    expect(setItemSpy).toHaveBeenCalledWith(
      "ONTKOPPELDE_DOCUMENTEN_ZOEKPARAMETERS",
      {
        maxResults: 25,
        order: "asc",
        page: 0,
        sort: "titel",
      },
    );
  });

  it("should use remembered user data when reloading (ngOnInit)", () => {
    const rememberedParams = {
      sort: "MockedTitle",
      order: "MockedOrder",
      maxResults: 99999,
      filtersType: "DetachedDocumentListParameters",
    };

    jest
      .spyOn(SessionStorageUtil, "getItem")
      .mockImplementation((key: string) => {
        if (key === "ONTKOPPELDE_DOCUMENTEN_ZOEKPARAMETERS") {
          return rememberedParams;
        }
        return null;
      });

    component.ngOnInit();

    expect(component["listParameters"]).toEqual(rememberedParams);
  });

  it("should return null from getDownloadURL when documentUUID is absent", () => {
    const doc = makeDetachedDocument({ documentUUID: undefined });
    expect(component["getDownloadURL"](doc)).toBeNull();
  });

  it("should return download URL from getDownloadURL when documentUUID is present", () => {
    jest
      .spyOn(infoService, "getDownloadURL")
      .mockReturnValue("/download/uuid-1");
    const doc = makeDetachedDocument({ documentUUID: "uuid-1" });
    expect(component["getDownloadURL"](doc)).toBe("/download/uuid-1");
    expect(infoService.getDownloadURL).toHaveBeenCalledWith("uuid-1");
  });

  it("should return ONTKOPPELDE_DOCUMENTEN from getWerklijst", () => {
    expect(component["getWerklijst"]()).toBe("ONTKOPPELDE_DOCUMENTEN");
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
      sort: "ontkoppeldOp",
      order: "desc",
    });
    expect(component.sort.active).toBe("ontkoppeldOp");
    expect(component.sort.direction).toBe("desc");
    expect(component.paginator.pageIndex).toBe(0);
    expect(filterChangeSpy).toHaveBeenCalled();
  });

  it("should parse JSON and apply parameters on zoekopdrachtChanged with truthy value", () => {
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    const params = {
      sort: "titel",
      order: "asc",
      filtersType: "DetachedDocumentListParameters",
    };
    component["zoekopdrachtChanged"]({
      json: JSON.stringify(params),
    } as Partial<
      GeneratedType<"RESTZoekopdracht">
    > as unknown as GeneratedType<"RESTZoekopdracht">);

    expect(component["listParameters"]).toMatchObject({
      filtersType: "DetachedDocumentListParameters",
    });
    expect(component.paginator.pageIndex).toBe(0);
    expect(filterChangeSpy).toHaveBeenCalled();
  });

  it("should call resetSearch on zoekopdrachtChanged with null", () => {
    const resetSpy = jest.spyOn(
      component as unknown as { resetSearch: () => void },
      "resetSearch",
    );
    component["zoekopdrachtChanged"](
      null as unknown as GeneratedType<"RESTZoekopdracht">,
    );
    expect(resetSpy).toHaveBeenCalled();
  });

  it("should set selectedInformationObject and open sidenav on openDrawer", () => {
    const openSpy = jest.fn().mockResolvedValue(undefined);
    component.actionsSidenav = { open: openSpy } as unknown as MatSidenav;
    const doc = makeDetachedDocument();

    component["openDrawer"](doc);

    expect(component["selectedInformationObject"]).toBe(doc);
    expect(openSpy).toHaveBeenCalled();
  });

  it("should reset page to 0 and persist to storage on ngOnDestroy", () => {
    const setItemSpy = jest.spyOn(SessionStorageUtil, "setItem");
    component["listParameters"].page = 5;

    component.ngOnDestroy();

    expect(setItemSpy).toHaveBeenCalledWith(
      "ONTKOPPELDE_DOCUMENTEN_ZOEKPARAMETERS",
      expect.objectContaining({ page: 0 }),
    );
  });

  it("should open confirm dialog and emit filterChange with snackbar on documentVerwijderen when confirmed", () => {
    jest.spyOn(component["dialog"], "open").mockReturnValue({
      afterClosed: () => of(true),
    } as Partial<MatDialogRef<unknown>> as unknown as MatDialogRef<unknown>);
    const snackbarSpy = jest
      .spyOn(utilService, "openSnackbar")
      .mockImplementation(() => {});
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    jest
      .spyOn(ontkoppeldeDocumentenService, "delete")
      .mockReturnValue(of(undefined) as never);

    const doc = makeDetachedDocument({ id: 42, titel: "My doc" });
    component["documentVerwijderen"](doc);

    expect(snackbarSpy).toHaveBeenCalledWith(
      "msg.document.verwijderen.uitgevoerd",
      {
        document: "My doc",
      },
    );
    expect(filterChangeSpy).toHaveBeenCalled();
  });

  it("should not emit filterChange when confirm dialog is cancelled on documentVerwijderen", () => {
    jest.spyOn(component["dialog"], "open").mockReturnValue({
      afterClosed: () => of(false),
    } as Partial<MatDialogRef<unknown>> as unknown as MatDialogRef<unknown>);
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    jest
      .spyOn(ontkoppeldeDocumentenService, "delete")
      .mockReturnValue(of(undefined) as never);

    component["documentVerwijderen"](makeDetachedDocument());

    expect(filterChangeSpy).not.toHaveBeenCalled();
  });

  it("should populate dataSource and filterOntkoppeldDoor from list response", () => {
    const docs = [
      makeDetachedDocument({
        id: 1,
        ontkoppeldDoor: {
          id: "u1",
          naam: "Alice",
        } as unknown as GeneratedType<"RestUser">,
      }),
      makeDetachedDocument({
        id: 2,
        ontkoppeldDoor: {
          id: "u2",
          naam: "Bob",
        } as unknown as GeneratedType<"RestUser">,
      }),
    ];
    jest
      .spyOn(ontkoppeldeDocumentenService, "list")
      .mockReturnValue(of({ totaal: 2, resultaten: docs }));

    component["filterChange"].emit();

    expect(component["dataSource"].data).toEqual(docs);
    expect(component.paginator.length).toBe(2);
    expect(component["filterOntkoppeldDoor"]).toEqual([
      { id: "u1", naam: "Alice" },
      { id: "u2", naam: "Bob" },
    ]);
  });

  it("should fall back to empty array and zero length when list response omits resultaten and totaal", () => {
    jest
      .spyOn(ontkoppeldeDocumentenService, "list")
      .mockReturnValue(
        of(
          {} as Partial<
            GeneratedType<"RESTResultaatRestDetachedDocument">
          > as unknown as GeneratedType<"RESTResultaatRestDetachedDocument">,
        ),
      );

    component["filterChange"].emit();

    expect(component["dataSource"].data).toEqual([]);
    expect(component.paginator.length).toBe(0);
    expect(component["filterOntkoppeldDoor"]).toEqual([]);
  });

  it("should reset pageIndex to 0 when sort changes", () => {
    component.paginator.pageIndex = 3;
    component.sort.sortChange.emit({ active: "titel", direction: "asc" });
    expect(component.paginator.pageIndex).toBe(0);
  });

  it("should return true from compareUser when user ids match", () => {
    const user1 = { id: "u1" } as Partial<
      GeneratedType<"RestUser">
    > as unknown as GeneratedType<"RestUser">;
    const user2 = { id: "u1" } as Partial<
      GeneratedType<"RestUser">
    > as unknown as GeneratedType<"RestUser">;
    expect(component["compareUser"](user1, user2)).toBe(true);
  });

  it("should return false from compareUser when user ids differ", () => {
    const user1 = { id: "u1" } as Partial<
      GeneratedType<"RestUser">
    > as unknown as GeneratedType<"RestUser">;
    const user2 = { id: "u2" } as Partial<
      GeneratedType<"RestUser">
    > as unknown as GeneratedType<"RestUser">;
    expect(component["compareUser"](user1, user2)).toBe(false);
  });
});
