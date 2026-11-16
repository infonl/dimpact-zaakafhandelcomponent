/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
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

const otherBpmnProcessDefinition: ProcessDefinition = {
  processEngine: "BPMN",
  key: "fakeOtherProcessDefinitionKey",
  naam: "Nieuw vergunningsproces",
};

const caseDefinitionNamedAsBpmnProcessDefinition: ProcessDefinition = {
  processEngine: "CMMN",
  key: "fakeVergunningCaseDefinitionKey",
  naam: "Vergunningsproces",
};

describe(ParametersStepProcesModelComponent.name, () => {
  const user = userEvent.setup();

  async function setup(
    form: ProcessModelForm,
    processDefinitions = [
      caseDefinition,
      otherCaseDefinition,
      bpmnProcessDefinition,
      otherBpmnProcessDefinition,
    ],
    isProcessDefinitionChangeable?: boolean,
    zaaktype = fromPartial<GeneratedType<"RestZaaktypeOverzicht">>({
      omschrijving: "Test zaaktype omschrijving",
      identificatie: "test-zaaktype",
      versiedatum: "2026-11-16",
      beginGeldigheid: "2026-11-16",
    }),
  ) {
    await render(ParametersStepProcesModelComponent, {
      imports: [NoopAnimationsModule],
      inputs: {
        form,
        zaaktype,
        isBound: form.controls.processDefinition.value !== null,
        isProcessDefinitionChangeable,
      },
      providers: [
        provideTranslateService(),
        {
          provide: ZaakafhandelParametersService,
          useValue: fromPartial<ZaakafhandelParametersService>({
            listProcessDefinitions: () => ({
              ...createQueryOptions(processDefinitions),
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

  it.each([
    ["without an end date", null, "zaps.step.procesmodel.versie"],
    ["with an end date", "2026-12-31", "zaps.step.procesmodel.versie.einde"],
  ])(
    "shows the identificatie and the validity of the zaaktype version %s",
    async (_situation, eindeGeldigheid, versionText) => {
      await setup(
        createProcessModelForm(null),
        undefined,
        true,
        fromPartial<GeneratedType<"RestZaaktypeOverzicht">>({
          omschrijving: "Test zaaktype omschrijving",
          identificatie: "test-zaaktype",
          versiedatum: "2026-11-16",
          beginGeldigheid: "2026-11-16",
          eindeGeldigheid,
        }),
      );

      expect(screen.getByText(versionText)).toBeVisible();
    },
  );

  describe("for a configuration that was never saved", () => {
    it("offers the process definitions of every process engine, without choosing one", async () => {
      const form = createProcessModelForm(null);
      await setup(form);

      expect(await offeredProcessDefinitions()).toEqual([
        "Generiek zaakafhandelmodel",
        "Ander zaakafhandelmodel",
        "Vergunningsproces",
        "Nieuw vergunningsproces",
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

  describe.each([
    [
      "BPMN",
      bpmnProcessDefinition,
      caseDefinition,
      "Generiek zaakafhandelmodel",
    ],
    ["CMMN", caseDefinition, bpmnProcessDefinition, "Vergunningsproces"],
  ] as const)(
    "for a configuration bound to %s whose zaaktype version has no zaken",
    (_engine, boundProcessDefinition, otherEngineProcessDefinition, name) => {
      it("offers the process definitions of every process engine", async () => {
        await setup(
          createProcessModelForm(boundProcessDefinition),
          undefined,
          true,
        );

        expect(await offeredProcessDefinitions()).toEqual([
          "Generiek zaakafhandelmodel",
          "Ander zaakafhandelmodel",
          "Vergunningsproces",
          "Nieuw vergunningsproces",
        ]);
      });

      it("lets the beheerder choose a process definition of the other process engine", async () => {
        const form = createProcessModelForm(boundProcessDefinition);
        await setup(form, undefined, true);

        await offeredProcessDefinitions();
        await user.click(screen.getByRole("option", { name }));

        expect(form.value.processDefinition).toEqual(
          otherEngineProcessDefinition,
        );
      });

      it("explains that the process definition can change until the first zaak", async () => {
        await setup(
          createProcessModelForm(boundProcessDefinition),
          undefined,
          true,
        );

        expect(
          screen.getByText("zaps.step.procesmodel.changeable"),
        ).toBeVisible();
      });

      it("does not explain a lock, as its process definition can change", async () => {
        await setup(
          createProcessModelForm(boundProcessDefinition),
          undefined,
          true,
        );

        expect(
          screen.queryByText("zaps.step.procesmodel.locked"),
        ).not.toBeInTheDocument();
      });
    },
  );

  it.each([
    ["BPMN", bpmnProcessDefinition],
    ["CMMN", caseDefinition],
  ] as const)(
    "explains why the beheerder cannot choose another process definition for a configuration bound to %s whose zaaktype version has zaken",
    async (_engine, boundProcessDefinition) => {
      await setup(
        createProcessModelForm(boundProcessDefinition),
        undefined,
        false,
      );

      expect(screen.getByText("zaps.step.procesmodel.locked")).toBeVisible();
    },
  );

  it("shows no hint for a configuration that is not bound yet", async () => {
    await setup(createProcessModelForm(null), undefined, true);

    expect(
      screen.queryByText("zaps.step.procesmodel.changeable"),
    ).not.toBeInTheDocument();

    expect(
      screen.queryByText("zaps.step.procesmodel.locked"),
    ).not.toBeInTheDocument();
  });

  it("marks only the chosen process definition as selected when a CMMN and a BPMN process definition have the same name", async () => {
    await setup(createProcessModelForm({ ...bpmnProcessDefinition }), [
      caseDefinitionNamedAsBpmnProcessDefinition,
      bpmnProcessDefinition,
    ]);

    await offeredProcessDefinitions();

    expect(
      screen
        .getAllByRole("option")
        .map((option) => option.getAttribute("aria-selected")),
    ).toEqual(["false", "true"]);
  });

  it("does not let the beheerder choose another process definition once the form is locked", async () => {
    const form = createProcessModelForm(caseDefinition);
    form.disable();
    await setup(form);

    expect(
      screen.getByRole("combobox", { name: /procesmodel/i }),
    ).toHaveAttribute("aria-disabled", "true");
    expect(form.controls.processDefinition.value).toEqual(caseDefinition);
  });
});
