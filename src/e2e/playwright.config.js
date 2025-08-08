import { defineConfig } from "@playwright/test";
import { defineBddConfig } from "playwright-bdd";

import "dotenv/config";

const BUSINESS_LANGUAGE = "en";

const testDir = defineBddConfig({
  language: BUSINESS_LANGUAGE,
  features: "bdd/**/*.feature",
  steps: ["bdd/**/steps.ts", "bdd/**/fixture.ts", "bdd/hooks.ts"],
  aiFix: {
    promptAttachment: true,
  },
  verbose: true,
});

export default defineConfig({
  testDir,
  reporter: [
    ["html", { open: "never" }],
    ["json", { outputFile: "reports/e2e-report.json" }],
  ],
  retries: 2,
  use: {
    baseURL: process.env.ZAC_URL,
    locale: BUSINESS_LANGUAGE,
    video: "retry-with-video",
    screenshot: "only-on-failure",
  },
});
