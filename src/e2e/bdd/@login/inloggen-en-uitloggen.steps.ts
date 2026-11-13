/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { Given, Then, When } from "../_core/fixture";
import { isRole } from "../_core/roles";
import { keycloakLoginPage, signIn } from "../support/keycloak";
import { dashboard, profileMenuButton } from "../support/zac";

function roleNamed(name: string) {
  if (!isRole(name)) throw new Error(`"${name}" is not a role of ZAC.`);
  return name;
}

Given("I am signed out", async ({ context }) => {
  expect(await context.cookies()).toEqual([]);
});

When("I open ZAC", async ({ page }) => {
  await page.goto("/");
});

When("I open ZAC again", async ({ page }) => {
  await page.goto("/");
});

When("I sign in as a {word}", async ({ page, accountOfRole }, role: string) => {
  await signIn(page, accountOfRole(roleNamed(role)));
});

When(
  "I sign in as a {word} with a wrong password",
  async ({ page, accountOfRole }, role: string) => {
    const { username, password } = accountOfRole(roleNamed(role));
    await signIn(page, { username, password: `not-${password}` });
  },
);

When("I sign out", async ({ page }) => {
  await profileMenuButton(page).click();
  await page.getByRole("menuitem", { name: "Uitloggen" }).click();
});

Then("I see the Keycloak login page", async ({ page }) => {
  await expect(keycloakLoginPage(page)).toBeVisible();
});

Then("I stay on the Keycloak login page", async ({ page }) => {
  await expect(keycloakLoginPage(page)).toBeVisible();
});

Then("I see my dashboard", async ({ page }) => {
  await expect(dashboard(page)).toBeVisible();
});

Then("I do not see my dashboard", async ({ page }) => {
  await expect(dashboard(page)).toBeHidden();
});

Then(
  "the header shows that I am signed in as the {word}",
  async ({ page, accountOfRole }, role: string) => {
    const { displayName } = accountOfRole(roleNamed(role));
    await profileMenuButton(page).click();
    await expect(
      page.getByRole("menuitem", { name: displayName }),
    ).toBeVisible();
    await page.keyboard.press("Escape");
  },
);

Then("I see the message {string}", async ({ page }, message: string) => {
  await expect(page.getByText(message)).toBeVisible();
});
