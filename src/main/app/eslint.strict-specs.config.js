/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
// Applied to changed spec files only, by the "Lint Changed Specs" workflow. The rules below
// are warnings in eslint.config.js so that a whole-project `npm run lint` stays green on the
// existing specs; here they are errors, so a spec you touch has to meet the standard before
// it can be merged.
const { defineConfig } = require("eslint/config");
const baseConfig = require("./eslint.config.js");

// ESLint replaces rule options rather than merging them, so the selectors have to be
// carried over from the base config instead of repeated here.
const [, ...restrictedSyntax] = baseConfig.find(
  (config) =>
    config.files?.includes("**/*.spec.ts") &&
    config.rules?.["no-restricted-syntax"],
).rules["no-restricted-syntax"];

module.exports = defineConfig(baseConfig, {
  files: ["**/*.spec.ts"],
  rules: {
    "testing-library/no-node-access": "error",
    "testing-library/prefer-user-event": "error",
    "testing-library/prefer-user-event-setup": "error",
    "testing-library/no-render-in-lifecycle": "error",
    "no-restricted-syntax": ["error", ...restrictedSyntax],
  },
});
