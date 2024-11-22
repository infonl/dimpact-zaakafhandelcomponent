/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { Config } from "jest";

const config: Config = {
  globalSetup: "<rootDir>/src/globalJest.js",
  preset: "jest-preset-angular",
  setupFilesAfterEnv: ["<rootDir>/src/setupJest.ts", "jest-extended/all"],
  moduleNameMapper: {
    "^src/(.*)$": "<rootDir>/src/$1",
    // Force module uuid to resolve with the CJS entry point, because Jest does not support package.json.exports. See https://github.com/uuidjs/uuid/issues/451
    uuid: require.resolve("uuid"),
  },
  reporters: [
    ["github-actions", { silent: false }],
    ["jest-junit", { outputDirectory: "reports", outputName: "report.xml" }],
    "summary",
  ],
};

export default config;
