/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { chromium, expect, type FullConfig, type Page } from "@playwright/test";

const testUsers = [
  {
    session: "auth/user1.json",
    username: process.env.QA_USERNAME,
    password: process.env.QA_PASSWORD,
  },
  {
    session: "auth/user2.json",
    username: process.env.QA_USERNAME_2,
    password: process.env.QA_PASSWORD_2,
  },
];

export async function login(page: Page, username: string, password: string) {
  await page.goto("/");
  await page.getByLabel("Username or email").fill(username);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await expect(page.getByText("account_circle").first()).toBeVisible({
    timeout: 30_000,
  });
}

// Logs in once per test user through Keycloak and saves the session, so the agent's tests start logged in.
export default async function globalSetup(config: FullConfig) {
  const { baseURL, locale } = config.projects[0].use;
  const browser = await chromium.launch();
  try {
    for (const { session, username, password } of testUsers) {
      if (!username || !password) continue;
      const page = await browser.newPage({ baseURL, locale });
      await login(page, username, password);
      await page.context().storageState({ path: session });
      await page.close();
      console.log(`Logged in and saved ${session}`);
    }
  } finally {
    await browser.close();
  }
}
