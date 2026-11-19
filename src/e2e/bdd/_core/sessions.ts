/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page, type TestInfo } from "@playwright/test";
import path from "node:path";
import { ENV } from "../../config/env";
import { ROLES, type Account, type Role } from "../../config/targets/target";
import { dashboard } from "../screens/header";
import { signIn } from "../screens/keycloak";

const SESSIONS_DIRECTORY = path.resolve(__dirname, "../../.auth");

export const SIGNED_OUT = { cookies: [], origins: [] };

export function isRole(name: string): name is Role {
  return ROLES.some((role) => role === name);
}

/** The role of a scenario is its role tag. A scenario without a role tag runs signed out. */
export function roleOfTags(tags: string[]): Role | undefined {
  const roles = ROLES.filter((role) => tags.includes(`@${role}`));
  if (roles.length > 1) {
    throw new Error(
      `A scenario runs as one role, but it has the role tags ${roles.map((role) => `@${role}`).join(" and ")}.`,
    );
  }
  return roles[0];
}

/** The account of the role in the target. Skips the test when the target has no account for the role. */
export function accountOrSkip(role: Role, testInfo: TestInfo) {
  const account = ENV.accounts[role];
  testInfo.skip(
    !account,
    `The target "${ENV.target}" has no account for the role ${role}.`,
  );
  // skip() ends the test when the target has no account, so there is one from here on.
  return account as Account;
}

/**
 * The file with the signed-in session of the role, which `saveSessionOf` wrote. The file belongs to the target, so a
 * run against another target never reuses it. Skips the test when the target has no account for the role.
 */
export function sessionOf(role: Role, testInfo: TestInfo) {
  accountOrSkip(role, testInfo);
  return path.join(SESSIONS_DIRECTORY, ENV.target, `${role}.json`);
}

/** Signs in as the role and saves the session, which the scenarios of the role then reuse. */
export async function saveSessionOf(
  role: Role,
  page: Page,
  testInfo: TestInfo,
) {
  const account = accountOrSkip(role, testInfo);
  await page.goto("/");
  await signIn(page, account);
  await expect(dashboard(page)).toBeVisible();
  await page.context().storageState({ path: sessionOf(role, testInfo) });
}
