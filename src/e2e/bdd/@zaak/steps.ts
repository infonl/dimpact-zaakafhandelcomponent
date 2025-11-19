/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { createBdd } from "playwright-bdd";
import { test } from "./fixture";

const { Given, When, Then } = createBdd(test);

Given("the case type {string} exists", async ({}, caseType: string) => {
  console.log(`TODO: ensure the case type ${caseType} exists, else make it`);
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

Then("the case gets created", async ({ page, caseNumber }) => {
  await page.waitForURL(/zaken\/ZAAK-\d{4}-\d+/);

  const url = page.url();
  caseNumber.value = url.split("/").pop();
  await page.waitForTimeout(5000); // Give SOLR time to index the case
});

Then("I see the case in my overview", async ({ page, caseNumber }) => {
  await page.goto("/zaken/werkvoorraad", { waitUntil: "networkidle" });
  expect(page.getByText(caseNumber.value)).toBeVisible();
});
