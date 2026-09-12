/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse, provideHttpClient } from "@angular/common/http";
import { provideNativeDateAdapter } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { BagZakenTabelComponent } from "./bag-zaken-tabel.component";

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
  const list = jest.fn();
  let detectChanges: () => void;
  let rerender: (options: {
    inputs: { BagObjectIdentificatie: string };
  }) => Promise<void>;

  function lastSearch() {
    return list.mock.lastCall![0] as Parameters<ZoekenService["list"]>[0];
  }

  function failNextSearch() {
    list.mockReturnValue({
      queryKey: ["failing-query"],
      queryFn: jest
        .fn()
        .mockRejectedValue(new HttpErrorResponse({ status: 500 })),
    });
  }

  async function settle() {
    await sleep();
    // the table creates the row views in one pass and binds their cells in the next
    detectChanges();
    detectChanges();
  }

  async function setup(zoekResultaat = makeZoekResultaat()) {
    list.mockReturnValue(createQueryOptions(zoekResultaat));

    const rendered = await render(BagZakenTabelComponent, {
      inputs: { BagObjectIdentificatie: "0363010000000001" },
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
      ],
    });

    detectChanges = rendered.detectChanges;
    rerender = rendered.rerender;
    await settle();
  }

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

  it("searches for open zaken only until afgeronde zaken are shown as well", async () => {
    await setup();

    expect(lastSearch().alleenOpenstaandeZaken).toBe(true);

    await user.click(
      screen.getByRole("switch", { name: "toonAfgerondeZaken" }),
    );
    await settle();

    expect(lastSearch().alleenOpenstaandeZaken).toBe(false);
  });

  it("returns to the first page when the filters change", async () => {
    await setup(makeZoekResultaat({ totaal: 25 }));

    await user.click(screen.getByRole("button", { name: "Next page" }));
    await settle();
    expect(lastSearch().page).toBe(1);

    await user.click(
      screen.getByRole("switch", { name: "toonAfgerondeZaken" }),
    );
    await settle();

    expect(lastSearch().page).toBe(0);
  });

  it("stays on the page it is showing when the search for the next one fails", async () => {
    await setup(
      makeZoekResultaat({
        totaal: 25,
        resultaten: [
          fromPartial<ZaakZoekObject>({ identificatie: "ZAAK-001" }),
        ],
      }),
    );
    failNextSearch();

    await user.click(screen.getByRole("button", { name: "Next page" }));
    await settle();

    expect(screen.getByRole("row", { name: /ZAAK-001/ })).toBeVisible();
    expect(screen.getByText("1 – 10 of 25")).toBeVisible();
  });

  it("keeps searching after a search has failed", async () => {
    await setup(makeZoekResultaat({ totaal: 25 }));
    failNextSearch();

    await user.click(screen.getByRole("button", { name: "Next page" }));
    await settle();
    expect(lastSearch().page).toBe(1);

    list.mockReturnValue(createQueryOptions(makeZoekResultaat({ totaal: 25 })));
    await user.click(screen.getByRole("button", { name: "Next page" }));
    await settle();

    expect(lastSearch().page).toBe(1);
    expect(screen.getByText("11 – 20 of 25")).toBeVisible();
  });

  it("searches again when it is pointed at another bag object", async () => {
    await setup();

    await rerender({ inputs: { BagObjectIdentificatie: "0363010000000002" } });
    await settle();

    expect(lastSearch().zoeken).toEqual(
      expect.objectContaining({ ZAAK_BAGOBJECTEN: "0363010000000002" }),
    );
  });
});
