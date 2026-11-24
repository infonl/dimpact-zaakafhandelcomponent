/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { APIRequestContext } from "@playwright/test";
import path from "node:path";
import { test as base } from "playwright-bdd";
import { requireZacUrl } from "../../config/env";
import type { Account, Role } from "../../config/targets/target";
import { createTestData, type TestData } from "../support/test-data";
import { zacApiClient, type ZacApi } from "../support/zac-api";
import { accountOrSkip, roleOfTags, sessionOf, SIGNED_OUT } from "./sessions";
import { skipWhenBlocked, skipWithoutMailbox } from "./tags";

export const coreTest = base.extend<{
  accountOfRole: (role: Role) => Account;
  apiAs: (role: Role) => Promise<ZacApi>;
  testData: TestData;
  blockedScenarios: void;
}>({
  blockedScenarios: [
    async ({ $tags }, use, testInfo) => {
      skipWhenBlocked($tags, testInfo);
      skipWithoutMailbox($tags, testInfo);
      await use();
    },
    { auto: true },
  ],

  storageState: async ({ $tags }, use, testInfo) => {
    const role = roleOfTags($tags);
    await use(role ? sessionOf(role, testInfo) : SIGNED_OUT);
  },

  accountOfRole: async ({}, use, testInfo) => {
    await use((role) => accountOrSkip(role, testInfo));
  },

  apiAs: async ({ playwright }, use, testInfo) => {
    const contexts: APIRequestContext[] = [];
    await use(async (role) => {
      const context = await playwright.request.newContext({
        storageState: sessionOf(role, testInfo),
      });
      contexts.push(context);
      return zacApiClient(context, requireZacUrl());
    });
    await Promise.all(contexts.map((context) => context.dispose()));
  },

  testData: async ({ apiAs }, use, testInfo) => {
    await use(
      createTestData({
        apiAs,
        // The output folder of the project is shared by all workers of the run, so their locks meet there.
        locksDirectory: path.join(testInfo.project.outputDir, ".locks"),
      }),
    );
  },
});
