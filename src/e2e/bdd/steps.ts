/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { createBdd } from "playwright-bdd";

const { When, Then } = createBdd();

When("I am on the {string} page", async ({ page }, route: string) => {
  await page.goto(route);
  await page.waitForLoadState("domcontentloaded");
  expect(page.url()).toContain(route);
});

When("I add a new {string} case", async ({ page }, caseType: string) => {
  await page.getByRole("combobox", { name: "Casetype" }).click();
  await page.getByRole("option", { name: caseType }).click();

  await page.getByRole("combobox", { name: "Assign case to group" }).click();
  await page.getByRole("option").first().click();

  await page.getByRole("combobox", { name: "Communication channel" }).click();
  await page.getByRole("option", { name: "E-mail" }).click();

  await page
    .getByRole("textbox", { name: "Description" })
    .fill("E2E test omschrijving");

  const response = page.waitForResponse(/zaken\/zaak/);
  await page.getByRole("button", { name: "Create" }).click();
  await response;
});

Then("I should see the message {string}", async ({ page }, message: string) => {
  expect(page.getByText(message)).toBeVisible();
});
