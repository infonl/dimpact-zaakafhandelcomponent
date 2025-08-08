import { defineConfig } from "@playwright/test";
import { defineBddConfig } from "playwright-bdd";

import "dotenv/config";

const BUSINESS_LANGUAGE = "en";

const testDir = defineBddConfig({
  language: BUSINESS_LANGUAGE,
  features: ["features/@auth/**/*.feature", "features/@zaak/**/*.feature"],
  steps: ["features/**/steps.ts"],
  aiFix: {
    promptAttachment: true,
  },
});

export default defineConfig({
  testDir,
  reporter: "html",
  use: {
    baseURL: process.env.ZAC_URL,
    locale: BUSINESS_LANGUAGE,
  },
});
