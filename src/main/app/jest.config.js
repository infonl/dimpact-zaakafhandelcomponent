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
  reporters: [
    "default",
    ["github-actions", { silent: false }],
    ["jest-junit", { outputDirectory: "reports", outputName: "report.xml" }],
    "summary",
  ],
  collectCoverageFrom: ["src/**/*.{js,ts}", "!src/**/*.spec.{js,ts}"],
};
