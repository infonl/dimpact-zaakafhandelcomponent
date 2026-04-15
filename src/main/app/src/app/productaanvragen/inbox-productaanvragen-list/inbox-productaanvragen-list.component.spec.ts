/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { EventEmitter } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialogRef } from "@angular/material/dialog";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter, Router } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import { SessionStorageUtil } from "src/app/shared/storage/session-storage.util";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { InboxProductaanvragenService } from "../inbox-productaanvragen.service";
import { InboxProductaanvragenListComponent } from "./inbox-productaanvragen-list.component";

type Productaanvraag = GeneratedType<"RESTInboxProductaanvraag">;

const makeProductaanvraag = (
  fields: Partial<Productaanvraag> = {},
): Productaanvraag =>
  ({
    id: 1,
    type: "type-A",
    ontvangstdatum: "2026-01-01",
    initiatorID: "user-1",
    aantalBijlagen: 2,
    aanvraagdocumentUUID: "uuid-1",
    ...fields,
  }) as Partial<Productaanvraag> as unknown as Productaanvraag;

describe(InboxProductaanvragenListComponent.name, () => {
  let fixture: ComponentFixture<InboxProductaanvragenListComponent>;
  let component: InboxProductaanvragenListComponent;
  let service: InboxProductaanvragenService;
  let infoService: InformatieObjectenService;
  let utilService: UtilService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        TranslateModule.forRoot(),
        InboxProductaanvragenListComponent,
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

    service = TestBed.inject(InboxProductaanvragenService);
    infoService = TestBed.inject(InformatieObjectenService);
    utilService = TestBed.inject(UtilService);

    jest
      .spyOn(service, "list")
      .mockReturnValue(
        of({ totaal: 0, resultaten: [], filterType: [] } as never),
      );
    jest.spyOn(Storage.prototype, "setItem").mockImplementation(() => {});
    jest.spyOn(Storage.prototype, "getItem").mockReturnValue(null);

    fixture = TestBed.createComponent(InboxProductaanvragenListComponent);
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

  it("should persist list parameters to session storage on updateListParameters", () => {
    const setItemSpy = jest.spyOn(SessionStorageUtil, "setItem");
    component.sort.active = "type";
    component.sort.direction = "asc";
    component.paginator.pageSize = 25;

    component["updateListParameters"]();

    expect(setItemSpy).toHaveBeenCalledWith(
      "INBOX_PRODUCTAANVRAGEN_ZOEKPARAMETERS",
      expect.objectContaining({ sort: "type", order: "asc", maxResults: 25 }),
    );
  });

  it("should return INBOX_PRODUCTAANVRAGEN from getWerklijst", () => {
    expect(component["getWerklijst"]()).toBe("INBOX_PRODUCTAANVRAGEN");
  });

  it("should return download URL from getDownloadURL", () => {
    jest
      .spyOn(infoService, "getDownloadURL")
      .mockReturnValue("/download/uuid-1");
    const doc = makeProductaanvraag({ aanvraagdocumentUUID: "uuid-1" });
    expect(component["getDownloadURL"](doc)).toBe("/download/uuid-1");
  });

  it("should reset pageIndex and emit filterChange on filtersChanged with non-select event", () => {
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    component.paginator.pageIndex = 3;

    component["filtersChanged"]({ event: "some text", filter: "initiatorID" });

    expect(component.paginator.pageIndex).toBe(0);
    expect(component["listParameters"].initiatorID).toBeUndefined();
    expect(filterChangeSpy).toHaveBeenCalled();
  });

  it("should set filter value on filtersChanged with MatSelectChange event", () => {
    component["filtersChanged"]({
      event: { value: "type-B" } as never,
      filter: "type",
    });
    expect(component["listParameters"].type).toBe("type-B");
  });

  it("should reset to default parameters on resetSearch", () => {
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    component["listParameters"] = { sort: "type", order: "asc" };
    component.sort.active = "type";
    component.sort.direction = "asc";
    component.paginator.pageIndex = 5;

    component["resetSearch"]();

    expect(component["listParameters"]).toMatchObject({
      sort: "id",
      order: "desc",
    });
    expect(component.sort.active).toBe("id");
    expect(component.sort.direction).toBe("desc");
    expect(component.paginator.pageIndex).toBe(0);
    expect(filterChangeSpy).toHaveBeenCalled();
  });

  it("should parse JSON and emit filterChange on zoekopdrachtChanged with json", () => {
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    const params = { sort: "type", order: "asc" };
    component["zoekopdrachtChanged"]({
      json: JSON.stringify(params),
    } as Partial<
      GeneratedType<"RESTZoekopdracht">
    > as unknown as GeneratedType<"RESTZoekopdracht">);
    expect(component["listParameters"]).toMatchObject(params);
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

  it("should emit filterChange on zoekopdrachtChanged with defined but no json", () => {
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    component["zoekopdrachtChanged"](
      {} as Partial<
        GeneratedType<"RESTZoekopdracht">
      > as unknown as GeneratedType<"RESTZoekopdracht">,
    );
    expect(filterChangeSpy).toHaveBeenCalled();
  });

  it("should expand row and set previewSrc on updateActive for new row", () => {
    jest.spyOn(service, "pdfPreview").mockReturnValue("/preview/uuid-1");
    const row = makeProductaanvraag();
    component["expandedRow"] = null;

    component["updateActive"](row);

    expect(component["expandedRow"]).toBe(row);
    expect(component["previewSrc"]).not.toBeNull();
  });

  it("should collapse row and clear previewSrc on updateActive for same row", () => {
    const row = makeProductaanvraag();
    component["expandedRow"] = row;
    component["previewSrc"] = "something" as never;

    component["updateActive"](row);

    expect(component["expandedRow"]).toBeNull();
    expect(component["previewSrc"]).toBeNull();
  });

  it("should navigate to zaak create on aanmakenZaak", () => {
    const router = TestBed.inject(Router);
    const navSpy = jest.spyOn(router, "navigateByUrl").mockResolvedValue(true);
    const row = makeProductaanvraag();

    component["aanmakenZaak"](row);

    expect(navSpy).toHaveBeenCalledWith("zaken/create", {
      state: { inboxProductaanvraag: row },
    });
  });

  it("should open confirm dialog and emit filterChange on inboxProductaanvragenVerwijderen when confirmed", () => {
    jest.spyOn(component["dialog"], "open").mockReturnValue({
      afterClosed: () => of(true),
    } as Partial<MatDialogRef<unknown>> as unknown as MatDialogRef<unknown>);
    const snackbarSpy = jest
      .spyOn(utilService, "openSnackbar")
      .mockImplementation(() => {});
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    jest.spyOn(service, "delete").mockReturnValue(of(undefined) as never);

    component["inboxProductaanvragenVerwijderen"](
      makeProductaanvraag({ id: 42 }),
    );

    expect(snackbarSpy).toHaveBeenCalledWith(
      "msg.inboxProductaanvraag.verwijderen.uitgevoerd",
    );
    expect(filterChangeSpy).toHaveBeenCalled();
  });

  it("should not emit filterChange on inboxProductaanvragenVerwijderen when cancelled", () => {
    jest.spyOn(component["dialog"], "open").mockReturnValue({
      afterClosed: () => of(false),
    } as Partial<MatDialogRef<unknown>> as unknown as MatDialogRef<unknown>);
    const filterChangeSpy = jest.spyOn(component["filterChange"], "emit");
    jest.spyOn(service, "delete").mockReturnValue(of(undefined) as never);

    component["inboxProductaanvragenVerwijderen"](makeProductaanvraag());

    expect(filterChangeSpy).not.toHaveBeenCalled();
  });

  it("should populate dataSource and filterType from list response", () => {
    const rows = [
      makeProductaanvraag({ id: 1 }),
      makeProductaanvraag({ id: 2 }),
    ];
    jest.spyOn(service, "list").mockReturnValue(
      of({
        totaal: 2,
        resultaten: rows,
        filterType: ["type-A", "type-B"],
      } as never),
    );
    component["filterChange"].emit();
    expect(component["dataSource"].data).toEqual(rows);
    expect(component.paginator.length).toBe(2);
    expect(component["filterType"]).toEqual(["type-A", "type-B"]);
  });

  it("should fall back to empty array when list response omits resultaten", () => {
    jest
      .spyOn(service, "list")
      .mockReturnValue(
        of({ totaal: 0, filterType: [] } as Partial<
          GeneratedType<"RESTResultaatRESTInboxProductaanvraag">
        > as unknown as GeneratedType<"RESTResultaatRESTInboxProductaanvraag">),
      );
    component["filterChange"].emit();
    expect(component["dataSource"].data).toEqual([]);
  });

  it("should reset page to 0 and persist to storage on ngOnDestroy", () => {
    const setItemSpy = jest.spyOn(SessionStorageUtil, "setItem");
    component["listParameters"].page = 5;
    component.ngOnDestroy();
    expect(setItemSpy).toHaveBeenCalledWith(
      "INBOX_PRODUCTAANVRAGEN_ZOEKPARAMETERS",
      expect.objectContaining({ page: 0 }),
    );
  });

  it("should reset pageIndex to 0 when sort changes", () => {
    component.paginator.pageIndex = 3;
    component.sort.sortChange.emit({ active: "type", direction: "asc" });
    expect(component.paginator.pageIndex).toBe(0);
  });
});
