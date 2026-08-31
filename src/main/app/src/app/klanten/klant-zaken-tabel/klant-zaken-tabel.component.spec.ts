/*
 * SPDX-FileCopyrightText: 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { inputBinding } from "@angular/core";
import { provideNativeDateAdapter } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { KlantenService } from "../klanten.service";
import { KlantZakenTabelComponent } from "./klant-zaken-tabel.component";

const persoon = fromPartial<GeneratedType<"RestPersoon">>({
  bsn: "999993896",
  temporaryPersonId: "fakeTemporaryPersonId",
  identificatieType: "BSN",
});

const bedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
  vestigingsnummer: "000012345678",
  kvkNummer: "12345678",
  identificatieType: "VN",
});

const zaak = (
  identificatie: string,
  betrokkenen: Record<string, string[]> = {},
) => fromPartial<ZaakZoekObject>({ identificatie, betrokkenen });

describe(KlantZakenTabelComponent.name, () => {
  const user = userEvent.setup();
  const list = jest.fn();
  const listRoltypen = jest.fn();
  let detectChanges: () => void;

  function lastSearch() {
    return list.mock.lastCall![0] as Parameters<ZoekenService["list"]>[0];
  }

  function betrokkenhedenOf(identificatie: string) {
    const row = screen.getByRole("row", { name: new RegExp(identificatie) });
    const betrokkeneCell = within(row).getAllByRole("cell")[1];
    return within(betrokkeneCell)
      .queryAllByText(/./)
      .map((element) => element.textContent?.trim());
  }

  async function setup({
    klant = persoon,
    zaken = [],
    roltypen = ["Initiator", "Belanghebbende", "Medewerker"],
  }: {
    klant?: GeneratedType<"RestBedrijf" | "RestPersoon">;
    zaken?: ZaakZoekObject[];
    roltypen?: string[];
  } = {}) {
    list.mockReturnValue(
      createQueryOptions(
        fromPartial<ZoekResultaat<ZaakZoekObject>>({
          resultaten: zaken,
          totaal: zaken.length,
          filters: {},
        }),
      ),
    );
    listRoltypen.mockReturnValue(
      of(
        roltypen.map((naam) =>
          fromPartial<GeneratedType<"RestRoltype">>({ naam }),
        ),
      ),
    );

    const rendered = await render(KlantZakenTabelComponent, {
      bindings: [inputBinding("klant", () => klant)],
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideQueryClient(testQueryClient),
        provideHttpClient(),
        provideRouter([]),
        provideNativeDateAdapter(),
        {
          provide: ZoekenService,
          useValue: fromPartial<ZoekenService>({ list }),
        },
        {
          provide: KlantenService,
          useValue: fromPartial<KlantenService>({ listRoltypen }),
        },
      ],
    });

    detectChanges = rendered.detectChanges;
    await sleep();
    // the table creates the row views in one pass and binds their cells in the next
    detectChanges();
    detectChanges();
  }

  async function chooseRoltype(roltype: string) {
    await user.click(
      screen.getByRole("combobox", { name: "betrokkeneRoltype.-kies-" }),
    );
    await user.click(screen.getByRole("option", { name: roltype }));
    await sleep();
    detectChanges();
  }

  it("lists the zaken of the klant", async () => {
    await setup({
      zaken: [zaak("ZAAK-001"), zaak("ZAAK-002"), zaak("ZAAK-003")],
    });

    expect(screen.getByRole("row", { name: /ZAAK-001/ })).toBeVisible();
    expect(screen.getByRole("row", { name: /ZAAK-002/ })).toBeVisible();
    expect(screen.getByRole("row", { name: /ZAAK-003/ })).toBeVisible();
  });

  describe("the betrokkenheden of the klant in a zaak", () => {
    it("shows every role the persoon has in the zaak", async () => {
      await setup({
        zaken: [
          zaak("ZAAK-001", {
            Melder: ["P-999993896"],
            Contactpersoon: ["P-999993896"],
          }),
        ],
      });

      expect(betrokkenhedenOf("ZAAK-001")).toEqual([
        "Melder",
        "Contactpersoon",
      ]);
    });

    it("leaves out roles held by another persoon", async () => {
      await setup({
        zaken: [
          zaak("ZAAK-001", {
            Melder: ["P-999993896"],
            Contactpersoon: ["P-999992958"],
          }),
        ],
      });

      expect(betrokkenhedenOf("ZAAK-001")).toEqual(["Melder"]);
    });

    it("shows no roles when the klant is not a betrokkene", async () => {
      await setup({
        zaken: [
          zaak("ZAAK-001", {
            Behandelaar: ["other-id"],
            Adviseur: ["another-id"],
          }),
        ],
      });

      expect(betrokkenhedenOf("ZAAK-001")).toEqual([]);
    });

    it("shows no roles when the zaak has no betrokkenen at all", async () => {
      await setup({ zaken: [zaak("ZAAK-001")] });

      expect(betrokkenhedenOf("ZAAK-001")).toEqual([]);
    });

    it("shows a role once when other betrokkenen hold it as well", async () => {
      await setup({
        zaken: [zaak("ZAAK-001", { Initiator: ["P-999993896", "other-id"] })],
      });

      expect(betrokkenhedenOf("ZAAK-001")).toEqual(["Initiator"]);
    });

    it("shows role names that were stored with underscores as separate words", async () => {
      await setup({
        zaken: [
          zaak("ZAAK-001", { Belanghebbende_Met_Spaties: ["P-999993896"] }),
        ],
      });

      expect(betrokkenhedenOf("ZAAK-001")).toEqual([
        "Belanghebbende Met Spaties",
      ]);
    });

    it("matches a bedrijf on its kvkNummer", async () => {
      await setup({
        klant: fromPartial<GeneratedType<"RestBedrijf">>({
          kvkNummer: "12345678",
          rsin: "123456789",
          identificatieType: "RSIN",
        }),
        zaken: [
          zaak("ZAAK-001", {
            Belanghebbende: ["K-12345678"],
            Adviseur: ["87654321"],
          }),
        ],
      });

      expect(betrokkenhedenOf("ZAAK-001")).toEqual(["Belanghebbende"]);
    });

    it("matches a bedrijf without a vestigingsnummer on its kvkNummer", async () => {
      await setup({
        klant: fromPartial<GeneratedType<"RestBedrijf">>({
          kvkNummer: "12345678",
          identificatieType: "RSIN",
        }),
        zaken: [
          zaak("ZAAK-001", {
            Belanghebbende: ["K-12345678"],
            Adviseur: ["87654321"],
          }),
        ],
      });

      expect(betrokkenhedenOf("ZAAK-001")).toEqual(["Belanghebbende"]);
    });

    it("matches a bedrijf on its vestigingsnummer", async () => {
      await setup({
        klant: bedrijf,
        zaken: [
          zaak("ZAAK-001", {
            Belanghebbende: ["V-12345678-000012345678"],
            Adviseur: ["87654321"],
          }),
        ],
      });

      expect(betrokkenhedenOf("ZAAK-001")).toEqual(["Belanghebbende"]);
    });

    it("shows a role once when it holds both the vestigingsnummer and the kvkNummer", async () => {
      await setup({
        klant: bedrijf,
        zaken: [
          zaak("ZAAK-001", {
            Belanghebbende: ["V-12345678-000012345678", "K-12345678"],
          }),
        ],
      });

      expect(betrokkenhedenOf("ZAAK-001")).toEqual(["Belanghebbende"]);
    });

    it("shows both roles when the vestigingsnummer and the kvkNummer are in different ones", async () => {
      await setup({
        klant: bedrijf,
        zaken: [
          zaak("ZAAK-001", {
            Belanghebbende: ["V-12345678-000012345678"],
            Adviseur: ["K-12345678"],
          }),
        ],
      });

      expect(betrokkenhedenOf("ZAAK-001")).toEqual([
        "Belanghebbende",
        "Adviseur",
      ]);
    });

    it("falls back to the kvkNummer when the vestigingsnummer holds no role", async () => {
      await setup({
        klant: bedrijf,
        zaken: [zaak("ZAAK-001", { Adviseur: ["K-12345678"] })],
      });

      expect(betrokkenhedenOf("ZAAK-001")).toEqual(["Adviseur"]);
    });
  });

  describe("filtering on a betrokkene roltype", () => {
    it("searches on any betrokkenheid until a roltype is chosen", async () => {
      await setup();

      expect(lastSearch().zoeken).toEqual(
        expect.objectContaining({ ZAAK_BETROKKENEN: "P-999993896" }),
      );
    });

    it("searches on the chosen roltype", async () => {
      await setup({ roltypen: ["Behandelaar"] });

      await chooseRoltype("Behandelaar");

      expect(lastSearch().zoeken).toEqual(
        expect.objectContaining({
          zaak_betrokkene_Behandelaar: "P-999993896",
        }),
      );
    });

    it("searches on a roltype whose name has spaces", async () => {
      await setup({ roltypen: ["Belanghebbende Met Spaties"] });

      await chooseRoltype("Belanghebbende Met Spaties");

      expect(lastSearch().zoeken).toEqual(
        expect.objectContaining({
          zaak_betrokkene_Belanghebbende_Met_Spaties: "P-999993896",
        }),
      );
    });
  });
});
