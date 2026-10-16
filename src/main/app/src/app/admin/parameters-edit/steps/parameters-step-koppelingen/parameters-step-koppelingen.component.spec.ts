/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen, waitFor } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ReferentieTabelService } from "../../../referentie-tabel.service";
import {
  createKoppelingenForm,
  toBetrokkeneKoppelingen,
  toBrpDoelbindingen,
} from "./koppelingen-form";
import { ParametersStepKoppelingenComponent } from "./parameters-step-koppelingen.component";

const brpSearchValues = ["zoeken-1", "zoeken-2"];
const brpViewValues = ["raadplegen-1", "raadplegen-2"];
const brpProcessingValues = ["verwerken-1", "verwerken-2"];

const switchedOff = {
  betrokkeneKoppelingen: {
    isBrpKoppelenEnabled: false,
    isKvkKoppelenEnabled: false,
  },
  brpDoelbindingen: {
    zoekWaarde: "",
    raadpleegWaarde: "",
    verwerkingregisterWaarde: "",
  },
};

const switchedOn = {
  betrokkeneKoppelingen: {
    isBrpKoppelenEnabled: true,
    isKvkKoppelenEnabled: true,
  },
  brpDoelbindingen: {
    zoekWaarde: brpSearchValues[1],
    raadpleegWaarde: brpViewValues[1],
    verwerkingregisterWaarde: brpProcessingValues[1],
  },
};

describe.each([
  [
    "CMMN",
    (stored: typeof switchedOff) =>
      fromPartial<GeneratedType<"RestZaaktypeConfiguration">>(stored),
  ],
  [
    "BPMN",
    (stored: typeof switchedOff) =>
      fromPartial<GeneratedType<"RestZaaktypeBpmnConfiguration">>(stored),
  ],
])("Koppelingen step of a %s zaaktype", (_engine, configuration) => {
  const user = userEvent.setup();

  async function setup({
    stored = switchedOff,
    brpDoelbindingSetupEnabled = true,
  } = {}) {
    const form = createKoppelingenForm(configuration(stored), {
      isBrpDoelbindingSetupEnabled: brpDoelbindingSetupEnabled,
    });

    await render(ParametersStepKoppelingenComponent, {
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      inputs: { form, brpDoelbindingSetupEnabled },
      providers: [
        {
          provide: ReferentieTabelService,
          useValue: fromPartial<ReferentieTabelService>({
            listBrpSearchValues: () => ({
              ...createQueryOptions(brpSearchValues),
              queryKey: ["brpSearchValues"],
            }),
            listBrpViewValues: () => ({
              ...createQueryOptions(brpViewValues),
              queryKey: ["brpViewValues"],
            }),
            listBrpProcessingValues: () => ({
              ...createQueryOptions(brpProcessingValues),
              queryKey: ["brpProcessingValues"],
            }),
          }),
        },
      ],
    });

    return form;
  }

  const brpKoppelen = () =>
    screen.getByRole("switch", { name: "gegevens.koppelen.brp" });
  const kvkKoppelen = () =>
    screen.getByRole("switch", { name: "gegevens.koppelen.kvk" });
  const doelbinding = (label: string) =>
    screen.queryByRole("combobox", { name: label });

  async function chooseBrpDoelbinding(values: Record<string, string>) {
    for (const [label, value] of Object.entries(values)) {
      await user.click(screen.getByRole("combobox", { name: label }));
      await user.click(screen.getByRole("option", { name: value }));
    }
  }

  it("gives both koppelingen an accessible name, switched off by default", async () => {
    await setup();

    expect(brpKoppelen()).not.toBeChecked();
    expect(kvkKoppelen()).not.toBeChecked();
  });

  it("shows the stored koppelingen and BRP doelbindingen", async () => {
    await setup({ stored: switchedOn });

    expect(brpKoppelen()).toBeChecked();
    expect(kvkKoppelen()).toBeChecked();
    await waitFor(() =>
      expect(doelbinding("brp-doelbinding.zoek-waarde")).toHaveTextContent(
        brpSearchValues[1],
      ),
    );
  });

  it("asks for the BRP doelbinding values once BRP koppelen is switched on", async () => {
    await setup();

    expect(doelbinding("brp-doelbinding.zoek-waarde")).not.toBeInTheDocument();

    await user.click(brpKoppelen());

    expect(doelbinding("brp-doelbinding.zoek-waarde")).toBeVisible();
    expect(doelbinding("brp-doelbinding.raadpleeg-waarde")).toBeVisible();
    expect(
      doelbinding("brp-doelbinding.verwerkingregister-waarde"),
    ).toBeVisible();
  });

  it("demands every BRP doelbinding value once BRP koppelen is switched on", async () => {
    const form = await setup();

    await user.click(brpKoppelen());

    expect(form.valid).toBe(false);

    await chooseBrpDoelbinding({
      "brp-doelbinding.zoek-waarde": brpSearchValues[0],
      "brp-doelbinding.raadpleeg-waarde": brpViewValues[0],
      "brp-doelbinding.verwerkingregister-waarde": brpProcessingValues[0],
    });

    expect(form.valid).toBe(true);
  });

  it("drops the demand when BRP koppelen is switched back off", async () => {
    const form = await setup();

    await user.click(brpKoppelen());
    await user.click(brpKoppelen());

    expect(doelbinding("brp-doelbinding.zoek-waarde")).not.toBeInTheDocument();
    expect(form.valid).toBe(true);
  });

  it("forgets the chosen BRP doelbinding values when BRP koppelen is switched off", async () => {
    await setup();

    await user.click(brpKoppelen());
    await chooseBrpDoelbinding({
      "brp-doelbinding.zoek-waarde": brpSearchValues[0],
    });
    await user.click(brpKoppelen());
    await user.click(brpKoppelen());

    expect(doelbinding("brp-doelbinding.zoek-waarde")).not.toHaveTextContent(
      brpSearchValues[0],
    );
  });

  it("asks for no BRP doelbinding when the environment does not set them up", async () => {
    const form = await setup({ brpDoelbindingSetupEnabled: false });

    await user.click(brpKoppelen());

    expect(doelbinding("brp-doelbinding.zoek-waarde")).not.toBeInTheDocument();
    expect(form.valid).toBe(true);
  });

  it("stores the koppelingen that are switched on", async () => {
    const form = await setup();

    await user.click(kvkKoppelen());

    expect(toBetrokkeneKoppelingen(form)).toEqual({
      isBrpKoppelenEnabled: false,
      isKvkKoppelenEnabled: true,
    });
  });

  it("keeps the stored koppelingen and BRP doelbindingen when the step is never opened", () => {
    const form = createKoppelingenForm(configuration(switchedOn), {
      isBrpDoelbindingSetupEnabled: true,
    });

    expect(form.valid).toBe(true);
    expect(toBetrokkeneKoppelingen(form)).toEqual(
      switchedOn.betrokkeneKoppelingen,
    );
    expect(toBrpDoelbindingen(form)).toEqual(switchedOn.brpDoelbindingen);
  });
});
