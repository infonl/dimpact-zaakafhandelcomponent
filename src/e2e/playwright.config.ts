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
  timeout: ENV.timeouts.testMilliseconds,
  expect: { timeout: ENV.timeouts.expectMilliseconds },
  reporter: ENV.runsInCi
    ? [
        ["html", { open: "never" }],
        ["github"],
        ["list"],
        // scripts/github/comment-e2e-result.sh lists the failed scenarios of this report on the pull request
        ["json", { outputFile: "reports/results.json" }],
      ]
    : [["html", { open: "never" }], ["list"]],
  use: {
    ...devices["Desktop Chrome"],
    baseURL: ENV.zacUrl,
    locale: "nl-NL",
    timezoneId: "Europe/Amsterdam",
    trace: !ENV.recordsTraces ? "off" : ENV.runsInCi ? "on-first-retry" : "on",
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
      // Live scenarios need the real external systems, which only some targets have.
      grepInvert: ENV.runsLiveScenarios ? undefined : /@live-env-only/,
    },
  ],
});
