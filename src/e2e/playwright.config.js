import { defineConfig } from "@playwright/test";
import { defineBddConfig } from "playwright-bdd";

import "dotenv/config";

const BUSINESS_LANGUAGE = "en";

const testDir = defineBddConfig({
  language: BUSINESS_LANGUAGE,
  features: "bdd/**/*.feature",
  steps: ["bdd/**/steps.ts", "bdd/**/fixture.ts"],
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
