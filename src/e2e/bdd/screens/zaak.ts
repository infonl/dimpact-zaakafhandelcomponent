/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";

/** The page of one zaak. */
export function zaakPage(page: Page) {
  async function tab(name: string) {
    await page.getByRole("tab", { name }).click();
    return page.getByRole("tabpanel", { name });
  }

  return {
    async open(identificatie: string) {
      await page.goto(`/zaken/${identificatie}`);
      await this.expectOpened(identificatie);
    },

    async expectOpened(identificatie: string) {
      await expect(
        page.getByRole("heading", { name: identificatie, level: 1 }),
      ).toBeVisible();
    },

    /** Expects the field of the Gegevens tab, such as "Status" or "Groep", to have the value. */
    async expectGegeven(field: string, value: string) {
      await expect(await tab("Gegevens")).toContainText(
        new RegExp(`${field}\\s*${value}`),
      );
    },

    async expectInitiator(naam: string) {
      await expect(
        page.getByRole("button", { name: `Initiator ${naam}` }),
      ).toBeVisible();
    },

    async expectBagObject(adres: string) {
      await expect(await tab("BAG objecten")).toContainText(adres);
    },

    async expectNoOntvangstbevestiging() {
      await expect(
        page.getByRole("option", { name: "Geen bevestiging verstuurd" }),
      ).toBeVisible();
    },
  };
}
