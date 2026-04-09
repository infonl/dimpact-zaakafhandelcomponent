/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { of, throwError } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { UtilService } from "../../core/service/util.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { MaterialModule } from "../../shared/material/material.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { IntakeAfrondenDialogComponent } from "./intake-afronden-dialog.component";

describe(IntakeAfrondenDialogComponent.name, () => {
  let component: IntakeAfrondenDialogComponent;
  let fixture: ComponentFixture<IntakeAfrondenDialogComponent>;
  let zakenService: ZakenService;
  let mailtemplateService: MailtemplateService;
  let klantenService: KlantenService;
  let planItemsService: PlanItemsService;
  const mockDialogRef = { close: jest.fn() };

  const mockPlanItem = fromPartial<GeneratedType<"RESTPlanItem">>({
    id: "plan-item-1",
    userEventListenerActie: "INTAKE_AFRONDEN",
  });

  const mockAfzender = fromPartial<GeneratedType<"RestZaakAfzender">>({
    defaultMail: true,
    mail: "beheerder@example.com",
    replyTo: "reply@example.com",
  });

  const mockContactGegevens = fromPartial<GeneratedType<"RestContactDetails">>({
    emailadres: "initiator@example.com",
  });

  const mockMailtemplateOntvankelijk = fromPartial<
    GeneratedType<"RESTMailtemplate">
  >({
    onderwerp: "Zaak ontvankelijk",
    body: "<p>Uw zaak is ontvangen</p>",
  });

  const mockMailtemplateNietOntvankelijk = fromPartial<
    GeneratedType<"RESTMailtemplate">
  >({
    onderwerp: "Zaak niet ontvankelijk",
    body: "<p>Uw zaak is niet ontvankelijk</p>",
  });

  function makeZaak(
    intakeMail:
      | "BESCHIKBAAR_AAN"
      | "BESCHIKBAAR_UIT"
      | "NIET_BESCHIKBAAR" = "BESCHIKBAAR_AAN",
    temporaryPersonId: string | null = "person-123",
  ) {
    return fromPartial<GeneratedType<"RestZaak">>({
      uuid: "zaak-uuid",
      zaaktype: fromPartial({
        zaakafhandelparameters: fromPartial({ intakeMail }),
      }),
      initiatorIdentificatie: temporaryPersonId
        ? fromPartial<GeneratedType<"BetrokkeneIdentificatie">>({
            type: "BSN",
            temporaryPersonId,
          })
        : null,
    });
  }

  async function setup(zaak: GeneratedType<"RestZaak"> = makeZaak()) {
    mockDialogRef.close = jest.fn();

    await TestBed.configureTestingModule({
      declarations: [IntakeAfrondenDialogComponent],
      imports: [
        ReactiveFormsModule,
        TranslateModule.forRoot(),
        MaterialModule,
        NoopAnimationsModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: MAT_DIALOG_DATA,
          useValue: { zaak, planItem: mockPlanItem },
        },
        { provide: MatDialogRef, useValue: mockDialogRef },
        ZakenService,
        PlanItemsService,
        MailtemplateService,
        KlantenService,
        UtilService,
      ],
    }).compileComponents();

    zakenService = TestBed.inject(ZakenService);
    mailtemplateService = TestBed.inject(MailtemplateService);
    klantenService = TestBed.inject(KlantenService);
    planItemsService = TestBed.inject(PlanItemsService);

    jest
      .spyOn(zakenService, "listAfzendersVoorZaak")
      .mockReturnValue(of([mockAfzender]));
    jest
      .spyOn(zakenService, "readDefaultAfzenderVoorZaak")
      .mockReturnValue(of(mockAfzender));
    jest
      .spyOn(mailtemplateService, "findMailtemplate")
      .mockImplementation((key) =>
        key === "ZAAK_ONTVANKELIJK"
          ? of(mockMailtemplateOntvankelijk)
          : of(mockMailtemplateNietOntvankelijk),
      );
    jest
      .spyOn(klantenService, "getContactDetailsForPerson")
      .mockReturnValue(of(mockContactGegevens));

    fixture = TestBed.createComponent(IntakeAfrondenDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  describe("mail availability", () => {
    it("mailBeschikbaar is true when intakeMail is BESCHIKBAAR_AAN", async () => {
      await setup(makeZaak("BESCHIKBAAR_AAN"));
      expect(component.mailBeschikbaar).toBe(true);
    });

    it("mailBeschikbaar is true when intakeMail is BESCHIKBAAR_UIT", async () => {
      await setup(makeZaak("BESCHIKBAAR_UIT"));
      expect(component.mailBeschikbaar).toBe(true);
    });

    it("mailBeschikbaar is false when intakeMail is NIET_BESCHIKBAAR", async () => {
      await setup(makeZaak("NIET_BESCHIKBAAR"));
      expect(component.mailBeschikbaar).toBe(false);
    });

    it("sendMailDefault is true when intakeMail is BESCHIKBAAR_AAN", async () => {
      await setup(makeZaak("BESCHIKBAAR_AAN"));
      expect(component.sendMailDefault).toBe(true);
    });

    it("sendMailDefault is false when intakeMail is BESCHIKBAAR_UIT", async () => {
      await setup(makeZaak("BESCHIKBAAR_UIT"));
      expect(component.sendMailDefault).toBe(false);
    });
  });

  describe("initiatorEmail", () => {
    it("is set from contact details when temporaryPersonId is present", async () => {
      await setup(makeZaak("BESCHIKBAAR_AAN", "person-123"));
      expect(component.initiatorEmail).toBe(mockContactGegevens.emailadres);
    });

    it("is set from zaakSpecificContactDetails when available, without calling klantenService", async () => {
      const zaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: "zaak-uuid",
        zaaktype: fromPartial({
          zaakafhandelparameters: fromPartial({
            intakeMail: "BESCHIKBAAR_AAN",
          }),
        }),
        zaakSpecificContactDetails: fromPartial({
          emailAddress: "zaak@example.com",
        }),
      });
      await setup(zaak);

      expect(component.initiatorEmail).toBe("zaak@example.com");
      expect(klantenService.getContactDetailsForPerson).not.toHaveBeenCalled();
    });

    it("is undefined when initiator has no temporaryPersonId", async () => {
      await setup(makeZaak("BESCHIKBAAR_AAN", null));
      expect(component.initiatorEmail).toBeUndefined();
      expect(klantenService.getContactDetailsForPerson).not.toHaveBeenCalled();
    });
  });

  describe("ontvankelijk validation", () => {
    beforeEach(async () => setup());

    it("makes reden required when ontvankelijk is false", () => {
      component.formGroup.get("ontvankelijk")?.setValue(false);
      expect(
        component.formGroup.get("reden")?.errors?.["required"],
      ).toBeTruthy();
    });

    it("clears reden required when ontvankelijk is true", () => {
      component.formGroup.get("ontvankelijk")?.setValue(false);
      component.formGroup.get("ontvankelijk")?.setValue(true);
      expect(component.formGroup.get("reden")?.errors).toBeNull();
    });
  });

  describe("sendMail toggle", () => {
    beforeEach(async () => setup(makeZaak("BESCHIKBAAR_UIT")));

    it("adds required validators to verzender and ontvanger when sendMail is checked", () => {
      component.formGroup.get("sendMail")?.setValue(true);
      component.formGroup.get("verzender")?.setValue(null); // clear pre-filled default
      expect(
        component.formGroup.get("verzender")?.errors?.["required"],
      ).toBeTruthy();
      expect(
        component.formGroup.get("ontvanger")?.errors?.["required"],
      ).toBeTruthy();
    });

    it("removes validators from verzender and ontvanger when sendMail is unchecked", () => {
      component.formGroup.get("sendMail")?.setValue(true);
      component.formGroup.get("sendMail")?.setValue(false);
      expect(component.formGroup.get("verzender")?.errors).toBeNull();
      expect(component.formGroup.get("ontvanger")?.errors).toBeNull();
    });
  });

  describe("setInitiatorEmail", () => {
    beforeEach(async () => setup());

    it("sets ontvanger to initiatorEmail", () => {
      component["setInitiatorEmail"]();
      expect(component.formGroup.get("ontvanger")?.value).toBe(
        component.initiatorEmail,
      );
    });
  });

  describe("close", () => {
    beforeEach(async () => setup());

    it("calls dialogRef.close()", () => {
      component["close"]();
      expect(mockDialogRef.close).toHaveBeenCalled();
    });
  });

  describe("afronden", () => {
    beforeEach(async () => setup());

    it("calls planItemsService with correct mail data when ontvankelijk and sendMail are true", () => {
      jest
        .spyOn(planItemsService, "doUserEventListenerPlanItem")
        .mockReturnValue(of(undefined) as never);

      component.formGroup.patchValue({
        ontvankelijk: true,
        sendMail: true,
        verzender: mockAfzender,
        ontvanger: "recipient@example.com",
      });

      component["afronden"]();

      expect(planItemsService.doUserEventListenerPlanItem).toHaveBeenCalledWith(
        expect.objectContaining({
          actie: "INTAKE_AFRONDEN",
          planItemInstanceId: mockPlanItem.id,
          zaakUuid: "zaak-uuid",
          zaakOntvankelijk: true,
          restMailGegevens: expect.objectContaining({
            verzender: mockAfzender.mail,
            replyTo: mockAfzender.replyTo,
            ontvanger: "recipient@example.com",
            onderwerp: mockMailtemplateOntvankelijk.onderwerp,
            body: mockMailtemplateOntvankelijk.body,
            createDocumentFromMail: true,
          }),
        }),
      );
    });

    it("uses niet-ontvankelijk mailtemplate when ontvankelijk is false", () => {
      jest
        .spyOn(planItemsService, "doUserEventListenerPlanItem")
        .mockReturnValue(of(undefined) as never);

      component.formGroup.patchValue({
        ontvankelijk: false,
        reden: "Onvoldoende informatie",
        sendMail: true,
        verzender: mockAfzender,
        ontvanger: "recipient@example.com",
      });

      component["afronden"]();

      expect(planItemsService.doUserEventListenerPlanItem).toHaveBeenCalledWith(
        expect.objectContaining({
          restMailGegevens: expect.objectContaining({
            onderwerp: mockMailtemplateNietOntvankelijk.onderwerp,
            body: mockMailtemplateNietOntvankelijk.body,
          }),
        }),
      );
    });

    it("sends null restMailGegevens when sendMail is false", () => {
      jest
        .spyOn(planItemsService, "doUserEventListenerPlanItem")
        .mockReturnValue(of(undefined) as never);

      component.formGroup.patchValue({ ontvankelijk: true, sendMail: false });

      component["afronden"]();

      expect(planItemsService.doUserEventListenerPlanItem).toHaveBeenCalledWith(
        expect.objectContaining({ restMailGegevens: null }),
      );
    });

    it("closes dialog with true on success", () => {
      jest
        .spyOn(planItemsService, "doUserEventListenerPlanItem")
        .mockReturnValue(of(undefined) as never);

      component.formGroup.patchValue({ ontvankelijk: true });
      component["afronden"]();

      expect(mockDialogRef.close).toHaveBeenCalledWith(true);
    });

    it("closes dialog with false on error", () => {
      jest
        .spyOn(planItemsService, "doUserEventListenerPlanItem")
        .mockReturnValue(throwError(() => new Error("server error")) as never);

      component.formGroup.patchValue({ ontvankelijk: true });
      component["afronden"]();

      expect(mockDialogRef.close).toHaveBeenCalledWith(false);
    });
  });
});
