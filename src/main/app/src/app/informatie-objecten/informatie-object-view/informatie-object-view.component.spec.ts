/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { DocumentIconComponent } from "../../shared/document-icon/document-icon.component";
import { InformatieObjectIndicatiesComponent } from "../../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { InformatieObjectViewComponent } from "./informatie-object-view.component";
import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { MatNavListItemHarness } from "@angular/material/list/testing";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { Vertrouwelijkheidaanduiding } from "../model/vertrouwelijkheidaanduiding.enum";

describe(InformatieObjectViewComponent.name, () => {
  let component: InformatieObjectViewComponent;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;

  let informatieObjectenService: InformatieObjectenService;

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
    };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [
        InformatieObjectViewComponent,
        SideNavComponent,
        StaticTextComponent,
      ],
      imports: [
        MaterialModule,
        NoopAnimationsModule,
        InformatieObjectIndicatiesComponent,
        TranslateModule.forRoot(),
        VertrouwelijkaanduidingToTranslationKeyPipe,
        DocumentIconComponent,
        PipesModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({ zaak, informatieObject: enkelvoudigInformatieobject }),
          },
        },
        VertrouwelijkaanduidingToTranslationKeyPipe,
      ],
    }).compileComponents();

    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    jest
      .spyOn(informatieObjectenService, "readEnkelvoudigInformatieobject")
      .mockReturnValue(of(enkelvoudigInformatieobject));

    fixture = TestBed.createComponent(InformatieObjectViewComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
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
});
