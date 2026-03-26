/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { Component, EventEmitter } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import { TranslateModule } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { ZoekComponent } from "./zoek.component";

@Component({
  selector: "zac-document-zoek-object",
  template: "",
  standalone: false,
})
class StubDocumentZoekObjectComponent {}

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
      declarations: [ZoekComponent, StubDocumentZoekObjectComponent],
    })
      .overrideComponent(ZoekComponent, {
        set: {
          template:
            '<zac-document-zoek-object *ngFor="let zoekObject of zoekResultaat.resultaten"></zac-document-zoek-object>',
        },
      })
      .compileComponents();

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
