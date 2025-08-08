/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { createBdd } from "playwright-bdd";
import { test } from "./fixture";

const { When, Then } = createBdd(test);

When("I add a new {string} case", async ({ page }, caseType: string) => {
  await page.getByRole("button", { name: "Zaak toevoegen" }).click();

  await page.getByRole("combobox", { name: "Casetype" }).click();
  await page.getByRole("option", { name: caseType }).click();

  await page.getByRole("combobox", { name: "Communication channel" }).click();
  await page.getByRole("option", { name: "E-mail" }).click();

  await page
    .getByRole("textbox", { name: "Description" })
    .fill("E2E test omschrijving");

  await page.getByRole("button", { name: "Create" }).click();
  await page.waitForResponse(/zaken\/zaak/);
  await page.waitForURL(/zaken\/ZAAK-\d{4}-\d+/);
});

Then("the case gets created", async ({ page, caseNumber }) => {
  const url = page.url();
  caseNumber = url.split("/").pop();
  console.log({ url, caseNumber });
});

Then("I see the case in my overview", async ({ page, caseNumber }) => {
  await page.goto("/zaken/werkvoorraad");
  expect(page.getByText(caseNumber)).toBeVisible();
});
