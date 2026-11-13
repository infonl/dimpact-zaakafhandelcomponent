/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { Page } from "@playwright/test";

export function keycloakLoginPage(page: Page) {
  return page.getByRole("heading", { name: "Sign in to your account" });
}

export async function signIn(
  page: Page,
  credentials: { username: string; password: string },
) {
  await page
    .getByRole("textbox", { name: "Username or email" })
    .fill(credentials.username);
  await page
    .getByRole("textbox", { name: "Password" })
    .fill(credentials.password);
  await page.getByRole("button", { name: "Sign In" }).click();
}
