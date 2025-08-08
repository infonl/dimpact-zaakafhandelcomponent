/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { createBdd } from "playwright-bdd";
import { test } from "../fixture";

const { Given, When } = createBdd(test);

Given("a valid CMMN case exists", async ({}) => {
  console.log("ensure a case exists, else make it");
});

When("I add a new {string} CMMN case", async ({ page }, caseType: string) => {
  await page.getByRole("combobox", { name: "Casetype" }).click();
  await page.getByRole("option", { name: caseType }).click();

  await page.getByRole("combobox", { name: "Communication channel" }).click();
  await page.getByRole("option", { name: "E-mail" }).click();

  await page
    .getByRole("textbox", { name: "Description" })
    .fill("E2E test omschrijving");

  const response = page.waitForResponse(/zaken\/zaak/);
  await page.getByRole("button", { name: "Create" }).click();
  await response;
});
