/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import {
  ComponentRef,
  provideExperimentalZonelessChangeDetection,
} from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { MatTableHarness } from "@angular/material/table/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import moment from "moment";
import { EMPTY } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { InformatieObjectVerzendenComponent } from "./informatie-object-verzenden.component";

describe(InformatieObjectVerzendenComponent.name, () => {
  let component: InformatieObjectVerzendenComponent;
  let componentRef: ComponentRef<InformatieObjectVerzendenComponent>;
  let fixture: ComponentFixture<InformatieObjectVerzendenComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let informatieObjectenService: InformatieObjectenService;
  let foutAfhandelingService: FoutAfhandelingService;
  let utilService: UtilService;

  const mockSideNav = fromPartial<MatDrawer>({
    close: jest.fn().mockReturnValue(Promise.resolve("close")),
  });

  const mockDocuments = [
    fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
      uuid: "document-uuid-1",
      titel: "Document 1",
      bestandsnaam: "document-1.pdf",
    }),
    fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
      uuid: "document-uuid-2",
      titel: "Document 2",
      bestandsnaam: "document-2.pdf",
    }),
  ];

  const makeZaak = (fields: Partial<GeneratedType<"RestZaak">> = {}) =>
    fromPartial<GeneratedType<"RestZaak">>({
      uuid: "zaak-uuid-001",
      ...fields,
    });

  const fillInValidForm = () => {
    component["form"].patchValue({
      documenten: [mockDocuments[0]],
      verzenddatum: moment("2026-06-12"),
      toelichting: null,
    });
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        InformatieObjectVerzendenComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
        provideExperimentalZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideMomentDateAdapter(),
        provideQueryClient(testQueryClient),
        InformatieObjectenService,
        UtilService,
      ],
    }).compileComponents();

    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
    utilService = TestBed.inject(UtilService);
    httpTestingController = TestBed.inject(HttpTestingController);

    jest
      .spyOn(
        informatieObjectenService,
        "listInformatieobjectenVoorVerzendenQuery",
      )
      .mockImplementation((zaakUuid) =>
        fromPartial({
          queryKey: ["teVerzenden", zaakUuid],
          queryFn: () => Promise.resolve(mockDocuments),
        }),
      );
    jest.spyOn(utilService, "openSnackbar");
    jest.spyOn(foutAfhandelingService, "foutAfhandelen").mockReturnValue(EMPTY);

    fixture = TestBed.createComponent(InformatieObjectVerzendenComponent);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;

    componentRef.setInput("sideNav", mockSideNav);
    componentRef.setInput("zaak", makeZaak());

    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    testQueryClient.clear();
    httpTestingController.verify();
  });

  describe("toolbar", () => {
    it("renders the toolbar title", () => {
      const toolbar = fixture.nativeElement.querySelector("mat-toolbar span");
      expect(toolbar.textContent.trim()).toBe("actie.document.verzenden");
    });

    it("closes the sideNav when the close button is clicked", async () => {
      const closeButton = await loader.getHarness(
        MatButtonHarness.with({ ancestor: "mat-toolbar" }),
      );
      await closeButton.click();

      expect(mockSideNav.close).toHaveBeenCalled();
    });
  });

  describe("document list", () => {
    it("loads the documents that can be sent for the active zaak", async () => {
      expect(
        informatieObjectenService.listInformatieobjectenVoorVerzendenQuery,
      ).toHaveBeenCalledWith("zaak-uuid-001");

      const table = await loader.getHarness(MatTableHarness);
      expect(await table.getRows()).toHaveLength(mockDocuments.length);
    });

    it("loads the documents of the new zaak when the active zaak changes", async () => {
      componentRef.setInput("zaak", makeZaak({ uuid: "zaak-uuid-002" }));
      await fixture.whenStable();

      expect(
        informatieObjectenService.listInformatieobjectenVoorVerzendenQuery,
      ).toHaveBeenCalledWith("zaak-uuid-002");
    });
  });

  describe("form validation", () => {
    it("is invalid when no document is selected", () => {
      fillInValidForm();
      component["form"].controls.documenten.setValue([]);

      expect(component["form"].invalid).toBe(true);
    });

    it("is invalid without a verzenddatum", () => {
      fillInValidForm();
      component["form"].controls.verzenddatum.setValue(null);

      expect(component["form"].invalid).toBe(true);
    });

    it("is valid without a toelichting", () => {
      fillInValidForm();

      expect(component["form"].valid).toBe(true);
    });
  });

  describe("submit", () => {
    it("sends the selected documents for the active zaak", async () => {
      fillInValidForm();
      component["form"].controls.toelichting.setValue("test toelichting");

      component["submit"]();
      await sleep();

      const request = httpTestingController.expectOne({
        method: "POST",
        url: "/rest/informatieobjecten/informatieobjecten/verzenden",
      });
      expect(request.request.body).toEqual({
        zaakUuid: "zaak-uuid-001",
        verzenddatum: moment("2026-06-12").toISOString(),
        informatieobjecten: ["document-uuid-1"],
        toelichting: "test toelichting",
      });

      request.flush(null);
    });

    it("emits documentSent and shows a snackbar after a successful send", async () => {
      const emitSpy = jest.spyOn(component["documentSent"], "emit");
      fillInValidForm();

      component["submit"]();
      await sleep();

      httpTestingController
        .expectOne({
          method: "POST",
          url: "/rest/informatieobjecten/informatieobjecten/verzenden",
        })
        .flush(null);
      await sleep(100);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.document.verzenden.uitgevoerd",
      );
      expect(emitSpy).toHaveBeenCalled();
    });

    it("shows the plural snackbar message when multiple documents are sent", async () => {
      fillInValidForm();
      component["form"].controls.documenten.setValue(mockDocuments);

      component["submit"]();
      await sleep();

      httpTestingController
        .expectOne({
          method: "POST",
          url: "/rest/informatieobjecten/informatieobjecten/verzenden",
        })
        .flush(null);
      await sleep(100);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.documenten.verzenden.uitgevoerd",
      );
    });

    it("shows an error and keeps the panel open when sending fails", async () => {
      const emitSpy = jest.spyOn(component["documentSent"], "emit");
      fillInValidForm();

      component["submit"]();
      await sleep();

      httpTestingController
        .expectOne({
          method: "POST",
          url: "/rest/informatieobjecten/informatieobjecten/verzenden",
        })
        .flush(null, { status: 500, statusText: "Internal Server Error" });
      await sleep(100);

      expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalled();
      expect(emitSpy).not.toHaveBeenCalled();
      expect(mockSideNav.close).not.toHaveBeenCalled();
    });
  });

  describe("form buttons", () => {
    it("disables the submit button when the form is invalid", async () => {
      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.verzenden/ }),
      );

      expect(await submitButton.isDisabled()).toBe(true);
    });

    it("enables the submit button when the form is valid", async () => {
      fillInValidForm();
      fixture.detectChanges();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.verzenden/ }),
      );

      expect(await submitButton.isDisabled()).toBe(false);
    });

    it("closes the sideNav when the cancel button is clicked", async () => {
      const cancelButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.annuleren/ }),
      );
      await cancelButton.click();

      expect(mockSideNav.close).toHaveBeenCalled();
    });
  });
});
