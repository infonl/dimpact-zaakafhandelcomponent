/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TranslateLoader, TranslateModule } from "@ngx-translate/core";
import { render, screen, within } from "@testing-library/angular";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { ZaakZoekObject } from "../../../zoeken/model/zaken/zaak-zoek-object";
import { GeneratedType } from "../../utils/generated-types";
import { IndicatiesLayout } from "../indicaties.component";
import { ZaakIndicatiesComponent } from "./zaak-indicaties.component";

const translations = {
  reden: "Reden",
  "msg.zaak.relatie": "Gekoppeld aan {{identificatie}}",
  "msg.zaak.relaties": "Gekoppeld aan {{aantal}} zaken",
};

const makeZaak = (
  fields: Partial<GeneratedType<"RestZaak">> = {},
): GeneratedType<"RestZaak"> =>
  fromPartial<GeneratedType<"RestZaak">>({
    gerelateerdeZaken: [],
    ...fields,
  });

const makeZaakZoekObject = (fields: Partial<ZaakZoekObject> = {}) =>
  fromPartial<ZaakZoekObject>({ ...fields });

const setup = (inputs: {
  layout: IndicatiesLayout;
  zaak?: GeneratedType<"RestZaak">;
  zaakZoekObject?: ZaakZoekObject;
}) =>
  render(ZaakIndicatiesComponent, {
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

describe(ZaakIndicatiesComponent.name, () => {
  describe("given a zaak", () => {
    it("shows no indicaties when the zaak has none", async () => {
      await setup({
        layout: IndicatiesLayout.COMPACT,
        zaak: makeZaak({ indicaties: [] }),
      });

      expect(screen.queryAllByRole("option")).toHaveLength(0);
    });

    it("shows OPSCHORTING as a highlighted 'pause' chip with the reden van opschorting", async () => {
      await setup({
        layout: IndicatiesLayout.COMPACT,
        zaak: makeZaak({
          indicaties: ["OPSCHORTING"],
          redenOpschorting: "fakeRedenOpschorting",
        }),
      });

      const chip = chipWithTooltip(
        "indicatie.OPSCHORTING: Reden: fakeRedenOpschorting",
      );
      expect(within(chip).getByText("pause")).toBeInTheDocument();
      expect(chip).toHaveClass("mat-mdc-chip-highlighted");
    });

    it("shows HEROPEND as a highlighted 'restart_alt' chip with the status toelichting", async () => {
      await setup({
        layout: IndicatiesLayout.COMPACT,
        zaak: makeZaak({
          indicaties: ["HEROPEND"],
          status: {
            naam: "fakeStatusNaam",
            toelichting: "fakeStatusToelichting",
          },
        }),
      });

      const chip = chipWithTooltip("indicatie.HEROPEND: fakeStatusToelichting");
      expect(within(chip).getByText("restart_alt")).toBeInTheDocument();
      expect(chip).toHaveClass("mat-mdc-chip-highlighted");
    });

    it("shows HOOFDZAAK as a plain 'account_tree' chip naming its only deelzaak", async () => {
      await setup({
        layout: IndicatiesLayout.COMPACT,
        zaak: makeZaak({
          indicaties: ["HOOFDZAAK"],
          gerelateerdeZaken: [
            { relatieType: "DEELZAAK", identificatie: "fakeDeelzaak1" },
            { relatieType: "VERVOLG", identificatie: "fakeVervolgzaak" },
          ],
        }),
      });

      const chip = chipWithTooltip(
        "indicatie.HOOFDZAAK: Gekoppeld aan fakeDeelzaak1",
      );
      expect(within(chip).getByText("account_tree")).toHaveAttribute(
        "outlined",
        "false",
      );
      expect(chip).not.toHaveClass("mat-mdc-chip-highlighted");
    });

    it("shows HOOFDZAAK with the number of deelzaken when it has more than one", async () => {
      await setup({
        layout: IndicatiesLayout.COMPACT,
        zaak: makeZaak({
          indicaties: ["HOOFDZAAK"],
          gerelateerdeZaken: [
            { relatieType: "DEELZAAK", identificatie: "fakeDeelzaak1" },
            { relatieType: "DEELZAAK", identificatie: "fakeDeelzaak2" },
          ],
        }),
      });

      expect(
        chipWithTooltip("indicatie.HOOFDZAAK: Gekoppeld aan 2 zaken"),
      ).toBeInTheDocument();
    });

    it("shows DEELZAAK as an outlined 'account_tree' chip naming its hoofdzaak", async () => {
      await setup({
        layout: IndicatiesLayout.COMPACT,
        zaak: makeZaak({
          indicaties: ["DEELZAAK"],
          gerelateerdeZaken: [
            { relatieType: "HOOFDZAAK", identificatie: "fakeHoofdzaak" },
          ],
        }),
      });

      const chip = chipWithTooltip(
        "indicatie.DEELZAAK: Gekoppeld aan fakeHoofdzaak",
      );
      expect(within(chip).getByText("account_tree")).toHaveAttribute(
        "outlined",
        "true",
      );
      expect(chip).not.toHaveClass("mat-mdc-chip-highlighted");
    });

    it("shows HOOFDZAAK and DEELZAAK without relation details when the zaak has no gerelateerde zaken", async () => {
      await setup({
        layout: IndicatiesLayout.COMPACT,
        zaak: makeZaak({
          indicaties: ["HOOFDZAAK", "DEELZAAK"],
          gerelateerdeZaken: [],
        }),
      });

      expect(chipWithTooltip("indicatie.HOOFDZAAK")).toBeInTheDocument();
      expect(chipWithTooltip("indicatie.DEELZAAK")).toBeInTheDocument();
    });

    it("shows VERLENGD as a plain 'update' chip with the reden van verlenging", async () => {
      await setup({
        layout: IndicatiesLayout.COMPACT,
        zaak: makeZaak({
          indicaties: ["VERLENGD"],
          redenVerlenging: "fakeRedenVerlenging",
        }),
      });

      const chip = chipWithTooltip(
        "indicatie.VERLENGD: Reden: fakeRedenVerlenging",
      );
      expect(within(chip).getByText("update")).toBeInTheDocument();
      expect(chip).not.toHaveClass("mat-mdc-chip-highlighted");
    });

    it("shows ONTVANGSTBEVESTIGING_NIET_VERSTUURD as a plain 'unsubscribe' chip without toelichting", async () => {
      await setup({
        layout: IndicatiesLayout.COMPACT,
        zaak: makeZaak({ indicaties: ["ONTVANGSTBEVESTIGING_NIET_VERSTUURD"] }),
      });

      const chip = chipWithTooltip(
        "indicatie.ONTVANGSTBEVESTIGING_NIET_VERSTUURD",
      );
      expect(within(chip).getByText("unsubscribe")).toBeInTheDocument();
      expect(chip).not.toHaveClass("mat-mdc-chip-highlighted");
    });

    it("shows every indicatie of the zaak, in the order given", async () => {
      await setup({
        layout: IndicatiesLayout.EXTENDED,
        zaak: makeZaak({
          indicaties: ["VERLENGD", "ONTVANGSTBEVESTIGING_NIET_VERSTUURD"],
        }),
      });

      const options = screen.getAllByRole("option");
      expect(options).toHaveLength(2);
      expect(options[0]).toHaveAccessibleName("indicatie.VERLENGD");
      expect(options[1]).toHaveAccessibleName(
        "indicatie.ONTVANGSTBEVESTIGING_NIET_VERSTUURD",
      );
    });

    it("EXTENDED labels the chip with the indicatie name and puts only the toelichting in the tooltip", async () => {
      await setup({
        layout: IndicatiesLayout.EXTENDED,
        zaak: makeZaak({
          indicaties: ["OPSCHORTING"],
          redenOpschorting: "fakeRedenOpschorting",
        }),
      });

      const chip = chipWithTooltip("Reden: fakeRedenOpschorting");
      expect(
        within(chip).getByRole("option", { name: "indicatie.OPSCHORTING" }),
      ).toBeInTheDocument();
    });

    it("replaces the indicaties when the zaak input changes", async () => {
      const { fixture } = await setup({
        layout: IndicatiesLayout.EXTENDED,
        zaak: makeZaak({ indicaties: ["OPSCHORTING"] }),
      });

      fixture.componentRef.setInput(
        "zaak",
        makeZaak({
          indicaties: ["VERLENGD"],
          redenVerlenging: "fakeRedenVerlenging",
        }),
      );
      fixture.detectChanges();
      await fixture.whenStable();

      const options = screen.getAllByRole("option");
      expect(options).toHaveLength(1);
      expect(options[0]).toHaveAccessibleName("indicatie.VERLENGD");
      expect(chipWithTooltip("Reden: fakeRedenVerlenging")).toBeInTheDocument();
    });
  });

  describe("given a zaakZoekObject", () => {
    it("shows no indicaties when the zaakZoekObject has none", async () => {
      await setup({
        layout: IndicatiesLayout.SEARCH,
        zaakZoekObject: makeZaakZoekObject({ indicaties: [] }),
      });

      expect(screen.queryAllByRole("option")).toHaveLength(0);
    });

    it("takes the toelichtingen of OPSCHORTING, HEROPEND and VERLENGD from the zaakZoekObject", async () => {
      await setup({
        layout: IndicatiesLayout.SEARCH,
        zaakZoekObject: makeZaakZoekObject({
          indicaties: ["OPSCHORTING", "HEROPEND", "VERLENGD"],
          redenOpschorting: "fakeRedenOpschorting",
          statusToelichting: "fakeStatusToelichting",
          redenVerlenging: "fakeRedenVerlenging",
        }),
      });

      expect(
        chipWithTooltip("indicatie.OPSCHORTING: Reden: fakeRedenOpschorting"),
      ).toHaveClass("mat-mdc-chip-highlighted");
      expect(
        chipWithTooltip("indicatie.HEROPEND: fakeStatusToelichting"),
      ).toHaveClass("mat-mdc-chip-highlighted");
      expect(
        chipWithTooltip("indicatie.VERLENGD: Reden: fakeRedenVerlenging"),
      ).not.toHaveClass("mat-mdc-chip-highlighted");
    });

    it("shows HOOFDZAAK and DEELZAAK without relation details", async () => {
      await setup({
        layout: IndicatiesLayout.SEARCH,
        zaakZoekObject: makeZaakZoekObject({
          indicaties: ["HOOFDZAAK", "DEELZAAK"],
        }),
      });

      expect(chipWithTooltip("indicatie.HOOFDZAAK")).toBeInTheDocument();
      expect(chipWithTooltip("indicatie.DEELZAAK")).toBeInTheDocument();
    });

    it("replaces the indicaties when the zaakZoekObject input changes", async () => {
      const { fixture } = await setup({
        layout: IndicatiesLayout.COMPACT,
        zaakZoekObject: makeZaakZoekObject({ indicaties: ["OPSCHORTING"] }),
      });

      fixture.componentRef.setInput(
        "zaakZoekObject",
        makeZaakZoekObject({
          indicaties: ["ONTVANGSTBEVESTIGING_NIET_VERSTUURD"],
        }),
      );
      fixture.detectChanges();
      await fixture.whenStable();

      expect(screen.getAllByRole("option")).toHaveLength(1);
      expect(
        chipWithTooltip("indicatie.ONTVANGSTBEVESTIGING_NIET_VERSTUURD"),
      ).toBeInTheDocument();
    });
  });

  it("shows the indicaties of the zaak when both a zaak and a zaakZoekObject are given", async () => {
    await setup({
      layout: IndicatiesLayout.EXTENDED,
      zaak: makeZaak({ indicaties: ["VERLENGD"] }),
      zaakZoekObject: makeZaakZoekObject({ indicaties: ["OPSCHORTING"] }),
    });

    const options = screen.getAllByRole("option");
    expect(options).toHaveLength(1);
    expect(options[0]).toHaveAccessibleName("indicatie.VERLENGD");
  });

  it("shows no indicaties when neither a zaak nor a zaakZoekObject is given", async () => {
    await setup({ layout: IndicatiesLayout.COMPACT });

    expect(screen.queryAllByRole("option")).toHaveLength(0);
  });
});
