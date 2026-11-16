/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { APIRequestContext } from "@playwright/test";
import path from "node:path";
import { test as base } from "playwright-bdd";
import type { Account, Role } from "../../config/targets/target";
import { withLock } from "../support/lock";
import { accountOrSkip } from "./accounts";
import { roleOfTags, sessionFileOfRole } from "./roles";

const SIGNED_OUT = { cookies: [], origins: [] };

export const coreTest = base.extend<{
  accountOfRole: (role: Role) => Account;
  apiAs: (role: Role) => Promise<APIRequestContext>;
  whileNoOtherWorkerDoes: <Result>(
    name: string,
    action: () => Promise<Result>,
  ) => Promise<Result>;
}>({
  storageState: async ({ $tags }, use, testInfo) => {
    const role = roleOfTags($tags);
    if (!role) {
      await use(SIGNED_OUT);
      return;
    }
    accountOrSkip(role, testInfo);
    await use(sessionFileOfRole(role));
  },

  accountOfRole: async ({}, use, testInfo) => {
    await use((role) => accountOrSkip(role, testInfo));
  },

  apiAs: async ({ playwright, baseURL }, use, testInfo) => {
    const contexts: APIRequestContext[] = [];
    await use(async (role) => {
      accountOrSkip(role, testInfo);
      const context = await playwright.request.newContext({
        baseURL,
        storageState: sessionFileOfRole(role),
      });
      contexts.push(context);
      return context;
    });
    await Promise.all(contexts.map((context) => context.dispose()));
  },

  whileNoOtherWorkerDoes: async ({}, use, testInfo) => {
    const locksDirectory = path.join(testInfo.project.outputDir, ".locks");
    await use((name, action) => withLock(locksDirectory, name, action));
  },
});
