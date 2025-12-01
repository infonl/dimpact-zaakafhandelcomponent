/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatNavListItemHarness } from "@angular/material/list/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of, ReplaySubject } from "rxjs";
import { testQueryClient } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { IdentityService } from "../../identity/identity.service";
import { DocumentIconComponent } from "../../shared/document-icon/document-icon.component";
import { InformatieObjectIndicatiesComponent } from "../../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectEditComponent } from "../informatie-object-edit/informatie-object-edit.component";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { FileFormat } from "../model/file-format";
import { Vertrouwelijkheidaanduiding } from "../model/vertrouwelijkheidaanduiding.enum";
import { InformatieObjectViewComponent } from "./informatie-object-view.component";

describe(InformatieObjectViewComponent.name, () => {
  let component: InformatieObjectViewComponent;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;

  let informatieObjectenService: InformatieObjectenService;

  const mockActivatedRoute = {
    data: new ReplaySubject<{
      zaak: GeneratedType<"RestZaak">;
      informatieObject: GeneratedType<"RestEnkelvoudigInformatieobject">;
    }>(1),
  };

  const zaak: GeneratedType<"RestZaak"> = {
    uuid: "zaak-001",
    identificatie: "test",
    indicaties: [],
    omschrijving: "test omschrijving",
    vertrouwelijkheidaanduiding: Vertrouwelijkheidaanduiding.openbaar,
    rechten: {},
    zaaktype: {
      uuid: "zaaktype-001",
    },
  };

  const enkelvoudigInformatieobject: GeneratedType<"RestEnkelvoudigInformatieobject"> =
    {
      uuid: "enkelvoudig-informatieobject-001",
      informatieobjectTypeUUID: "test-uuid",
      indicaties: [],
      titel: "test informatieobject",
      vertrouwelijkheidaanduiding: Vertrouwelijkheidaanduiding.openbaar,
      rechten: {},
      formaat: FileFormat.DOCX,
    };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [
        InformatieObjectViewComponent,
        SideNavComponent,
        StaticTextComponent,
        InformatieObjectEditComponent,
      ],
      imports: [
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
          vertrouwelijkheidaanduiding: Vertrouwelijkheidaanduiding.openbaar,
          rechten: {},
        }),
      );

    const identityService = TestBed.inject(IdentityService);
    testQueryClient.setQueryData(identityService.readLoggedInUser().queryKey, {
      id: "1234",
      naam: "Test User",
    });

    const configuratieService = TestBed.inject(ConfiguratieService);
    jest.spyOn(configuratieService, "listTalen").mockReturnValue(of([]));

    fixture = TestBed.createComponent(InformatieObjectViewComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);

    mockActivatedRoute.data.next({
      zaak,
      informatieObject: enkelvoudigInformatieobject,
    });
  });

  describe("actie.nieuwe.versie.toevoegen", () => {
    it("should not have a button when the user does not have the right to add a new version", async () => {
      jest
        .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
        .mockReturnValue(
          of({
            ...enkelvoudigInformatieobject,
            rechten: {
              toevoegenNieuweVersie: false,
            },
          }),
        );

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
            rechten: {
              toevoegenNieuweVersie: true,
            },
          }),
        );

      const button = await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.nieuwe.versie.toevoegen" }),
      );
      const host = await button.host();
      await host.click();

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
            rechten: {
              converteren: true,
            },
          }),
        );

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
            rechten: {
              converteren: false,
            },
          }),
        );

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
            rechten: {
              converteren: true,
            },
          }),
        );
      mockActivatedRoute.data.next({
        zaak,
        informatieObject: {
          ...enkelvoudigInformatieobject,
          formaat: FileFormat.TEXT,
        },
      });
      fixture.detectChanges();
      await fixture.whenStable();

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({ title: "actie.converteren" }),
      );

      expect(button).toBeNull();
    });
  });
});
