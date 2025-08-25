/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentRef } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatDatepickerInputHarness } from "@angular/material/datepicker/testing";
import { MatFormFieldHarness } from "@angular/material/form-field/testing";
import { MatIconModule } from "@angular/material/icon";
import { MatInputHarness } from "@angular/material/input/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import moment from "moment";
import { of } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { IdentityService } from "../../identity/identity.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { Vertrouwelijkheidaanduiding } from "../model/vertrouwelijkheidaanduiding.enum";
import { InformatieObjectEditComponent } from "./informatie-object-edit.component";

describe(InformatieObjectEditComponent.name, () => {
  let component: InformatieObjectEditComponent;
  let componentRef: ComponentRef<InformatieObjectEditComponent>;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;
  let identityService: IdentityService;
  let informatieObjectenService: InformatieObjectenService;
  let configuratieService: ConfiguratieService;
  let translateService: TranslateService;

  const mockSideNav = fromPartial<MatDrawer>({
    close: jest.fn(),
  });

  const enkelvoudigInformatieObjectVersieGegevens = fromPartial<
    GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">
  >({
    uuid: "123",
    titel: "Test Title",
    beschrijving: "Test Description",
    vertrouwelijkheidaanduiding: Vertrouwelijkheidaanduiding.intern,
    informatieobjectTypeUUID: "456",
    auteur: "Test Author",
    bestandsnaam: "Test File Name",
    formaat: "Test Format",
    taal: fromPartial<GeneratedType<"RestTaal">>({ naam: "Nederlands" }),
    ontvangstdatum: new Date().toDateString(),
    toelichting: "Test Explanation",
    verzenddatum: new Date().toDateString(),
    zaakUuid: "789",
    status: "IN_BEWERKING",
    file: "file",
  });

  const mockInformatieObjectTypes = [
    fromPartial<GeneratedType<"RestInformatieobjecttype">>({
      uuid: "456",
      omschrijving: "Test Type",
    }),
  ];

  const mockTalen = [
    fromPartial<GeneratedType<"RestTaal">>({
      id: "nl",
      naam: "Nederlands",
      code: "nl",
      name: "Dutch",
      local: "Nederlands",
    }),
  ];

  const mockFile = new File(["test content"], "test-file.txt", {
    type: "text/plain",
  });

  // This test is sometimes cheating slightly in order to fill the form (`component['form']....`)
  // instead of setting the fields properly through the `TestbedHarnessEnvironment`.
  // This form contains a file field which makes it (nearly) impossible to test properly
  // As a refactor, it would be nice to have a custom method to fill the form (via the UI elements)
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [InformatieObjectEditComponent],
      imports: [
        FormsModule,
        ReactiveFormsModule,
        MatIconModule,
        MaterialModule,
        TranslateModule.forRoot(),
        VertrouwelijkaanduidingToTranslationKeyPipe,
        MaterialFormBuilderModule,
        NoopAnimationsModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: MatDrawer,
          useValue: mockSideNav,
        },
        VertrouwelijkaanduidingToTranslationKeyPipe,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(InformatieObjectEditComponent);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;
    loader = TestbedHarnessEnvironment.loader(fixture);

    identityService = TestBed.inject(IdentityService);
    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    configuratieService = TestBed.inject(ConfiguratieService);
    translateService = TestBed.inject(TranslateService);

    // Mock services
    jest
      .spyOn(identityService, "readLoggedInUser")
      .mockReturnValue(of({ id: "1234", naam: "Test User" }));

    jest
      .spyOn(informatieObjectenService, "listInformatieobjecttypesForZaak")
      .mockReturnValue(of(mockInformatieObjectTypes));

    jest.spyOn(configuratieService, "listTalen").mockReturnValue(of(mockTalen));

    jest
      .spyOn(informatieObjectenService, "updateEnkelvoudigInformatieobject")
      .mockReturnValue(
        of(fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({})),
      );

    jest
      .spyOn(translateService, "instant")
      .mockImplementation((key: string | string[]) =>
        typeof key === "string" ? key : key[0],
      );

    // Set required inputs
    componentRef.setInput("sideNav", mockSideNav);
    componentRef.setInput("zaakUuid", "test-zaak-uuid");

    fixture.detectChanges();
  });

  describe("Component initialization", () => {
    it("should create", () => {
      expect(component).toBeTruthy();
    });

    it("should display form fields", async () => {
      const formFields = await loader.getAllHarnesses(MatFormFieldHarness);
      expect(formFields.length).toBeGreaterThan(0);
    });

    it("should display submit and cancel buttons", async () => {
      const buttons = await loader.getAllHarnesses(MatButtonHarness);
      expect(buttons.length).toBeGreaterThanOrEqual(2);
    });
  });

  describe("when no `infoObject` is present", () => {
    it("should not call `identityService.readLoggedInUser`", () => {
      const readLoggedInUser = jest.spyOn(identityService, "readLoggedInUser");

      componentRef.setInput("infoObject", undefined);
      fixture.detectChanges();

      expect(readLoggedInUser).not.toHaveBeenCalled();
    });

    it("should display empty form fields", async () => {
      componentRef.setInput("infoObject", undefined);
      fixture.detectChanges();

      const inputs = await loader.getAllHarnesses(MatInputHarness);
      for (const input of inputs) {
        const value = await input.getValue();
        expect(value).toBe("");
      }
    });
  });

  describe("when an `infoObject` is passed", () => {
    beforeEach(() => {
      componentRef.setInput(
        "infoObject",
        enkelvoudigInformatieObjectVersieGegevens,
      );
      fixture.detectChanges();
    });

    it("should call `identityService.readLoggedInUser`", () => {
      const readLoggedInUser = jest.spyOn(identityService, "readLoggedInUser");

      componentRef.setInput(
        "infoObject",
        enkelvoudigInformatieObjectVersieGegevens,
      );

      expect(readLoggedInUser).toHaveBeenCalled();
    });

    it("should load informatieobject types", () => {
      const listInformatieObjectTypes = jest.spyOn(
        informatieObjectenService,
        "listInformatieobjecttypesForZaak",
      );

      expect(listInformatieObjectTypes).toHaveBeenCalledWith("test-zaak-uuid");
    });
  });

  describe("Form interactions", () => {
    it.skip("should update titel input when file is selected", async () => {
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(mockFile);

      const inputDebugEl = fixture.debugElement.query(
        By.css("input[type=file]"),
      );
      inputDebugEl.nativeElement.files = dataTransfer.files;
      inputDebugEl.nativeElement.dispatchEvent(new InputEvent("change"));

      fixture.detectChanges();

      // Check that the titel input is updated
      const titleInput = await loader.getHarness(
        MatInputHarness.with({ placeholder: "titel" }),
      );

      const value = await titleInput.getValue();
      expect(value).toBe("test-file");
    });

    it("should disable verzenddatum and status when ontvangstdatum is set", async () => {
      const ontvangstdatum = moment();

      const [ontvangstdatumInput, verzenddatumInput] =
        await loader.getAllHarnesses(MatDatepickerInputHarness);
      await ontvangstdatumInput.setValue(ontvangstdatum.format("YYYY-MM-DD"));

      expect(await verzenddatumInput.isDisabled()).toBe(true);
    });
  });

  describe("Submit", () => {
    beforeEach(() => {
      componentRef.setInput(
        "infoObject",
        enkelvoudigInformatieObjectVersieGegevens,
      );
      fixture.detectChanges();
    });

    it("should call updateEnkelvoudigInformatieobject when submit button is clicked", async () => {
      const updateSpy = jest.spyOn(
        informatieObjectenService,
        "updateEnkelvoudigInformatieobject",
      );

      component["form"].patchValue({
        bestand: mockFile,
        titel: "Test Title",
        beschrijving: "Test Description",
        taal: mockTalen[0],
        status: { label: "In bewerking", value: "IN_BEWERKING" },
        informatieobjectType: mockInformatieObjectTypes[0],
        vertrouwelijkheidaanduiding: { label: "Intern", value: "intern" },
        auteur: "Test Author",
        toelichting: "Test Explanation",
      });

      fixture.detectChanges();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.toevoegen" }),
      );

      await submitButton.click();
      expect(updateSpy).toHaveBeenCalledWith(
        "123",
        "test-zaak-uuid",
        expect.objectContaining({
          titel: "Test Title",
          beschrijving: "Test Description",
          auteur: "Test Author",
          toelichting: "Test Explanation",
        }),
      );
    });

    it("should emit document event after successful update", async () => {
      const emitSpy = jest.spyOn(component.document, "emit");

      component["form"].patchValue({
        bestand: mockFile,
        titel: "Test Title",
        taal: mockTalen[0],
        status: { label: "In bewerking", value: "IN_BEWERKING" },
        informatieobjectType: mockInformatieObjectTypes[0],
        vertrouwelijkheidaanduiding: { label: "Intern", value: "intern" },
        auteur: "Test Author",
      });

      fixture.detectChanges();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.toevoegen" }),
      );

      if (submitButton) {
        await submitButton.click();
        expect(emitSpy).toHaveBeenCalled();
      }
    });

    it("should close side nav after successful update", async () => {
      component["form"].patchValue({
        bestand: mockFile,
        titel: "Test Title",
        taal: mockTalen[0],
        status: { label: "In bewerking", value: "IN_BEWERKING" },
        informatieobjectType: mockInformatieObjectTypes[0],
        vertrouwelijkheidaanduiding: { label: "Intern", value: "intern" },
        auteur: "Test Author",
      });

      fixture.detectChanges();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.toevoegen" }),
      );

      await submitButton.click();
      expect(mockSideNav.close).toHaveBeenCalled();
    });
  });

  describe("Reset", () => {
    it("should reset form and close side nav when cancel button is clicked", async () => {
      component["form"].patchValue({
        bestand: mockFile,
        titel: "Test Title",
      });

      fixture.detectChanges();

      // Find and click cancel button
      const cancelButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.annuleren" }),
      );

      await cancelButton.click();
      expect(mockSideNav.close).toHaveBeenCalled();
    });
  });
});
