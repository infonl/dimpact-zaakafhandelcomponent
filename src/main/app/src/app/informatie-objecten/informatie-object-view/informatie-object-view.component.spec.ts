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
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatNavListItemHarness } from "@angular/material/list/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of, ReplaySubject } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { IdentityService } from "../../identity/identity.service";
import { DocumentIconComponent } from "../../shared/document-icon/document-icon.component";
import { InformatieObjectIndicatiesComponent } from "../../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { RedenDialogData } from "../../shared/dialog/reden-dialog-form/reden-dialog-form.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { InformatieObjectEditComponent } from "../informatie-object-edit/informatie-object-edit.component";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { FileFormat } from "../model/file-format";
import { InformatieObjectViewComponent } from "./informatie-object-view.component";

describe(InformatieObjectViewComponent.name, () => {
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
        InformatieObjectEditComponent,
        SideNavComponent,
        StaticTextComponent,
        MaterialModule,
        InformatieObjectIndicatiesComponent,
        TranslateModule.forRoot(),
        VertrouwelijkaanduidingToTranslationKeyPipe,
        DocumentIconComponent,
        PipesModule,
        MaterialFormBuilderModule,
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
        VertrouwelijkaanduidingToTranslationKeyPipe,
      ],
    }).compileComponents();

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

    const identityService = TestBed.inject(IdentityService);
    testQueryClient.setQueryData(identityService.readLoggedInUser().queryKey, {
      id: "1234",
      naam: "Test User",
    });

    const configuratieService = TestBed.inject(ConfiguratieService);
    jest.spyOn(configuratieService, "listTalen").mockReturnValue(of([]));

    const foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
    jest
      .spyOn(foutAfhandelingService, "httpErrorAfhandelen")
      .mockReturnValue(of());

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
              toevoegenNieuweVersie: false,
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
              toevoegenNieuweVersie: true,
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
              converteren: true,
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
              converteren: false,
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
              converteren: true,
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
              ontgrendelen: true,
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
              ontgrendelen: false,
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

    it("should call unlockInformatieObject with zaakUuid when clicked and a zaak is present", async () => {
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            gelockedDoor: { id: "user-001", naam: "Test User" },
            rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({
              ontgrendelen: true,
            }),
          }),
        );
      const unlockSpy = jest
        .spyOn(informatieObjectenService, "unlockInformatieObject")
        .mockReturnValue(of({}));
      mockActivatedRoute.data.next({
        informatieObject: enkelvoudigInformatieobject,
      });

      const button = await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.unlock" }),
      );
      await button.click();

      expect(unlockSpy).toHaveBeenCalledWith(
        enkelvoudigInformatieobject.uuid,
        zaak.uuid,
      );
    });

    it("should call unlockInformatieObject without zaakUuid when clicked and no zaak is present", async () => {
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            gelockedDoor: { id: "user-001", naam: "Test User" },
            rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({
              ontgrendelen: true,
            }),
          }),
        );
      const unlockSpy = jest
        .spyOn(informatieObjectenService, "unlockInformatieObject")
        .mockReturnValue(of({}));
      jest
        .spyOn(informatieObjectenService, "listZaakInformatieobjecten")
        .mockReturnValue(of([]));
      mockActivatedRoute.data.next({
        informatieObject: enkelvoudigInformatieobject,
      });

      const button = await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.unlock" }),
      );
      await button.click();

      expect(unlockSpy).toHaveBeenCalledWith(
        enkelvoudigInformatieobject.uuid,
        undefined,
      );
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
