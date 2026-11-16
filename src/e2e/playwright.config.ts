/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { defineConfig, devices } from "@playwright/test";
import { defineBddConfig } from "playwright-bdd";
import { ENV } from "./config/env";

const journeysDirectory = defineBddConfig({
  featuresRoot: "bdd",
  features: "bdd/**/*.feature",
  steps: ["bdd/**/*.steps.ts", "bdd/fixtures.ts"],
});

export default defineConfig({
  fullyParallel: true,
  forbidOnly: ENV.runsInCi,
  retries: ENV.runsInCi ? 1 : 0,
  failOnFlakyTests: ENV.runsInCi,
  workers: ENV.runsInCi ? 2 : 4,
  timeout: 120_000,
  expect: { timeout: 10_000 },
  reporter: ENV.runsInCi
    ? [["html", { open: "never" }], ["github"], ["list"]]
    : [["html", { open: "never" }], ["list"]],
  use: {
    ...devices["Desktop Chrome"],
    baseURL: ENV.zacUrl,
    locale: "nl-NL",
    timezoneId: "Europe/Amsterdam",
    trace: ENV.runsInCi ? "on-first-retry" : "on",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [
    {
      name: "setup",
      testDir: "setup",
      testMatch: /.*\.setup\.ts/,
    },
    {
      name: "journeys",
      testDir: journeysDirectory,
      dependencies: ["setup"],
      grepInvert: /@live-env-only/,
    },
  ],
});
