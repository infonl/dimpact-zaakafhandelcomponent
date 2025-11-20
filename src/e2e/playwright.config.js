/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { defineConfig } from "@playwright/test";
import { defineBddConfig } from "playwright-bdd";
import { ENV } from "./bdd/types";

const testDir = defineBddConfig({
  language: ENV.businessLanguage,
  features: "bdd/**/*.feature",
  steps: ["bdd/**/steps.ts", "bdd/**/fixture.ts", "bdd/**/hooks.ts"],
  aiFix: {
    promptAttachment: true,
  },
  verbose: true,
});

export default defineConfig({
  testDir,
  reporter: [["html", { open: "never" }]],
  retries: process.env.CI ? 2 : 1,
  use: {
    baseURL: ENV.baseUrl,
    locale: ENV.businessLanguage,
    trace: "on",
  },
});
