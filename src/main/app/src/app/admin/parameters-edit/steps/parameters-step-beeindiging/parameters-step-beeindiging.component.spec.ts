/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ZaakafhandelParametersService } from "../../../zaakafhandel-parameters.service";
import { ParametersStepBeeindigingComponent } from "./parameters-step-beeindiging.component";
import {
  createZaakbeeindigingForm,
  toZaakbeeindigParameters,
} from "./zaakbeeindiging-form";

const reden = fromPartial<GeneratedType<"RestZaakbeeindigReden">>({
  id: "1",
  naam: "Reden 1",
});

const otherReden = fromPartial<GeneratedType<"RestZaakbeeindigReden">>({
  id: "2",
  naam: "Reden 2",
});

const afgehandeld = fromPartial<GeneratedType<"RestResultaattype">>({
  id: "resultaat-1",
  naam: "Afgehandeld",
});

const ingetrokken = fromPartial<GeneratedType<"RestResultaattype">>({
  id: "resultaat-2",
  naam: "Ingetrokken",
});

const storedBeeindiging = {
  zaakNietOntvankelijkResultaattype: afgehandeld,
  zaakbeeindigParameters: [
    { id: 7, zaakbeeindigReden: otherReden, resultaattype: ingetrokken },
  ],
};

const configuration =
  fromPartial<GeneratedType<"RestZaaktypeConfiguration">>(storedBeeindiging);

describe("Beeindiging step", () => {
  const user = userEvent.setup();

  async function setup() {
    const form = createZaakbeeindigingForm(configuration);

    await render(ParametersStepBeeindigingComponent, {
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      inputs: { form, zaaktypeUuid: "zaaktype-uuid" },
      providers: [
        {
          provide: ZaakafhandelParametersService,
          useValue: fromPartial<ZaakafhandelParametersService>({
            listZaakbeeindigRedenen: () => ({
              ...createQueryOptions([reden, otherReden]),
              queryKey: ["zaakbeeindigRedenen"],
            }),
            listResultaattypes: () => ({
              ...createQueryOptions([afgehandeld, ingetrokken]),
              queryKey: ["resultaattypes"],
            }),
          }),
        },
      ],
    });

    expect(await screen.findByRole("row", { name: /Reden 1/ })).toBeVisible();

    return form;
  }

  function redenRow(naam: string) {
    return screen.getByRole("row", { name: new RegExp(naam) });
  }

  async function chooseResultaat(row: HTMLElement, naam: string) {
    // the select of a table cell has no form field around it, so the click has to land inside the select
    await user.click(within(row).getByText("resultaat.kies"));
    await user.click(screen.getByRole("option", { name: naam }));
  }

  it("lists the zaak niet ontvankelijk reden before the redenen of the zaaktype", async () => {
    await setup();

    const rows = screen.getAllByRole("row");

    expect(rows[1]).toHaveAccessibleName(/zaak-is-niet-ontvankelijk/);
    expect(rows[2]).toHaveAccessibleName(/Reden 1/);
    expect(rows[3]).toHaveAccessibleName(/Reden 2/);
  });

  it("always ends a zaak that is niet ontvankelijk", async () => {
    await setup();

    const checkbox = within(redenRow("zaak-is-niet-ontvankelijk")).getByRole(
      "checkbox",
    );

    expect(checkbox).toBeChecked();
    expect(checkbox).toBeDisabled();
  });

  it("shows the stored redenen as chosen and the others as unchosen", async () => {
    await setup();

    expect(within(redenRow("Reden 2")).getByRole("checkbox")).toBeChecked();
    expect(within(redenRow("Reden 1")).getByRole("checkbox")).not.toBeChecked();
  });

  it("demands a resultaat for a reden that is chosen", async () => {
    const form = await setup();
    const row = redenRow("Reden 1");

    await user.click(within(row).getByRole("checkbox"));

    expect(form.valid).toBe(false);

    await chooseResultaat(row, "Afgehandeld");

    expect(form.valid).toBe(true);
  });

  it("drops the demand for a resultaat when the reden is unchosen again", async () => {
    const form = await setup();
    const checkbox = within(redenRow("Reden 1")).getByRole("checkbox");

    await user.click(checkbox);
    await user.click(checkbox);

    expect(checkbox).not.toBeChecked();
    expect(form.valid).toBe(true);
  });

  it("stores only the chosen redenen, with their resultaat", async () => {
    const form = await setup();
    const row = redenRow("Reden 1");

    await user.click(within(row).getByRole("checkbox"));
    await chooseResultaat(row, "Afgehandeld");
    await user.click(within(redenRow("Reden 2")).getByRole("checkbox"));

    expect(toZaakbeeindigParameters(form)).toEqual([
      {
        id: null,
        zaakbeeindigReden: reden,
        resultaattype: afgehandeld,
      },
    ]);
  });

  it("keeps the stored redenen when the step is never opened", () => {
    const form = createZaakbeeindigingForm(configuration);

    expect(form.controls.zaakNietOntvankelijkResultaattype.value).toEqual(
      afgehandeld,
    );
    expect(toZaakbeeindigParameters(form)).toEqual([
      { id: 7, zaakbeeindigReden: otherReden, resultaattype: ingetrokken },
    ]);
  });
});
