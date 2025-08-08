/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { createBdd } from "playwright-bdd";
import { test } from "fixtures/zaak.fixture";

const { Given, When, Then } = createBdd(test);

When("I add a new {string} case", async ({ page }, caseType: string) => {
  await page.getByRole("button", { name: "Zaak toevoegen" }).click();
  await page.getByRole("textbox", { name: "Casetype" }).click();
  await page.getByRole("option", { name: caseType }).click();

  await page
    .getByRole("textbox", { name: "Communication channel" })
    .fill("E-mail");
  await page.getByRole("option", { name: " E-mail " }).fill("E-mail");
  await page.getByRole("option", { name: " E-mail " }).click();

  await page
    .getByRole("textbox", { name: "Description" })
    .fill("E2E test omschrijving");
  await page.getByRole("button", { name: "Aanmaken" }).click();
});

Then("the case gets created", async ({ page, caseNumber }) => {
  caseNumber = await page.getByText("ZAAK-").first().textContent();
  expect(page.url()).toContain("/zaken/ZAAK-");
});

Then("I see the case in my overview", async ({ page, caseNumber }) => {
  await page.goto("/zaken/werkvoorraad");
  expect(page.getByText(caseNumber)).toBeVisible();
});
