/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Locator } from "@playwright/test";
import {
  FIVE_SECONDS_IN_MS,
  FORTY_SECONDS_IN_MS,
  TWO_SECONDS_IN_MS,
} from "../support/time-constants";

function selectedFormioOption(select: Locator) {
  return select.locator("option:checked");
}

// Form.io reloads the options of a select when the field in its `refreshOn` changes, or when ZAC
// redraws the form, and can drop the chosen value while it does. Choose again until the choice holds.
// Leaves a select alone that already shows the option, so that it fires no change to refresh on.
export async function chooseFormioOption(select: Locator, optionLabel: string) {
  await expect(async () => {
    if (
      (
        await selectedFormioOption(select).textContent({
          timeout: FIVE_SECONDS_IN_MS,
        })
      )?.trim() !== optionLabel
    ) {
      await select.selectOption(
        { label: optionLabel },
        { timeout: FIVE_SECONDS_IN_MS },
      );
    }
    await expect(selectedFormioOption(select)).toHaveText(optionLabel, {
      timeout: TWO_SECONDS_IN_MS,
    });
  }).toPass({ timeout: FORTY_SECONDS_IN_MS });
}
