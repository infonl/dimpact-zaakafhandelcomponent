/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import {
  ComponentRef,
  provideExperimentalZonelessChangeDetection,
} from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { randomUUID } from "crypto";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { MailService } from "../mail.service";
import { OntvangstbevestigingComponent } from "./ontvangstbevestiging.component";

describe(OntvangstbevestigingComponent.name, () => {
  let component: OntvangstbevestigingComponent;
  let componentRef: ComponentRef<OntvangstbevestigingComponent>;
  let fixture: ComponentFixture<OntvangstbevestigingComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let zakenService: ZakenService;
  let informatieObjectenService: InformatieObjectenService;
  let mailtemplateService: MailtemplateService;
  let klantenService: KlantenService;
  let utilService: UtilService;

  const mockSideNav = fromPartial<MatDrawer>({
    close: jest.fn(),
  });

  const mockZaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "test-zaak-uuid",
    identificatie: "ZAAK-2025-001",
    initiatorIdentificatie: fromPartial<
      GeneratedType<"BetrokkeneIdentificatie">
    >({
      type: "BSN",
      temporaryPersonId: randomUUID(),
    }),
  });

  const mockAfzenders = [
    fromPartial<GeneratedType<"RestZaakAfzender">>({
      defaultMail: true,
      id: 1,
      mail: "beheerder-test-1@example.com",
      speciaal: true,
      suffix: "gegevens.mail.afzender.MEDEWERKER",
    }),
    fromPartial<GeneratedType<"RestZaakAfzender">>({
      defaultMail: false,
      mail: "gemeente-adorp-test@example.com",
      speciaal: true,
      suffix: "gegevens.mail.afzender.GEMEENTE",
    }),
  ];

  const mockDefaultAfzender = mockAfzenders[0];

  const mockMailtemplate = fromPartial<GeneratedType<"RESTMailtemplate">>({
    onderwerp: "<p>Ontvangstbevestiging van zaak {ZAAK_NUMMER}</p>",
    body: "<p>Beste {ZAAK_INITIATOR},</p><p></p><p>Wij hebben uw verzoek ontvangen en deze op {ZAAK_REGISTRATIEDATUM} geregistreerd als zaak {ZAAK_NUMMER}. U kunt dit kenmerk noemen als u contact heeft over de zaak.</p><p></p><p></p><p>Met vriendelijke groet,</p><p></p><p>Gemeente Dommeldam</p>",
    defaultMailtemplate: true,
    variabelen: [
      "ZAAK_NUMMER",
      "ZAAK_TYPE",
      "ZAAK_STATUS",
      "ZAAK_REGISTRATIEDATUM",
      "ZAAK_STARTDATUM",
      "ZAAK_STREEFDATUM",
      "ZAAK_FATALEDATUM",
      "ZAAK_OMSCHRIJVING",
      "ZAAK_TOELICHTING",
      "ZAAK_INITIATOR",
      "ZAAK_INITIATOR_ADRES",
    ],
  });

  const mockDocuments = [
    fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
      uuid: "doc-1",
      titel: "Document 1",
      bestandsnaam: "document-1.pdf",
    }),
    fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
      uuid: "doc-2",
      titel: "Document 2",
      bestandsnaam: "document-2.pdf",
    }),
  ];

  const mockContactGegevens = fromPartial<GeneratedType<"RestContactDetails">>({
    emailadres: "initiator@example.com",
    telefoonnummer: "0612345678",
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        OntvangstbevestigingComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
        provideExperimentalZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        ZakenService,
        InformatieObjectenService,
        MailService,
        MailtemplateService,
        KlantenService,
        UtilService,
      ],
    }).compileComponents();

    zakenService = TestBed.inject(ZakenService);
    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    mailtemplateService = TestBed.inject(MailtemplateService);
    klantenService = TestBed.inject(KlantenService);
    utilService = TestBed.inject(UtilService);
    httpTestingController = TestBed.inject(HttpTestingController);

    jest
      .spyOn(zakenService, "listAfzendersVoorZaak")
      .mockReturnValue(of(mockAfzenders));
    jest
      .spyOn(zakenService, "readDefaultAfzenderVoorZaak")
      .mockReturnValue(of(mockDefaultAfzender));
    jest
      .spyOn(informatieObjectenService, "listEnkelvoudigInformatieobjecten")
      .mockReturnValue(of(mockDocuments));
    jest
      .spyOn(mailtemplateService, "findMailtemplate")
      .mockReturnValue(of(mockMailtemplate));
    jest
      .spyOn(klantenService, "getContactDetailsForPerson")
      .mockReturnValue(of(mockContactGegevens));

    fixture = TestBed.createComponent(OntvangstbevestigingComponent);
    componentRef = fixture.componentRef;
    componentRef.setInput("sideNav", mockSideNav);
    componentRef.setInput("zaak", mockZaak);

    loader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    testQueryClient.clear();
    httpTestingController.verify();
  });

  describe("ngOnInit", () => {
    it("should load documents for the zaak", () => {
      expect(component["documents"]).toEqual(mockDocuments);
    });

    it("should load afzenders for the zaak", () => {
      expect(component["afzenders"]).toEqual(mockAfzenders);
    });

    it("should set default afzender in form", () => {
      expect(component["form"].controls.verzender.value).toEqual(
        mockDefaultAfzender,
      );
    });

    it("should load mailtemplate and set form values", () => {
      expect(component["form"].controls.onderwerp.value).toEqual(
        mockMailtemplate.onderwerp,
      );
      expect(component["form"].controls.body.value).toEqual(
        mockMailtemplate.body,
      );
      expect(component["variables"]).toEqual(mockMailtemplate.variabelen);
    });

    it("should prioritize contact details email address when initiator has temporaryPersonId", () => {
      expect(component["contactEmailAddress"]).toEqual(
        mockContactGegevens.emailadres,
      );
    });

    it("should use zaakSpecificContactDetails email address when available and skip contact details lookup", () => {
      const emailAddress = "zaak-contact@example.com";
      fixture = TestBed.createComponent(OntvangstbevestigingComponent);
      componentRef = fixture.componentRef;
      componentRef.setInput("sideNav", mockSideNav);
      componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          uuid: "test-zaak-uuid",
          zaakSpecificContactDetails: fromPartial({
            emailAddress,
          }),
        }),
      );
      jest.mocked(klantenService.getContactDetailsForPerson).mockClear();
      fixture.detectChanges();

      expect(fixture.componentInstance["contactEmailAddress"]).toBe(
        emailAddress,
      );
      expect(klantenService.getContactDetailsForPerson).not.toHaveBeenCalled();
    });

    it("should not load contact details when initiator has no temporaryPersonId", () => {
      fixture = TestBed.createComponent(OntvangstbevestigingComponent);
      componentRef = fixture.componentRef;
      componentRef.setInput("sideNav", mockSideNav);
      componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          uuid: "test-zaak-uuid",
          initiatorIdentificatie: null,
        }),
      );
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component["contactEmailAddress"]).toBeNull();
    });
  });

  describe("setOntvanger", () => {
    it("should set ontvanger field with contact email address", () => {
      component["setOntvanger"]();

      expect(component["form"].controls.ontvanger.value).toEqual(
        mockContactGegevens.emailadres,
      );
    });

    it("should set ontvanger to null when contactEmailAddress is null", () => {
      component["contactEmailAddress"] = null;

      component["setOntvanger"]();

      expect(component["form"].controls.ontvanger.value).toBeNull();
    });
  });

  describe("form validation", () => {
    it("should mark form as invalid when required fields are empty", () => {
      component["form"].reset();
      component["form"].markAllAsTouched();
      expect(component["form"].valid).toBe(false);
    });

    it("should reject invalid email format for ontvanger", () => {
      component["form"].controls.ontvanger.setValue("invalid-email");
      expect(component["form"].controls.ontvanger.errors?.["email"]).toBe(true);
    });

    it("should accept valid email format for ontvanger", () => {
      component["form"].controls.ontvanger.setValue("valid@example.com");
      expect(component["form"].controls.ontvanger.errors).toBeNull();
    });

    it("should validate onderwerp maxLength", () => {
      const longSubject = "a".repeat(101);
      component["form"].controls.onderwerp.setValue(longSubject);

      expect(
        component["form"].controls.onderwerp.errors?.["maxlength"],
      ).toBeTruthy();
    });

    it("should mark form as valid when all required fields are filled correctly", () => {
      component["form"].patchValue({
        verzender: mockDefaultAfzender,
        ontvanger: "test@example.com",
        onderwerp: "Test onderwerp",
        body: "Test body",
        bijlagen: [],
      });

      expect(component["form"].valid).toBe(true);
    });
  });

  describe("submit", () => {
    it("should call sendAcknowledgeReceipt mutation with correct data", async () => {
      component["form"].patchValue({
        verzender: mockDefaultAfzender,
        ontvanger: "recipient@example.com",
        onderwerp: "<p>Test onderwerp</p>",
        body: "<p>Test body</p>",
        bijlagen: [mockDocuments[0]],
      });

      component["submit"]();

      // Wait for mutation to trigger HTTP request
      await new Promise((resolve) => setTimeout(resolve, 0));

      const req = httpTestingController.expectOne(
        (request) =>
          request.url.includes("/rest/mail/acknowledge/") &&
          request.method === "POST",
      );

      expect(req.request.body).toMatchObject({
        verzender: mockDefaultAfzender.mail,
        replyTo: undefined,
        ontvanger: "recipient@example.com",
        onderwerp: "<p>Test onderwerp</p>",
        body: "<p>Test body</p>",
        bijlagen: mockDocuments[0].uuid,
        createDocumentFromMail: true,
      });

      req.flush({});
    });

    it("should join multiple bijlagen UUIDs with semicolon", async () => {
      component["form"].patchValue({
        verzender: mockDefaultAfzender,
        ontvanger: "recipient@example.com",
        onderwerp: "<p>Test onderwerp</p>",
        body: "<p>Test body</p>",
        bijlagen: mockDocuments,
      });

      component["submit"]();

      // Wait for mutation to trigger HTTP request
      await new Promise((resolve) => setTimeout(resolve, 0));

      const req = httpTestingController.expectOne(
        (request) =>
          request.url.includes("/rest/mail/acknowledge/") &&
          request.method === "POST",
      );

      expect(req.request.body.bijlagen).toBe("doc-1;doc-2");

      req.flush({});
    });

    it("should emit ontvangstBevestigd on successful submission", async () => {
      jest.spyOn(utilService, "openSnackbar");
      const emitSpy = jest.spyOn(component["ontvangstBevestigd"], "emit");

      component["form"].patchValue({
        verzender: mockDefaultAfzender,
        ontvanger: "recipient@example.com",
        onderwerp: "<p>Test onderwerp</p>",
        body: "<p>Test body</p>",
        bijlagen: [],
      });

      component["submit"]();

      // Wait for mutation to trigger HTTP request
      await new Promise((resolve) => setTimeout(resolve, 0));

      const req = httpTestingController.expectOne(
        (request) =>
          request.url.includes("/rest/mail/acknowledge/") &&
          request.method === "POST",
      );

      req.flush({});

      // Wait for mutation to complete
      await new Promise((resolve) => setTimeout(resolve, 100));

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.email.verstuurd",
      );
      expect(emitSpy).toHaveBeenCalledWith(true);
    });
  });

  describe("form buttons", () => {
    it("should have enabled submit button when form is valid", async () => {
      component["form"].patchValue({
        verzender: mockDefaultAfzender,
        ontvanger: "test@example.com",
        onderwerp: "Test onderwerp",
        body: "Test body",
      });
      fixture.detectChanges();

      const buttons = await loader.getAllHarnesses(MatButtonHarness);
      const submitButton = buttons.find(
        async (btn) => (await btn.getText()) === "actie.versturen",
      );

      expect(await submitButton?.isDisabled()).toBe(false);
    });
  });

  describe("close button", () => {
    it("closes the sideNav when clicked", async () => {
      const closeButton = await loader.getHarness(
        MatButtonHarness.with({ ancestor: "mat-toolbar" }),
      );
      await closeButton.click();
      expect(mockSideNav.close).toHaveBeenCalled();
    });
  });

  describe("set-ontvanger button", () => {
    it("is shown when contactGegevens has an email address", async () => {
      const personButtons = await loader.getAllHarnesses(
        MatButtonHarness.with({ text: /person/ }),
      );
      expect(personButtons).toHaveLength(1);
    });

    it("is hidden when contactEmailAddress is null", async () => {
      component["contactEmailAddress"] = null;
      fixture.detectChanges();

      const personButtons = await loader.getAllHarnesses(
        MatButtonHarness.with({ text: /person/ }),
      );
      expect(personButtons).toHaveLength(0);
    });
  });
});
