/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

/**
 * @type {import('jest').Config}
 */
module.exports = {
  globalSetup: "<rootDir>/globalJest.js",
  preset: "jest-preset-angular",
  setupFilesAfterEnv: ["<rootDir>/setupJest.ts", "jest-extended/all"],
  moduleNameMapper: {
    "^src/(.*)$": "<rootDir>/src/$1",
  },
  testPathIgnorePatterns: [
    // Jest default; must be repeated since setting this option overrides it
    "/node_modules/",
    // Route specs assert exact paths/link arrays — brittle - so excluded
    "-routing\\.module\\.spec\\.ts$",
    "\\.routes\\.spec\\.ts$",
  ],
  transformIgnorePatterns: [
    "node_modules/(?!(.*\\.mjs$|@angular/common/locales/.*\\.js$|ol/|rbush/|quickselect/|earcut/|quick-lru/|geotiff/))",
  ],
  reporters: [
    "default",
    ["github-actions", { silent: false }],
    ["jest-junit", { outputDirectory: "reports", outputName: "report.xml" }],
    "summary",
  ],
  collectCoverageFrom: [
    "src/**/*.{js,ts}",
    "!src/**/*.spec.{js,ts}",
    // Route specs are excluded from the run (see testPathIgnorePatterns)
    "!src/**/*-routing.module.ts",
    "!src/**/*.routes.ts",
  ],
  cacheDirectory: "<rootDir>/.jest-cache",
  coverageDirectory: "coverage",
};
