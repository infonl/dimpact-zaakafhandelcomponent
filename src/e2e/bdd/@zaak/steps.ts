/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { createBdd } from "playwright-bdd";
import { test } from "./fixture";

const { Then } = createBdd(test);

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
