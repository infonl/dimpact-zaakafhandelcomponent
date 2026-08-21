/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { fireEvent, render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { createMutationOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParametersEditCmmnComponent } from "./parameters-edit-cmmn.component";

// rendering this seven step form once per test needs more room than the default timeout
describe("Human tasks form step", () => {
  const user = userEvent.setup();

  const caseDefinition = fromPartial<GeneratedType<"RESTCaseDefinition">>({
    key: "case-1",
    naam: "Case Definition 1",
  });

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

  const zaakafhandelParameters = fromPartial<
    GeneratedType<"RestZaaktypeConfiguration">
  >({
    caseDefinition,
    defaultGroepId: "test-group-id",
    defaultBehandelaarId: "test-user-id",
    zaaktype: { uuid: "test-uuid" },
    zaakNietOntvankelijkResultaattype: {
      id: "resultaat-1",
      naam: "Afgehandeld",
    },
    zaakAfzenders: [
      {
        speciaal: false,
        defaultMail: true,
        mail: "test@example.com",
        replyTo: undefined,
      },
    ],
    humanTaskParameters,
    mailtemplateKoppelingen: [],
    zaakbeeindigParameters: [],
    smartDocuments: { enabledGlobally: false, enabledForZaaktype: false },
    userEventListenerParameters: [],
    betrokkeneKoppelingen: { brpKoppelen: false, kvkKoppelen: false },
    brpDoelbindingen: {
      zoekWaarde: "",
      raadpleegWaarde: "",
      verwerkingregisterWaarde: "",
    },
    productaanvraagtype: null,
    automaticEmailConfirmation: {
      enabled: false,
      templateName: null,
      emailSender: null,
      emailReply: null,
    },
  });

  async function setup() {
    const updateZaakafhandelparameters = createMutationOptions(
      zaakafhandelParameters,
    );

    const { fixture } = await render(ParametersEditCmmnComponent, {
      imports: [TranslateModule.forRoot(), RouterModule, NoopAnimationsModule],
      providers: [
        provideQueryClient(testQueryClient),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: fromPartial<ActivatedRoute>({
            data: of({
              parameters: {
                zaakafhandelParameters,
                isSavedZaakafhandelParameters: true,
              },
            }),
          }),
        },
        {
          provide: ZaakafhandelParametersService,
          useValue: fromPartial<ZaakafhandelParametersService>({
            listCaseDefinitions: () => of([caseDefinition]),
            listFormulierDefinities: () => of(formulierDefinities),
            listReplyTos: () => of([{ mail: "reply1@example.com" }]),
            listZaakbeeindigRedenen: () => of([]),
            listResultaattypes: () => of([]),
            updateZaakafhandelparameters: () => updateZaakafhandelparameters,
          }),
        },
        {
          provide: ReferentieTabelService,
          useValue: fromPartial<ReferentieTabelService>({
            listReferentieTabellen: () =>
              of([{ id: 1, code: "ADVIES", name: "Advies" }]),
            listAfzenders: () => of(["other@example.com"]),
            listBrpSearchValues: () => of([]),
            listBrpViewValues: () => of([]),
            listBrpProcessingValues: () => of([]),
          }),
        },
        {
          provide: IdentityService,
          useValue: fromPartial<IdentityService>({
            listGroups: () => of([{ id: "test-group-id", naam: "test-group" }]),
            listUsersInGroup: () =>
              of([{ id: "test-user-id", naam: "test-user" }]),
          }),
        },
        {
          provide: MailtemplateBeheerService,
          useValue: fromPartial<MailtemplateBeheerService>({
            listKoppelbareMailtemplates: () => of([]),
          }),
        },
        {
          provide: ConfiguratieService,
          useValue: fromPartial<ConfiguratieService>({
            readBrpDoelbindingSetupEnabled: () => of(false),
          }),
        },
      ],
    });

    await fixture.whenStable();
    fixture.detectChanges();

    await user.click(screen.getByRole("tab", { name: /gegevens.humantasks/ }));

    return {
      humanTasks: screen.getByRole("tabpanel", {
        name: /gegevens.humantasks/,
      }),
      updateZaakafhandelparameters,
    };
  }

  function taskHeader(humanTasks: HTMLElement, task: string) {
    return within(humanTasks).getByRole("button", { name: new RegExp(task) });
  }

  function taskFields(humanTasks: HTMLElement, task: string) {
    return within(humanTasks).getByRole("region", { name: new RegExp(task) });
  }

  async function chooseFormulierDefinitie(
    humanTasks: HTMLElement,
    task: string,
    formulierDefinitieId: string,
  ) {
    await user.click(
      within(taskFields(humanTasks, task)).getByRole("combobox", {
        name: "formulierDefinitie",
      }),
    );
    await user.click(
      screen.getByRole("option", {
        name: `formulierDefinitie.${formulierDefinitieId}`,
      }),
    );
  }

  it("lists the human tasks alphabetically by name", async () => {
    const { humanTasks } = await setup();

    expect(
      within(humanTasks)
        .getAllByText(/^(Taak 1|Uitstel aanvragen|Verlenging aanvragen)$/)
        .map((title) => title.textContent?.trim()),
    ).toEqual(["Taak 1", "Uitstel aanvragen", "Verlenging aanvragen"]);
  });

  it("shows the settings of a human task when its panel is opened", async () => {
    const { humanTasks } = await setup();

    await user.click(taskHeader(humanTasks, "Taak 1"));

    const fields = within(taskFields(humanTasks, "Taak 1"));
    expect(
      fields.getByRole("combobox", { name: "formulierDefinitie" }),
    ).toBeVisible();
    expect(fields.getByRole("combobox", { name: "groep" })).toBeVisible();
    expect(
      fields.getByRole("spinbutton", { name: "doorlooptijd" }),
    ).toHaveValue(5);
  });

  it("shows a human task as active by default", async () => {
    const { humanTasks } = await setup();

    expect(
      within(taskHeader(humanTasks, "Taak 1")).getByRole("switch"),
    ).toBeChecked();
  });

  it("marks a human task without a form definition as incomplete", async () => {
    const { humanTasks } = await setup();

    expect(
      within(taskHeader(humanTasks, "Taak 1")).getByText("error"),
    ).toBeVisible();
    expect(
      within(taskHeader(humanTasks, "Uitstel aanvragen")).getByText(
        "check_circle",
      ),
    ).toBeVisible();
  });

  it("marks a human task as complete once a form definition is chosen", async () => {
    const { humanTasks } = await setup();

    await user.click(taskHeader(humanTasks, "Taak 1"));
    await chooseFormulierDefinitie(
      humanTasks,
      "Taak 1",
      "DEFAULT_TAAKFORMULIER",
    );

    expect(
      within(taskHeader(humanTasks, "Taak 1")).getByText("check_circle"),
    ).toBeVisible();
  });

  it("marks a human task with a negative doorlooptijd as incomplete", async () => {
    const { humanTasks } = await setup();

    await user.click(taskHeader(humanTasks, "Uitstel aanvragen"));
    const doorlooptijd = within(
      taskFields(humanTasks, "Uitstel aanvragen"),
    ).getByRole("spinbutton", { name: "doorlooptijd" });

    // a minus sign cannot be typed into a number input in jsdom, so the whole value is entered at once
    fireEvent.input(doorlooptijd, { target: { value: "-1" } });

    expect(
      within(taskHeader(humanTasks, "Uitstel aanvragen")).getByText("error"),
    ).toBeVisible();
  });

  it("asks for a reference table per field of the chosen form definition", async () => {
    const { humanTasks } = await setup();

    await user.click(taskHeader(humanTasks, "Taak 1"));
    await chooseFormulierDefinitie(humanTasks, "Taak 1", "ADVIES");

    expect(
      within(taskFields(humanTasks, "Taak 1")).getByRole("combobox", {
        name: "referentietabel.ADVIES",
      }),
    ).toBeVisible();
  });

  it("saves the doorlooptijd and the active state as they were filled in", async () => {
    const { humanTasks, updateZaakafhandelparameters } = await setup();

    await user.click(taskHeader(humanTasks, "Taak 1"));
    await chooseFormulierDefinitie(
      humanTasks,
      "Taak 1",
      "DEFAULT_TAAKFORMULIER",
    );

    const fields = within(taskFields(humanTasks, "Taak 1"));
    await user.clear(fields.getByRole("spinbutton", { name: "doorlooptijd" }));
    await user.type(
      fields.getByRole("spinbutton", { name: "doorlooptijd" }),
      "10",
    );
    await user.click(
      within(taskHeader(humanTasks, "Taak 1")).getByRole("switch"),
    );

    await user.click(
      within(humanTasks).getByRole("button", { name: "actie.opslaan" }),
    );
    await sleep();

    expect(updateZaakafhandelparameters.mutationFn.mock.calls[0][0]).toEqual(
      expect.objectContaining({
        humanTaskParameters: expect.arrayContaining([
          expect.objectContaining({
            planItemDefinition: expect.objectContaining({ naam: "Taak 1" }),
            doorlooptijd: 10,
            actief: false,
            formulierDefinitieId: "DEFAULT_TAAKFORMULIER",
          }),
        ]),
      }),
    );
  });
});
