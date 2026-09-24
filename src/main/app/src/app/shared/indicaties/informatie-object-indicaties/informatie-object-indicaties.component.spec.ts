/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TranslateLoader, TranslateModule } from "@ngx-translate/core";
import { render, screen, within } from "@testing-library/angular";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { DocumentZoekObject } from "../../../zoeken/model/documenten/document-zoek-object";
import { GeneratedType } from "../../utils/generated-types";
import { IndicatiesLayout } from "../indicaties.component";
import { InformatieObjectIndicatiesComponent } from "./informatie-object-indicaties.component";

const translations = {
  "msg.document.vergrendeld": "Vergrendeld door {{gebruiker}}",
  "msg.document.besluit": "Vastgelegd in een besluit",
};

const dutchShortDate = (day: string, month: string, year: string) =>
  [day, month, year].join("‑");

const makeDocument = (
  indicaties: GeneratedType<"DocumentIndicatie">[],
): GeneratedType<"RestEnkelvoudigInformatieobject"> =>
  fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
    gelockedDoor: { id: "fakeUserId", naam: "fakeGelockedDoorNaam" },
    ondertekening: { soort: "fakeDocumentSoort", datum: "2024-01-15" },
    verzenddatum: "2024-01-20",
    indicaties,
  });

const makeDocumentZoekObject = (
  indicaties: GeneratedType<"DocumentIndicatie">[],
): DocumentZoekObject =>
  fromPartial<DocumentZoekObject>({
    vergrendeldDoor: "fakeVergrendeldDoor",
    ondertekeningSoort: "fakeZoekObjectSoort",
    ondertekeningDatum: "2024-03-10",
    verzenddatum: "2024-03-15",
    indicaties,
  });

const tooltipOf = (
  indicatie: GeneratedType<"DocumentIndicatie">,
  toelichting: string,
) =>
  toelichting
    ? `indicatie.${indicatie}: ${toelichting}`
    : `indicatie.${indicatie}`;

