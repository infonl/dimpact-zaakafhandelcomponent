/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { fromPartial } from "../../../../test-helpers";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ExternAdviesVastleggenTaskForm } from "./extern-advies-vastleggen-task-form";

describe("ExternAdviesVastleggenTaskForm", () => {
  let formulier: ExternAdviesVastleggenTaskForm;

  const mockZaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "zaak-uuid",
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        { provide: TranslateService, useValue: {} },
      ],
    });

    formulier = TestBed.inject(ExternAdviesVastleggenTaskForm);
  });

  describe("requestForm", () => {
    describe("field structure", () => {
      it("should return exactly 3 fields", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.length).toBe(3);
      });

      it("should return fields in the expected order", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.map((f) => f.key)).toEqual(["vraag", "adviseur", "bron"]);
      });

      it("should render vraag as a textarea", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.find((f) => f.key === "vraag")?.type).toBe("textarea");
      });

      it("should render adviseur as an input", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.find((f) => f.key === "adviseur")?.type).toBe("input");
      });

      it("should render bron as a textarea", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.find((f) => f.key === "bron")?.type).toBe("textarea");
      });
    });

    describe("vraag field", () => {
      it("should initialize vraag as empty string", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.find((f) => f.key === "vraag")?.control?.value).toBe("");
      });

      it("should require vraag", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "vraag")?.control;
        control?.setValue("");
        control?.markAsTouched();

        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should enforce maxLength of 1000 on vraag", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "vraag")?.control;
        control?.setValue("a".repeat(1001));

        expect(control?.errors?.["maxlength"]).toBeDefined();
      });

      it("should accept a valid vraag within 1000 characters", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "vraag")?.control;
        control?.setValue("a".repeat(1000));

        expect(control?.errors).toBeNull();
      });
    });

    describe("adviseur field", () => {
      it("should initialize adviseur as empty string", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.find((f) => f.key === "adviseur")?.control?.value).toBe(
          "",
        );
      });

      it("should require adviseur", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "adviseur")?.control;
        control?.setValue("");
        control?.markAsTouched();

        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should enforce maxLength of 1000 on adviseur", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "adviseur")?.control;
        control?.setValue("a".repeat(1001));

        expect(control?.errors?.["maxlength"]).toBeDefined();
      });

      it("should accept a valid adviseur within 1000 characters", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "adviseur")?.control;
        control?.setValue("a".repeat(1000));

        expect(control?.errors).toBeNull();
      });
    });

    describe("bron field", () => {
      it("should initialize bron as empty string", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.find((f) => f.key === "bron")?.control?.value).toBe("");
      });

      it("should require bron", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "bron")?.control;
        control?.setValue("");
        control?.markAsTouched();

        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should enforce maxLength of 1000 on bron", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "bron")?.control;
        control?.setValue("a".repeat(1001));

        expect(control?.errors?.["maxlength"]).toBeDefined();
      });

      it("should accept a valid bron within 1000 characters", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "bron")?.control;
        control?.setValue("a".repeat(1000));

        expect(control?.errors).toBeNull();
      });
    });
  });

  describe("handleForm", () => {
    const mockTaak = fromPartial<GeneratedType<"RestTask">>({
      zaakUuid: "zaak-uuid",
      taakdata: {},
    });

    describe("field structure", () => {
      it("should return exactly 5 fields", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.length).toBe(5);
      });

      it("should return fields in the expected order", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.map((f) => f.key)).toEqual([
          "intro",
          "vraag",
          "adviseur",
          "bron",
          "externAdvies",
        ]);
      });

      it("should render intro as plain-text", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "intro")?.type).toBe("plain-text");
      });

      it("should render vraag as plain-text", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "vraag")?.type).toBe("plain-text");
      });

      it("should render adviseur as plain-text", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "adviseur")?.type).toBe(
          "plain-text",
        );
      });

      it("should render bron as plain-text", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "bron")?.type).toBe("plain-text");
      });

      it("should render externAdvies as textarea", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "externAdvies")?.type).toBe(
          "textarea",
        );
      });
    });

    describe("intro field", () => {
      it("should set the raw i18n key as value (translation delegated to template)", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "intro")?.control?.value).toBe(
          "msg.extern.advies.vastleggen.behandelen",
        );
      });
    });

    describe("vraag field", () => {
      it("should have label vraag", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "vraag")?.label).toBe("vraag");
      });

      it("should not have a control (pre-fill delegated to TaakViewComponent)", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "vraag")?.control).toBeUndefined();
      });
    });

    describe("adviseur field", () => {
      it("should have label adviseur", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "adviseur")?.label).toBe(
          "adviseur",
        );
      });

      it("should not have a control (pre-fill delegated to TaakViewComponent)", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(
          fields.find((f) => f.key === "adviseur")?.control,
        ).toBeUndefined();
      });
    });

    describe("bron field", () => {
      it("should have label bron", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "bron")?.label).toBe("bron");
      });

      it("should not have a control (pre-fill delegated to TaakViewComponent)", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "bron")?.control).toBeUndefined();
      });
    });

    describe("externAdvies field", () => {
      it("should initialize externAdvies as empty string when taakdata is empty", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(
          fields.find((f) => f.key === "externAdvies")?.control?.value,
        ).toBe("");
      });

      it("should pre-fill externAdvies from taakdata when previously saved", async () => {
        const taakWithData = fromPartial<GeneratedType<"RestTask">>({
          ...mockTaak,
          taakdata: { externAdvies: "eerder opgeslagen advies" },
        });

        const fields = await formulier.handleForm(taakWithData);

        expect(
          fields.find((f) => f.key === "externAdvies")?.control?.value,
        ).toBe("eerder opgeslagen advies");
      });

      it("should require externAdvies", async () => {
        const fields = await formulier.handleForm(mockTaak);

        const control = fields.find((f) => f.key === "externAdvies")?.control;
        control?.setValue("");
        control?.markAsTouched();

        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should enforce maxLength of 1000 on externAdvies", async () => {
        const fields = await formulier.handleForm(mockTaak);

        const control = fields.find((f) => f.key === "externAdvies")?.control;
        control?.setValue("a".repeat(1001));

        expect(control?.errors?.["maxlength"]).toBeDefined();
      });

      it("should accept a valid externAdvies within 1000 characters", async () => {
        const fields = await formulier.handleForm(mockTaak);

        const control = fields.find((f) => f.key === "externAdvies")?.control;
        control?.setValue("a".repeat(1000));

        expect(control?.errors).toBeNull();
      });
    });
  });
});
