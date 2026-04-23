/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { of } from "rxjs";
import { fromPartial } from "../../../../test-helpers";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { MailtemplateService } from "../../../mailtemplate/mailtemplate.service";
import { SelectFormField } from "../../../shared/material-form-builder/form-components/select/select-form-field";
import { FieldType } from "../../../shared/material-form-builder/model/field-type.enum";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { TakenService } from "../../../taken/taken.service";
import { ZakenService } from "../../../zaken/zaken.service";
import { ExternAdviesMail } from "./extern-advies-mail";

describe(ExternAdviesMail.name, () => {
  let formulier: ExternAdviesMail;
  let translateService: TranslateService;
  let zakenService: ZakenService;
  let mailtemplateService: MailtemplateService;
  let informatieObjectenService: InformatieObjectenService;

  const mockZaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "zaak-uuid",
  });

  const mockTaak = fromPartial<GeneratedType<"RestTask">>({
    toelichting: undefined,
    taakdocumenten: [],
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
      providers: [
        provideHttpClient(),
        TakenService,
        ZakenService,
        MailtemplateService,
        InformatieObjectenService,
      ],
    });

    translateService = TestBed.inject(TranslateService);
    zakenService = TestBed.inject(ZakenService);
    mailtemplateService = TestBed.inject(MailtemplateService);
    informatieObjectenService = TestBed.inject(InformatieObjectenService);

    jest.spyOn(translateService, "instant").mockReturnValue("translated-value");
    jest.spyOn(zakenService, "listAfzendersVoorZaak").mockReturnValue(of([]));
    jest
      .spyOn(zakenService, "readDefaultAfzenderVoorZaak")
      .mockReturnValue(of(null));
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

    formulier = new ExternAdviesMail(
      translateService,
      TestBed.inject(TakenService),
      informatieObjectenService,
      mailtemplateService,
      zakenService,
    );

    formulier.zaak = mockZaak;
    formulier.taak = mockTaak;
    formulier.humanTaskData = {};
    formulier.dataElementen = {};
  });

  describe("taakinformatieMapping", () => {
    it("should map uitkomst to the externAdvies field key", () => {
      expect(formulier.taakinformatieMapping.uitkomst).toBe("externAdvies");
    });
  });

  describe("getBehandelTitel", () => {
    it("should call translate.instant with the extern advies behandel title key", () => {
      formulier.getBehandelTitel();

      expect(translateService.instant).toHaveBeenCalledWith(
        "title.taak.extern-advies.verwerken",
      );
    });

    it("should return the translated title", () => {
      const result = formulier.getBehandelTitel();

      expect(result).toBe("translated-value");
    });
  });

  describe("initStartForm (_initStartForm)", () => {
    beforeEach(() => {
      formulier.initStartForm();
    });

    describe("taakStuurGegevens", () => {
      it("should set sendMail to true", () => {
        expect(formulier.humanTaskData.taakStuurGegevens?.sendMail).toBe(true);
      });

      it("should set mail to TAAK_ADVIES_EXTERN", () => {
        expect(formulier.humanTaskData.taakStuurGegevens?.mail).toBe(
          "TAAK_ADVIES_EXTERN",
        );
      });
    });

    describe("service calls", () => {
      it("should call listAfzendersVoorZaak with the zaak uuid", () => {
        expect(zakenService.listAfzendersVoorZaak).toHaveBeenCalledWith(
          "zaak-uuid",
        );
      });

      it("should call readDefaultAfzenderVoorZaak with the zaak uuid", () => {
        expect(zakenService.readDefaultAfzenderVoorZaak).toHaveBeenCalledWith(
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
        expect(() => formulier.getFormField("adviseur")).not.toThrow();
      });

      it("should be required", () => {
        const field = formulier.getFormField("adviseur");
        field.formControl.setValue("");
        field.formControl.markAsTouched();

        expect(field.formControl.errors?.["required"]).toBeDefined();
      });

      it("should enforce maxlength of 1000", () => {
        const field = formulier.getFormField("adviseur") as unknown as {
          maxlength: number;
        };

        expect(field.maxlength).toBe(1000);
      });
    });

    describe("verzender field", () => {
      it("should exist in the form", () => {
        expect(() => formulier.getFormField("verzender")).not.toThrow();
      });

      it("should be required", () => {
        const field = formulier.getFormField("verzender");
        field.formControl.setValue(null);
        field.formControl.markAsTouched();

        expect(field.formControl.errors?.["required"]).toBeDefined();
      });

      it("should pre-fill with the default afzender mail string from readDefaultAfzenderVoorZaak", () => {
        formulier.humanTaskData = {};
        jest
          .spyOn(zakenService, "readDefaultAfzenderVoorZaak")
          .mockReturnValue(of(mockDefaultAfzender));
        formulier.initStartForm();

        const field = formulier.getFormField("verzender");

        expect(field.formControl.value).toBe(mockDefaultAfzender.mail);
      });

      it("should remain null when readDefaultAfzenderVoorZaak emits null", () => {
        const field = formulier.getFormField("verzender");

        expect(field.formControl.value).toBeNull();
      });

      it("should have options from listAfzendersVoorZaak", () => {
        formulier.humanTaskData = {};
        jest
          .spyOn(zakenService, "listAfzendersVoorZaak")
          .mockReturnValue(of([mockAfzender, mockDefaultAfzender]));
        formulier.initStartForm();

        const field = formulier.getFormField("verzender") as SelectFormField<
          GeneratedType<"RestZaakAfzender">
        >;
        let emittedOptions: GeneratedType<"RestZaakAfzender">[] = [];
        field.options.subscribe((opts) => (emittedOptions = opts));

        expect(emittedOptions.length).toBe(2);
      });
    });

    describe("replyTo field", () => {
      it("should exist in the form", () => {
        expect(() => formulier.getFormField("replyTo")).not.toThrow();
      });

      it("should not be required", () => {
        const field = formulier.getFormField("replyTo");
        field.formControl.setValue(null);
        field.formControl.markAsTouched();

        expect(field.formControl.errors?.["required"]).toBeUndefined();
      });
    });

    describe("emailadres field", () => {
      it("should exist in the form", () => {
        expect(() => formulier.getFormField("emailadres")).not.toThrow();
      });

      it("should be required", () => {
        const field = formulier.getFormField("emailadres");
        field.formControl.setValue("");
        field.formControl.markAsTouched();

        expect(field.formControl.errors?.["required"]).toBeDefined();
      });

      it("should reject an invalid email address", () => {
        const field = formulier.getFormField("emailadres");
        field.formControl.setValue("not-an-email");

        expect(field.formControl.errors?.["email"]).toBeDefined();
      });

      it("should accept a valid email address", () => {
        const field = formulier.getFormField("emailadres");
        field.formControl.setValue("valid@example.com");

        expect(field.formControl.errors).toBeNull();
      });
    });

    describe("body field", () => {
      it("should be an html-editor field", () => {
        expect(formulier.getFormField("body").fieldType).toBe(
          FieldType.HTML_EDITOR,
        );
      });

      it("should be required", () => {
        const field = formulier.getFormField("body");
        field.formControl.setValue("");
        field.formControl.markAsTouched();

        expect(field.formControl.errors?.["required"]).toBeDefined();
      });

      it("should wire the mailtemplate observable to the body field", () => {
        const field = formulier.getFormField("body") as unknown as {
          mailtemplateBody$: unknown;
        };

        expect(field.mailtemplateBody$).toBeDefined();
      });
    });

    describe("bijlagen field", () => {
      it("should be a documenten-lijst field", () => {
        expect(formulier.getFormField("bijlagen").fieldType).toBe(
          FieldType.DOCUMENTEN_LIJST,
        );
      });
    });

    describe("verzender → replyTo subscription", () => {
      it("should update replyTo when verzender changes to a known afzender", () => {
        formulier.humanTaskData = {};
        jest
          .spyOn(zakenService, "listAfzendersVoorZaak")
          .mockReturnValue(of([mockAfzender, mockDefaultAfzender]));
        formulier.initStartForm();

        const verzenderField = formulier.getFormField(
          "verzender",
        ) as SelectFormField<GeneratedType<"RestZaakAfzender">>;
        const replyToField = formulier.getFormField("replyTo");

        // Subscribing to options causes the tap to fire, populating valueOptions
        verzenderField.options.subscribe();

        verzenderField.formControl.setValue(
          mockAfzender as unknown as GeneratedType<"RestZaakAfzender">,
        );

        expect(replyToField.formControl.value).toBe("reply@example.com");
      });

      it("should set replyTo to undefined when verzender changes to an unknown value", () => {
        formulier.humanTaskData = {};
        jest
          .spyOn(zakenService, "listAfzendersVoorZaak")
          .mockReturnValue(of([mockAfzender]));
        formulier.initStartForm();

        const verzenderField = formulier.getFormField(
          "verzender",
        ) as SelectFormField<GeneratedType<"RestZaakAfzender">>;
        const replyToField = formulier.getFormField("replyTo");

        verzenderField.options.subscribe();

        verzenderField.formControl.setValue(
          fromPartial<GeneratedType<"RestZaakAfzender">>({
            mail: "unknown@example.com",
          }),
        );

        expect(replyToField.formControl.value).toBeUndefined();
      });
    });
  });

  describe("initBehandelForm (_initBehandelForm)", () => {
    describe("field types and readonly state (editable mode)", () => {
      beforeEach(() => {
        formulier.initBehandelForm(false);
      });

      it("should render adviseur as a ReadonlyFormField", () => {
        expect(formulier.getFormField("adviseur").fieldType).toBe(
          FieldType.READONLY,
        );
      });

      it("should render verzender as a ReadonlyFormField", () => {
        expect(formulier.getFormField("verzender").fieldType).toBe(
          FieldType.READONLY,
        );
      });

      it("should render emailadres as a ReadonlyFormField", () => {
        expect(formulier.getFormField("emailadres").fieldType).toBe(
          FieldType.READONLY,
        );
      });

      it("should render body as a ReadonlyFormField", () => {
        expect(formulier.getFormField("body").fieldType).toBe(
          FieldType.READONLY,
        );
      });

      it("should render externAdvies as a textarea (not readonly)", () => {
        expect(formulier.getFormField("externAdvies").fieldType).toBe(
          FieldType.TEXTAREA,
        );
        expect(formulier.getFormField("externAdvies").readonly).toBe(false);
      });

      it("should require externAdvies", () => {
        const field = formulier.getFormField("externAdvies");
        field.formControl.setValue("");
        field.formControl.markAsTouched();

        expect(field.formControl.errors?.["required"]).toBeDefined();
      });

      it("should enforce maxlength of 1000 on externAdvies", () => {
        const field = formulier.getFormField("externAdvies") as unknown as {
          maxlength: number;
        };

        expect(field.maxlength).toBe(1000);
      });
    });

    describe("externAdvies pre-fill from dataElementen", () => {
      it("should pre-fill externAdvies when dataElementen contains a previously saved value", () => {
        formulier.dataElementen = { externAdvies: "eerder opgeslagen advies" };

        formulier.initBehandelForm(false);

        expect(formulier.getFormField("externAdvies").formControl.value).toBe(
          "eerder opgeslagen advies",
        );
      });

      it("should initialize externAdvies as null when dataElementen is empty", () => {
        formulier.initBehandelForm(false);

        expect(
          formulier.getFormField("externAdvies").formControl.value,
        ).toBeNull();
      });
    });

    describe("readonly mode", () => {
      it("should render externAdvies as readonly when initBehandelForm is called with readonly=true", () => {
        formulier.initBehandelForm(true);

        expect(formulier.getFormField("externAdvies").readonly).toBe(true);
      });
    });
  });
});
