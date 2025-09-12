/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClientTestingModule } from "@angular/common/http/testing";
import { EventEmitter } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ZacHttpClient } from "src/app/shared/http/zac-http-client";
import { SessionStorageUtil } from "src/app/shared/storage/session-storage.util";
import { InboxDocumentenListComponent } from "./inbox-documenten-list.component";

describe("InboxDocumentenListComponent tests", () => {
  let fixture: ComponentFixture<InboxDocumentenListComponent>;
  let component: InboxDocumentenListComponent;
  let zacHttpClient: ZacHttpClient;
  let putSpy: jest.SpyInstance;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, TranslateModule.forRoot()],
      declarations: [InboxDocumentenListComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { data: of({ tabelGegevens: { aantalPerPagina: 10 } }) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(InboxDocumentenListComponent);
    component = fixture.componentInstance;
    zacHttpClient = TestBed.inject(ZacHttpClient);

    putSpy = jest.spyOn(zacHttpClient, "PUT");

    jest.spyOn(Storage.prototype, "setItem").mockImplementation(() => {});
    jest.spyOn(Storage.prototype, "getItem").mockReturnValue(null);

    component.sort = new MatSort();
    component.paginator = {
      pageSize: 10,
      page: new EventEmitter<PageEvent>(),
    } as MatPaginator;

    component.ngAfterViewInit();
  });

  it("should remember user data in SessionStorageUtil when updating list parameters", () => {
    const setItemSpy = jest.spyOn(SessionStorageUtil, "setItem");

    component.sort.active = "titel";
    component.sort.direction = "asc";
    component.paginator.pageSize = 25;

    component.updateListParameters();

    expect(setItemSpy).toHaveBeenCalledWith("INBOX_DOCUMENTEN_ZOEKPARAMETERS", {
      maxResults: 25,
      order: "asc",
      page: undefined,
      sort: "titel",
    });
  });

  it("should use remembered user data when reloading (ngOnInit)", () => {
    const rememberedParams = {
      sort: "titel",
      order: "asc",
      maxResults: 25,
      filtersType: "InboxDocumentListParameters",
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

    expect(component.listParameters).toEqual(rememberedParams);
  });

  /**
   * TODO make this test work
   * Due to the bad logic of the component it is hard to test the preservation of pagination
   * after a page reload. The component heavily relies on the lifecycle hooks ngOnInit and
   * ngAfterViewInit, which makes it hard to simulate a page reload in a test.
   * The test below is an attempt to simulate a page reload by calling ngOnInit and
   * ngAfterViewInit again, but it does not work as expected.
   * The test is therefore skipped for now until the component is refactored.
   */
  //   it("should preserve pagination after simulated page reload", () => {
});
