/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, When } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import {
  FIFTEEN_SECONDS_IN_MS,
  ONE_MINUTE_IN_MS,
  TWENTY_SECONDS_IN_MS,
  TWO_MINUTES_IN_MS,
} from "../support/time-constants";
import { users } from "../support/worlds/users";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers } from "../utils/schemes";

export async function login(
  world: CustomWorld,
  username: string,
  password: string,
) {
  await world.page.getByLabel("Username or email").fill(username);
  await world.page.getByText("Password").fill(password);
  await world.page.getByRole("button", { name: "Sign In" }).click();
}

async function loginToZac(this: CustomWorld, user: string) {
  const parsedUser = worldUsers.parse(user);
  const { username, password } = this.worldParameters.users[parsedUser];

  await login(this, username, password);
}

async function waitForPage(world: CustomWorld) {
  const account_circle = world.page.getByText("account_circle");
  const loginHeader = world.page.getByText("ZAAKAFHANDELCOMPONENT");
  return account_circle
    .or(loginHeader)
    .first()
    .waitFor({ state: "attached", timeout: TWENTY_SECONDS_IN_MS })
    .then(() => true)
    .catch(() => false);
}

async function logout(world: CustomWorld) {
  if (!(await waitForPage(world))) return;
  if (!(await world.page.getByText("account_circle").isVisible())) return;
  await world.page.getByText("account_circle").first().click();
  await world.page.getByText("Uitloggen").first().click();
}

// Keycloak can land on a page without the login form, e.g. a blank page or an expired-login page.
async function isLoginFormShown(world: CustomWorld) {
  return world.page
    .getByLabel("Username or email")
    .waitFor({ timeout: FIFTEEN_SECONDS_IN_MS })
    .then(() => true)
    .catch(() => false);
}

async function isLoggedIn(world: CustomWorld, user: keyof typeof users) {
  if (!(await waitForPage(world))) return false;
  const account_circle = world.page.getByText("account_circle");
  if (!(await account_circle.isVisible())) return false;
  await account_circle.click();
  const { username } = users[user];
  const profileText = world.page.getByRole("menu").filter({
    hasText: username,
  });
  const isVisible = await profileText.isVisible();
  await world.page.keyboard.press("Escape");
  return isVisible;
}

When(
  "Employee {string} logs in to zac",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: string) {
    await loginToZac.call(this, user);
  },
);

When(
  "Employee {string} logs out of zac",
  async function (this: CustomWorld, _: string) {
    await logout(this);
  },
);

// @deprecated
When(
  "{string} logs in",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: string) {
    await loginToZac.call(this, user);
  },
);

Given(
  "{string} is logged in to zac",
  { timeout: TWO_MINUTES_IN_MS },
  async function (this: CustomWorld, user: keyof typeof users) {
    const expectedUrl = this.worldParameters.urls["zac"];
    await this.openUrl(expectedUrl);
    let loggedIn = await isLoggedIn(this, user);
    let tries = 0;
    while (!loggedIn && tries++ < 4) {
      await logout(this);
      if (await isLoginFormShown(this)) {
        await loginToZac.call(this, user);
      } else {
        await this.openUrl(expectedUrl);
      }
      loggedIn = await isLoggedIn(this, user);
    }
    expect(loggedIn, `Failed to log in to ZAC as "${user}"`).toBe(true);
  },
);
