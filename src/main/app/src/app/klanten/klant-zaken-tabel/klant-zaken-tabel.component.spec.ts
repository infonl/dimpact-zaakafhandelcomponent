/*
 * SPDX-FileCopyrightText: 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  HttpTestingController,
  TestRequest,
} from "@angular/common/http/testing";
import { inputBinding } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { provideNativeDateAdapter } from "@angular/material/core";
import { MatDialog } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { Router } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import {
  createQueryOptions,
  fromPartial,
  provideZoekenServiceWithoutRetries,
} from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import { FoutDetailedDialogComponent } from "../../fout-afhandeling/dialog/fout-detailed-dialog.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekParameters } from "../../zoeken/model/zoek-parameters";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
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
  const listRoltypen = jest.fn();
  const dialog = { open: jest.fn(), closeAll: jest.fn() };
  let detectChanges: () => void;
  let httpTestingController: HttpTestingController;
  let searches: ZoekParameters[];

  function lastSearch() {
    return searches.at(-1)!;
  }

  function betrokkenhedenOf(identificatie: string) {
    const row = screen.getByRole("row", { name: new RegExp(identificatie) });
    const betrokkeneCell = within(row).getAllByRole("cell")[1];
    return within(betrokkeneCell)
      .queryAllByText(/./)
      .map((element) => element.textContent?.trim());
  }

  async function settle() {
    await sleep();
    detectChanges();
    await sleep();
    // the table creates the row views in one pass and binds their cells in the next
    detectChanges();
    detectChanges();
  }

  async function nextSearch() {
    await settle();
    const request = httpTestingController.expectOne("/rest/zoeken/list");
    searches.push(request.request.body as ZoekParameters);
    return request;
  }

  async function respondWith(
    request: TestRequest,
    zaken: ZaakZoekObject[] = [],
    totaal = zaken.length,
  ) {
    request.flush(
      fromPartial<ZoekResultaat<ZaakZoekObject>>({
        resultaten: zaken,
        totaal,
        filters: {},
      }),
    );
    await settle();
  }

  async function fail(request: TestRequest) {
    request.flush(
      { message: "fakeMessage", exception: "fakeException" },
      { status: 400, statusText: "Bad Request" },
    );
    await settle();
  }

  async function setup({
    klant = persoon,
    zaken = [],
    totaal = zaken.length,
    roltypen = ["Initiator", "Belanghebbende", "Medewerker"],
  }: {
    klant?: GeneratedType<"RestBedrijf" | "RestPersoon">;
    zaken?: ZaakZoekObject[];
    totaal?: number;
    roltypen?: string[];
  } = {}) {
    searches = [];
    listRoltypen.mockReturnValue({
      ...createQueryOptions(
        roltypen.map((naam) =>
          fromPartial<GeneratedType<"RestRoltype">>({ naam }),
        ),
      ),
      queryKey: ["/rest/klanten/roltype"],
    });

    const rendered = await render(KlantZakenTabelComponent, {
      bindings: [inputBinding("klant", () => klant)],
      imports: [NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        provideNativeDateAdapter(),
        provideZoekenServiceWithoutRetries(),
        { provide: MatDialog, useValue: dialog },
        {
          provide: KlantenService,
          useValue: fromPartial<KlantenService>({ listRoltypen }),
        },
      ],
    });

    detectChanges = rendered.detectChanges;
    httpTestingController = TestBed.inject(HttpTestingController);
    await respondWith(await nextSearch(), zaken, totaal);
  }

  async function chooseRoltype(roltype: string) {
    await user.click(
      screen.getByRole("combobox", { name: "betrokkene-roltype.kies" }),
    );
    await user.click(screen.getByRole("option", { name: roltype }));
  }

  it("lists the zaken of the klant", async () => {
    await setup({
      zaken: [zaak("ZAAK-001"), zaak("ZAAK-002"), zaak("ZAAK-003")],
    });

    expect(screen.getByRole("row", { name: /ZAAK-001/ })).toBeVisible();
    expect(screen.getByRole("row", { name: /ZAAK-002/ })).toBeVisible();
    expect(screen.getByRole("row", { name: /ZAAK-003/ })).toBeVisible();
  });

  it("stays on the page it is showing when the search for the next one fails", async () => {
    await setup({ zaken: [zaak("ZAAK-001")], totaal: 25 });

    await user.click(screen.getByRole("button", { name: "Next page" }));
    await fail(await nextSearch());

    expect(screen.getByRole("row", { name: /ZAAK-001/ })).toBeVisible();
    expect(screen.getByText("1 – 10 of 25")).toBeVisible();
  });

  describe("when a search on a roltype fails", () => {
    beforeEach(async () => {
      await setup({ zaken: [zaak("ZAAK-001")], roltypen: ["Behandelaar"] });
      await chooseRoltype("Behandelaar");
      await fail(await nextSearch());
    });

    it("keeps the zaken it is showing", () => {
      expect(screen.getByRole("row", { name: /ZAAK-001/ })).toBeVisible();
    });

    it("puts the roltype filter back on that of the zaken it is showing", () => {
      expect(
        screen.getByRole("combobox", { name: "betrokkene-roltype.kies" }),
      ).not.toHaveTextContent("Behandelaar");
    });

    it("searches on any betrokkenheid again when another filter changes next", async () => {
      await user.click(
        screen.getByRole("switch", { name: "toon-afgeronde-zaken" }),
      );
      await respondWith(await nextSearch());

      expect(lastSearch().zoeken).toEqual(
        expect.objectContaining({ ZAAK_BETROKKENEN: "P-999993896" }),
      );
    });

    it("opens one dialog that says the previous zaken and filters are shown again", () => {
      expect(dialog.open).toHaveBeenCalledTimes(1);
      expect(dialog.open).toHaveBeenCalledWith(FoutDetailedDialogComponent, {
        data: expect.objectContaining({
          waarschuwing: "msg.zoeken.mislukt.vorige.zoekopdracht",
          details: "fakeException",
        }),
      });
    });
  });

  it("sends the user to the error page when a search fails because the session has expired", async () => {
    await setup({ zaken: [zaak("ZAAK-001")], totaal: 25 });
    const navigate = jest
      .spyOn(TestBed.inject(Router), "navigate")
      .mockResolvedValue(true);

    await user.click(screen.getByRole("button", { name: "Next page" }));
    (await nextSearch()).error(new ProgressEvent("error"), { status: 0 });
    await settle();

    expect(navigate).toHaveBeenCalledWith(["/fout"]);
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it("shows the first page from the cache at once when the filters are cleared, as that search succeeded before", async () => {
    await setup({ zaken: [zaak("ZAAK-001")], totaal: 25 });
    await user.click(screen.getByRole("button", { name: "Next page" }));
    await respondWith(await nextSearch(), [zaak("ZAAK-011")], 25);

    await user.click(
      screen.getByRole("button", { name: "actie.zoekopdracht.wissen" }),
    );
    await settle();

    httpTestingController.expectNone("/rest/zoeken/list");
    expect(screen.getByRole("row", { name: /ZAAK-001/ })).toBeVisible();
    expect(screen.getByText("1 – 10 of 25")).toBeVisible();
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
      await respondWith(await nextSearch());

      expect(lastSearch().zoeken).toEqual(
        expect.objectContaining({
          zaak_betrokkene_Behandelaar: "P-999993896",
        }),
      );
    });

    it("searches on a roltype whose name has spaces", async () => {
      await setup({ roltypen: ["Belanghebbende Met Spaties"] });

      await chooseRoltype("Belanghebbende Met Spaties");
      await respondWith(await nextSearch());

      expect(lastSearch().zoeken).toEqual(
        expect.objectContaining({
          zaak_betrokkene_Belanghebbende_Met_Spaties: "P-999993896",
        }),
      );
    });
  });
});
