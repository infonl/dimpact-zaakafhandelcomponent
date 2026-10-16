/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { ZaakafhandelParametersService } from "../../../zaakafhandel-parameters.service";
import { ParametersStepProcesModelComponent } from "./parameters-step-proces-model.component";
import {
  createProcessModelForm,
  ProcessDefinition,
  ProcessModelForm,
} from "./process-model-form";

const caseDefinition: ProcessDefinition = {
  processEngine: "CMMN",
  key: "fakeCaseDefinitionKey",
  naam: "Generiek zaakafhandelmodel",
};

const otherCaseDefinition: ProcessDefinition = {
  processEngine: "CMMN",
  key: "fakeOtherCaseDefinitionKey",
  naam: "Ander zaakafhandelmodel",
};

const bpmnProcessDefinition: ProcessDefinition = {
  processEngine: "BPMN",
  key: "fakeProcessDefinitionKey",
  naam: "Vergunningsproces",
};

describe(ParametersStepProcesModelComponent.name, () => {
  const user = userEvent.setup();

  async function setup(
    form: ProcessModelForm,
    savedProcessDefinition: ProcessDefinition | null = null,
  ) {
    await render(ParametersStepProcesModelComponent, {
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      inputs: {
        form,
        zaaktypeOmschrijving: "Test zaaktype omschrijving",
        savedProcessDefinition,
      },
      providers: [
        {
          provide: ZaakafhandelParametersService,
          useValue: fromPartial<ZaakafhandelParametersService>({
            listProcessDefinitions: () => ({
              ...createQueryOptions([
                caseDefinition,
                otherCaseDefinition,
                bpmnProcessDefinition,
              ]),
              queryKey: ["processDefinitions"],
            }),
          }),
        },
      ],
    });
  }

  async function offeredProcessDefinitions() {
    await user.click(screen.getByRole("combobox", { name: /procesmodel/i }));
    return (await screen.findAllByRole("option")).map((option) =>
      option.textContent?.trim(),
    );
  }

  it("shows the zaaktype it configures", async () => {
    await setup(createProcessModelForm(null));

    expect(
      screen.getByRole("heading", { name: "Test zaaktype omschrijving" }),
    ).toBeVisible();
  });

  describe("for a configuration that was never saved", () => {
    it("offers the process definitions of every process engine, without choosing one", async () => {
      const form = createProcessModelForm(null);
      await setup(form);

      expect(await offeredProcessDefinitions()).toEqual([
        "Generiek zaakafhandelmodel",
        "Ander zaakafhandelmodel",
        "Vergunningsproces",
      ]);
      expect(form.valid).toBe(false);
    });

    it("lets the beheerder choose a process definition", async () => {
      const form = createProcessModelForm(null);
      await setup(form);

      await offeredProcessDefinitions();
      await user.click(
        screen.getByRole("option", { name: "Vergunningsproces" }),
      );

      expect(form.value.processDefinition).toEqual(bpmnProcessDefinition);
      expect(form.valid).toBe(true);
    });
  });

  describe("for a saved configuration", () => {
    it("offers only the process definitions of the process engine that it is bound to", async () => {
      await setup(createProcessModelForm(caseDefinition), caseDefinition);

      expect(await offeredProcessDefinitions()).toEqual([
        "Generiek zaakafhandelmodel",
        "Ander zaakafhandelmodel",
      ]);
    });
  });
});
