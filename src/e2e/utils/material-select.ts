/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Locator } from "@playwright/test";
import {
  FIVE_SECONDS_IN_MS,
  FORTY_SECONDS_IN_MS,
} from "../support/time-constants";

// Material ignores a click on a select or autocomplete whose options have not loaded yet,
// so a single click can leave the panel closed for good.
export async function openMaterialPanel(trigger: Locator) {
  await expect(async () => {
    if ((await trigger.getAttribute("aria-expanded")) !== "true") {
      await trigger.click();
    }
    await expect(trigger).toHaveAttribute("aria-expanded", "true", {
      timeout: FIVE_SECONDS_IN_MS,
    });
  }).toPass({ timeout: FORTY_SECONDS_IN_MS });
}

export async function chooseMaterialOption(trigger: Locator, option: Locator) {
  await openMaterialPanel(trigger);
  await option.click({ timeout: FORTY_SECONDS_IN_MS });
}
