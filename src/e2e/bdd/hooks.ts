/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { createBdd } from "playwright-bdd";
import { test as loginTest } from "./@login/fixture";
import { DEFAULT_USER, ENV } from "./types";

const { AfterStep, Before } = createBdd();
const { Before: BeforeWithAuth } = createBdd(loginTest);

Before({}, async ({ page }) => {
  await page.context().clearCookies();
  await page.goto("");
});

BeforeWithAuth({ tags: "@auth" }, async ({ userToLogin, signIn }) => {
  if (userToLogin.value) return;
  const defaultUser = ENV.users[DEFAULT_USER];
  if (!defaultUser) throw new Error("Default user not found");
  userToLogin.value = defaultUser;

  await signIn();
});

AfterStep({ tags: "@timeout" }, async ({ page }) => {
  await page.waitForTimeout(10000);
});
