/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { render, screen, waitFor, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { IdentityService } from "../../../../identity/identity.service";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ReferentieTabelService } from "../../../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../../../zaakafhandel-parameters.service";
import {
  createHumanTasksForm,
  createHumanTasksFormForProcessDefinition,
  toHumanTaskParameters,
} from "./human-tasks-form";
import { ParametersStepHumantasksComponent } from "./parameters-step-humantasks.component";

const formulierDefinities = [
  fromPartial<GeneratedType<"RESTTaakFormulierDefinitie">>({
    id: "DEFAULT_TAAKFORMULIER",
    veldDefinities: [],
  }),
  fromPartial<GeneratedType<"RESTTaakFormulierDefinitie">>({
    id: "ADVIES",
    veldDefinities: [{ naam: "ADVIES" }],
  }),
];

const adviesTabel = fromPartial<GeneratedType<"RestReferenceTable">>({
  id: 1,
  code: "ADVIES",
  name: "Advies",
});

const humanTaskParameters = [
  fromPartial<GeneratedType<"RESTHumanTaskParameters">>({
    planItemDefinition: { id: "task-1", naam: "Taak 1", type: "HUMAN_TASK" },
    actief: true,
    doorlooptijd: 5,
    formulierDefinitieId: undefined,
    referentieTabellen: [],
  }),
  fromPartial<GeneratedType<"RESTHumanTaskParameters">>({
    planItemDefinition: {
      id: "task-3",
      naam: "Verlenging aanvragen",
      type: "HUMAN_TASK",
    },
    actief: true,
    doorlooptijd: 5,
    formulierDefinitieId: "DEFAULT_TAAKFORMULIER",
    referentieTabellen: [],
  }),
  fromPartial<GeneratedType<"RESTHumanTaskParameters">>({
    planItemDefinition: {
      id: "task-2",
      naam: "Uitstel aanvragen",
      type: "HUMAN_TASK",
    },
    actief: true,
    doorlooptijd: 5,
    formulierDefinitieId: "DEFAULT_TAAKFORMULIER",
    referentieTabellen: [],
  }),
];

