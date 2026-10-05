/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, waitFor } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ContactmomentenService } from "../contactmomenten.service";
import { KlantContactmomentenTabelComponent } from "./klant-contactmomenten-tabel.component";

describe(KlantContactmomentenTabelComponent.name, () => {
  const user = userEvent.setup();

  let pendingSearches: ((
    resultaat: GeneratedType<"RESTResultaatRestContactmoment">,
  ) => void)[] = [];
  let detectChanges: () => void;
  let setLoading: jest.SpyInstance;
  let rerender: (options: {
    inputs: { bsn?: string; vestigingsnummer?: string };
  }) => Promise<void>;

  let searchCount = 0;
  const listContactmomenten = jest.fn(
    (
      _parameters: Parameters<ContactmomentenService["listContactmomenten"]>[0],
    ) => ({
      queryKey: ["contactmomenten", ++searchCount],
      queryFn: () =>
        new Promise<GeneratedType<"RESTResultaatRestContactmoment">>(
          (resolve) => pendingSearches.push(resolve),
        ),
    }),
  );

  function lastSearch() {
    return listContactmomenten.mock.lastCall![0];
  }

  async function setup(inputs: { bsn?: string; vestigingsnummer?: string }) {
    setLoading = jest
      .spyOn(UtilService.prototype, "setLoading")
      .mockImplementation(() => undefined);
    const rendered = await render(KlantContactmomentenTabelComponent, {
      inputs,
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideQueryClient(testQueryClient),
        provideHttpClient(),
        {
          provide: ContactmomentenService,
          useValue: fromPartial<ContactmomentenService>({
            listContactmomenten: listContactmomenten as never,
          }),
        },
      ],
    });

    detectChanges = rendered.detectChanges;
    rerender = rendered.rerender;
    await sleep();
    detectChanges();
  }

  function resolveContactmomenten(
    resultaat: GeneratedType<"RESTResultaatRestContactmoment">,
  ) {
    pendingSearches.forEach((resolve) => resolve(resultaat));
    pendingSearches = [];
  }

  async function receive(
    resultaten: GeneratedType<"RestContactmoment">[],
    totaal = resultaten.length,
  ) {
    resolveContactmomenten(
      fromPartial<GeneratedType<"RESTResultaatRestContactmoment">>({
        resultaten,
        totaal,
      }),
    );
    await sleep();
  }

  afterEach(async () => {
    // a search still in flight is cancelled when the query cache is cleared, which rejects into the next test
    resolveContactmomenten(
      fromPartial<GeneratedType<"RESTResultaatRestContactmoment">>({}),
    );
    await sleep();
  });

  it("searches the contactmomenten of a persoon", async () => {
    await setup({ bsn: "999993896" });

    expect(lastSearch()).toEqual(
      expect.objectContaining({ bsn: "999993896", page: 0 }),
    );
  });

  it("searches the contactmomenten of a vestiging", async () => {
    await setup({ vestigingsnummer: "000099998888" });

    expect(lastSearch()).toEqual(
      expect.objectContaining({ vestigingsnummer: "000099998888", page: 0 }),
    );
  });

  it("searches without a klant when neither a bsn nor a vestigingsnummer is given", async () => {
    await setup({});

    expect(lastSearch().bsn).toBeUndefined();
    expect(lastSearch().vestigingsnummer).toBeUndefined();
  });

  it("announces that it is loading until the contactmomenten arrive", async () => {
    await setup({ bsn: "999993896" });

    expect(screen.getByText("msg.loading")).toBeVisible();

    await receive([]);

    await waitFor(() => expect(screen.queryByText("msg.loading")).toBeNull());
  });

  it("turns the loading indicator on until the contactmomenten arrive", async () => {
    await setup({ bsn: "999993896" });

    expect(setLoading).toHaveBeenLastCalledWith(true);

    await receive([]);

    expect(setLoading).toHaveBeenLastCalledWith(false);
  });

  it("lists the contactmomenten it found", async () => {
    await setup({ bsn: "999993896" });

    await receive([
      fromPartial<GeneratedType<"RestContactmoment">>({
        kanaal: "telefoon",
        initiatiefnemer: "burger",
        medewerker: "jan.de.vries",
        tekst: "Vraag over aanvraag",
      }),
      fromPartial<GeneratedType<"RestContactmoment">>({
        kanaal: "email",
        initiatiefnemer: "gemeente",
        medewerker: "piet.pietersen",
        tekst: "Bevestiging ontvangen",
      }),
    ]);

    expect(
      await screen.findByRole("row", {
        name: /telefoon burger jan\.de\.vries Vraag over aanvraag/,
      }),
    ).toBeVisible();
    expect(
      screen.getByRole("row", {
        name: /email gemeente piet\.pietersen Bevestiging ontvangen/,
      }),
    ).toBeVisible();
    expect(screen.getByText("1 – 2 of 2")).toBeVisible();
  });

  it("shows a column for each field of a contactmoment", async () => {
    await setup({ bsn: "999993896" });

    for (const column of [
      "contactmoment.registratiedatum",
      "contactmoment.kanaal",
      "contactmoment.initiatiefnemer",
      "contactmoment.medewerker",
      "contactmoment.tekst",
    ]) {
      expect(screen.getByRole("columnheader", { name: column })).toBeVisible();
    }
  });

  it("shows the total number of contactmomenten in the paginator", async () => {
    await setup({ bsn: "999993896" });

    await receive([fromPartial<GeneratedType<"RestContactmoment">>({})], 12);

    expect(await screen.findByText("1 – 5 of 12")).toBeVisible();
  });

  it("shows an empty message when there are no contactmomenten", async () => {
    await setup({ bsn: "999993896" });

    await receive([]);

    expect(await screen.findByText("msg.geen.gegevens.gevonden")).toBeVisible();
    expect(screen.getByText("0 of 0")).toBeVisible();
  });

  it("shows an empty message when a resultaat has no resultaten and no totaal", async () => {
    await setup({ bsn: "999993896" });

    resolveContactmomenten(
      fromPartial<GeneratedType<"RESTResultaatRestContactmoment">>({}),
    );
    await sleep();

    expect(await screen.findByText("msg.geen.gegevens.gevonden")).toBeVisible();
    expect(screen.getByText("0 of 0")).toBeVisible();
  });

  it("searches the next page of contactmomenten", async () => {
    await setup({ bsn: "999993896" });
    await receive([fromPartial<GeneratedType<"RestContactmoment">>({})], 10);

    await user.click(await screen.findByRole("button", { name: "Next page" }));
    await sleep();

    expect(lastSearch().page).toBe(1);
  });

  it("returns to the first page when it is pointed at another klant", async () => {
    await setup({ bsn: "999993896" });
    await receive([fromPartial<GeneratedType<"RestContactmoment">>({})], 10);
    await user.click(await screen.findByRole("button", { name: "Next page" }));
    await sleep();

    await rerender({ inputs: { bsn: "111111111" } });
    await sleep();

    expect(lastSearch().page).toBe(0);
  });

  it("keeps searching for the bsn it was first rendered with when the bsn changes", async () => {
    await setup({ bsn: "999993896", vestigingsnummer: "000099998888" });
    await receive([]);

    await rerender({
      inputs: { bsn: "999990408", vestigingsnummer: "000099998888" },
    });
    await receive([]);

    expect(lastSearch()).toEqual(
      expect.objectContaining({
        bsn: "999993896",
        vestigingsnummer: "000099998888",
      }),
    );
  });

  it("keeps searching for the vestigingsnummer it was first rendered with when the vestigingsnummer changes", async () => {
    await setup({ bsn: "999993896", vestigingsnummer: "000099998888" });
    await receive([]);

    await rerender({
      inputs: { bsn: "999993896", vestigingsnummer: "000011112222" },
    });
    await receive([]);

    expect(lastSearch()).toEqual(
      expect.objectContaining({
        bsn: "999993896",
        vestigingsnummer: "000099998888",
      }),
    );
  });
});
