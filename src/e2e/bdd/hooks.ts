/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { createBdd } from "playwright-bdd";
import { test } from "./@login/fixture";

const { AfterStep, Before } = createBdd();
const { Before: BeforeWithAth } = createBdd(test);

Before({}, async ({ page }) => {
  await page.context().clearCookies();
  await page.goto("");
});

BeforeWithAth({ tags: "@auth" }, async ({ userToLogin, signIn }) => {
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
