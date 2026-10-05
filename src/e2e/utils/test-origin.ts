/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { Browser } from "@playwright/test";
import { execSync } from "child_process";
import os from "os";

function describeBrowser(browser: Browser | null) {
  // Playwright reports Edge as chromium; CI runs on the msedge channel.
  const browserName = process.env.CI ? "Edge" : "Chromium";
  return `${browserName} ${browser?.version() ?? ""}`.trimEnd();
}

function currentBranchName() {
  if (process.env.CI) return process.env.GITHUB_REF_NAME;
  try {
    return execSync("git rev-parse --abbrev-ref HEAD", {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "ignore"],
    }).trim();
  } catch {
    return undefined;
  }
}

export function describeTestOrigin(
  scenarioName: string,
  browser: Browser | null,
  timestampUtc: string,
) {
  const run = process.env.CI
    ? `${process.env.GITHUB_SERVER_URL}/${process.env.GITHUB_REPOSITORY}/actions/runs/${process.env.GITHUB_RUN_ID}`
    : "local machine";
  const user = process.env.CI
    ? process.env.GITHUB_ACTOR
    : os.userInfo().username;
  return [
    "CREATED BY: E2E test",
    `SCENARIO: ${scenarioName}`,
    `RUN: ${run}`,
    `USER: ${user}`,
    `BRANCH: ${currentBranchName() ?? "unknown"}`,
    `BROWSER: ${describeBrowser(browser)}`,
    `DATE TIME: ${timestampUtc}`,
  ].join("\n");
}

export function describeCaseDescription(
  testType: "e2e test" | "e2e BDD test",
  timestampUtc: string,
) {
  return `Aangemaakt door '${testType} - ${timestampUtc}'`;
}

/** Fits the 100-character limit of the reden fields. */
export function describeTestOriginShort(browser: Browser | null) {
  const environment = process.env.CI
    ? `GitHub Actions run ${process.env.GITHUB_RUN_ID}`
    : os.userInfo().username;
  return `E2E ${environment}, ${describeBrowser(browser)}`;
}
