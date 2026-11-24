/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";

function escapeForRegExp(text: string) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/** The page of one document. */
export function documentPage(page: Page) {
  const gegevens = page.getByRole("tabpanel", { name: "Gegevens" });

  return {
    async expectOpened(titel: string) {
      await expect(page).toHaveURL(/\/informatie-objecten\//);
      await expect(gegevens).toContainText(titel);
    },

    /** Expects the field of the Gegevens tab, such as "Titel", to have the value. */
    async expectGegeven(field: string, value: string) {
      await expect(gegevens).toContainText(
        new RegExp(`${field}\\s*${escapeForRegExp(value)}`),
      );
    },

    indicatie(indicatie: string) {
      return page.getByRole("option", { name: indicatie });
    },
  };
}
