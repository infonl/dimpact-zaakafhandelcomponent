/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import js from "@eslint/js";
import playwright from "eslint-plugin-playwright";
import tseslint from "typescript-eslint";

// Only the playwright-bdd suite is linted. The cucumber-js suite is removed when its last feature is replaced.
const journeyFiles = [
  "bdd/**/*.ts",
  "config/**/*.ts",
  "setup/**/*.ts",
  "playwright.config.ts",
];

export default tseslint.config(
  {
    ignores: [".features-gen/", "playwright-report/", "test-results/", "reports/"],
  },
  {
    files: journeyFiles,
    extends: [
      js.configs.recommended,
      tseslint.configs.recommendedTypeChecked,
      playwright.configs["flat/recommended"],
    ],
    languageOptions: {
      parserOptions: {
        project: "./tsconfig.playwright.json",
        tsconfigRootDir: import.meta.dirname,
      },
    },
    rules: {
      // Playwright fixtures and playwright-bdd steps use `({}, ...)` when they need no fixture.
      "no-empty-pattern": "off",
      "no-restricted-properties": [
        "error",
        {
          object: "process",
          property: "env",
          message:
            "Read the configuration from ENV in config/env.ts. Only the config folder reads environment variables.",
        },
      ],
      // A step definition has its expectations outside a test() call.
      "playwright/no-standalone-expect": "off",
      "playwright/no-networkidle": "error",
      "playwright/no-wait-for-timeout": "error",
    },
  },
  {
    files: ["config/**/*.ts"],
    rules: { "no-restricted-properties": "off" },
  },
);
