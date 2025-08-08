/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { createBdd } from "playwright-bdd";
import { test } from "./fixture";

const { Given, When, Then } = createBdd(test);

Given("I am on the ZAC login page", async ({ page }) => {
  expect(await page.title()).toContain("Sign in");
});

When(
  "I am signing in as {string}",
  async ({ userToLogin, signIn }, user: string) => {
    userToLogin.username = user;
    userToLogin.password = user;
    await signIn();
  }
);

Given("I log out of the system", async ({ page }) => {
  await page.getByRole("button", { name: "Gebruikers profiel" }).click();
  await page.getByRole("menuitem", { name: "Log out" }).click();
});

Then("I should be redirected to the dashboard", async ({ page }) => {
  expect(page.getByText(/^Dashboard$/)).toBeVisible();
});

Then("I should not have access to the dashboard", async ({ page }) => {
  await page.goto("");
  expect(page.getByText(/^Dashboard$/)).not.toBeVisible();
});
