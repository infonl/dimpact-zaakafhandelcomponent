/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";
import type { Schemas } from "../support/zac-api";

/** Waits for the answer of the search for the text, not of another search, such as the first one of the page. */
function answerOfSearchFor(page: Page, text: string) {
  return page.waitForResponse((response) => {
    if (!new URL(response.url()).pathname.endsWith("/rest/zoeken/list")) {
      return false;
    }
    const { zoeken } = response
      .request()
      .postDataJSON() as Schemas["RestZoekParameters"];
    return response.ok() && Object.values(zoeken ?? {}).includes(text);
  });
}

/** A list of zaken from the search index, such as the werkvoorraad. */
function zakenLijst(page: Page, path: string) {
  /** Filters the list on the zaaknummer, and waits for the answer of the search, so a missing zaak is really missing. */
  async function filterOnZaaknummer(identificatie: string) {
    const zaaknummerFilter = page.getByRole("textbox", { name: "Zaaknummer" });
    await zaaknummerFilter.fill(identificatie);
    await Promise.all([
      answerOfSearchFor(page, identificatie),
      zaaknummerFilter.press("Enter"),
    ]);
  }

  return {
    async open() {
      await page.goto(path);
    },

    /** The row of the zaak. The row of the filters can also hold the zaaknummer, but in a textbox, not in a cell. */
    zaak(identificatie: string) {
      return page
        .getByRole("row")
        .filter({ has: page.getByRole("cell", { name: identificatie }) });
    },

    async expectZaak(identificatie: string) {
      await filterOnZaaknummer(identificatie);
      await expect(this.zaak(identificatie)).toBeVisible();
    },

    async expectNoZaak(identificatie: string) {
      await filterOnZaaknummer(identificatie);
      await expect(this.zaak(identificatie)).toBeHidden();
    },
  };
}

/** The werkvoorraad of zaken: the zaken of the groepen of the user. */
export function zakenWerkvoorraad(page: Page) {
  return zakenLijst(page, "/zaken/werkvoorraad");
}

/** The list of afgehandelde zaken. */
export function afgehandeldeZaken(page: Page) {
  return zakenLijst(page, "/zaken/afgehandeld");
}
