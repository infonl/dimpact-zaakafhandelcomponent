/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";

/** A list of zaken from the search index, such as the werkvoorraad. */
function zakenLijst(page: Page, path: string) {
  /** Filters the list on the zaaknummer, and waits for the answer of the search, so a missing zaak is really missing. */
  async function filterOnZaaknummer(identificatie: string) {
    const zaaknummerFilter = page.getByRole("textbox", { name: "Zaaknummer" });
    await zaaknummerFilter.fill(identificatie);
    await Promise.all([
      page.waitForResponse(
        (response) =>
          new URL(response.url()).pathname.endsWith("/rest/zoeken/list") &&
          response.ok(),
      ),
      zaaknummerFilter.press("Enter"),
    ]);
    return page.getByRole("cell", { name: identificatie });
  }

  return {
    async open() {
      await page.goto(path);
    },

    async expectZaak(identificatie: string) {
      await expect(await filterOnZaaknummer(identificatie)).toBeVisible();
    },

    async expectNoZaak(identificatie: string) {
      await expect(await filterOnZaaknummer(identificatie)).toBeHidden();
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
