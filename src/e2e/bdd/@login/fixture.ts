/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { test as base } from "playwright-bdd";

export const test = base.extend<{
  signIn: () => Promise<void>;
  userToLogin: { username: string; password: string };
}>({
  signIn: async ({ page, userToLogin }, use) => {
    await use(async () => {
      await page.goto("");

      await page
        .getByRole("textbox", { name: "Username or email" })
        .fill(userToLogin.username);
      await page
        .getByRole("textbox", { name: "Password" })
        .fill(userToLogin.password);

      const signInRequest = page.waitForResponse(/login-actions\/authenticate/);
      await page.getByRole("button", { name: "Sign In" }).click();
      await signInRequest;

      await page.waitForTimeout(5000); // Give time to set all the session details and redirects
    });
  },
  userToLogin: async ({}, use) => {
    await use({ username: "", password: "" });
  },
});
