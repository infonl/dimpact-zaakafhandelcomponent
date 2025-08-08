/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { createBdd } from "playwright-bdd";
import { test } from "fixtures/authentication.fixture";

const { Given, When, Then } = createBdd(test);

When("I am on the {string} page", async ({ page }, route: string) => {
  await page.goto(route);
  await page.waitForLoadState("domcontentloaded");
  expect(page.url()).toContain(route);
});
