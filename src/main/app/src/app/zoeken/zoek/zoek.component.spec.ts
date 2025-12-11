/*
 * SPDX-FileCopyrightText: 2021-2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { EventEmitter, NO_ERRORS_SCHEMA } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import { TranslateModule } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { ZoekComponent } from "./zoek.component";

describe("ZoekComponent", () => {
  let component: ZoekComponent;
  let fixture: ComponentFixture<ZoekComponent>;

  const mockPaginator: Pick<
    MatPaginator,
    "page" | "pageIndex" | "pageSize" | "length"
  > = {
    page: new EventEmitter<PageEvent>(),
    pageIndex: 0,
    pageSize: 10,
    length: 0,
  };

  const mockSidenav = {
    open: jest.fn(),
    close: jest.fn(),
    openedStart: new Subject<void>(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [provideHttpClient(), provideHttpClientTesting()],
      declarations: [ZoekComponent],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(ZoekComponent);
    component = fixture.componentInstance;

    // Mock before detectChanges so template bindings work
    Object.defineProperty(component, "paginator", {
      get: () => () => mockPaginator,
    });
    Object.defineProperty(component, "zoekenSideNav", {
      get: () => () => mockSidenav,
    });

    component.zoekResultaat.resultaten = [{ type: "DOCUMENT" }];

    fixture.detectChanges();
  });

  it("should render DOCUMENT search object with a sidenav", () => {
    const documentElem: HTMLElement | null =
      fixture.nativeElement.querySelector("zac-document-zoek-object");

    expect(documentElem).toBeTruthy();
  });
});
