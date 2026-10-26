/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { render, screen, waitFor } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { IdentityService } from "../../../../identity/identity.service";
import {
  AlgemeenSettings,
  createAlgemeenForm,
  toAlgemeenSettings,
} from "./algemeen-form";
import { ParametersStepAlgemeenComponent } from "./parameters-step-algemeen.component";

const groups = [
  { id: "test-group-id", naam: "test-group" },
  { id: "test-group-id-2", naam: "test-group-2" },
];

const usersOfDefaultGroup = [
  { username: "test-user-id", naam: "test-user" },
  { username: "test-user-id-2", naam: "test-user-2" },
];

const stored: AlgemeenSettings = {
  defaultGroepId: "test-group-id",
  defaultBehandelaarId: "test-user-id",
  einddatumGeplandWaarschuwing: 3,
  uiterlijkeEinddatumAfdoeningWaarschuwing: 2,
  productaanvraagtype: "productaanvraag-1",
};

describe(ParametersStepAlgemeenComponent.name, () => {
  const user = userEvent.setup();

  async function setup({
    settings = stored,
    isZaakspecifiekAutoriseerbaar = false,
  }: {
    settings?: Partial<AlgemeenSettings>;
    isZaakspecifiekAutoriseerbaar?: boolean;
  } = {}) {
    const form = createAlgemeenForm(settings);

    await render(ParametersStepAlgemeenComponent, {
      imports: [NoopAnimationsModule],
      inputs: {
        form,
        zaaktype: {
          uuid: "test-uuid",
          omschrijving: "fakeZaaktypeOmschrijving",
          hasServicenorm: true,
        },
        isZaakspecifiekAutoriseerbaar,
      },
      providers: [
        provideTranslateService(),
        {
          provide: IdentityService,
          useValue: fromPartial<IdentityService>({
            listBehandelaarGroupsForZaaktype: (
              zaaktypeDescription: string,
            ) => ({
              ...createQueryOptions(
                zaaktypeDescription === "fakeZaaktypeOmschrijving"
                  ? groups
                  : [],
              ),
              queryKey: ["behandelaarGroups", zaaktypeDescription],
            }),
            listUsersInGroup: (groupId: string) => ({
              ...createQueryOptions(
                groupId === "test-group-id" ? usersOfDefaultGroup : [],
              ),
              queryKey: ["usersInGroup", groupId],
            }),
          }),
        },
      ],
    });

    return form;
  }

  const groep = () =>
    screen.getByRole("combobox", { name: /^groep.standaard$/i });
  const behandelaar = () =>
    screen.getByRole("combobox", { name: /behandelaar.standaard/i });

  async function optionsOf(combobox: HTMLElement) {
    await user.click(combobox);
    return screen
      .getAllByRole("option")
      .map((option) => option.textContent?.trim());
  }

  async function chooseOption(combobox: HTMLElement, option: string) {
    await user.click(combobox);
    await user.click(screen.getByRole("option", { name: option }));
  }

  it("demands a groep", async () => {
    const form = await setup({ settings: { defaultGroepId: null } });

    expect(form.valid).toBe(false);

    await chooseOption(groep(), "test-group");

    expect(form.valid).toBe(true);
  });

  it("offers only the behandelaar groepen of the zaaktype as groep", async () => {
    await setup();

    expect(await optionsOf(groep())).toEqual(["test-group", "test-group-2"]);
  });

  it("shows a zaaktype without zaakspecifieke autorisatie as such", async () => {
    await setup();

    expect(screen.getByText("zaakspecifiek-autoriseerbaar")).toBeVisible();
    expect(screen.getByText("actie.nee")).toBeVisible();
  });

  it("shows a zaaktype with zaakspecifieke autorisatie as such", async () => {
    await setup({ isZaakspecifiekAutoriseerbaar: true });

    expect(screen.getByText("actie.ja")).toBeVisible();
  });

  it("preselects the stored groep and behandelaar", async () => {
    await setup();

    await waitFor(() => expect(groep()).toHaveTextContent("test-group"));
    await waitFor(() => expect(behandelaar()).toHaveTextContent("test-user"));
  });

  it("offers the medewerkers of the default groep as behandelaar", async () => {
    await setup();

    await waitFor(() => expect(behandelaar()).toHaveTextContent("test-user"));

    expect(await optionsOf(behandelaar())).toEqual([
      "geen.generiek",
      "test-user",
      "test-user-2",
    ]);
  });

  it("forgets the behandelaar when another groep is chosen", async () => {
    const form = await setup();

    await chooseOption(groep(), "test-group-2");

    await waitFor(() =>
      expect(form.controls.defaultBehandelaarId.value).toBeNull(),
    );
    expect(behandelaar()).not.toHaveTextContent("test-user");
    expect(await optionsOf(behandelaar())).toEqual(["geen.generiek"]);
  });

  it("forgets a stored groep that the zaaktype no longer offers", async () => {
    const form = await setup({
      settings: { ...stored, defaultGroepId: "removed-group-id" },
    });

    await waitFor(() => expect(form.controls.defaultGroepId.value).toBeNull());
    expect(form.valid).toBe(false);
  });

  it("lets the beheerder set both deadline warning windows of a zaaktype with a servicenorm", async () => {
    await setup();

    expect(screen.getAllByLabelText(/waarschuwing/i)).toHaveLength(2);
  });

  it("stores the productaanvraagtype as it is filled in", async () => {
    const form = await setup();
    const productaanvraagtype = screen.getByRole("textbox", {
      name: /productaanvraagtype/i,
    });

    await user.clear(productaanvraagtype);
    await user.type(productaanvraagtype, "productaanvraag-2");

    expect(toAlgemeenSettings(form).productaanvraagtype).toBe(
      "productaanvraag-2",
    );
  });

  it("keeps the stored settings when the step is never opened", () => {
    const form = createAlgemeenForm(stored);

    expect(form.valid).toBe(true);
    expect(toAlgemeenSettings(form)).toEqual(stored);
  });
});