describe(ParametersStepHumantasksComponent.name, () => {
  const user = userEvent.setup();

  async function setup() {
    const form = createHumanTasksForm(humanTaskParameters);

    await render(ParametersStepHumantasksComponent, {
      imports: [NoopAnimationsModule],
      inputs: { form, zaaktypeOmschrijving: "fakeOmschrijving" },
      providers: [
        provideTranslateService(),
        {
          provide: ZaakafhandelParametersService,
          useValue: fromPartial<ZaakafhandelParametersService>({
            listFormulierDefinities: () => ({
              ...createQueryOptions(formulierDefinities),
              queryKey: ["formulierDefinities"],
            }),
          }),
        },
        {
          provide: ReferentieTabelService,
          useValue: fromPartial<ReferentieTabelService>({
            listReferentieTabellenQuery: () => ({
              ...createQueryOptions([adviesTabel]),
              queryKey: ["referentieTabellen"],
            }),
          }),
        },
        {
          provide: IdentityService,
          useValue: fromPartial<IdentityService>({
            listBehandelaarGroupsForZaaktypeQuery: () => ({
              ...createQueryOptions([
                { id: "test-group-id", naam: "test-group" },
              ]),
              queryKey: ["behandelaarGroups"],
            }),
          }),
        },
      ],
    });

    return form;
  }

  function taskHeader(task: string) {
    return screen.getByRole("button", { name: new RegExp(task) });
  }

  function taskFields(task: string) {
    return screen.getByRole("region", { name: new RegExp(task) });
  }

  async function chooseFormulierDefinitie(
    task: string,
    formulierDefinitieLabel: string,
  ) {
    await user.click(
      within(taskFields(task)).getByRole("combobox", {
        name: /^formulier-definitie$/i,
      }),
    );
    await user.click(
      await screen.findByRole("option", { name: formulierDefinitieLabel }),
    );
  }

  it("lists the human tasks alphabetically by name", async () => {
    await setup();

    expect(
      screen
        .getAllByText(/^(Taak 1|Uitstel aanvragen|Verlenging aanvragen)$/)
        .map((title) => title.textContent?.trim()),
    ).toEqual(["Taak 1", "Uitstel aanvragen", "Verlenging aanvragen"]);
  });

  it("shows the settings of a human task when its panel is opened", async () => {
    await setup();

    await user.click(taskHeader("Taak 1"));

    const fields = within(taskFields("Taak 1"));
    expect(
      fields.getByRole("combobox", { name: /^formulier-definitie$/i }),
    ).toBeVisible();
    expect(
      fields.getByRole("combobox", { name: /^groep.standaard$/i }),
    ).toBeVisible();
    expect(
      fields.getByRole("spinbutton", { name: /^doorlooptijd$/i }),
    ).toHaveValue(5);
  });

  it("shows a human task as active by default", async () => {
    await setup();

    expect(within(taskHeader("Taak 1")).getByRole("switch")).toBeChecked();
  });

  it("marks a human task without a form definition as incomplete", async () => {
    await setup();

    expect(within(taskHeader("Taak 1")).getByText("error")).toBeVisible();
    expect(
      within(taskHeader("Uitstel aanvragen")).getByText("check_circle"),
    ).toBeVisible();
  });

  it("marks a human task as complete once a form definition is chosen", async () => {
    await setup();

    await user.click(taskHeader("Taak 1"));
    await chooseFormulierDefinitie(
      "Taak 1",
      "formulier-definitie.default-taakformulier",
    );

    expect(
      within(taskHeader("Taak 1")).getByText("check_circle"),
    ).toBeVisible();
  });

  it("marks a human task with a negative doorlooptijd as incomplete", async () => {
    await setup();

    await user.click(taskHeader("Uitstel aanvragen"));
    const doorlooptijd = within(taskFields("Uitstel aanvragen")).getByRole(
      "spinbutton",
      { name: /^doorlooptijd$/i },
    );

    await user.clear(doorlooptijd);
    await user.paste("-1");

    expect(
      within(taskHeader("Uitstel aanvragen")).getByText("error"),
    ).toBeVisible();
  });

  it("asks for a reference table per field of the chosen form definition, preset to the table of that field", async () => {
    await setup();

    await user.click(taskHeader("Taak 1"));
    await chooseFormulierDefinitie("Taak 1", "formulier-definitie.advies");

    await waitFor(() =>
      expect(
        within(taskFields("Taak 1")).getByRole("combobox", {
          name: /^referentietabel.advies$/i,
        }),
      ).toHaveTextContent("Advies"),
    );
  });

  it("stores the doorlooptijd and the active state as they were filled in", async () => {
    const form = await setup();

    await user.click(taskHeader("Taak 1"));
    await chooseFormulierDefinitie(
      "Taak 1",
      "formulier-definitie.default-taakformulier",
    );
    const doorlooptijd = within(taskFields("Taak 1")).getByRole("spinbutton", {
      name: /^doorlooptijd$/i,
    });
    await user.clear(doorlooptijd);
    await user.type(doorlooptijd, "10");
    await user.click(within(taskHeader("Taak 1")).getByRole("switch"));

    expect(toHumanTaskParameters(form)).toContainEqual(
      expect.objectContaining({
        planItemDefinition: expect.objectContaining({ naam: "Taak 1" }),
        doorlooptijd: 10,
        actief: false,
        formulierDefinitieId: "DEFAULT_TAAKFORMULIER",
      }),
    );
  });

  it("starts the human tasks of a newly chosen case definition as active, with their default formulier and the default groep", () => {
    const form = createHumanTasksFormForProcessDefinition(
      fromPartial<GeneratedType<"RestProcessDefinition">>({
        humanTaskDefinitions: [
          {
            id: "task-advies",
            naam: "Advies",
            defaultFormulierDefinitie: "ADVIES",
          },
        ],
      }),
      "fakeGroepId",
    );

    expect(toHumanTaskParameters(form)).toEqual([
      expect.objectContaining({
        planItemDefinition: expect.objectContaining({ id: "task-advies" }),
        formulierDefinitieId: "ADVIES",
        defaultGroepId: "fakeGroepId",
        actief: true,
      }),
    ]);
  });
});
