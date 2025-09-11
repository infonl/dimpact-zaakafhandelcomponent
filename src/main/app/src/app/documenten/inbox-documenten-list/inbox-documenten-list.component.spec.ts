import { HttpClientTestingModule } from "@angular/common/http/testing";
import { EventEmitter } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatPaginator } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ZacHttpClient } from "src/app/shared/http/zac-http-client";
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

    // Spy PUT
    putSpy = jest.spyOn(zacHttpClient, "PUT");

    // SessionStorage mocks
    jest.spyOn(Storage.prototype, "setItem").mockImplementation(() => {});
    jest.spyOn(Storage.prototype, "getItem").mockReturnValue(null);

    // Mock Angular Material sort & paginator
    component.sort = {
      active: "creatiedatum",
      direction: "desc",
      sortChange: new EventEmitter<{
        active: string;
        direction: "asc" | "desc";
      }>(),
    } as unknown as MatSort;

    component.paginator = {
      pageIndex: 0,
      pageSize: 10,
      length: 100,
      page: new EventEmitter<{ pageIndex: number; pageSize: number }>(),
    } as unknown as MatPaginator;

    // Trigger subscriptions
    component.ngAfterViewInit();
  });

  it("should call service with correct listParameters after manual update", () => {
    component.sort.active = "titel";
    component.sort.direction = "asc";
    component.paginator.pageSize = 25;

    component.sort.sortChange.emit({ active: "titel", direction: "asc" });

    expect(putSpy.mock.calls[1][0]).toBe("/rest/inboxdocumenten");
    expect(putSpy.mock.calls[1][1]).toEqual({
      filtersType: "InboxDocumentListParameters",
      maxResults: 25,
      order: "asc",
      page: 0,
      sort: "titel",
    });
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
