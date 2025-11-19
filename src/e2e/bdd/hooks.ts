/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Before } from "./fixture";
import { DEFAULT_USER, ENV } from "./types";

Before({}, async ({ page }) => {
  await page.context().clearCookies();
  await page.goto("");
});

Before({ tags: "@auth" }, async ({ userToLogin, signIn }) => {
  if (userToLogin.value) return;
  const defaultUser = ENV.users[DEFAULT_USER];
  if (!defaultUser) throw new Error("Default user not found");
  userToLogin.value = defaultUser;

  await signIn();
});
