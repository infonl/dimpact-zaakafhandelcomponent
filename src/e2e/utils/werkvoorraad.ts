/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Locator, type Page } from "@playwright/test";
import {
  FORTY_SECONDS_IN_MS,
  TEN_SECONDS_IN_MS,
} from "../support/time-constants";
import { caseDescriptionPrefix } from "./test-origin";
import { CMMN_ZAAKTYPE_NAME } from "./zaaktypes";

const SELECT_ROW_LABEL = "Selecteren";

/**
 * The rows of the zaken werkvoorraad of the open zaken that the Cucumber scenarios create.
 *
 * The werkvoorraad of a shared environment also holds zaken of other tests and of people. ZAC skips the ones that it
 * cannot assign, such as a closed zaak or a zaak of a zaaktype that the groep has no rights on, so selecting the first
 * rows of the list can select zaken that never get a behandelaar.
 */
export function e2eZaakRows(page: Page) {
  return page
    .getByRole("row")
    .filter({
      has: page.getByRole("cell", { name: CMMN_ZAAKTYPE_NAME, exact: true }),
    })
    .filter({ hasText: caseDescriptionPrefix("E2E test") });
}

export async function expectAtLeastRows(rows: Locator, minimum: number) {
  await expect
    .poll(() => rows.count(), { timeout: TEN_SECONDS_IN_MS })
    .toBeGreaterThanOrEqual(minimum);
}

export async function selectFirstRows(rows: Locator, count: number) {
  for (let index = 0; index < count; index++) {
    await rows
      .getByLabel(SELECT_ROW_LABEL, { exact: true })
      .first()
      .setChecked(true);
  }
}

/**
 * Selects the first rows and waits until ZAC lists all of them with a behandelaar, which the Vrijgeven button counts.
 * The list shows an assignment only after the search index has it, so the page is reloaded until it does.
 */
export async function selectFirstRowsWithBehandelaar(
  page: Page,
  rows: Locator,
  count: number,
) {
  await expect(async () => {
    await Promise.all([page.waitForResponse(/zoeken\/list/), page.reload()]);
    await expectAtLeastRows(rows, count);
    await selectFirstRows(rows, count);
    await expect(page.locator("#vrijgeven_button")).toContainText(
      new RegExp(`(^|\\D)${count}(\\D|$)`),
      { timeout: TEN_SECONDS_IN_MS },
    );
  }).toPass({ timeout: FORTY_SECONDS_IN_MS });
}
