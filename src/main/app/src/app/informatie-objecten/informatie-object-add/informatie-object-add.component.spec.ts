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
import { ComponentRef } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatDatepickerInputHarness } from "@angular/material/datepicker/testing";
import { MatFormFieldHarness } from "@angular/material/form-field/testing";
import { MatIconModule } from "@angular/material/icon";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideQueryClient,
  provideTanStackQuery,
} from "@tanstack/angular-query-experimental";
import { fromPartial } from "@total-typescript/shoehorn";
import moment from "moment";
import { of } from "rxjs";
import { sleep, testQueryClient } from "../../../../setupJest";
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
  let httpTestingController: HttpTestingController;

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

  const mockLoggedInUser = fromPartial<GeneratedType<"RestUser">>({
    id: "1234",
    naam: "Test User",
  });

  const mockFormInput = {
    bestand: mockFile,
    titel: "Test Title",
    beschrijving: "Test Description",
    status: { label: "In bewerking", value: "IN_BEWERKING" },
    informatieobjectType: mockInformatieObjectTypes[0],
    creatiedatum: moment("2025-09-24T11:59:23.111Z"),
    ontvangstdatum: null,
    verzenddatum: moment("2025-09-24T11:59:23.333Z"),
    vertrouwelijkheidaanduiding: { label: "Intern", value: "intern" },
    taal: mockTalen[0],
    auteur: "Test Author",
  } satisfies Parameters<InformatieObjectAddComponent["form"]["patchValue"]>[0];

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
        provideTanStackQuery(testQueryClient),
        provideQueryClient(testQueryClient),
        {
          provide: MatDrawer,
          useValue: mockSideNav,
        },
        VertrouwelijkaanduidingToTranslationKeyPipe,
      ],
    }).compileComponents();

    identityService = TestBed.inject(IdentityService);

    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    jest
      .spyOn(informatieObjectenService, "listInformatieobjecttypesForZaak")
      .mockReturnValue(of(mockInformatieObjectTypes));

    httpTestingController = TestBed.inject(HttpTestingController);

    testQueryClient.setQueryData(
      identityService.readLoggedInUser().queryKey,
      mockLoggedInUser,
    );

    fixture = TestBed.createComponent(InformatieObjectAddComponent);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;
    loader = TestbedHarnessEnvironment.loader(fixture);

    // Set required inputs
    componentRef.setInput("sideNav", mockSideNav);
    componentRef.setInput("zaakUuid", mockZaak.uuid);

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
  });

  describe("Form interactions", () => {
    it("should have creatiedatum prefilled with todays and disable verzenddatum and status when ontvangstdatum is set", async () => {
      const [creatiedatum] = await loader.getAllHarnesses(
        MatDatepickerInputHarness,
      );

      expect(await creatiedatum.getValue()).toBe(moment().format("YYYY-MM-DD"));
    });

    it("should disable the verzenddatum and status when ontvangstdatum is set", async () => {
      const [, ontvangstdatumInput, verzenddatumInput] =
        await loader.getAllHarnesses(MatDatepickerInputHarness);

      await ontvangstdatumInput.setValue(moment().format("YYYY-MM-DD"));
      expect(await verzenddatumInput.isDisabled()).toBe(true);
    });
  });

  describe("Submit adding document to a Zaak", () => {
    beforeEach(() => {
      component["form"].patchValue(mockFormInput);
      fixture.detectChanges();
    });

    it("should call createEnkelvoudigInformatieobject when submit button is clicked", async () => {
      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.toevoegen" }),
      );
      await submitButton.click();

      const req = httpTestingController.expectOne(
        `/rest/informatieobjecten/informatieobject/${mockZaak.uuid}/${mockZaak.uuid}?taakObject=false`,
      );
      expect(req.request.method).toEqual("POST");

      const data = req.request.body as FormData;
      expect(data).toBeInstanceOf(FormData);

      const formDataObject = Object.fromEntries(data.entries());
      expect(formDataObject.titel).toBe(mockFormInput.titel);
      expect(formDataObject.bestandsnaam).toBe(mockFile.name);
      expect(formDataObject.status).toBe("IN_BEWERKING");
      expect(formDataObject.file).toBeInstanceOf(File);
    });

    it("should emit document event after form add and submit", async () => {
      const emitSpy = jest.spyOn(component["document"], "emit");

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.toevoegen" }),
      );

      await submitButton.click();
      httpTestingController
        .expectOne(
          `/rest/informatieobjecten/informatieobject/${mockZaak.uuid}/${mockZaak.uuid}?taakObject=false`,
        )
        .flush(null);
      await sleep();

      expect(emitSpy).toHaveBeenCalled();
    });

    it("should close side nav after successful add", async () => {
      const sideNavCloseSpy = jest.spyOn(mockSideNav, "close");

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.toevoegen" }),
      );
      await submitButton.click();
      httpTestingController
        .expectOne(
          `/rest/informatieobjecten/informatieobject/${mockZaak.uuid}/${mockZaak.uuid}?taakObject=false`,
        )
        .flush(null);
      await sleep();

      expect(sideNavCloseSpy).toHaveBeenCalled();
    });
  });

  describe("Reset", () => {
    it("should close side nav when cancel button is clicked", async () => {
      const sideNavCloseSpy = jest.spyOn(mockSideNav, "close");

      const cancelButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.annuleren" }),
      );
      await cancelButton.click();

      expect(sideNavCloseSpy).toHaveBeenCalled();
    });
  });

  describe("Adding multiple documents to a Zaak", () => {
    beforeEach(() => {
      component["form"].patchValue({
        ...mockFormInput,
        addOtherInfoObject: true,
      });
      fixture.detectChanges();
    });

    it("should not close the side nav", async () => {
      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.toevoegen" }),
      );

      await submitButton.click();
      httpTestingController
        .expectOne(
          `/rest/informatieobjecten/informatieobject/${mockZaak.uuid}/${mockZaak.uuid}?taakObject=false`,
        )
        .flush(null);
      await sleep();

      expect(mockSideNav.close).not.toHaveBeenCalled();
    });

    it("should reset the form", async () => {
      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.toevoegen" }),
      );

      await submitButton.click();
      httpTestingController
        .expectOne(
          `/rest/informatieobjecten/informatieobject/${mockZaak.uuid}/${mockZaak.uuid}?taakObject=false`,
        )
        .flush(null);
      await sleep();

      expect(component["form"].value).toEqual(
        expect.objectContaining({
          bestand: null,
          titel: null,
          auteur: mockLoggedInUser.naam,
          creatiedatum: expect.any(moment),
          addOtherInfoObject: true,
        }),
      );
    });
  });
});
