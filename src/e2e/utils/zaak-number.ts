/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Page, expect } from "@playwright/test";

/**
 * Waits until the zaak number is shown on the page.
 * The number of places that show it (page title, breadcrumbs, zaak details) is a layout choice
 * of the application, so this checks that it is shown and not how often.
 */
export async function expectZaakNumberVisible(
  page: Page,
  zaakNumber: string,
  timeout: number,
) {
  await expect(page.getByText(zaakNumber).first()).toBeVisible({ timeout });
}
