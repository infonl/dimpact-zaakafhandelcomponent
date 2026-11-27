/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { APIRequestContext } from "@playwright/test";
import path from "node:path";
import { test as base } from "playwright-bdd";
import { ENV, requireZacUrl } from "../../config/env";
import type { Account, Role } from "../../config/targets/target";
import {
  describeServerError,
  SERVER_ERROR,
  type ServerErrorResponse,
} from "../support/server-errors";
import { createTestData, type TestData } from "../support/test-data";
import { zacApiClient, type ZacApi } from "../support/zac-api";
import { accountOrSkip, roleOfTags, sessionOf, SIGNED_OUT } from "./sessions";
import { skipWhenBlocked } from "./tags";

export const coreTest = base.extend<{
  accountOfRole: (role: Role) => Account;
  apiAs: (role: Role) => Promise<ZacApi>;
  testData: TestData;
  reportServerError: (response: ServerErrorResponse) => void;
  blockedScenarios: void;
}>({
  blockedScenarios: [
    async ({ $tags }, use, testInfo) => {
      skipWhenBlocked($tags, testInfo);
      await use();
    },
    { auto: true },
  ],

  reportServerError: async ({}, use, testInfo) => {
    await use((response) =>
      testInfo.annotations.push({
        type: SERVER_ERROR,
        description: describeServerError(response),
      }),
    );
  },

  context: async ({ context, reportServerError }, use) => {
    const pending: Promise<void>[] = [];
    context.on("response", (response) => {
      const url = new URL(response.url());
      if (response.status() < 500 || !ENV.zacUrl) return;
      if (url.origin !== new URL(ENV.zacUrl).origin) return;
      const report = (body: string) =>
        reportServerError({
          method: response.request().method(),
          url: response.url(),
          status: response.status(),
          body,
        });
      // The answer is gone when the page has navigated away from it.
      pending.push(response.text().then(report, () => report("")));
    });
    await use(context);
    await Promise.race([
      Promise.all(pending),
      new Promise((resolve) => setTimeout(resolve, 5_000)),
    ]);
  },

  storageState: async ({ $tags }, use, testInfo) => {
    const role = roleOfTags($tags);
    await use(role ? sessionOf(role, testInfo) : SIGNED_OUT);
  },

  accountOfRole: async ({}, use, testInfo) => {
    await use((role) => accountOrSkip(role, testInfo));
  },

  apiAs: async ({ playwright, reportServerError }, use, testInfo) => {
    const contexts: APIRequestContext[] = [];
    await use(async (role) => {
      const context = await playwright.request.newContext({
        storageState: sessionOf(role, testInfo),
      });
      contexts.push(context);
      return zacApiClient(context, requireZacUrl(), {
        onServerError: reportServerError,
      });
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
