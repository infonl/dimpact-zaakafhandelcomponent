/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { test as base, createBdd } from "playwright-bdd";
import { User } from "./types";

export const test = base.extend<{
  signIn: () => Promise<void>;
  userToLogin: { value: User | null };
}>({
  signIn: async ({ page, userToLogin }, use) => {
    await use(async () => {
      const { value } = userToLogin;
      if (!value) throw new Error("User to login not set");

      await page.goto("");

      await page
        .getByRole("textbox", { name: "Username or email" })
        .fill(value.username);
      await page
        .getByRole("textbox", { name: "Password" })
        .fill(value.password);

      const signInRequest = page.waitForResponse(/login-actions\/authenticate/);
      await page.getByRole("button", { name: "Sign In" }).click();
      await signInRequest;

      await page.waitForTimeout(5000); // Give time to set all the session details and redirects
    });
  },
  userToLogin: async ({}, use) => {
    await use({ value: null });
  },
});

export const { Given, When, Then, Before, AfterStep } = createBdd(test);
