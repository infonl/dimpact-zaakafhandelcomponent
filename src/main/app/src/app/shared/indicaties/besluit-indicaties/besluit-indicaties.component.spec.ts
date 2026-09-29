/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TranslateLoader, TranslateModule } from "@ngx-translate/core";
import { render, screen, within } from "@testing-library/angular";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../utils/generated-types";
import { IndicatiesLayout } from "../indicaties.component";
import { BesluitIndicatiesComponent } from "./besluit-indicaties.component";

const translations = {
  "besluit.vervalreden.INGETROKKEN_OVERHEID": "Ingetrokken door de overheid",
  "besluit.vervalreden.INGETROKKEN_BELANGHEBBENDE":
    "Ingetrokken door de belanghebbende",
};

const makeBesluit = (
  fields: Partial<GeneratedType<"RestBesluit">> = {},
): GeneratedType<"RestBesluit"> =>
  fromPartial<GeneratedType<"RestBesluit">>({
    isIngetrokken: false,
    ...fields,
  });

const setup = (besluit: GeneratedType<"RestBesluit">) =>
  render(BesluitIndicatiesComponent, {
    inputs: { besluit, layout: IndicatiesLayout.EXTENDED },
    imports: [
      TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useValue: { getTranslation: () => of(translations) },
        },
        lang: "nl",
      }),
    ],
  });

const chipWithTooltip = (tooltip: string) =>
  screen.getByRole("presentation", { description: tooltip });

describe(BesluitIndicatiesComponent.name, () => {
  it("shows no indicaties when the besluit is not ingetrokken", async () => {
    await setup(makeBesluit({ isIngetrokken: false }));

    expect(screen.queryAllByRole("option")).toHaveLength(0);
  });

  it("shows INGETROKKEN as a plain 'stop' chip with the translated vervalreden as tooltip", async () => {
    await setup(
      makeBesluit({
        isIngetrokken: true,
        vervalreden: "INGETROKKEN_OVERHEID",
      }),
    );

    const chip = chipWithTooltip("Ingetrokken door de overheid");
    const option = within(chip).getByRole("option", {
      name: "indicatie.INGETROKKEN",
    });
    expect(within(option).getByText("stop")).toBeInTheDocument();
    expect(chip).not.toHaveClass("mat-mdc-chip-highlighted");
  });

  it("shows INGETROKKEN once the besluit input changes to an ingetrokken besluit", async () => {
    const { fixture } = await setup(makeBesluit({ isIngetrokken: false }));

    fixture.componentRef.setInput(
      "besluit",
      makeBesluit({
        isIngetrokken: true,
        vervalreden: "INGETROKKEN_BELANGHEBBENDE",
      }),
    );
    fixture.detectChanges();
    await fixture.whenStable();

    expect(
      within(chipWithTooltip("Ingetrokken door de belanghebbende")).getByRole(
        "option",
        { name: "indicatie.INGETROKKEN" },
      ),
    ).toBeInTheDocument();
  });

  it("removes INGETROKKEN when the besluit input changes to a besluit that is not ingetrokken", async () => {
    const { fixture } = await setup(
      makeBesluit({
        isIngetrokken: true,
        vervalreden: "INGETROKKEN_OVERHEID",
      }),
    );

    fixture.componentRef.setInput(
      "besluit",
      makeBesluit({ isIngetrokken: false }),
    );
    fixture.detectChanges();
    await fixture.whenStable();

    expect(screen.queryAllByRole("option")).toHaveLength(0);
  });
});
