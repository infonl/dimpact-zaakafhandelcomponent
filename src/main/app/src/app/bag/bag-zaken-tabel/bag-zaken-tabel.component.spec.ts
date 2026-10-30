/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  HttpTestingController,
  TestRequest,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { provideNativeDateAdapter } from "@angular/material/core";
import { MatDialog } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { Router } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import {
  fromPartial,
  provideZoekenServiceWithoutRetries,
} from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import { FoutDetailedDialogComponent } from "../../fout-afhandeling/dialog/fout-detailed-dialog.component";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekParameters } from "../../zoeken/model/zoek-parameters";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
import { BagZakenTabelComponent } from "./bag-zaken-tabel.component";

const ZOEKEN_URL = "/rest/zoeken/list";

const makeZoekResultaat = (
  fields: Partial<ZoekResultaat<ZaakZoekObject>> = {},
): ZoekResultaat<ZaakZoekObject> =>
  fromPartial<ZoekResultaat<ZaakZoekObject>>({
    totaal: 0,
    resultaten: [],
    filters: {},
    ...fields,
  });

describe(BagZakenTabelComponent.name, () => {
  const user = userEvent.setup();
  const dialog = { open: jest.fn(), closeAll: jest.fn() };
  let detectChanges: () => void;
  let rerender: (options: {
    inputs: { BagObjectIdentificatie: string };
  }) => Promise<void>;
  let httpTestingController: HttpTestingController;
  let searches: ZoekParameters[];

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
    const request = httpTestingController.expectOne(ZOEKEN_URL);
    searches.push(request.request.body as ZoekParameters);
    return request;
  }

  function lastSearch() {
    return searches.at(-1)!;
  }

  async function respondWith(
    request: TestRequest,
    zoekResultaat = makeZoekResultaat(),
  ) {
    request.flush(zoekResultaat);
    await settle();
  }

  async function fail(request: TestRequest) {
    request.flush(
      { message: "fakeMessage", exception: "fakeException" },
      { status: 400, statusText: "Bad Request" },
    );
    await settle();
  }

  async function setup(zoekResultaat = makeZoekResultaat()) {
    searches = [];
    const rendered = await render(BagZakenTabelComponent, {
      inputs: { BagObjectIdentificatie: "0363010000000001" },
      imports: [NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        provideNativeDateAdapter(),
        provideZoekenServiceWithoutRetries(),
        { provide: MatDialog, useValue: dialog },
      ],
    });

    detectChanges = rendered.detectChanges;
    rerender = rendered.rerender;
    httpTestingController = TestBed.inject(HttpTestingController);
    await respondWith(await nextSearch(), zoekResultaat);
  }

  const twentyFiveZaken = () =>
    makeZoekResultaat({
      totaal: 25,
      resultaten: [fromPartial<ZaakZoekObject>({ identificatie: "ZAAK-001" })],
    });

  it("searches for the zaken of the bag object it was given", async () => {
    await setup(
      makeZoekResultaat({
        totaal: 1,
        resultaten: [
          fromPartial<ZaakZoekObject>({
            identificatie: "ZAAK-001",
          }),
        ],
      }),
    );

    expect(lastSearch()).toEqual(
      expect.objectContaining({
        type: "ZAAK",
        zoeken: expect.objectContaining({
          ZAAK_BAGOBJECTEN: "0363010000000001",
        }),
      }),
    );
    expect(screen.getByRole("row", { name: /ZAAK-001/ })).toBeVisible();
  });

  it("searches once when it is first rendered", async () => {
    await setup();

    await settle();
    httpTestingController.expectNone(ZOEKEN_URL);
    expect(searches).toHaveLength(1);
  });

  it("shows that it is loading until the first search answers", async () => {
    searches = [];
    const rendered = await render(BagZakenTabelComponent, {
      inputs: { BagObjectIdentificatie: "0363010000000001" },
      imports: [NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        provideNativeDateAdapter(),
        provideZoekenServiceWithoutRetries(),
      ],
    });
    detectChanges = rendered.detectChanges;
    httpTestingController = TestBed.inject(HttpTestingController);
    const firstSearch = await nextSearch();

    expect(screen.getByText("msg.loading")).toBeVisible();

    await respondWith(firstSearch);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("searches for open zaken only until afgeronde zaken are shown as well", async () => {
    await setup();

    expect(lastSearch().alleenOpenstaandeZaken).toBe(true);

    await user.click(
      screen.getByRole("switch", { name: "toon-afgeronde-zaken" }),
    );
    await respondWith(await nextSearch());

    expect(lastSearch().alleenOpenstaandeZaken).toBe(false);
  });

  it("returns to the first page when the filters change", async () => {
    await setup(twentyFiveZaken());

    await user.click(screen.getByRole("button", { name: "Next page" }));
    await respondWith(await nextSearch(), twentyFiveZaken());
    expect(lastSearch().page).toBe(1);

    await user.click(
      screen.getByRole("switch", { name: "toon-afgeronde-zaken" }),
    );
    await respondWith(await nextSearch(), twentyFiveZaken());

    expect(lastSearch().page).toBe(0);
  });

  it("returns to the first page when the sort changes", async () => {
    await setup(twentyFiveZaken());

    await user.click(screen.getByRole("button", { name: "Next page" }));
    await respondWith(await nextSearch(), twentyFiveZaken());

    await user.click(screen.getByRole("button", { name: "status" }));
    await respondWith(await nextSearch(), twentyFiveZaken());

    expect(lastSearch()).toEqual(
      expect.objectContaining({
        page: 0,
        sorteerVeld: "ZAAK_STATUS",
        sorteerRichting: "asc",
      }),
    );
  });

  it("stays on the page it is showing when the search for the next one fails", async () => {
    await setup(twentyFiveZaken());

    await user.click(screen.getByRole("button", { name: "Next page" }));
    await fail(await nextSearch());

    expect(screen.getByRole("row", { name: /ZAAK-001/ })).toBeVisible();
    expect(screen.getByText("1 – 10 of 25")).toBeVisible();
  });

  it("keeps searching after a search has failed", async () => {
    await setup(twentyFiveZaken());

    await user.click(screen.getByRole("button", { name: "Next page" }));
    await fail(await nextSearch());
    expect(lastSearch().page).toBe(1);

    await user.click(screen.getByRole("button", { name: "Next page" }));
    await respondWith(await nextSearch(), twentyFiveZaken());

    expect(lastSearch().page).toBe(1);
    expect(screen.getByText("11 – 20 of 25")).toBeVisible();
  });

  describe("when a search with another filter fails", () => {
    beforeEach(async () => {
      await setup(twentyFiveZaken());
      await user.click(
        screen.getByRole("switch", { name: "toon-afgeronde-zaken" }),
      );
      await fail(await nextSearch());
    });

    it("puts the filter back on that of the zaken it is showing", () => {
      expect(
        screen.getByRole("switch", { name: "toon-afgeronde-zaken" }),
      ).not.toBeChecked();
    });

    it("searches with the filter of the zaken it is showing when the page changes next", async () => {
      await user.click(screen.getByRole("button", { name: "Next page" }));
      await respondWith(await nextSearch(), twentyFiveZaken());

      expect(lastSearch().alleenOpenstaandeZaken).toBe(true);
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
    await setup(twentyFiveZaken());
    const navigate = jest
      .spyOn(TestBed.inject(Router), "navigate")
      .mockResolvedValue(true);

    await user.click(screen.getByRole("button", { name: "Next page" }));
    (await nextSearch()).error(new ProgressEvent("error"), { status: 0 });
    await settle();

    expect(navigate).toHaveBeenCalledWith(["/fout"]);
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it("searches again when it is pointed at another bag object", async () => {
    await setup();

    await rerender({ inputs: { BagObjectIdentificatie: "0363010000000002" } });
    await respondWith(await nextSearch());

    expect(lastSearch().zoeken).toEqual(
      expect.objectContaining({ ZAAK_BAGOBJECTEN: "0363010000000002" }),
    );
  });

  it("returns to the first page when it is pointed at another bag object", async () => {
    await setup(twentyFiveZaken());

    await user.click(screen.getByRole("button", { name: "Next page" }));
    await respondWith(await nextSearch(), twentyFiveZaken());
    expect(lastSearch().page).toBe(1);

    await rerender({ inputs: { BagObjectIdentificatie: "0363010000000002" } });
    await respondWith(await nextSearch());

    expect(lastSearch().page).toBe(0);
  });
});
