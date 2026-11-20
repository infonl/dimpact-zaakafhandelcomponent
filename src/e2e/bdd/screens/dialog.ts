/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Locator, type Page } from "@playwright/test";

/** A dialog, found by its accessible name or, for a dialog that has none yet, by its heading. */
export function findDialog(page: Page, name: string) {
  return page
    .getByRole("dialog", { name })
    .or(
      page
        .getByRole("dialog")
        .filter({ has: page.getByRole("heading", { name }) }),
    );
}

/** Clicks the button that confirms the dialog, and waits until the dialog closes. */
export async function confirmDialog(dialog: Locator, button: string) {
  await dialog.getByRole("button", { name: button }).click();
  await expect(dialog).toBeHidden();
}
