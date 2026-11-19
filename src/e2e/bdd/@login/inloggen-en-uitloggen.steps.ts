/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { isRole } from "../_core/sessions";
import { Given, Then, When } from "../fixtures";
import { dashboard, profileMenuButton } from "../screens/header";
import {
  invalidCredentialsMessage,
  keycloakLoginPage,
  signIn,
} from "../screens/keycloak";

function roleNamed(name: string) {
  if (!isRole(name)) throw new Error(`"${name}" is not a role of ZAC.`);
  return name;
}

Given("I am signed out", async ({ context }) => {
  await context.clearCookies();
});

When("I open ZAC( again)", async ({ page }) => {
  await page.goto("/");
});

When("I sign in as a {word}", async ({ page, accountOfRole }, role: string) => {
  await signIn(page, accountOfRole(roleNamed(role)));
});

When(
  "I sign in as a {word} with a wrong password",
  async ({ page, accountOfRole }, role: string) => {
    const account = accountOfRole(roleNamed(role));
    const wrongPassword = account.username + account.password;
    await signIn(page, { ...account, password: wrongPassword });
  },
);

When("I sign out", async ({ page }) => {
  await profileMenuButton(page).click();
  await page.getByRole("menuitem", { name: "Uitloggen" }).click();
});

Then("I (still )see the Keycloak login page", async ({ page }) => {
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

Then("Keycloak says that the credentials are invalid", async ({ page }) => {
  await expect(invalidCredentialsMessage(page)).toBeVisible();
});
