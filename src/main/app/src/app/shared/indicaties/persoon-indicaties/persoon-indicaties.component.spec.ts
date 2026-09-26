/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TranslateModule } from "@ngx-translate/core";
import { render, screen, within } from "@testing-library/angular";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../utils/generated-types";
import { IndicatiesLayout } from "../indicaties.component";
import { PersoonIndicatiesComponent } from "./persoon-indicaties.component";

const persoonIndicatieMetadata: {
  indicatie: GeneratedType<"RestPersoonIndicaties">;
  expectedIcon: string;
}[] = [
  {
    indicatie: "GEHEIMHOUDING_OP_PERSOONSGEGEVENS",
    expectedIcon: "passkey",
  },
  { indicatie: "NIET_INGEZETENE", expectedIcon: "person_off" },
  { indicatie: "IN_ONDERZOEK", expectedIcon: "person_search" },
  { indicatie: "ONDER_CURATELE", expectedIcon: "account_child_invert" },
  { indicatie: "OPSCHORTING_BIJHOUDING", expectedIcon: "person_alert" },
  { indicatie: "OVERLEDEN", expectedIcon: "deceased" },
  { indicatie: "MINISTERIELE_REGELING", expectedIcon: "order_approve" },
  { indicatie: "EMIGRATIE", expectedIcon: "travel" },
];

const makePersoon = (
  indicaties: GeneratedType<"RestPersoonIndicaties">[],
): GeneratedType<"RestPersoon"> =>
  fromPartial<GeneratedType<"RestPersoon">>({ indicaties });

const setup = (
  persoon: GeneratedType<"RestPersoon"> | undefined,
  layout: IndicatiesLayout,
) =>
  render(PersoonIndicatiesComponent, {
    inputs: { persoon, layout },
    imports: [TranslateModule.forRoot()],
  });

const chipWithTooltip = (tooltip: string) =>
  screen.getByRole("presentation", { description: tooltip });

describe(PersoonIndicatiesComponent.name, () => {
  it("shows no indicaties when the persoon has none", async () => {
    await setup(makePersoon([]), IndicatiesLayout.COMPACT);

    expect(screen.queryAllByRole("option")).toHaveLength(0);
  });

  it("shows no indicaties while the persoon is still undefined, and shows them once it arrives", async () => {
    const { fixture } = await setup(undefined, IndicatiesLayout.EXTENDED);

    expect(screen.queryAllByRole("option")).toHaveLength(0);

    fixture.componentRef.setInput("persoon", makePersoon(["OVERLEDEN"]));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(
      screen.getByRole("option", { name: "indicatie.OVERLEDEN" }),
    ).toBeInTheDocument();
  });

  it.each(persoonIndicatieMetadata)(
    "shows $indicatie as a highlighted chip with icon '$expectedIcon'",
    async ({ indicatie, expectedIcon }) => {
      await setup(makePersoon([indicatie]), IndicatiesLayout.COMPACT);

      const chip = chipWithTooltip(`indicatie.${indicatie}`);
      expect(within(chip).getByText(expectedIcon)).toBeInTheDocument();
      expect(chip).toHaveClass("mat-mdc-chip-highlighted");
    },
  );

  it("shows every indicatie of the persoon, in the order given", async () => {
    await setup(
      makePersoon(["GEHEIMHOUDING_OP_PERSOONSGEGEVENS", "OVERLEDEN"]),
      IndicatiesLayout.EXTENDED,
    );

    const options = screen.getAllByRole("option");
    expect(options).toHaveLength(2);
    expect(options[0]).toHaveAccessibleName(
      "indicatie.GEHEIMHOUDING_OP_PERSOONSGEGEVENS",
    );
    expect(options[1]).toHaveAccessibleName("indicatie.OVERLEDEN");
  });

  it("replaces the indicaties when the persoon input changes", async () => {
    const { fixture } = await setup(
      makePersoon(["NIET_INGEZETENE"]),
      IndicatiesLayout.EXTENDED,
    );

    fixture.componentRef.setInput(
      "persoon",
      makePersoon(["OVERLEDEN", "EMIGRATIE"]),
    );
    fixture.detectChanges();
    await fixture.whenStable();

    const options = screen.getAllByRole("option");
    expect(options).toHaveLength(2);
    expect(options[0]).toHaveAccessibleName("indicatie.OVERLEDEN");
    expect(options[1]).toHaveAccessibleName("indicatie.EMIGRATIE");
  });

  it("clears the indicaties when the persoon input changes to one without indicaties", async () => {
    const { fixture } = await setup(
      makePersoon(["NIET_INGEZETENE"]),
      IndicatiesLayout.EXTENDED,
    );

    fixture.componentRef.setInput("persoon", makePersoon([]));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(screen.queryAllByRole("option")).toHaveLength(0);
  });

  describe("layout", () => {
    it("EXTENDED labels each chip with the translated indicatie name", async () => {
      await setup(makePersoon(["OVERLEDEN"]), IndicatiesLayout.EXTENDED);

      const option = screen.getByRole("option", {
        name: "indicatie.OVERLEDEN",
      });
      expect(within(option).getByText("deceased")).toBeInTheDocument();
      expect(screen.getByRole("listbox")).toHaveClass("extended");
    });

    it.each([IndicatiesLayout.COMPACT, IndicatiesLayout.SEARCH])(
      "%s shows only the icon, with the translated indicatie name in the tooltip",
      async (layout) => {
        await setup(makePersoon(["OVERLEDEN"]), layout);

        const chip = chipWithTooltip("indicatie.OVERLEDEN");
        expect(within(chip).getByRole("option")).toHaveAccessibleName("");
        expect(within(chip).getByText("deceased")).toBeInTheDocument();
        expect(screen.getByRole("listbox")).toHaveClass(layout.toLowerCase());
      },
    );

    it("switches from EXTENDED to COMPACT when the layout input changes, keeping the indicaties", async () => {
      const { fixture } = await setup(
        makePersoon(["OVERLEDEN", "EMIGRATIE", "IN_ONDERZOEK"]),
        IndicatiesLayout.EXTENDED,
      );

      fixture.componentRef.setInput("layout", IndicatiesLayout.COMPACT);
      fixture.detectChanges();
      await fixture.whenStable();

      const options = screen.getAllByRole("option");
      expect(options).toHaveLength(3);
      options.forEach((option) => expect(option).toHaveAccessibleName(""));
      expect(screen.getByRole("listbox")).toHaveClass("compact");
      expect(screen.getByRole("listbox")).not.toHaveClass("extended");
    });
  });
});
