/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

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
import { ReactiveFormsModule } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { MailService } from "../mail.service";
import { MailCreateComponent } from "./mail-create.component";

describe(MailCreateComponent.name, () => {
  let component: MailCreateComponent;
  let componentRef: ComponentRef<MailCreateComponent>;
  let fixture: ComponentFixture<MailCreateComponent>;
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
      temporaryPersonId: "person-123",
    }),
  });

  const mockAfzenders = [
    fromPartial<GeneratedType<"RestZaakAfzender">>({
      defaultMail: true,
      id: 1,
      mail: "beheerder@example.com",
      speciaal: true,
      suffix: "gegevens.mail.afzender.MEDEWERKER",
    }),
    fromPartial<GeneratedType<"RestZaakAfzender">>({
      defaultMail: false,
      mail: "gemeente@example.com",
      speciaal: true,
      suffix: "gegevens.mail.afzender.GEMEENTE",
    }),
  ];

  const mockDefaultAfzender = mockAfzenders[0];

  const mockMailtemplate = fromPartial<GeneratedType<"RESTMailtemplate">>({
    onderwerp: "<p>Bevestiging ontvangst</p>",
    body: "<p>Geachte,</p>",
    variabelen: ["ZAAK_NUMMER", "ZAAK_TYPE"],
  });

  const mockDocuments = [
    fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
      uuid: "doc-1",
      titel: "Document 1",
    }),
    fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
      uuid: "doc-2",
      titel: "Document 2",
    }),
  ];

  const mockContactGegevens = fromPartial<GeneratedType<"RestContactDetails">>({
    emailadres: "initiator@example.com",
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [MailCreateComponent],
      imports: [
        ReactiveFormsModule,
        TranslateModule.forRoot(),
        PipesModule,
        MaterialModule,
        MaterialFormBuilderModule,
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

    fixture = TestBed.createComponent(MailCreateComponent);
    componentRef = fixture.componentRef;
    componentRef.setInput("sideNav", mockSideNav);
    componentRef.setInput("zaak", mockZaak);

    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    testQueryClient.clear();
    httpTestingController.verify();
  });

  describe("ngOnInit", () => {
    it("should load verzender options", () => {
      expect(component["verzenderOptions"]).toEqual(mockAfzenders);
    });

    it("should set default afzender in form", () => {
      expect(component["form"].controls.verzender.value).toEqual(
        mockDefaultAfzender,
      );
    });

    it("should load mailtemplate and patch form values", () => {
      expect(component["form"].controls.onderwerp.value).toEqual(
        mockMailtemplate.onderwerp,
      );
      expect(component["form"].controls.body.value).toEqual(
        mockMailtemplate.body,
      );
      expect(component["variabelen"]).toEqual(mockMailtemplate.variabelen);
    });

    it("should load documents for the zaak", () => {
      expect(component["documents"]).toEqual(mockDocuments);
    });

    it("should prioritize contact details email address when initiator has temporaryPersonId", () => {
      expect(component["contactEmailAddress"]).toEqual(
        mockContactGegevens.emailadres,
      );
    });

    it("should use zaakSpecificContactDetails email address when available and skip contact details lookup", () => {
      const emailAddress = "zaak-contact@example.com";
      const localFixture = TestBed.createComponent(MailCreateComponent);
      localFixture.componentRef.setInput("sideNav", mockSideNav);
      localFixture.componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          uuid: "test-zaak-uuid",
          zaakSpecificContactDetails: fromPartial({ emailAddress }),
        }),
      );
      jest.mocked(klantenService.getContactDetailsForPerson).mockClear();
      localFixture.detectChanges();

      expect(localFixture.componentInstance["contactEmailAddress"]).toBe(
        emailAddress,
      );
      expect(klantenService.getContactDetailsForPerson).not.toHaveBeenCalled();
    });

    it("should not set contactEmailAddress when initiator has no temporaryPersonId", () => {
      const localFixture = TestBed.createComponent(MailCreateComponent);
      localFixture.componentRef.setInput("sideNav", mockSideNav);
      localFixture.componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          uuid: "test-zaak-uuid",
          initiatorIdentificatie: null,
        }),
      );
      localFixture.detectChanges();

      expect(localFixture.componentInstance["contactEmailAddress"]).toBeNull();
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

  describe("onFormSubmit", () => {
    it("should call sendMail mutation with correct data", async () => {
      component["form"].patchValue({
        verzender: mockDefaultAfzender,
        ontvanger: "recipient@example.com",
        onderwerp: "<p>Test onderwerp</p>",
        body: "<p>Test body</p>",
        bijlagen: [mockDocuments[0]],
      });

      component.onFormSubmit();

      await new Promise((resolve) => setTimeout(resolve, 0));

      const req = httpTestingController.expectOne(
        (request) =>
          request.url.includes("/rest/mail/send/") && request.method === "POST",
      );

      expect(req.request.body).toMatchObject({
        verzender: mockDefaultAfzender.mail,
        replyTo: undefined,
        ontvanger: "recipient@example.com",
        onderwerp: "Test onderwerp",
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

      component.onFormSubmit();

      await new Promise((resolve) => setTimeout(resolve, 0));

      const req = httpTestingController.expectOne(
        (request) =>
          request.url.includes("/rest/mail/send/") && request.method === "POST",
      );

      expect(req.request.body.bijlagen).toBe("doc-1;doc-2");

      req.flush({});
    });

    it("should emit mailVerstuurd(true) and show snackbar on success", async () => {
      jest.spyOn(utilService, "openSnackbar");
      const emitSpy = jest.spyOn(component["mailVerstuurd"], "emit");

      component["form"].patchValue({
        verzender: mockDefaultAfzender,
        ontvanger: "recipient@example.com",
        onderwerp: "<p>Test onderwerp</p>",
        body: "<p>Test body</p>",
        bijlagen: [],
      });

      component.onFormSubmit();

      await new Promise((resolve) => setTimeout(resolve, 0));

      const req = httpTestingController.expectOne(
        (request) =>
          request.url.includes("/rest/mail/send/") && request.method === "POST",
      );

      req.flush({});

      await new Promise((resolve) => setTimeout(resolve, 100));

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.email.verstuurd",
      );
      expect(emitSpy).toHaveBeenCalledWith(true);
    });

    it("should emit mailVerstuurd(false) on error", async () => {
      const emitSpy = jest.spyOn(component["mailVerstuurd"], "emit");

      component["form"].patchValue({
        verzender: mockDefaultAfzender,
        ontvanger: "recipient@example.com",
        onderwerp: "<p>Test onderwerp</p>",
        body: "<p>Test body</p>",
        bijlagen: [],
      });

      component.onFormSubmit();

      await new Promise((resolve) => setTimeout(resolve, 0));

      const req = httpTestingController.expectOne(
        (request) =>
          request.url.includes("/rest/mail/send/") && request.method === "POST",
      );

      req.flush({}, { status: 500, statusText: "Internal Server Error" });

      await new Promise((resolve) => setTimeout(resolve, 100));

      expect(emitSpy).toHaveBeenCalledWith(false);
    });
  });
});
