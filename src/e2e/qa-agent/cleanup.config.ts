/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { defineConfig, devices } from "@playwright/test";
import os from "os";
import path from "path";

// A config of its own, so the cleanup never clears the screenshots and reports of the QA agent run.
export default defineConfig({
  testDir: "./cleanup",
  outputDir: path.join(os.tmpdir(), "qa-agent-cleanup"),
  timeout: 10 * 60_000,
  retries: 0,
  workers: 1,
  reporter: [["line"]],
  use: {
    baseURL: process.env.ZAC_URL,
    locale: "nl-NL",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
