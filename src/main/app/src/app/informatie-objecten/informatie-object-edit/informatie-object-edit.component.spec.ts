/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatIconModule } from "@angular/material/icon";
import { MatDrawer } from "@angular/material/sidenav";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { FormComponent } from "src/app/shared/material-form-builder/form/form/form.component";
import { updateComponentInputs } from "../../../test-helpers";
import { IdentityService } from "../../identity/identity.service";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { Vertrouwelijkheidaanduiding } from "../model/vertrouwelijkheidaanduiding.enum";
import { InformatieObjectEditComponent } from "./informatie-object-edit.component";

describe(InformatieObjectEditComponent.name, () => {
  let component: InformatieObjectEditComponent;
  let fixture: ComponentFixture<typeof component>;
  let identityService: IdentityService;

  const enkelvoudigInformatieObjectVersieGegevens: GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens"> =
    {
      uuid: "123",
      titel: "Test Title",
      beschrijving: "Test Description",
      vertrouwelijkheidaanduiding: Vertrouwelijkheidaanduiding.intern,
      informatieobjectTypeUUID: "456",
      auteur: "Test Author",
      bestandsnaam: "Test File Name",
      formaat: "Test Format",
      taal: fromPartial<GeneratedType<"RestTaal">>({}),
      ontvangstdatum: new Date().toDateString(),
      toelichting: "Test Explanation",
      verzenddatum: new Date().toDateString(),
      zaakUuid: "789",
      status: "IN_BEWERKING",
      file: "file",
    };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [InformatieObjectEditComponent, FormComponent],
      imports: [
        FormsModule,
        ReactiveFormsModule,
        MatIconModule,
        TranslateModule.forRoot(),
        VertrouwelijkaanduidingToTranslationKeyPipe,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        MatDrawer,
        VertrouwelijkaanduidingToTranslationKeyPipe,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(InformatieObjectEditComponent);
    fixture.detectChanges();

    component = fixture.componentInstance;
    identityService = TestBed.inject(IdentityService);

    jest
      .spyOn(identityService, "readLoggedInUser")
      .mockReturnValue(of({ id: "1234", naam: "Test User" }));
  });

  describe("when no `infoObject` is present", () => {
    it("should not build the form", () => {
      updateComponentInputs(component, { infoObject: null });

      expect(component.fields).toBeDefined();
      expect(component.fields.length).toBe(0);
    });

    it("should not call `identityService.readLoggedInUser`", () => {
      const readLoggedInUser = jest.spyOn(identityService, "readLoggedInUser");
      updateComponentInputs(component, { infoObject: null });

      expect(readLoggedInUser).not.toHaveBeenCalled();
    });
  });

  describe("when an `infoObject` is passed", () => {
    it("should build the form", () => {
      updateComponentInputs(component, {
        infoObject: enkelvoudigInformatieObjectVersieGegevens,
      });

      expect(component.fields.length).toBe(8);
    });

    it("should call `identityService.readLoggedInUser`", async () => {
      const readLoggedInUser = jest.spyOn(identityService, "readLoggedInUser");

      updateComponentInputs(component, { infoObject: null });

      expect(readLoggedInUser).not.toHaveBeenCalled();
    });
  });
});
