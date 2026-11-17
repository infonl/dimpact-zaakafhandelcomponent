/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { Page } from "@playwright/test";
import { ENV } from "../../config/env";

const { signInPage } = ENV;

export function keycloakLoginPage(page: Page) {
  return page.getByRole("heading", { name: signInPage.heading });
}

export function invalidCredentialsMessage(page: Page) {
  return page.getByText(signInPage.invalidCredentialsMessage);
}

export async function signIn(
  page: Page,
  credentials: { username: string; password: string },
) {
  await page
    .getByRole("textbox", { name: signInPage.usernameField })
    .fill(credentials.username);
  await page
    .getByRole("textbox", { name: signInPage.passwordField })
    .fill(credentials.password);
  await page.getByRole("button", { name: signInPage.signInButton }).click();
}
