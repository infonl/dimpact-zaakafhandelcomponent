/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { HttpEventType, provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentRef, provideZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatDatepickerInputHarness } from "@angular/material/datepicker/testing";
import { MatFormFieldHarness } from "@angular/material/form-field/testing";
import { MatIconModule } from "@angular/material/icon";
import { MatInputHarness } from "@angular/material/input/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import {
  provideQueryClient,
  provideTanStackQuery,
} from "@tanstack/angular-query-experimental";
import { screen } from "@testing-library/angular";
import moment from "moment";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
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
  let httpTestingController: HttpTestingController;

  const mockSideNav = fromPartial<MatDrawer>({
    close: jest.fn(),
  });

  const enkelvoudigInformatieObjectVersieGegevens = fromPartial<
    GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">
  >({
    uuid: "123",
    titel: "Test Title",
    beschrijving: "Test Description",
    vertrouwelijkheidaanduiding: "INTERN",
    informatieobjectTypeUUID: "456",
    auteur: "Test Author",
    bestandsnaam: "Test File Name",
    formaat: "Test Format",
    taal: fromPartial<GeneratedType<"RestTaal">>({ naam: "Nederlands" }),
    ontvangstdatum: new Date().toDateString(),
    toelichting: "Test Explanation",
    verzenddatum: new Date().toDateString(),
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
      code: "dut",
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
      imports: [
        InformatieObjectEditComponent,
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
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTanStackQuery(testQueryClient),
        provideQueryClient(testQueryClient),
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
    testQueryClient.setQueryData(identityService.readLoggedInUser().queryKey, {
      id: "1234",
      naam: "Test User",
    });

    jest
      .spyOn(informatieObjectenService, "listInformatieobjecttypesForZaak")
      .mockReturnValue(of(mockInformatieObjectTypes));

    jest.spyOn(configuratieService, "listTalen").mockReturnValue(of(mockTalen));

    httpTestingController = TestBed.inject(HttpTestingController);

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
      const ensureQueryData = jest.spyOn(testQueryClient, "ensureQueryData");

      componentRef.setInput("infoObject", undefined);
      fixture.detectChanges();

      expect(ensureQueryData).not.toHaveBeenCalledWith(
        expect.objectContaining({
          queryKey: identityService.readLoggedInUser().queryKey,
        }),
      );
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

    it("should call `ensureQueryData` when no author is set on the infoObject", async () => {
      const ensureQueryData = jest.spyOn(testQueryClient, "ensureQueryData");

      componentRef.setInput("infoObject", {
        ...enkelvoudigInformatieObjectVersieGegevens,
        auteur: null,
      });
      fixture.detectChanges();
      await fixture.whenStable();

      expect(ensureQueryData).toHaveBeenCalledWith(
        expect.objectContaining({
          queryKey: identityService.readLoggedInUser().queryKey,
        }),
      );
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
    it("should update titel input when file is selected", () => {
      component["form"].controls.bestand.setValue(mockFile);
      fixture.detectChanges();

      expect(component["form"].controls.titel.value).toBe("test-file");
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
    const updateUrl = `/rest/informatieobjecten/informatieobject/${enkelvoudigInformatieObjectVersieGegevens.uuid}?zaak=test-zaak-uuid`;

    const mockFormInput = {
      bestand: mockFile,
      titel: "Test Title",
      beschrijving: "Test Description",
      taal: mockTalen[0],
      status: { label: "In bewerking", value: "IN_BEWERKING" },
      informatieobjectType: mockInformatieObjectTypes[0],
      vertrouwelijkheidaanduiding: {
        label: "vertrouwelijkheidaanduiding.INTERN",
        value: "INTERN",
      },
      auteur: "Test Author",
      toelichting: "Test Explanation",
    } satisfies Parameters<
      InformatieObjectEditComponent["form"]["patchValue"]
    >[0];

    const submitAndUpload = async () => {
      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.toevoegen" }),
      );
      await submitButton.click();
      await new Promise(requestAnimationFrame);

      return httpTestingController.expectOne(updateUrl);
    };

    beforeEach(() => {
      componentRef.setInput(
        "infoObject",
        enkelvoudigInformatieObjectVersieGegevens,
      );
      fixture.detectChanges();

      component["form"].patchValue(mockFormInput);
      component["form"].markAsDirty();
      fixture.detectChanges();
    });

    it("adds the new version with a PUT of the document naming its zaak, sending the file and the metadata", async () => {
      const request = await submitAndUpload();

      expect(request.request.method).toBe("PUT");
      const formData = request.request.body as FormData;
      expect(formData).toBeInstanceOf(FormData);
      const formDataObject = Object.fromEntries(formData.entries());
      expect(formDataObject.titel).toBe(mockFormInput.titel);
      expect(formDataObject.beschrijving).toBe(mockFormInput.beschrijving);
      expect(formDataObject.auteur).toBe(mockFormInput.auteur);
      expect(formDataObject.toelichting).toBe(mockFormInput.toelichting);
      expect(formDataObject.status).toBe("IN_BEWERKING");
      expect(formDataObject.bestandsnaam).toBe(mockFile.name);
      expect(formDataObject.file).toBeInstanceOf(File);
      expect(JSON.parse(formDataObject.taal as string)).toEqual(mockTalen[0]);
      expect(formDataObject).not.toHaveProperty("uuid");
      expect(formDataObject).not.toHaveProperty("zaakUuid");
    });

    it("sends the status even though the status control is disabled because the document has an ontvangstdatum", async () => {
      expect(component["form"].controls.status.disabled).toBe(true);

      const request = await submitAndUpload();

      const formDataObject = Object.fromEntries(
        (request.request.body as FormData).entries(),
      );
      expect(formDataObject.status).toBe("IN_BEWERKING");
    });

    it("emits the updated document after a successful update", async () => {
      const emitSpy = jest.spyOn(component["document"], "emit");
      const updatedDocument = fromPartial<
        GeneratedType<"RestEnkelvoudigInformatieobject">
      >({ uuid: "123", versie: 2 });

      const request = await submitAndUpload();
      request.flush(updatedDocument);
      await sleep();

      expect(emitSpy).toHaveBeenCalledWith(updatedDocument);
    });

    it("closes the side nav after a successful update", async () => {
      const request = await submitAndUpload();
      request.flush({});
      await sleep();

      expect(mockSideNav.close).toHaveBeenCalled();
    });

    it("disables the submit button while the new version is being uploaded", async () => {
      const submitButton = screen.getByRole("button", {
        name: /actie.toevoegen/,
      }) as HTMLButtonElement;
      expect(submitButton.disabled).toBe(false);

      await submitAndUpload();
      fixture.detectChanges();

      expect(submitButton.disabled).toBe(true);
    });
  });

  describe("Upload progress", () => {
    beforeEach(() => {
      componentRef.setInput(
        "infoObject",
        enkelvoudigInformatieObjectVersieGegevens,
      );
      fixture.detectChanges();

      component["form"].patchValue({
        bestand: mockFile,
        titel: "Test Title",
        taal: mockTalen[0],
        status: { label: "In bewerking", value: "IN_BEWERKING" },
        informatieobjectType: mockInformatieObjectTypes[0],
        vertrouwelijkheidaanduiding: {
          label: "vertrouwelijkheidaanduiding.INTERN",
          value: "INTERN",
        },
        auteur: "Test Author",
      });
      component["form"].markAsDirty();
      fixture.detectChanges();
    });

    const submitAndUpload = async () => {
      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.toevoegen" }),
      );
      await submitButton.click();
      await new Promise(requestAnimationFrame);

      return httpTestingController.expectOne(
        `/rest/informatieobjecten/informatieobject/${enkelvoudigInformatieObjectVersieGegevens.uuid}?zaak=test-zaak-uuid`,
      );
    };

    it("reports how much of the new version has been uploaded to the global progress indicator", async () => {
      const utilService = TestBed.inject(UtilService);
      const request = await submitAndUpload();

      expect(request.request.reportProgress).toBe(true);
      expect(utilService.progress()).toEqual({
        percentage: 0,
        description: "msg.document.uploaden.voortgang",
      });

      request.event({
        type: HttpEventType.UploadProgress,
        loaded: 40,
        total: 100,
      });
      await sleep();

      expect(utilService.progress()).toEqual({
        percentage: 40,
        description: "msg.document.uploaden.voortgang",
      });
    });

    it("stops reporting progress once the upload has finished", async () => {
      const utilService = TestBed.inject(UtilService);
      const request = await submitAndUpload();

      request.event({
        type: HttpEventType.UploadProgress,
        loaded: 100,
        total: 100,
      });
      request.flush({});
      await sleep();

      expect(utilService.progress()).toBeNull();
    });

    it("stops reporting progress when the new version is refused because it is too large", async () => {
      const utilService = TestBed.inject(UtilService);
      const request = await submitAndUpload();

      request.event({
        type: HttpEventType.UploadProgress,
        loaded: 40,
        total: 100,
      });
      request.flush(
        { message: "msg.error.file.size-exceeded" },
        { status: 413, statusText: "Payload Too Large" },
      );
      await sleep();

      expect(utilService.progress()).toBeNull();
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

  describe("Submit button behavior", () => {
    beforeEach(() => {
      componentRef.setInput(
        "infoObject",
        enkelvoudigInformatieObjectVersieGegevens,
      );
      fixture.detectChanges();
    });

    describe("when no changes are made", () => {
      it("should keep submit button disabled", async () => {
        fixture.detectChanges();

        const submitButton = await loader.getHarness(
          MatButtonHarness.with({ text: "actie.toevoegen" }),
        );

        expect(await submitButton.isDisabled()).toBe(true);
      });
    });

    describe("when file is uploaded", () => {
      it("should enable submit button when file is selected", async () => {
        component["form"].controls.bestand.setValue(mockFile);
        component["form"].controls.bestand.markAsDirty();
        fixture.detectChanges();

        const submitButton = await loader.getHarness(
          MatButtonHarness.with({ text: "actie.toevoegen" }),
        );

        expect(await submitButton.isDisabled()).toBe(false);
      });

      it("should disable submit button when file is removed", async () => {
        component["form"].controls.bestand.setValue(mockFile);
        component["form"].controls.bestand.markAsDirty();
        fixture.detectChanges();

        const submitButton = await loader.getHarness(
          MatButtonHarness.with({ text: "actie.toevoegen" }),
        );
        expect(await submitButton.isDisabled()).toBe(false);

        component["form"].controls.bestand.setValue(null);
        component["form"].markAsPristine();
        fixture.detectChanges();

        expect(await submitButton.isDisabled()).toBe(true);
      });
    });

    describe("when metadata is changed", () => {
      it("should enable submit button when title is changed", async () => {
        component["form"].controls.titel.setValue("New Title");
        component["form"].controls.titel.markAsDirty();
        component["form"].markAsDirty();
        fixture.detectChanges();

        const submitButton = await loader.getHarness(
          MatButtonHarness.with({ text: "actie.toevoegen" }),
        );

        expect(await submitButton.isDisabled()).toBe(false);
      });

      it("should enable submit button when description is changed", async () => {
        component["form"].controls.beschrijving.setValue("New Description");
        component["form"].controls.beschrijving.markAsDirty();
        component["form"].markAsDirty();
        fixture.detectChanges();

        const submitButton = await loader.getHarness(
          MatButtonHarness.with({ text: "actie.toevoegen" }),
        );

        expect(await submitButton.isDisabled()).toBe(false);
      });

      describe("when both file and metadata are changed", () => {
        it("should keep submit button enabled", async () => {
          component["form"].controls.bestand.setValue(mockFile);
          component["form"].controls.titel.setValue("New Title");
          component["form"].controls.titel.markAsDirty();
          component["form"].markAsDirty();
          fixture.detectChanges();

          const submitButton = await loader.getHarness(
            MatButtonHarness.with({ text: "actie.toevoegen" }),
          );

          expect(await submitButton.isDisabled()).toBe(false);
        });
      });

      describe("when form has validation errors", () => {
        it("should keep submit button disabled even with file selected", async () => {
          component["form"].controls.bestand.setValue(mockFile);
          component["form"].controls.titel.setValue("");
          component["form"].controls.titel.markAsDirty();
          component["form"].markAsDirty();
          fixture.detectChanges();

          const submitButton = await loader.getHarness(
            MatButtonHarness.with({ text: "actie.toevoegen" }),
          );

          expect(await submitButton.isDisabled()).toBe(true);
        });

        it("should keep submit button disabled even with metadata changed", async () => {
          component["form"].controls.beschrijving.setValue("New Description");
          component["form"].controls.titel.setValue("");
          component["form"].controls.titel.markAsDirty();
          component["form"].controls.beschrijving.markAsDirty();
          component["form"].markAsDirty();
          fixture.detectChanges();

          const submitButton = await loader.getHarness(
            MatButtonHarness.with({ text: "actie.toevoegen" }),
          );

          expect(await submitButton.isDisabled()).toBe(true);
        });
      });
    });
  });
});