const setup = (inputs: {
  layout: IndicatiesLayout;
  document?: GeneratedType<"RestEnkelvoudigInformatieobject">;
  documentZoekObject?: DocumentZoekObject;
}) =>
  render(InformatieObjectIndicatiesComponent, {
    inputs,
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

describe(InformatieObjectIndicatiesComponent.name, () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("shows no indicaties when neither a document nor a documentZoekObject is given", async () => {
    await setup({ layout: IndicatiesLayout.COMPACT });

    expect(screen.queryAllByRole("option")).toHaveLength(0);
  });

  describe("given a document", () => {
    it.each<{
      indicatie: GeneratedType<"DocumentIndicatie">;
      expectedIcon: string;
      isHighlighted: boolean;
      expectedToelichting: string;
    }>([
      {
        indicatie: "VERGRENDELD",
        expectedIcon: "lock",
        isHighlighted: true,
        expectedToelichting: "Vergrendeld door fakeGelockedDoorNaam",
      },
      {
        indicatie: "ONDERTEKEND",
        expectedIcon: "fact_check",
        isHighlighted: false,
        expectedToelichting: `fakeDocumentSoort-${dutchShortDate("15", "01", "2024")}`,
      },
      {
        indicatie: "BESLUIT",
        expectedIcon: "gavel",
        isHighlighted: false,
        expectedToelichting: "Vastgelegd in een besluit",
      },
      {
        indicatie: "GEBRUIKSRECHT",
        expectedIcon: "privacy_tip",
        isHighlighted: true,
        expectedToelichting: "",
      },
      {
        indicatie: "VERZONDEN",
        expectedIcon: "local_post_office",
        isHighlighted: false,
        expectedToelichting: dutchShortDate("20", "01", "2024"),
      },
    ])(
      "shows $indicatie as a chip with icon '$expectedIcon' (highlighted: $isHighlighted) and toelichting '$expectedToelichting'",
      async ({
        indicatie,
        expectedIcon,
        isHighlighted,
        expectedToelichting,
      }) => {
        await setup({
          layout: IndicatiesLayout.COMPACT,
          document: makeDocument([indicatie]),
        });

        const chip = chipWithTooltip(tooltipOf(indicatie, expectedToelichting));
        expect(within(chip).getByText(expectedIcon)).toBeInTheDocument();
        expect(chip.classList.contains("mat-mdc-chip-highlighted")).toBe(
          isHighlighted,
        );
      },
    );

    it("shows every indicatie of the document, in the order given", async () => {
      await setup({
        layout: IndicatiesLayout.EXTENDED,
        document: makeDocument(["VERGRENDELD", "ONDERTEKEND", "BESLUIT"]),
      });

      const options = screen.getAllByRole("option");
      expect(options).toHaveLength(3);
      expect(options[0]).toHaveAccessibleName("indicatie.VERGRENDELD");
      expect(options[1]).toHaveAccessibleName("indicatie.ONDERTEKEND");
      expect(options[2]).toHaveAccessibleName("indicatie.BESLUIT");
    });

    it("EXTENDED labels the chip with the indicatie name and puts only the toelichting in the tooltip", async () => {
      await setup({
        layout: IndicatiesLayout.EXTENDED,
        document: makeDocument(["VERGRENDELD"]),
      });

      const chip = chipWithTooltip("Vergrendeld door fakeGelockedDoorNaam");
      expect(
        within(chip).getByRole("option", { name: "indicatie.VERGRENDELD" }),
      ).toBeInTheDocument();
    });

    it("skips an unknown indicatie and warns about it", async () => {
      const warn = jest.spyOn(console, "warn").mockImplementation(() => {});

      await setup({
        layout: IndicatiesLayout.COMPACT,
        document: makeDocument([
          "ONBEKEND" as unknown as GeneratedType<"DocumentIndicatie">,
          "BESLUIT",
        ]),
      });

      expect(screen.getAllByRole("option")).toHaveLength(1);
      expect(
        chipWithTooltip("indicatie.BESLUIT: Vastgelegd in een besluit"),
      ).toBeInTheDocument();
      expect(warn).toHaveBeenCalledWith(expect.stringContaining("ONBEKEND"));
    });

    it("replaces the indicaties when the document input changes", async () => {
      const { fixture } = await setup({
        layout: IndicatiesLayout.EXTENDED,
        document: makeDocument(["VERGRENDELD", "BESLUIT"]),
      });

      fixture.componentRef.setInput("document", makeDocument(["BESLUIT"]));
      fixture.detectChanges();
      await fixture.whenStable();

      const options = screen.getAllByRole("option");
      expect(options).toHaveLength(1);
      expect(options[0]).toHaveAccessibleName("indicatie.BESLUIT");
    });
  });

  describe("given a documentZoekObject", () => {
    it.each<{
      indicatie: GeneratedType<"DocumentIndicatie">;
      expectedIcon: string;
      isHighlighted: boolean;
      expectedToelichting: string;
    }>([
      {
        indicatie: "VERGRENDELD",
        expectedIcon: "lock",
        isHighlighted: true,
        expectedToelichting: "Vergrendeld door fakeVergrendeldDoor",
      },
      {
        indicatie: "ONDERTEKEND",
        expectedIcon: "fact_check",
        isHighlighted: false,
        expectedToelichting: `fakeZoekObjectSoort-${dutchShortDate("10", "03", "2024")}`,
      },
      {
        indicatie: "BESLUIT",
        expectedIcon: "gavel",
        isHighlighted: false,
        expectedToelichting: "Vastgelegd in een besluit",
      },
      {
        indicatie: "GEBRUIKSRECHT",
        expectedIcon: "privacy_tip",
        isHighlighted: true,
        expectedToelichting: "",
      },
      {
        indicatie: "VERZONDEN",
        expectedIcon: "local_post_office",
        isHighlighted: false,
        expectedToelichting: dutchShortDate("15", "03", "2024"),
      },
    ])(
      "shows $indicatie as a chip with icon '$expectedIcon' (highlighted: $isHighlighted) and toelichting '$expectedToelichting'",
      async ({
        indicatie,
        expectedIcon,
        isHighlighted,
        expectedToelichting,
      }) => {
        await setup({
          layout: IndicatiesLayout.SEARCH,
          documentZoekObject: makeDocumentZoekObject([indicatie]),
        });

        const chip = chipWithTooltip(tooltipOf(indicatie, expectedToelichting));
        expect(within(chip).getByText(expectedIcon)).toBeInTheDocument();
        expect(chip.classList.contains("mat-mdc-chip-highlighted")).toBe(
          isHighlighted,
        );
      },
    );

    it("replaces the indicaties when the documentZoekObject input changes", async () => {
      const { fixture } = await setup({
        layout: IndicatiesLayout.SEARCH,
        documentZoekObject: makeDocumentZoekObject(["VERGRENDELD"]),
      });

      fixture.componentRef.setInput(
        "documentZoekObject",
        makeDocumentZoekObject(["VERZONDEN"]),
      );
      fixture.detectChanges();
      await fixture.whenStable();

      expect(screen.getAllByRole("option")).toHaveLength(1);
      expect(
        chipWithTooltip(
          tooltipOf("VERZONDEN", dutchShortDate("15", "03", "2024")),
        ),
      ).toBeInTheDocument();
    });
  });

  it("uses the documentZoekObject, not the document, when both are given", async () => {
    await setup({
      layout: IndicatiesLayout.COMPACT,
      document: makeDocument(["BESLUIT"]),
      documentZoekObject: makeDocumentZoekObject(["VERGRENDELD"]),
    });

    expect(screen.getAllByRole("option")).toHaveLength(1);
    expect(
      chipWithTooltip(
        "indicatie.VERGRENDELD: Vergrendeld door fakeVergrendeldDoor",
      ),
    ).toBeInTheDocument();
  });
});
