/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, When } from "@cucumber/cucumber";
import { profiles } from "../support/worlds/userProfiles";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers } from "../utils/schemes";

const ONE_MINUTE_IN_MS = 60_000;

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
  return account_circle.or(loginHeader).waitFor();
}

async function logout(world: CustomWorld) {
  await waitForPage(world);
  if (!(await world.page.getByText("account_circle").isVisible())) return;
  await world.page.getByText("account_circle").first().click();
  await world.page.getByText("Uitloggen").first().click();
}

async function isLoggedIn(world: CustomWorld, user: keyof typeof profiles) {
  await waitForPage(world);
  const account_circle = world.page.getByText("account_circle");
  if (!(await account_circle.isVisible())) return false;
  await account_circle.click();
  const { username } = profiles[user];
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
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user: keyof typeof profiles) {
    const expectedUrl = this.worldParameters.urls["zac"];
    await this.openUrl(expectedUrl);
    let tries = 0;
    while (!(await isLoggedIn(this, user)) && tries++ < 4) {
      await logout(this);
      await loginToZac.call(this, user);
    }
  },
);
