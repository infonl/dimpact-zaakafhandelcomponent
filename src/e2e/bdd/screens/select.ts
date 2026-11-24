/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";

/** Chooses the option of a select. The select ignores a click until ZAC has loaded its options. */
export async function chooseOption(page: Page, select: string, optie: string) {
  const option = page.getByRole("option", { name: optie, exact: true });
  await expect(async () => {
    if (await option.isVisible()) return;
    await page
      .getByRole("combobox", { name: select })
      .click({ timeout: 1_000 });
    await expect(option).toBeVisible({ timeout: 1_000 });
  }).toPass({ timeout: 30_000 });
  await option.click();
}

/** Chooses the option of an autocomplete, which only shows the options that match what is typed. */
export async function chooseFromAutocomplete(
  page: Page,
  autocomplete: string,
  optie: string,
) {
  const field = page.getByRole("combobox", { name: autocomplete, exact: true });
  await field.click();
  await field.fill(optie);
  await page.getByRole("option", { name: optie, exact: true }).click();
}
