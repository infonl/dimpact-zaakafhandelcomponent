/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { Browser, Page } from "@playwright/test";
import { describeTestOriginShort } from "./test-origin";

/**
 * The Verdelen and Vrijgeven dialogs of the zaken and taken werkvoorraad.
 *
 * The werkvoorraad behind a dialog has filters and buttons with the same names as the fields of the dialog,
 * such as the Groep filter, so every control in the dialog is looked up in the dialog only.
 */
export async function assignInDialog(
  page: Page,
  browser: Browser | null,
  options: { groupName?: string; assigneeName?: string } = {},
) {
  await page.getByRole("button", { name: /verdelen/i }).click();
  const dialog = page.getByRole("dialog");

  await dialog.getByLabel(/groep/i).click();
  await pickOption(page, options.groupName);

  await dialog.getByLabel(/medewerker/i).click();
  await pickOption(page, options.assigneeName);

  await dialog.getByLabel(/reden/i).fill(describeTestOriginShort(browser));
  await dialog.getByRole("button", { name: /verdelen/i }).click();
}

export async function releaseInDialog(page: Page, browser: Browser | null) {
  await page.getByRole("button", { name: /vrijgeven/i }).click();
  const dialog = page.getByRole("dialog");

  await dialog.getByLabel(/reden/i).fill(describeTestOriginShort(browser));
  await dialog.getByRole("button", { name: /vrijgeven/i }).click();
}

async function pickOption(page: Page, name?: string) {
  const option = name
    ? page.getByRole("option", { name })
    : page.getByRole("option").first();
  await option.click();
}
