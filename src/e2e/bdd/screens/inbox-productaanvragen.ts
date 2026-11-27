/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { Page } from "@playwright/test";

/** The inbox with the productaanvragen of a type that ZAC does not know. */
export function inboxProductaanvragen(page: Page) {
  return {
    /** Opens the inbox and waits until it has the list, so a missing productaanvraag is really missing. */
    async open() {
      await Promise.all([
        page.waitForResponse(
          (response) =>
            new URL(response.url()).pathname.endsWith(
              "/rest/inbox-productaanvragen",
            ) && response.ok(),
        ),
        page.goto("/productaanvragen/inbox"),
      ]);
    },

    productaanvraag(type: string) {
      return page.getByRole("row").filter({ hasText: type });
    },

    async createZaakFrom(type: string) {
      await this.productaanvraag(type)
        .getByRole("button", { name: "Zaak aanmaken" })
        .click();
    },
  };
}
