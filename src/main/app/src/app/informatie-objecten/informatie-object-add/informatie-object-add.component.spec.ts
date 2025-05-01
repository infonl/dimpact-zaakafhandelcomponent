/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, fakeAsync, TestBed } from "@angular/core/testing";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatIconModule } from "@angular/material/icon";
import { MatDrawer } from "@angular/material/sidenav";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { FormComponent } from "src/app/shared/material-form-builder/form/form/form.component";
import { IdentityService } from "../../identity/identity.service";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { InformatieObjectAddComponent } from "./informatie-object-add.component";
import { Taak } from "src/app/taken/model/taak";

describe(InformatieObjectAddComponent.name, () => {
  let component: InformatieObjectAddComponent;
  let fixture: ComponentFixture<typeof component>;
  let identityService: IdentityService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [InformatieObjectAddComponent, FormComponent],
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

    fixture = TestBed.createComponent(InformatieObjectAddComponent);
    fixture.detectChanges();

    component = fixture.componentInstance;
    identityService = TestBed.inject(IdentityService);

    jest
      .spyOn(identityService, "readLoggedInUser")
      .mockReturnValue(of({ id: "1234", naam: "Test User" }));
  });

  describe("When the form is submitted", () => {
    it("should cap the title to 100 characters", fakeAsync(() => {
      const longFileName = "a".repeat(110) + ".pdf";
      const mockFile = new File(["dummy content"], longFileName, {
        type: "application/pdf",
      });

      component.fields$.subscribe(() => {
        fixture.detectChanges();

        const bestandControl = component.form.formGroup.get("bestand");
        const titelControl = component.form.formGroup.get("titel");

        expect(bestandControl).toBeDefined();
        expect(titelControl).toBeDefined();

        bestandControl?.setValue(mockFile);

        const expectedTitle = longFileName
          .replace(/\.[^/.]+$/, "")
          .substring(0, 100);

        expect(titelControl?.value).toBe(expectedTitle);
        expect(titelControl?.value.length).toBeLessThanOrEqual(100);
      });
    }));

    it("should return taak's zaakUuid when zaak is not set", () => {
      component.zaak = undefined;
      component.taak = { zaakUuid: "expected-uuid", id: "t-id" } as Taak;

      expect(component["getZaakUuid"]()).toBe("expected-uuid");
    });
  });
});
