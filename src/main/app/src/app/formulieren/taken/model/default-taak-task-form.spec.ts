/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "../../../../test-helpers";
import { FormField } from "../../../shared/form/form";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { DefaultTaakTaskForm } from "./default-taak-task-form";

describe(DefaultTaakTaskForm.name, () => {
  let formulier: DefaultTaakTaskForm;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [provideHttpClient()],
    });

    formulier = TestBed.inject(DefaultTaakTaskForm);
  });

  describe("requestForm (_initStartForm)", () => {
    let fields: FormField[];

    beforeEach(async () => {
      fields = await formulier.requestForm();
    });

    it("should expose only the redenStart field", () => {
      expect(fields).toHaveLength(1);
      expect(fields[0].key).toBe("redenStart");
    });

    describe("redenStart field", () => {
      it("should be a textarea", () => {
        expect(fields.find((f) => f.key === "redenStart")?.type).toBe(
          "textarea",
        );
      });

      it("should be initialised to null", () => {
        expect(
          fields.find((f) => f.key === "redenStart")?.control?.value,
        ).toBeNull();
      });

      it("should be required", () => {
        const control = fields.find((f) => f.key === "redenStart")?.control;
        control?.setValue("");
        control?.markAsTouched();
        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should enforce maxlength of 1000", () => {
        const control = fields.find((f) => f.key === "redenStart")?.control;
        control?.setValue("a".repeat(1001));
        expect(control?.errors?.["maxlength"]).toBeDefined();
      });

      it("should accept a valid value", () => {
        const control = fields.find((f) => f.key === "redenStart")?.control;
        control?.setValue("a".repeat(1000));
        expect(control?.errors).toBeNull();
      });
    });
  });

  describe("handleForm (_initBehandelForm)", () => {
    describe("field structure (editable mode)", () => {
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

      it("should expose redenStart and afhandeling fields", () => {
        expect(fields).toHaveLength(2);
        expect(fields.map((f) => f.key)).toEqual(["redenStart", "afhandeling"]);
      });

      it("should render redenStart as plain-text", () => {
        const field = fields.find((f) => f.key === "redenStart");
        expect(field?.type).toBe("plain-text");
        expect(field?.label).toBe("redenStart");
      });

      it("should render afhandeling as a textarea", () => {
        expect(fields.find((f) => f.key === "afhandeling")?.type).toBe(
          "textarea",
        );
      });

      it("should render afhandeling as editable when task is OPEN and user can wijzigen", () => {
        expect(fields.find((f) => f.key === "afhandeling")?.readonly).toBe(
          false,
        );
      });

      it("should require afhandeling", () => {
        const control = fields.find((f) => f.key === "afhandeling")?.control;
        control?.setValue("");
        control?.markAsTouched();
        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should enforce maxlength of 1000 on afhandeling", () => {
        const control = fields.find((f) => f.key === "afhandeling")?.control;
        control?.setValue("a".repeat(1001));
        expect(control?.errors?.["maxlength"]).toBeDefined();
      });

      it("should accept a valid afhandeling value", () => {
        const control = fields.find((f) => f.key === "afhandeling")?.control;
        control?.setValue("a".repeat(1000));
        expect(control?.errors).toBeNull();
      });
    });

    describe("afhandeling pre-fill from taakdata", () => {
      it("should pre-fill afhandeling when taakdata contains a previously saved value", async () => {
        const fields = await formulier.handleForm(
          fromPartial<GeneratedType<"RestTask">>({
            taakdata: { afhandeling: "eerder opgeslagen afhandeling" },
            status: "OPEN" as GeneratedType<"TaakStatus">,
            rechten: fromPartial({ wijzigen: true }),
          }),
        );
        expect(
          fields.find((f) => f.key === "afhandeling")?.control?.value,
        ).toBe("eerder opgeslagen afhandeling");
      });

      it("should initialize afhandeling as null when taakdata is empty", async () => {
        const fields = await formulier.handleForm(
          fromPartial<GeneratedType<"RestTask">>({
            taakdata: {},
            status: "OPEN" as GeneratedType<"TaakStatus">,
            rechten: fromPartial({ wijzigen: true }),
          }),
        );
        expect(
          fields.find((f) => f.key === "afhandeling")?.control?.value,
        ).toBeNull();
      });

      it("should initialize afhandeling as null when taakdata itself is missing", async () => {
        const fields = await formulier.handleForm(
          fromPartial<GeneratedType<"RestTask">>({
            status: "OPEN" as GeneratedType<"TaakStatus">,
            rechten: fromPartial({ wijzigen: true }),
          }),
        );
        expect(
          fields.find((f) => f.key === "afhandeling")?.control?.value,
        ).toBeNull();
      });
    });

    describe("readonly mode", () => {
      it("should render afhandeling as readonly when task is AFGEROND", async () => {
        const fields = await formulier.handleForm(
          fromPartial<GeneratedType<"RestTask">>({
            taakdata: {},
            status: "AFGEROND" as GeneratedType<"TaakStatus">,
            rechten: fromPartial({ wijzigen: true }),
          }),
        );
        expect(fields.find((f) => f.key === "afhandeling")?.readonly).toBe(
          true,
        );
      });

      it("should render afhandeling as readonly when user cannot wijzigen", async () => {
        const fields = await formulier.handleForm(
          fromPartial<GeneratedType<"RestTask">>({
            taakdata: {},
            status: "OPEN" as GeneratedType<"TaakStatus">,
            rechten: fromPartial({ wijzigen: false }),
          }),
        );
        expect(fields.find((f) => f.key === "afhandeling")?.readonly).toBe(
          true,
        );
      });

      it("should render afhandeling as readonly when rechten is missing", async () => {
        const fields = await formulier.handleForm(
          fromPartial<GeneratedType<"RestTask">>({
            taakdata: {},
            status: "OPEN" as GeneratedType<"TaakStatus">,
          }),
        );
        expect(fields.find((f) => f.key === "afhandeling")?.readonly).toBe(
          true,
        );
      });
    });
  });
});
