/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";

/** The werkvoorraad of zaken: the zaken of the groepen of the user. */
export function zakenWerkvoorraad(page: Page) {
  return {
    async open() {
      await page.goto("/zaken/werkvoorraad");
    },

    async expectZaak(identificatie: string) {
      const zaaknummerFilter = page
        .getByRole("table")
        .getByRole("textbox")
        // eslint-disable-next-line playwright/no-nth-methods -- the filters have no accessible name until the fix that names this one "Zaaknummer" is on main
        .first();
      await zaaknummerFilter.fill(identificatie);
      await zaaknummerFilter.press("Enter");
      await expect(
        page.getByRole("cell", { name: identificatie }),
      ).toBeVisible();
    },
  };
}
