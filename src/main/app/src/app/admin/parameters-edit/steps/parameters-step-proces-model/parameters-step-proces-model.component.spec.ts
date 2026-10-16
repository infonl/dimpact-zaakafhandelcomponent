/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TranslateModule } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { ProcessModelMethod } from "../../../model/parameters/process-model-method";
import { ParametersStepProcesModelComponent } from "./parameters-step-proces-model.component";
import { createProcessModelForm, ProcessModelForm } from "./process-model-form";

describe(ParametersStepProcesModelComponent.name, () => {
  const user = userEvent.setup();

  async function setup(form: ProcessModelForm) {
    await render(ParametersStepProcesModelComponent, {
      imports: [TranslateModule.forRoot()],
      inputs: { form, zaaktypeOmschrijving: "Test zaaktype omschrijving" },
    });
  }

  it("shows the zaaktype it configures", async () => {
    await setup(createProcessModelForm(null));

    expect(
      screen.getByRole("heading", { name: "Test zaaktype omschrijving" }),
    ).toBeVisible();
  });

  describe("for a configuration that was never saved", () => {
    it("offers both engines without choosing one", async () => {
      const form = createProcessModelForm(null);
      await setup(form);

      expect(screen.getByRole("radio", { name: "CMMN" })).not.toBeChecked();
      expect(screen.getByRole("radio", { name: "BPMN" })).not.toBeChecked();
      expect(form.valid).toBe(false);
    });

    it.each<ProcessModelMethod>(["CMMN", "BPMN"])(
      "lets the beheerder choose %s",
      async (engine) => {
        const form = createProcessModelForm(null);
        await setup(form);

        await user.click(screen.getByRole("radio", { name: engine }));

        expect(form.value.options?.value).toBe(engine);
        expect(form.valid).toBe(true);
      },
    );
  });

  describe.each<ProcessModelMethod>(["CMMN", "BPMN"])(
    "for a saved %s configuration",
    (engine) => {
      async function setupSaved() {
        const form = createProcessModelForm(engine);
        form.disable();
        await setup(form);
      }

      it("shows its engine as chosen", async () => {
        await setupSaved();

        expect(screen.getByRole("radio", { name: engine })).toBeChecked();
      });

      it("offers no choice of engine", async () => {
        await setupSaved();

        expect(screen.getByRole("radio", { name: "CMMN" })).toBeDisabled();
        expect(screen.getByRole("radio", { name: "BPMN" })).toBeDisabled();
      });
    },
  );
});
