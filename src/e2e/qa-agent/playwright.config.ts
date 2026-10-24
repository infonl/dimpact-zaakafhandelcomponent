/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { defineConfig, devices } from "@playwright/test";

// Kept apart from the Cucumber and BDD suites, so tests written by the QA Agent run never interfere with them.
export default defineConfig({
  testDir: "./tests",
  outputDir: "../../../qa-output/test-results",
  timeout: 90_000,
  expect: { timeout: 15_000 },
  // One retry tells flaky from broken; more retries would hide real bugs.
  retries: 1,
  workers: 1,
  reporter: [
    ["line"],
    ["json", { outputFile: "../../../qa-output/playwright-results.json" }],
    [
      "html",
      { outputFolder: "../../../qa-output/playwright-report", open: "never" },
    ],
  ],
  use: {
    baseURL: process.env.ZAC_URL,
    locale: "nl-NL",
    screenshot: "on",
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
