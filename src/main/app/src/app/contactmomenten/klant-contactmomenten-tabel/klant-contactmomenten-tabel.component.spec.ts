/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ContactmomentenService } from "../contactmomenten.service";
import { KlantContactmomentenTabelComponent } from "./klant-contactmomenten-tabel.component";

describe(KlantContactmomentenTabelComponent.name, () => {
  const user = userEvent.setup();

  let resolveContactmomenten: (
    resultaat: GeneratedType<"RESTResultaatRestContactmoment">,
  ) => void;
  let detectChanges: () => void;
  let rerender: (options: {
    inputs: { bsn?: string; vestigingsnummer?: string };
  }) => Promise<void>;

  const listContactmomenten = jest.fn(() => ({
    queryKey: ["contactmomenten", listContactmomenten.mock.calls.length],
    queryFn: () =>
      new Promise<GeneratedType<"RESTResultaatRestContactmoment">>(
        (resolve) => (resolveContactmomenten = resolve),
      ),
  }));

  function lastSearch() {
    return listContactmomenten.mock.lastCall![0] as Parameters<
      ContactmomentenService["listContactmomenten"]
    >[0];
  }

  async function setup(inputs: { bsn?: string; vestigingsnummer?: string }) {
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
    // the table creates the row views in one pass and binds their cells in the next
    detectChanges();
    detectChanges();
  }

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

    expect(screen.queryByText("msg.loading")).toBeNull();
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
      screen.getByRole("columnheader", {
        name: "contactmoment.registratiedatum",
      }),
    ).toBeVisible();
    expect(
      screen.getByRole("row", {
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

  it("shows an empty message when there are no contactmomenten", async () => {
    await setup({ bsn: "999993896" });

    await receive([]);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
    expect(screen.getByText("0 of 0")).toBeVisible();
  });

  it("searches the next page of contactmomenten", async () => {
    await setup({ bsn: "999993896" });
    await receive([fromPartial<GeneratedType<"RestContactmoment">>({})], 10);

    await user.click(screen.getByRole("button", { name: "Next page" }));
    await sleep();

    expect(lastSearch().page).toBe(1);
  });

  it("returns to the first page when it is pointed at another klant", async () => {
    await setup({ bsn: "999993896" });
    await receive([fromPartial<GeneratedType<"RestContactmoment">>({})], 10);
    await user.click(screen.getByRole("button", { name: "Next page" }));
    await sleep();

    await rerender({ inputs: { bsn: "111111111" } });
    await sleep();

    expect(lastSearch().page).toBe(0);
  });
});
