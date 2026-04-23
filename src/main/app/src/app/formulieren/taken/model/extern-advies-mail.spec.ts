/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { fromPartial } from "../../../../test-helpers";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { MailtemplateService } from "../../../mailtemplate/mailtemplate.service";
import { FormField } from "../../../shared/form/form";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ZakenService } from "../../../zaken/zaken.service";
import { ExternAdviesMailTaskForm } from "./extern-advies-mail-task-form";

describe(ExternAdviesMailTaskForm.name, () => {
  let formulier: ExternAdviesMailTaskForm;
  let zakenService: ZakenService;
  let mailtemplateService: MailtemplateService;
  let informatieObjectenService: InformatieObjectenService;

  const mockZaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "zaak-uuid",
  });

  const mockAfzender = fromPartial<GeneratedType<"RestZaakAfzender">>({
    mail: "afzender@example.com",
    replyTo: "reply@example.com",
    suffix: "Afdeling X",
  });

  const mockDefaultAfzender = fromPartial<GeneratedType<"RestZaakAfzender">>({
    mail: "default@example.com",
    replyTo: "default-reply@example.com",
    suffix: "Default afdeling",
    defaultMail: true,
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [provideHttpClient()],
    });

    zakenService = TestBed.inject(ZakenService);
    mailtemplateService = TestBed.inject(MailtemplateService);
    informatieObjectenService = TestBed.inject(InformatieObjectenService);

    jest.spyOn(zakenService, "listAfzendersVoorZaak").mockReturnValue(of([]));
    jest.spyOn(mailtemplateService, "findMailtemplate").mockReturnValue(
      of(
        fromPartial<GeneratedType<"RESTMailtemplate">>({
          body: "mail-template-body",
          variabelen: [],
        }),
      ),
    );
    jest
      .spyOn(informatieObjectenService, "listEnkelvoudigInformatieobjecten")
      .mockReturnValue(of([]));

    formulier = TestBed.inject(ExternAdviesMailTaskForm);
  });

  describe("requestForm (_initStartForm)", () => {
    let fields: FormField[];

    beforeEach(async () => {
      fields = await formulier.requestForm(mockZaak);
    });

    describe("taakStuurGegevens", () => {
      it("should set sendMail to true", () => {
        expect(
          fields.find((f) => f.key === "taakStuurGegevens.sendMail")?.control
            ?.value,
        ).toBe(true);
      });

      it("should set mail to TAAK_ADVIES_EXTERN", () => {
        expect(
          fields.find((f) => f.key === "taakStuurGegevens.mail")?.control
            ?.value,
        ).toBe("TAAK_ADVIES_EXTERN");
      });
    });

    describe("service calls", () => {
      it("should call listAfzendersVoorZaak with the zaak uuid", () => {
        expect(zakenService.listAfzendersVoorZaak).toHaveBeenCalledWith(
          "zaak-uuid",
        );
      });

      it("should call findMailtemplate with TAAK_ADVIES_EXTERN and the zaak uuid", () => {
        expect(mailtemplateService.findMailtemplate).toHaveBeenCalledWith(
          "TAAK_ADVIES_EXTERN",
          "zaak-uuid",
        );
      });

      it("should call listEnkelvoudigInformatieobjecten with the zaak uuid for bijlagen", () => {
        expect(
          informatieObjectenService.listEnkelvoudigInformatieobjecten,
        ).toHaveBeenCalledWith({ zaakUUID: "zaak-uuid" });
      });
    });

    describe("adviseur field", () => {
      it("should exist in the form", () => {
        expect(fields.find((f) => f.key === "adviseur")).toBeDefined();
      });

      it("should be required", () => {
        const control = fields.find((f) => f.key === "adviseur")?.control;
        control?.setValue("");
        control?.markAsTouched();
        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should enforce maxlength of 1000", () => {
        const control = fields.find((f) => f.key === "adviseur")?.control;
        control?.setValue("a".repeat(1001));
        expect(control?.errors?.["maxlength"]).toBeDefined();
      });
    });

    describe("verzender field", () => {
      it("should exist in the form", () => {
        expect(fields.find((f) => f.key === "verzender")).toBeDefined();
      });

      it("should be required", () => {
        const control = fields.find((f) => f.key === "verzender")?.control;
        control?.setValue(null);
        control?.markAsTouched();
        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should pre-fill with the default afzender mail string", async () => {
        jest
          .spyOn(zakenService, "listAfzendersVoorZaak")
          .mockReturnValue(of([mockAfzender, mockDefaultAfzender]));
        fields = await formulier.requestForm(mockZaak);
        const control = fields.find((f) => f.key === "verzender")?.control;
        expect((control?.value as { mail?: string } | null)?.mail).toBe(
          mockDefaultAfzender.mail,
        );
      });

      it("should remain null when no default afzender exists", () => {
        const control = fields.find((f) => f.key === "verzender")?.control;
        expect(control?.value).toBeNull();
      });

      it("should have options from listAfzendersVoorZaak", async () => {
        jest
          .spyOn(zakenService, "listAfzendersVoorZaak")
          .mockReturnValue(of([mockAfzender, mockDefaultAfzender]));
        fields = await formulier.requestForm(mockZaak);
        const field = fields.find((f) => f.key === "verzender");
        expect(
          "options" in field! ? (field.options as unknown[]).length : 0,
        ).toBe(2);
      });
    });

    describe("replyTo field", () => {
      it("should exist in the form", () => {
        expect(fields.find((f) => f.key === "replyTo")).toBeDefined();
      });

      it("should not be required", () => {
        const control = fields.find((f) => f.key === "replyTo")?.control;
        control?.markAsTouched();
        expect(control?.errors?.["required"]).toBeUndefined();
      });
    });

    describe("emailadres field", () => {
      it("should exist in the form", () => {
        expect(fields.find((f) => f.key === "emailadres")).toBeDefined();
      });

      it("should be required", () => {
        const control = fields.find((f) => f.key === "emailadres")?.control;
        control?.setValue("");
        control?.markAsTouched();
        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should reject an invalid email address", () => {
        const control = fields.find((f) => f.key === "emailadres")?.control;
        control?.setValue("not-an-email");
        expect(control?.errors?.["email"]).toBeDefined();
      });

      it("should accept a valid email address", () => {
        const control = fields.find((f) => f.key === "emailadres")?.control;
        control?.setValue("valid@example.com");
        expect(control?.errors).toBeNull();
      });
    });

    describe("body field", () => {
      it("should be an html-editor field", () => {
        expect(fields.find((f) => f.key === "body")?.type).toBe("html-editor");
      });

      it("should be required", () => {
        const control = fields.find((f) => f.key === "body")?.control;
        control?.setValue("");
        control?.markAsTouched();
        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should wire the mailtemplate body to the body field", () => {
        expect(fields.find((f) => f.key === "body")?.control?.value).toBe(
          "mail-template-body",
        );
      });
    });

    describe("bijlagen field", () => {
      it("should be a documents field", () => {
        expect(fields.find((f) => f.key === "bijlagen")?.type).toBe(
          "documents",
        );
      });
    });

    describe("verzender → replyTo subscription", () => {
      it("should update replyTo when verzender changes to a known afzender", async () => {
        jest
          .spyOn(zakenService, "listAfzendersVoorZaak")
          .mockReturnValue(of([mockAfzender, mockDefaultAfzender]));
        fields = await formulier.requestForm(mockZaak);

        const verzenderControl = fields.find((f) => f.key === "verzender")
          ?.control;
        const replyToControl = fields.find((f) => f.key === "replyTo")?.control;

        verzenderControl?.setValue(
          fromPartial<GeneratedType<"RestZaakAfzender">>({
            mail: "afzender@example.com",
            replyTo: "reply@example.com",
          }),
        );

        expect(replyToControl?.value).toBe("reply@example.com");
      });

      it("should set replyTo to null when verzender is cleared", async () => {
        fields = await formulier.requestForm(mockZaak);
        fields.find((f) => f.key === "verzender")?.control?.setValue(null);
        expect(
          fields.find((f) => f.key === "replyTo")?.control?.value,
        ).toBeNull();
      });
    });
  });

  describe("handleForm (_initBehandelForm)", () => {
    describe("field types and readonly state (editable mode)", () => {
      let fields: FormField[];

      beforeEach(async () => {
        fields = await formulier.handleForm(
          fromPartial<GeneratedType<"RestTask">>({
            taakdata: {},
            status: "OPEN" as GeneratedType<"TaakStatus">,
            rechten: fromPartial({ wijzigen: true }),
          }),
        );
      });

      it("should render adviseur as plain-text", () => {
        expect(fields.find((f) => f.key === "adviseur")?.type).toBe(
          "plain-text",
        );
      });

      it("should render verzender as plain-text", () => {
        expect(fields.find((f) => f.key === "verzender")?.type).toBe(
          "plain-text",
        );
      });

      it("should render emailadres as plain-text", () => {
        expect(fields.find((f) => f.key === "emailadres")?.type).toBe(
          "plain-text",
        );
      });

      it("should render body as plain-text", () => {
        expect(fields.find((f) => f.key === "body")?.type).toBe("plain-text");
      });

      it("should render externAdvies as a textarea (not readonly)", () => {
        const field = fields.find((f) => f.key === "externAdvies");
        expect(field?.type).toBe("textarea");
        expect(field?.readonly).toBe(false);
      });

      it("should require externAdvies", () => {
        const control = fields.find((f) => f.key === "externAdvies")?.control;
        control?.setValue("");
        control?.markAsTouched();
        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should enforce maxlength of 1000 on externAdvies", () => {
        const control = fields.find((f) => f.key === "externAdvies")?.control;
        control?.setValue("a".repeat(1001));
        expect(control?.errors?.["maxlength"]).toBeDefined();
      });
    });

    describe("externAdvies pre-fill from taakdata", () => {
      it("should pre-fill externAdvies when taakdata contains a previously saved value", async () => {
        const fields = await formulier.handleForm(
          fromPartial<GeneratedType<"RestTask">>({
            taakdata: { externAdvies: "eerder opgeslagen advies" },
            status: "OPEN" as GeneratedType<"TaakStatus">,
            rechten: fromPartial({ wijzigen: true }),
          }),
        );
        expect(
          fields.find((f) => f.key === "externAdvies")?.control?.value,
        ).toBe("eerder opgeslagen advies");
      });

      it("should initialize externAdvies as null when taakdata is empty", async () => {
        const fields = await formulier.handleForm(
          fromPartial<GeneratedType<"RestTask">>({
            taakdata: {},
            status: "OPEN" as GeneratedType<"TaakStatus">,
            rechten: fromPartial({ wijzigen: true }),
          }),
        );
        expect(
          fields.find((f) => f.key === "externAdvies")?.control?.value,
        ).toBeNull();
      });
    });

    describe("readonly mode", () => {
      it("should render externAdvies as readonly when task is AFGEROND", async () => {
        const fields = await formulier.handleForm(
          fromPartial<GeneratedType<"RestTask">>({
            taakdata: {},
            status: "AFGEROND" as GeneratedType<"TaakStatus">,
            rechten: fromPartial({ wijzigen: true }),
          }),
        );
        expect(fields.find((f) => f.key === "externAdvies")?.readonly).toBe(
          true,
        );
      });
    });
  });
});
