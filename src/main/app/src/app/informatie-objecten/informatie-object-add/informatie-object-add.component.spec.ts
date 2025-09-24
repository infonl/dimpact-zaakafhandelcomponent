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
import { InformatieObjectAddComponent } from "./informatie-object-add.component";

describe(InformatieObjectAddComponent.name, () => {
  let component: InformatieObjectAddComponent;
  let componentRef: ComponentRef<InformatieObjectAddComponent>;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;
  let identityService: IdentityService;
  let informatieObjectenService: InformatieObjectenService;
  let configuratieService: ConfiguratieService;
  let translateService: TranslateService;

  const mockSideNav = fromPartial<MatDrawer>({
    close: jest.fn(),
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

  const mockZaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "b7e8f9a2-4c3d-11ee-bb2f-0242ac130003",
  });

  const mockTaak = fromPartial<GeneratedType<"RestTask">>({
    id: "task-1234",
    zaakUuid: "b7e8f9a2-4c3d-11ee-bb2f-0242ac130003",
  });

  // This test is sometimes cheating slightly in order to fill the form (`component['form']....`)
  // instead of setting the fields properly through the `TestbedHarnessEnvironment`.
  // This form contains a file field which makes it (nearly) impossible to test properly
  // As a refactor, it would be nice to have a custom method to fill the form (via the UI elements)
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [InformatieObjectAddComponent],
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

    fixture = TestBed.createComponent(InformatieObjectAddComponent);
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

    // jest.spyOn(configuratieService, "listTalen").mockReturnValue(of(mockTalen));

    jest
      .spyOn(informatieObjectenService, "createEnkelvoudigInformatieobject")
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
    componentRef.setInput("zaak", mockZaak);

    fixture.detectChanges();
  });

  describe("Component initialization", () => {
    it("should create", () => {
      expect(component).toBeTruthy();
    });

    it("should display form fields", async () => {
      const formFields = await loader.getAllHarnesses(MatFormFieldHarness);
      expect(formFields.length).toBeGreaterThanOrEqual(11);
    });

    it("should display submit and cancel buttons", async () => {
      const buttons = await loader.getAllHarnesses(MatButtonHarness);
      expect(buttons.length).toBeGreaterThanOrEqual(2);
    });

    it("should load informatieobject types", () => {
      const listInformatieObjectTypes = jest.spyOn(
        informatieObjectenService,
        "listInformatieobjecttypesForZaak",
      );
      expect(listInformatieObjectTypes).toHaveBeenCalledWith(mockZaak.uuid);
    });

    it("should call `identityService.readLoggedInUser`", () => {
      const readLoggedInUser = jest.spyOn(identityService, "readLoggedInUser");
      const readDefaultTaal = jest.spyOn(
        configuratieService,
        "readDefaultTaal",
      );

      component.ngOnInit();
      expect(readDefaultTaal).toHaveBeenCalled();
      expect(readLoggedInUser).toHaveBeenCalled();
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

    it("should have creatiedatum prefilled with todays and disable verzenddatum and status when ontvangstdatum is set", async () => {
      const ontvangstdatum = moment();

      const [creatiedatum, ontvangstdatumInput, verzenddatumInput] =
        await loader.getAllHarnesses(MatDatepickerInputHarness);

      expect(await creatiedatum.getValue()).toBe(moment().format("YYYY-MM-DD"));

      await ontvangstdatumInput.setValue(ontvangstdatum.format("YYYY-MM-DD"));
      expect(await verzenddatumInput.isDisabled()).toBe(true);
    });
  });

  describe("Submit adding document to a Zaak", () => {
    beforeEach(() => {
      componentRef.setInput("zaak", mockZaak);
      fixture.detectChanges();
    });

    it("should call createEnkelvoudigInformatieobject when submit button is clicked", async () => {
      const createSpy = jest.spyOn(
        informatieObjectenService,
        "createEnkelvoudigInformatieobject",
      );
      const creatiedatum = moment("2025-09-24T11:59:23.111Z");
      const verzenddatum = moment("2025-09-24T11:59:23.333Z");

      component["form"].patchValue({
        bestand: mockFile,
        titel: "Test Title",
        beschrijving: "Test Description",
        taal: mockTalen[0],
        status: { label: "In bewerking", value: "IN_BEWERKING" },
        informatieobjectType: mockInformatieObjectTypes[0],
        vertrouwelijkheidaanduiding: { label: "Intern", value: "intern" },
        auteur: "Test Author",
        creatiedatum,
        verzenddatum,
      });

      fixture.detectChanges();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.toevoegen" }),
      );

      await submitButton.click();
      expect(createSpy).toHaveBeenCalledWith(
        mockZaak.uuid,
        mockZaak.uuid,
        {
          bestand: mockFile,
          titel: "Test Title",
          formaat: "text/plain",
          beschrijving: "Test Description",
          status: "IN_BEWERKING",
          informatieobjectTypeUUID: mockInformatieObjectTypes[0].uuid,
          bestandsnaam: "test-file.txt",
          creatiedatum: "2025-09-24T11:59:23.111Z",
          ontvangstdatum: undefined,
          verzenddatum: "2025-09-24T11:59:23.333Z",
          vertrouwelijkheidaanduiding: "intern",
          taal: mockTalen[0].code,
          auteur: "Test Author",
        },
        false,
      );
    });

    it("should emit document event after form add and submit", async () => {
      const emitSpy = jest.spyOn(component.document, "emit");

      component["form"].patchValue({
        bestand: mockFile,
        titel: "Test Title",
        beschrijving: "Test Description",
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
      expect(emitSpy).toHaveBeenCalled();
    });

    it("should close side nav after successful add", async () => {
      component["form"].patchValue({
        bestand: mockFile,
        titel: "Test Title",
        beschrijving: "Test Description",
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
    it("should close side nav when cancel button is clicked", async () => {
      component["form"].patchValue({
        bestand: mockFile,
        titel: "Test Title",
      });

      fixture.detectChanges();

      const cancelButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.annuleren" }),
      );

      await cancelButton.click();
      expect(mockSideNav.close).toHaveBeenCalled();
    });
  });

  describe("Submit adding document to a Taak", () => {
    beforeEach(() => {
      componentRef.setInput("taak", mockTaak);
      fixture.detectChanges();
    });

    it("should call createEnkelvoudigInformatieobject when submit button is clicked", async () => {
      const createSpy = jest.spyOn(
        informatieObjectenService,
        "createEnkelvoudigInformatieobject",
      );
      const creatiedatum = moment("2025-09-24T11:59:23.111Z");
      const verzenddatum = moment("2025-09-24T11:59:23.333Z");

      component["form"].patchValue({
        bestand: mockFile,
        titel: "Test Title",
        beschrijving: "Test Description",
        taal: mockTalen[0],
        status: { label: "In bewerking", value: "IN_BEWERKING" },
        informatieobjectType: mockInformatieObjectTypes[0],
        vertrouwelijkheidaanduiding: { label: "Intern", value: "intern" },
        auteur: "Test Author",
        creatiedatum,
        verzenddatum,
      });

      fixture.detectChanges();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.toevoegen" }),
      );

      await submitButton.click();
      expect(createSpy).toHaveBeenCalledWith(
        mockTaak.zaakUuid,
        mockTaak.id,
        {
          bestand: mockFile,
          titel: "Test Title",
          formaat: "text/plain",
          beschrijving: "Test Description",
          status: "IN_BEWERKING",
          informatieobjectTypeUUID: mockInformatieObjectTypes[0].uuid,
          bestandsnaam: "test-file.txt",
          creatiedatum: "2025-09-24T11:59:23.111Z",
          ontvangstdatum: undefined,
          verzenddatum: "2025-09-24T11:59:23.333Z",
          vertrouwelijkheidaanduiding: "intern",
          taal: mockTalen[0].code,
          auteur: "Test Author",
        },
        true,
      );
    });

    it("should emit document event after form add and submit", async () => {
      const emitSpy = jest.spyOn(component.document, "emit");

      component["form"].patchValue({
        bestand: mockFile,
        titel: "Test Title",
        beschrijving: "Test Description",
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
      expect(emitSpy).toHaveBeenCalled();
    });

    it("should close side nav after successful add", async () => {
      component["form"].patchValue({
        bestand: mockFile,
        titel: "Test Title",
        beschrijving: "Test Description",
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

  describe("Adding multiple documents to a Zaak", () => {
    beforeEach(() => {
      componentRef.setInput("zaak", mockZaak);
      fixture.detectChanges();
    });

    it("should call createEnkelvoudigInformatieobject when submit button is clicked", async () => {
      const createSpy = jest.spyOn(
        informatieObjectenService,
        "createEnkelvoudigInformatieobject",
      );
      const emitSpy = jest.spyOn(component.document, "emit");
      const creatiedatum = moment("2025-09-24T11:59:23.111Z");
      const verzenddatum = moment("2025-09-24T11:59:23.333Z");

      component["form"].patchValue({
        bestand: mockFile,
        titel: "Test Title",
        beschrijving: "Test Description",
        taal: mockTalen[0],
        status: { label: "In bewerking", value: "IN_BEWERKING" },
        informatieobjectType: mockInformatieObjectTypes[0],
        vertrouwelijkheidaanduiding: { label: "Intern", value: "intern" },
        auteur: "Test Author",
        creatiedatum,
        verzenddatum,
        addOtherInfoObject: true,
      });

      fixture.detectChanges();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.toevoegen" }),
      );

      await submitButton.click();
      expect(createSpy).toHaveBeenCalledWith(
        mockZaak.uuid,
        mockZaak.uuid,
        {
          bestand: mockFile,
          titel: "Test Title",
          formaat: "text/plain",
          beschrijving: "Test Description",
          status: "IN_BEWERKING",
          informatieobjectTypeUUID: mockInformatieObjectTypes[0].uuid,
          bestandsnaam: "test-file.txt",
          creatiedatum: "2025-09-24T11:59:23.111Z",
          ontvangstdatum: undefined,
          verzenddatum: "2025-09-24T11:59:23.333Z",
          vertrouwelijkheidaanduiding: "intern",
          taal: mockTalen[0].code,
          auteur: "Test Author",
        },
        false,
      );

      fixture.detectChanges();

      expect(emitSpy).toHaveBeenCalled();
      expect(mockSideNav.close).not.toHaveBeenCalled();

      expect(component["form"].pristine).toBe(true); // form pristine after adding document
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { creatiedatum: _ignoredCreatiedatum, ...objectToAssert } =
        component["form"].value;
      expect(objectToAssert).toEqual({
        bestand: null,
        titel: null,
        beschrijving: null,
        status: null,
        vertrouwelijkheidaanduiding: null,
        verzenddatum: null,
        informatieobjectType: null,
        ontvangstdatum: null,
        taal: null,
        auteur: "Test User",
        // creatiedatum will always be unequal since form sets its own moment() instance; so not validating this value here
        addOtherInfoObject: true,
      });
    });
  });
});
