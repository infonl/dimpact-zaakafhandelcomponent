/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { test } from "fixtures/authentication.fixture";
import { createBdd } from "playwright-bdd";

const { Given, When, Then } = createBdd(test);

Given("I am on the ZAC login page", async ({ page }) => {
  await page.goto("", { waitUntil: "networkidle" });
  expect(await page.title()).toContain("Sign in");
});

Given("I am signing in as {string}", async ({ userToLogin }, user: string) => {
  userToLogin.username = user;
  userToLogin.password = user;
});

Given(
  "I am signed in as {string}",
  async ({ userToLogin, signIn }, user: string) => {
    userToLogin.username = user;
    userToLogin.password = user;
    await signIn();
  }
);

When("I log in with my credentials", async ({ signIn, page }) => {
  await signIn();
});

When("I log out of the system", async ({ page }) => {
  await page.getByRole("button", { name: "Gebruikers profiel" }).click();
  await page.getByRole("menuitem", { name: "Log out" }).click();
});

Then("I should be redirected to the dashboard", async ({ page }) => {
  expect(page.getByText(/^Dashboard$/)).toBeVisible();
});

Then("I should see the message {string}", async ({ page }, message: string) => {
  expect(page.getByText(message)).toBeVisible();
});

Then("I should not have access to the dashboard", async ({ page }) => {
  await page.goto("");
  expect(page.getByText(/^Dashboard$/)).not.toBeVisible();
});
