/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { render, screen, waitFor, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import {
  createMutationOptions,
  createQueryOptions,
  fromPartial,
} from "src/test-helpers";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { IdentityService } from "../../identity/identity.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { SmartDocumentsService } from "../smart-documents.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParametersEditComponent } from "./parameters-edit.component";

type ZaaktypeConfiguration = GeneratedType<"RestZaaktypeConfiguration">;

const caseDefinition = fromPartial<GeneratedType<"RestProcessDefinition">>({
  processEngine: "CMMN",
  key: "fakeCaseDefinitionKey",
  naam: "Generiek zaakafhandelmodel",
  humanTaskDefinitions: [{ id: "fakeTaskId", naam: "Taak" }],
  userEventListenerDefinitions: [
    { id: "INTAKE_AFRONDEN", naam: "Intake afronden" },
  ],
});

const bpmnProcessDefinition = fromPartial<
  GeneratedType<"RestProcessDefinition">
>({
  processEngine: "BPMN",
  key: "fakeProcessDefinitionKey",
  naam: "Vergunningsproces",
});

const sharedSettings = {
  zaaktype: fromPartial<GeneratedType<"RestZaaktypeOverzicht">>({
    uuid: "fakeZaaktypeUuid",
    omschrijving: "fakeZaaktypeOmschrijving",
  }),
  defaultGroepId: "test-group-id",
  zaakNietOntvankelijkResultaattype: { id: "resultaat-1", naam: "Afgehandeld" },
  zaakbeeindigParameters: [],
  zaakAfzenders: [{ mail: "GEMEENTE", isSpeciaal: true, isDefaultMail: true }],
  mailtemplateKoppelingen: [],
  betrokkeneKoppelingen: {
    isBrpKoppelenEnabled: false,
    isKvkKoppelenEnabled: false,
  },
  brpDoelbindingen: {},
  smartDocuments: { isEnabledGlobally: true, isEnabledForZaaktype: true },
};

const formulierDefinities = [
  fromPartial<GeneratedType<"RestTaakFormulierDefinitie">>({
    id: "ADVIES",
    veldDefinities: [{ naam: "ADVIES" }],
  }),
];

const adviesReferentieTabel = fromPartial<GeneratedType<"RestReferenceTable">>({
  id: 1,
  code: "ADVIES",
  name: "Advies",
});

const cmmnConfiguration = fromPartial<ZaaktypeConfiguration>({
  ...sharedSettings,
  processDefinition: caseDefinition,
  humanTaskParameters: [],
  userEventListenerParameters: [],
});

const bpmnConfiguration = fromPartial<ZaaktypeConfiguration>({
  ...sharedSettings,
  processDefinition: bpmnProcessDefinition,
});

const unsavedConfiguration = fromPartial<ZaaktypeConfiguration>({
  ...sharedSettings,
  defaultGroepId: null,
  smartDocuments: { isEnabledGlobally: false, isEnabledForZaaktype: false },
});

const caseDefinitionSteps = [
  "gegevens.procesmodel",
  "gegevens.algemeen",
  "gegevens.humantasks",
  "gegevens.acties",
  "gegevens.mail",
  "gegevens.beeindiging",
  "gegevens.koppelingen",
];

const bpmnProcessDefinitionSteps = [
  "gegevens.procesmodel",
  "gegevens.algemeen",
  "gegevens.beeindiging",
  "gegevens.koppelingen",
];

const otherBpmnProcessDefinition = fromPartial<
  GeneratedType<"RestProcessDefinition">
>({
  processEngine: "BPMN",
  key: "fakeOtherProcessDefinitionKey",
  naam: "Nieuw vergunningsproces",
});

