/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { Then, When } from "../../fixtures";
import { parametersPage } from "../../screens/parameters";

When(
  "I open the zaakafhandelparameters of {seeded}",
  async ({ page }, zaaktype: string) => {
    await parametersPage(page).open(zaaktype);
  },
);

When(
  "I change the default groep to {seeded}",
  async ({ page }, groep: string) => {
    await parametersPage(page).chooseDefaultGroep(groep);
  },
);

When("I save the zaakafhandelparameters", async ({ page }) => {
  await page.getByRole("button", { name: "Opslaan" }).click();
});

Then("the default groep is {seeded}", async ({ page }, groep: string) => {
  await expect(parametersPage(page).defaultGroep).toContainText(groep);
});
