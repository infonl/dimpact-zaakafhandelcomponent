import { defineConfig } from "@playwright/test";
import { defineBddConfig } from "playwright-bdd";
import { ENV } from "./bdd/types";

const testDir = defineBddConfig({
  language: ENV.businessLanguage,
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
    baseURL: ENV.baseUrl,
    locale: ENV.businessLanguage,
    video: "retry-with-video",
    screenshot: "only-on-failure",
  },
});
