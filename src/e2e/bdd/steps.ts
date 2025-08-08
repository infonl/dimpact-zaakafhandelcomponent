/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { createBdd } from "playwright-bdd";
import { test } from "./@login/fixture";

const { When, Then, AfterStep, Before } = createBdd(test);

Before({}, async ({ page }) => {
  await page.context().clearCookies();
  await page.goto("");
});

Before({ tags: "@auth" }, async ({ userToLogin, signIn }) => {
  // Set default user credentials if not already set
  if (!userToLogin.username || !userToLogin.password) {
    userToLogin.username = "testuser1";
    userToLogin.password = "testuser1";
  }

  await signIn();
});

AfterStep({ tags: "@timeout" }, async ({ page }) => {
  await page.waitForTimeout(10000);
});

When("I am on the {string} page", async ({ page }, route: string) => {
  await page.goto(route);
  await page.waitForLoadState("domcontentloaded");
  expect(page.url()).toContain(route);
});

Then("I should see the message {string}", async ({ page }, message: string) => {
  expect(page.getByText(message)).toBeVisible();
});