describe(ParametersEditComponent.name, () => {
  const user = userEvent.setup();

  async function setup(
    configuration: ZaaktypeConfiguration,
    savedConfiguration = configuration,
    storedConfiguration = configuration,
  ) {
    const updateZaakafhandelparameters =
      createMutationOptions(savedConfiguration);
    const readZaakafhandelparameters = {
      ...createQueryOptions(storedConfiguration),
      queryKey: ["zaakafhandelparameters"],
    };

    await render(ParametersEditComponent, {
      imports: [NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: fromPartial<ActivatedRoute>({
            snapshot: { data: { parameters: configuration } },
          }),
        },
        {
          provide: ConfiguratieService,
          useValue: fromPartial<ConfiguratieService>({
            readBrpDoelbindingSetupEnabled: () => of(false),
          }),
        },
        {
          provide: ZaakafhandelParametersService,
          useValue: fromPartial<ZaakafhandelParametersService>({
            listProcessDefinitions: () => ({
              ...createQueryOptions([
                caseDefinition,
                bpmnProcessDefinition,
                otherBpmnProcessDefinition,
              ]),
              queryKey: ["processDefinitions"],
            }),
            updateZaakafhandelparameters: () => updateZaakafhandelparameters,
            readZaakafhandelparameters: () => readZaakafhandelparameters,
            listFormulierDefinities: () => ({
              ...createQueryOptions(formulierDefinities),
              queryKey: ["formulierDefinities"],
            }),
          }),
        },
        {
          provide: IdentityService,
          useValue: fromPartial<IdentityService>({
            listBehandelaarGroupsForZaaktype: () => ({
              ...createQueryOptions([
                { id: "test-group-id", naam: "test-group" },
              ]),
              queryKey: ["behandelaarGroups"],
            }),
            listUsersInGroup: () => ({
              ...createQueryOptions([]),
              queryKey: ["usersInGroup"],
            }),
          }),
        },
        {
          provide: ReferentieTabelService,
          useValue: fromPartial<ReferentieTabelService>({
            listBrpSearchValues: () => createQueryOptions([]),
            listBrpViewValues: () => createQueryOptions([]),
            listBrpProcessingValues: () => createQueryOptions([]),
            listReferentieTabellen: () => ({
              ...createQueryOptions([adviesReferentieTabel]),
              queryKey: ["referentieTabellen"],
            }),
          }),
        },
        {
          provide: SmartDocumentsService,
          useValue: fromPartial<SmartDocumentsService>({
            getAllSmartDocumentsTemplateGroups: () =>
              createQueryOptions([]) as never,
            getTemplatesMappingQuery: () => createQueryOptions([]) as never,
            addParentIdsToTemplates: () => [],
            addTemplateMappings: () => [],
            flattenGroups: () => [],
            getTemplateMappings: () => [],
            storeTemplatesMapping: () => createMutationOptions(null) as never,
          }),
        },
        {
          provide: InformatieObjectenService,
          useValue: fromPartial<InformatieObjectenService>({
            listInformatieobjecttypes: () => createQueryOptions([]) as never,
          }),
        },
      ],
    });

    expect(await screen.findAllByRole("tab")).not.toHaveLength(0);

    return { updateZaakafhandelparameters, readZaakafhandelparameters };
  }

  function expectSteps(labels: string[]) {
    const steps = screen.getAllByRole("tab");
    expect(steps).toHaveLength(labels.length);
    labels.forEach((label, index) =>
      expect(steps[index]).toHaveAccessibleName(new RegExp(`${label}$`)),
    );
  }

  const opslaan = () =>
    within(screen.getByRole("tabpanel")).getByRole("button", {
      name: "actie.opslaan",
    });

  async function goToStep(label: string) {
    await user.click(screen.getByRole("tab", { name: new RegExp(label) }));
    return screen.getByRole("tabpanel", { name: new RegExp(label) });
  }

  async function chooseProcessDefinition(naam: string) {
    await user.click(screen.getByRole("combobox", { name: /procesmodel/i }));
    await user.click(await screen.findByRole("option", { name: naam }));
  }

  describe.each([
    ["CMMN case definition", cmmnConfiguration, caseDefinitionSteps],
    ["BPMN process definition", bpmnConfiguration, bpmnProcessDefinitionSteps],
  ])(
    "for a configuration that is bound to a %s",
    (_definition, configuration, steps) => {
      it("shows the steps that its process definition has use for", async () => {
        await setup(configuration);

        expectSteps(steps);
      });

      it("starts at the algemeen step", async () => {
        await setup(configuration);

        expect(screen.getByRole("tab", { selected: true })).toHaveTextContent(
          "gegevens.algemeen",
        );
      });

      it("does not let the beheerder choose another process definition when the backend does not allow it", async () => {
        await setup(configuration);

        const procesModel = await goToStep("gegevens.procesmodel");

        expect(
          within(procesModel).getByRole("combobox", { name: /procesmodel/i }),
        ).toHaveAttribute("aria-disabled", "true");
        expect(
          within(procesModel).getByText(/^zaps\.step\.procesmodel\.locked\./),
        ).toBeInTheDocument();
      });

      it("does not read the configuration again when saving fails, as its process definition is locked already", async () => {
        const { updateZaakafhandelparameters, readZaakafhandelparameters } =
          await setup(configuration);
        updateZaakafhandelparameters.mutationFn.mockRejectedValue(
          new Error("msg.error.server.generic"),
        );

        await waitFor(() => expect(opslaan()).toBeEnabled());
        await user.click(opslaan());

        await waitFor(() =>
          expect(updateZaakafhandelparameters.mutationFn).toHaveBeenCalled(),
        );
        await new Promise((resolve) => setTimeout(resolve));
        expect(readZaakafhandelparameters.queryFn).not.toHaveBeenCalled();
      });

      it("stores its process definition and keeps SmartDocuments switched on without opening the koppelingen step", async () => {
        const { updateZaakafhandelparameters } = await setup(configuration);

        await waitFor(() => expect(opslaan()).toBeEnabled());
        await user.click(opslaan());

        await waitFor(() =>
          expect(updateZaakafhandelparameters.mutationFn).toHaveBeenCalledWith(
            expect.objectContaining({
              processDefinition: configuration.processDefinition,
              smartDocuments: {
                isEnabledGlobally: true,
                isEnabledForZaaktype: true,
              },
            }),
            expect.anything(),
          ),
        );
      });
    },
  );

  describe("for a BPMN zaaktype version whose process definition may change", () => {
    const changeableBpmnConfiguration = fromPartial<ZaaktypeConfiguration>({
      ...bpmnConfiguration,
      isProcessDefinitionChangeable: true,
    });

    it("offers only the BPMN process definitions", async () => {
      await setup(changeableBpmnConfiguration);

      const procesModel = await goToStep("gegevens.procesmodel");
      await user.click(
        within(procesModel).getByRole("combobox", { name: /procesmodel/i }),
      );

      expect(
        (await screen.findAllByRole("option")).map((option) =>
          option.textContent?.trim(),
        ),
      ).toEqual(["Vergunningsproces", "Nieuw vergunningsproces"]);
    });

    it("does not explain a lock", async () => {
      await setup(changeableBpmnConfiguration);

      const procesModel = await goToStep("gegevens.procesmodel");

      expect(
        within(procesModel).queryByText(/^zaps\.step\.procesmodel\.locked\./),
      ).not.toBeInTheDocument();
    });

    it("locks the process model and explains why when a zaak was created before the beheerder saved another one", async () => {
      const { updateZaakafhandelparameters } = await setup(
        changeableBpmnConfiguration,
        changeableBpmnConfiguration,
        bpmnConfiguration,
      );
      updateZaakafhandelparameters.mutationFn.mockRejectedValue(
        new Error("msg.error.process.definition.cannot.change"),
      );

      await goToStep("gegevens.procesmodel");
      await chooseProcessDefinition("Nieuw vergunningsproces");
      await waitFor(() => expect(opslaan()).toBeEnabled());
      await user.click(opslaan());

      expect(
        await screen.findByText("zaps.step.procesmodel.locked.bpmn"),
      ).toBeInTheDocument();
      const procesModelCombobox = within(
        screen.getByRole("tabpanel", { name: /gegevens.procesmodel/ }),
      ).getByRole("combobox", { name: /procesmodel/i });
      expect(procesModelCombobox).toHaveAttribute("aria-disabled", "true");
      await waitFor(() =>
        expect(procesModelCombobox).toHaveTextContent("Vergunningsproces"),
      );
      expect(procesModelCombobox).not.toHaveTextContent(
        "Nieuw vergunningsproces",
      );
    });

    it("keeps the chosen process model open to change when saving fails for another reason", async () => {
      const { updateZaakafhandelparameters, readZaakafhandelparameters } =
        await setup(changeableBpmnConfiguration);
      updateZaakafhandelparameters.mutationFn.mockRejectedValue(
        new Error("msg.error.server.generic"),
      );

      await goToStep("gegevens.procesmodel");
      await chooseProcessDefinition("Nieuw vergunningsproces");
      await waitFor(() => expect(opslaan()).toBeEnabled());
      await user.click(opslaan());

      await waitFor(() =>
        expect(readZaakafhandelparameters.queryFn).toHaveBeenCalled(),
      );
      await readZaakafhandelparameters.queryFn.mock.results[0].value;
      await new Promise((resolve) => setTimeout(resolve));

      await waitFor(() => {
        const procesModelCombobox = screen.getByRole("combobox", {
          name: /procesmodel/i,
        });
        expect(procesModelCombobox).not.toHaveAttribute(
          "aria-disabled",
          "true",
        );
        expect(procesModelCombobox).toHaveTextContent(
          "Nieuw vergunningsproces",
        );
      });
    });

    it("stores the other BPMN process definition that the beheerder chooses", async () => {
      const { updateZaakafhandelparameters } = await setup(
        changeableBpmnConfiguration,
      );

      await goToStep("gegevens.procesmodel");
      await chooseProcessDefinition("Nieuw vergunningsproces");
      await waitFor(() => expect(opslaan()).toBeEnabled());
      await user.click(opslaan());

      await waitFor(() =>
        expect(updateZaakafhandelparameters.mutationFn).toHaveBeenCalledWith(
          expect.objectContaining({
            processDefinition: otherBpmnProcessDefinition,
          }),
          expect.anything(),
        ),
      );
    });
  });

  it("stores the default reference table of each formulier field without opening the Taken step", async () => {
    const { updateZaakafhandelparameters } = await setup(
      fromPartial<ZaaktypeConfiguration>({
        ...cmmnConfiguration,
        humanTaskParameters: [
          {
            planItemDefinition: { id: "fakeTaskId", naam: "Taak" },
            formulierDefinitieId: "ADVIES",
            actief: true,
            referentieTabellen: [],
          },
        ],
      }),
    );

    await waitFor(() => expect(opslaan()).toBeEnabled());
    await user.click(opslaan());

    await waitFor(() =>
      expect(updateZaakafhandelparameters.mutationFn).toHaveBeenCalledWith(
        expect.objectContaining({
          humanTaskParameters: [
            expect.objectContaining({
              referentieTabellen: [
                { veld: "ADVIES", tabel: adviesReferentieTabel },
              ],
            }),
          ],
        }),
        expect.anything(),
      ),
    );
  });

  it("stores SmartDocuments switched off for the zaaktype once it is switched off in the koppelingen step", async () => {
    const { updateZaakafhandelparameters } = await setup(bpmnConfiguration);

    const koppelingen = await goToStep("gegevens.koppelingen");
    await user.click(
      within(koppelingen).getByRole("switch", {
        name: "title.smartdocuments.form",
      }),
    );
    await user.click(
      within(koppelingen).getByRole("button", { name: "actie.opslaan" }),
    );

    await waitFor(() =>
      expect(updateZaakafhandelparameters.mutationFn).toHaveBeenCalledWith(
        expect.objectContaining({
          smartDocuments: {
            isEnabledGlobally: true,
            isEnabledForZaaktype: false,
          },
        }),
        expect.anything(),
      ),
    );
  });

  it("lets the beheerder save a BPMN zaaktype without a default zaakafzender and keeps its zaakafzenders", async () => {
    const zaakAfzenders = [
      { mail: "GEMEENTE", isSpeciaal: true, isDefaultMail: false },
    ];
    const { updateZaakafhandelparameters } = await setup(
      fromPartial<ZaaktypeConfiguration>({
        ...bpmnConfiguration,
        zaakAfzenders,
      }),
    );

    await waitFor(() => expect(opslaan()).toBeEnabled());
    await user.click(opslaan());

    await waitFor(() =>
      expect(updateZaakafhandelparameters.mutationFn).toHaveBeenCalledWith(
        expect.objectContaining({
          zaakAfzenders: [expect.objectContaining(zaakAfzenders[0])],
        }),
        expect.anything(),
      ),
    );
  });

  it("stores the deadline warning window that a beheerder sets for a BPMN zaaktype", async () => {
    const { updateZaakafhandelparameters } = await setup(bpmnConfiguration);

    const algemeen = screen.getByRole("tabpanel", {
      name: /gegevens.algemeen/,
    });
    await user.type(
      within(algemeen).getByLabelText(
        /uiterlijke-einddatum-afdoening-waarschuwing/i,
      ),
      "2",
    );
    await user.click(
      within(algemeen).getByRole("button", { name: "actie.opslaan" }),
    );

    await waitFor(() =>
      expect(updateZaakafhandelparameters.mutationFn).toHaveBeenCalledWith(
        expect.objectContaining({
          uiterlijkeEinddatumAfdoeningWaarschuwing: 2,
        }),
        expect.anything(),
      ),
    );
  });

  describe("for a configuration that was never saved", () => {
    it("first shows only the process model step, without the option to save", async () => {
      await setup(unsavedConfiguration);

      expectSteps(["gegevens.procesmodel"]);
      expect(opslaan()).toBeDisabled();
    });

    it.each([
      ["Generiek zaakafhandelmodel", caseDefinitionSteps],
      ["Vergunningsproces", bpmnProcessDefinitionSteps],
    ])(
      "shows the steps that %s has use for once it is chosen",
      async (naam, steps) => {
        await setup(unsavedConfiguration);

        await chooseProcessDefinition(naam);

        await waitFor(() => expectSteps(steps));
      },
    );

    async function saveAsBpmnConfiguration() {
      await chooseProcessDefinition("Vergunningsproces");
      const algemeen = await goToStep("gegevens.algemeen");
      await user.click(
        within(algemeen).getByRole("combobox", { name: /^groep.standaard$/i }),
      );
      await user.click(
        await screen.findByRole("option", { name: "test-group" }),
      );
      await waitFor(() => expect(opslaan()).toBeEnabled());
      await user.click(opslaan());
    }

    it("follows the backend after saving, and no longer lets the beheerder choose another process definition when it does not allow it", async () => {
      await setup(unsavedConfiguration, bpmnConfiguration);

      await saveAsBpmnConfiguration();
      const procesModel = await goToStep("gegevens.procesmodel");

      await waitFor(() =>
        expect(
          within(procesModel).getByRole("combobox", { name: /procesmodel/i }),
        ).toHaveAttribute("aria-disabled", "true"),
      );
    });

    it("follows the backend after saving, and keeps letting the beheerder choose another process definition when it allows it", async () => {
      const { updateZaakafhandelparameters } = await setup(
        unsavedConfiguration,
        fromPartial<ZaaktypeConfiguration>({
          ...bpmnConfiguration,
          isProcessDefinitionChangeable: true,
        }),
      );

      await saveAsBpmnConfiguration();
      await waitFor(() =>
        expect(updateZaakafhandelparameters.mutationFn).toHaveBeenCalled(),
      );
      const procesModel = await goToStep("gegevens.procesmodel");

      await waitFor(() =>
        expect(
          within(procesModel).getByText("Vergunningsproces"),
        ).toBeInTheDocument(),
      );
      expect(
        within(procesModel).getByRole("combobox", { name: /procesmodel/i }),
      ).not.toHaveAttribute("aria-disabled", "true");
    });

    it("keeps the filled in settings when the beheerder goes back to the process model step and chooses another process definition", async () => {
      await setup(unsavedConfiguration);

      await chooseProcessDefinition("Vergunningsproces");
      const algemeen = await goToStep("gegevens.algemeen");
      await user.type(
        within(algemeen).getByRole("textbox", { name: /productaanvraagtype/i }),
        "fakeProductaanvraagtype",
      );
      await goToStep("gegevens.procesmodel");
      await chooseProcessDefinition("Generiek zaakafhandelmodel");
      await waitFor(() => expectSteps(caseDefinitionSteps));

      const algemeenOfCaseDefinition = await goToStep("gegevens.algemeen");

      expect(
        within(algemeenOfCaseDefinition).getByRole("textbox", {
          name: /productaanvraagtype/i,
        }),
      ).toHaveValue("fakeProductaanvraagtype");
    });
  });
});
