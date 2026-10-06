/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { Component, input } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatNavListItemHarness } from "@angular/material/list/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of, ReplaySubject } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { ConfirmDialogData } from "../../shared/confirm-dialog/confirm-dialog.component";
import { RedenDialogData } from "../../shared/dialog/reden-dialog-form/reden-dialog-form.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { InformatieObjectEditComponent } from "../informatie-object-edit/informatie-object-edit.component";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { FileFormat } from "../model/file-format";
import { InformatieObjectViewComponent } from "./informatie-object-view.component";

@Component({
  selector: "zac-informatie-object-edit",
  template: "",
  standalone: true,
})
class InformatieObjectEditStubComponent {
  readonly infoObject =
    input<GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">>();
  readonly sideNav = input.required<MatDrawer>();
  readonly zaakUuid = input.required<string>();
}

describe(InformatieObjectViewComponent.name, () => {
  const user = userEvent.setup();

  let component: InformatieObjectViewComponent;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;

  let informatieObjectenService: InformatieObjectenService;
  let zakenService: ZakenService;

  const mockActivatedRoute = {
    data: new ReplaySubject<{
      informatieObject: GeneratedType<"RestEnkelvoudigInformatieobject">;
    }>(1),
  };

  const zaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "zaak-001",
    identificatie: "test",
    indicaties: [],
    omschrijving: "test omschrijving",
    vertrouwelijkheidaanduiding: "OPENBAAR",
    rechten: fromPartial<GeneratedType<"RestZaakRechten">>({}),
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
      uuid: "zaaktype-001",
    }),
  });

  const zaakInformatieobject = fromPartial<
    GeneratedType<"RestZaakInformatieobject">
  >({
    zaakIdentificatie: zaak.identificatie,
  });

  const enkelvoudigInformatieobject = fromPartial<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >({
    uuid: "enkelvoudig-informatieobject-001",
    informatieobjectTypeUUID: "test-uuid",
    indicaties: [],
    titel: "test informatieobject",
    vertrouwelijkheidaanduiding: "OPENBAAR",
    rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({}),
    formaat: FileFormat.DOCX,
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        InformatieObjectViewComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideQueryClient(testQueryClient),
        {
          provide: ActivatedRoute,
          useValue: mockActivatedRoute,
        },
      ],
    })
      .overrideComponent(InformatieObjectViewComponent, {
        remove: { imports: [InformatieObjectEditComponent] },
        add: { imports: [InformatieObjectEditStubComponent] },
      })
      .compileComponents();

    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    jest
      .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
      .mockReturnValue(of(enkelvoudigInformatieobject));

    jest
      .spyOn(
        informatieObjectenService,
        "readHuidigeVersieEnkelvoudigInformatieObject",
      )
      .mockReturnValue(
        of({
          uuid: "enkelvoudig-informatieobject-001",
          informatieobjectTypeUUID: "test-uuid",
          titel: "test informatieobject",
          vertrouwelijkheidaanduiding: "OPENBAAR",
          rechten: {},
        }),
      );

    jest
      .spyOn(informatieObjectenService, "listZaakInformatieobjecten")
      .mockReturnValue(of([zaakInformatieobject]));

    jest
      .spyOn(informatieObjectenService, "listHistorie")
      .mockReturnValue(of([]));

    zakenService = TestBed.inject(ZakenService);
    jest.spyOn(zakenService, "readZaakByID").mockReturnValue(of(zaak));

    fixture = TestBed.createComponent(InformatieObjectViewComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);

    mockActivatedRoute.data.next({
      informatieObject: enkelvoudigInformatieobject,
    });

    fixture.detectChanges();
  });

  describe("actie.nieuwe.versie.toevoegen", () => {
    it("should not have a button when the user does not have the right to add a new version", async () => {
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({
              canToevoegenNieuweVersie: false,
            }),
          }),
        );
      mockActivatedRoute.data.next({
        informatieObject: enkelvoudigInformatieobject,
      });

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({ title: "actie.nieuwe.versie.toevoegen" }),
      );

      expect(button).toBeNull();
    });

    it("should open the sidebar when clicked", async () => {
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({
              canToevoegenNieuweVersie: true,
            }),
          }),
        );
      mockActivatedRoute.data.next({
        informatieObject: enkelvoudigInformatieobject,
      });

      const button = await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.nieuwe.versie.toevoegen" }),
      );
      await button.click();

      const sidebar = component.actionsSidenav;
      expect(sidebar.opened).toBe(true);
    });
  });

  describe("actie.converteren", () => {
    it("should have a button when the document is of format DOCX and the user has the right to convert a document", async () => {
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({
              canConverteren: true,
            }),
          }),
        );
      mockActivatedRoute.data.next({
        informatieObject: enkelvoudigInformatieobject,
      });

      const button = await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.converteren" }),
      );

      expect(button).toBeTruthy();
    });

    it("should not have a button when the document is of format DOCX and the user does not have the right to convert a document", async () => {
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({
              canConverteren: false,
            }),
          }),
        );
      mockActivatedRoute.data.next({
        informatieObject: enkelvoudigInformatieobject,
      });

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({ title: "actie.converteren" }),
      );

      expect(button).toBeNull();
    });

    it("should not have a button when the document is of format TEXT and the user has the right to convert a document", async () => {
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({
              canConverteren: true,
            }),
          }),
        );
      mockActivatedRoute.data.next({
        informatieObject: {
          ...enkelvoudigInformatieobject,
          formaat: FileFormat.TEXT,
        },
      });

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({ title: "actie.converteren" }),
      );

      expect(button).toBeNull();
    });

    it("converts the document within its zaak to PDF", async () => {
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({
              canConverteren: true,
            }),
          }),
        );
      mockActivatedRoute.data.next({
        informatieObject: enkelvoudigInformatieobject,
      });

      await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.converteren" }),
      );
      await user.click(
        screen.getByRole("button", { name: "actie.converteren" }),
      );
      await sleep();

      const request = TestBed.inject(HttpTestingController).expectOne(
        `/rest/informatieobjecten/informatieobject/${enkelvoudigInformatieobject.uuid}/convert?zaak=${zaak.uuid}`,
      );
      expect(request.request.method).toBe("POST");
      request.flush(null);
    });
  });

  describe("actie.unlock", () => {
    it("should not have a button when the document is not locked", async () => {
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            gelockedDoor: undefined,
            rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({
              canOntgrendelen: true,
            }),
          }),
        );
      mockActivatedRoute.data.next({
        informatieObject: enkelvoudigInformatieobject,
      });

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({ title: "actie.unlock" }),
      );

      expect(button).toBeNull();
    });

    it("should not have a button when the document is locked but the user does not have the right to unlock", async () => {
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            gelockedDoor: { id: "user-001", naam: "Test User" },
            rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({
              canOntgrendelen: false,
            }),
          }),
        );
      mockActivatedRoute.data.next({
        informatieObject: enkelvoudigInformatieobject,
      });

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({ title: "actie.unlock" }),
      );

      expect(button).toBeNull();
    });

    it("unlocks the document within its zaak when clicked and a zaak is present", async () => {
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            gelockedDoor: { id: "user-001", naam: "Test User" },
            rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({
              canOntgrendelen: true,
            }),
          }),
        );
      mockActivatedRoute.data.next({
        informatieObject: enkelvoudigInformatieobject,
      });

      await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.unlock" }),
      );
      await user.click(screen.getByRole("button", { name: "actie.unlock" }));
      await sleep();

      const request = TestBed.inject(HttpTestingController).expectOne(
        `/rest/informatieobjecten/informatieobject/${enkelvoudigInformatieobject.uuid}/unlock?zaak=${zaak.uuid}`,
      );
      expect(request.request.method).toBe("POST");
      request.flush(null);
    });

    it("unlocks the document without a zaak when clicked and no zaak is present", async () => {
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            gelockedDoor: { id: "user-001", naam: "Test User" },
            rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({
              canOntgrendelen: true,
            }),
          }),
        );
      jest
        .spyOn(informatieObjectenService, "listZaakInformatieobjecten")
        .mockReturnValue(of([]));
      mockActivatedRoute.data.next({
        informatieObject: enkelvoudigInformatieobject,
      });

      await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.unlock" }),
      );
      await user.click(screen.getByRole("button", { name: "actie.unlock" }));
      await sleep();

      const request = TestBed.inject(HttpTestingController).expectOne(
        ({ url }) =>
          url.startsWith(
            `/rest/informatieobjecten/informatieobject/${enkelvoudigInformatieobject.uuid}/unlock`,
          ),
      );
      expect(request.request.url).not.toContain("zaak=");
      request.flush(null);
    });
  });

  describe("actie.lock", () => {
    beforeEach(() => {
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            gelockedDoor: undefined,
            rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({
              canVergrendelen: true,
            }),
          }),
        );
      mockActivatedRoute.data.next({
        informatieObject: enkelvoudigInformatieobject,
      });
    });

    it("locks the document within its zaak and keeps the button disabled until the updated document replaces it", async () => {
      await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.lock" }),
      );
      await user.click(screen.getByRole("button", { name: "actie.lock" }));
      await sleep();

      const request = TestBed.inject(HttpTestingController).expectOne(
        `/rest/informatieobjecten/informatieobject/${enkelvoudigInformatieobject.uuid}/lock?zaak=${zaak.uuid}`,
      );
      expect(request.request.method).toBe("POST");
      request.flush(null);

      fixture.detectChanges();

      expect(screen.getByRole("button", { name: "actie.lock" })).toBeDisabled();
    });

    it("enables the button again when locking fails", async () => {
      jest
        .spyOn(TestBed.inject(FoutAfhandelingService), "foutAfhandelen")
        .mockReturnValue(of());
      await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.lock" }),
      );
      await user.click(screen.getByRole("button", { name: "actie.lock" }));
      await sleep();

      TestBed.inject(HttpTestingController)
        .expectOne(
          `/rest/informatieobjecten/informatieobject/${enkelvoudigInformatieobject.uuid}/lock?zaak=${zaak.uuid}`,
        )
        .flush(null, { status: 500, statusText: "Server Error" });

      await sleep();
      fixture.detectChanges();

      expect(screen.getByRole("button", { name: "actie.lock" })).toBeEnabled();
    });
  });

  describe("actie.ondertekenen", () => {
    let dialog: MatDialog;

    beforeEach(() => {
      dialog = TestBed.inject(MatDialog);
      jest
        .spyOn(dialog, "open")
        .mockReturnValue(fromPartial<MatDialogRef<unknown>>({}));
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({
              canOndertekenen: true,
            }),
          }),
        );
      mockActivatedRoute.data.next({
        informatieObject: enkelvoudigInformatieobject,
      });
    });

    it("does not sign the document while the confirmation dialog is still open", async () => {
      const button = await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.ondertekenen" }),
      );
      await button.click();
      await sleep();

      expect(dialog.open).toHaveBeenCalled();
      TestBed.inject(HttpTestingController).expectNone(
        `/rest/informatieobjecten/informatieobject/${enkelvoudigInformatieobject.uuid}/onderteken?zaak=${zaak.uuid}`,
      );
    });

    it("signs the document within its zaak once confirmed, so that the dialog can close", async () => {
      const button = await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.ondertekenen" }),
      );
      await button.click();
      const { observable } = jest.mocked(dialog.open).mock.calls.at(-1)![1]!
        .data as ConfirmDialogData;
      const onSigned = jest.fn();
      observable!.subscribe(onSigned);
      await sleep();

      const request = TestBed.inject(HttpTestingController).expectOne(
        `/rest/informatieobjecten/informatieobject/${enkelvoudigInformatieobject.uuid}/onderteken?zaak=${zaak.uuid}`,
      );
      expect(request.request.method).toBe("POST");
      request.flush(null);
      await sleep();

      expect(onSigned).toHaveBeenCalled();
    });
  });

  describe("actie.verwijderen", () => {
    const deleteUrl = `/rest/informatieobjecten/informatieobject/${enkelvoudigInformatieobject.uuid}`;

    let httpTestingController: HttpTestingController;
    let dialog: MatDialog;

    beforeEach(() => {
      httpTestingController = TestBed.inject(HttpTestingController);
      dialog = TestBed.inject(MatDialog);
      jest
        .spyOn(dialog, "open")
        .mockReturnValue(
          fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(false) }),
        );
    });

    describe("a document without a zaak", () => {
      it("does not delete it while the confirmation dialog is still open", () => {
        component.zaak = undefined;

        component["openDocumentVerwijderenDialog"]();

        httpTestingController.expectNone(deleteUrl);
      });
    });

    describe("a document belonging to a zaak", () => {
      it("reports a failing delete through the error handler", async () => {
        const foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
        const foutAfhandelen = jest
          .spyOn(foutAfhandelingService, "foutAfhandelen")
          .mockReturnValue(of());
        component.zaak = zaak;

        component["openDocumentVerwijderenDialog"]();
        const { callback } = jest.mocked(dialog.open).mock.calls.at(-1)![1]!
          .data as RedenDialogData;
        callback!("fakeReden").subscribe({ error: () => undefined });
        await new Promise(requestAnimationFrame);
        httpTestingController
          .expectOne(deleteUrl)
          .flush(null, { status: 500, statusText: "Server Error" });
        await new Promise(requestAnimationFrame);

        expect(foutAfhandelen).toHaveBeenCalled();
      });
    });
  });
});
