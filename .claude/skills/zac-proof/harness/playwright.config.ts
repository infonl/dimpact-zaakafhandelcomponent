/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { defineConfig } from "@playwright/test";

const headed = process.env.HEADED === "1";

export default defineConfig({
  testDir: ".",
  testMatch: process.env.SPEC ?? "walkthrough.spec.ts",
  outputDir: "./test-results",
  timeout: 5 * 60_000,
  expect: { timeout: 15_000 },
  retries: 0,
  workers: 1,
  reporter: [["list"]],
  use: {
    baseURL: process.env.ZAC_URL ?? "http://localhost:8080",
    headless: !headed,
    viewport: { width: 1440, height: 900 },
    video: { mode: "on", size: { width: 1440, height: 900 } },
    trace: "retain-on-failure",
    locale: "nl-NL",
    actionTimeout: 30_000,
    navigationTimeout: 60_000,
    launchOptions: { slowMo: headed ? 300 : 100 },
  },
});
