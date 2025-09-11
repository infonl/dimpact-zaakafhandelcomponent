import { ComponentFixture, TestBed, fakeAsync, tick } from "@angular/core/testing";
import { InboxDocumentenListComponent } from "./inbox-documenten-list.component";
import { InboxDocumentenService } from "../inbox-documenten.service";
import { ZacHttpClient } from "src/app/shared/http/zac-http-client";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { of } from "rxjs";
import { TranslateModule } from "@ngx-translate/core";
import { ActivatedRoute } from "@angular/router";
import { MatPaginatorModule } from "@angular/material/paginator";
import { MatSortModule } from "@angular/material/sort";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { HarnessLoader } from "@angular/cdk/testing";
import { MatPaginatorHarness } from "@angular/material/paginator/testing";

describe("InboxDocumentenListComponent – paginator harness tests", () => {
  let fixture: ComponentFixture<InboxDocumentenListComponent>;
  let component: InboxDocumentenListComponent;
  let loader: HarnessLoader;
  let zacHttpClient: ZacHttpClient;

  beforeEach(async () => {
    const zacHttpClientMock = {
      PUT: jest.fn().mockReturnValue(of({ totaal: 100, resultaten: [] })),
    };

    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        TranslateModule.forRoot(),
        MatPaginatorModule,
        MatSortModule,
        BrowserAnimationsModule,
      ],
      declarations: [InboxDocumentenListComponent],
      providers: [
        InboxDocumentenService,
        { provide: ZacHttpClient, useValue: zacHttpClientMock },
        { provide: ActivatedRoute, useValue: { data: of({}) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(InboxDocumentenListComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    zacHttpClient = TestBed.inject(ZacHttpClient);

    jest.spyOn(Storage.prototype, "setItem").mockImplementation(() => {});
    jest.spyOn(Storage.prototype, "getItem").mockReturnValue(null);
        Object.defineProperty(component, "isLoadingResults", {
      set: jest.fn(),
      get: () => true,
    });


    fixture.detectChanges(); // runs ngOnInit + ngAfterViewInit
  });

  it("should call service with correct listParameters when paginator page changes", fakeAsync(async () => {
    const paginator = await loader.getHarness(MatPaginatorHarness);

    await paginator.goToNextPage(); // triggers page change
    tick();
    fixture.detectChanges();

    expect(zacHttpClient.PUT).toHaveBeenCalledWith(
      "/rest/inboxdocumenten",
      expect.objectContaining({
        page: 1,
        maxResults: 10,
        sort: "creatiedatum",
        order: "desc",
      })
    );
  }));
});
