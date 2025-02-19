/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import {BrowserAnimationsModule, NoopAnimationsModule} from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { DocumentIconComponent } from "../../shared/document-icon/document-icon.component";
import { InformatieObjectIndicatiesComponent } from "../../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { InformatieObjectViewComponent } from "./informatie-object-view.component";

import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { By } from "@angular/platform-browser";
import { GeneratedType } from "../../shared/utils/generated-types";
import {Vertrouwelijkheidaanduiding} from "../model/vertrouwelijkheidaanduiding.enum";

describe(InformatieObjectViewComponent.name, () => {
  let component: InformatieObjectViewComponent;
  let fixture: ComponentFixture<typeof component>;

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
      informatieobjectTypeUUID: "test-uuid",
      indicaties: [],
      titel: "test informatieobject",
      vertrouwelijkheidaanduiding: Vertrouwelijkheidaanduiding.openbaar
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

    fixture = TestBed.createComponent(InformatieObjectViewComponent);
    component = fixture.componentInstance;
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should display the sidebar when 'actie.nieuwe.versie.toevoegen' is clicked", () => {
    component.activeSideAction = "actie.nieuwe.versie.toevoegen";
    fixture.detectChanges();

    const sidebar = fixture.debugElement.query(
      By.css("zac-informatie-object-edit"),
    );
    expect(sidebar).toBeTruthy();
  });
});
