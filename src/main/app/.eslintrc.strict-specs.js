/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
// Applied to changed spec files only, by scripts/lint-changed-files.sh and the
// "Lint Changed Files" workflow. The rules below are warnings in .eslintrc.js so that a
// whole-project `npm run lint` stays green on the existing specs; here they are errors, so
// a spec you touch has to meet the standard before it can be merged.
const baseConfig = require("./.eslintrc.js");

// ESLint replaces rule options rather than merging them, so the selectors have to be
// carried over from the base config instead of repeated here.
const [, ...restrictedSyntax] = baseConfig.overrides.find((override) =>
  override.files.includes("**/*.spec.ts"),
).rules["no-restricted-syntax"];

module.exports = {
  extends: ["./.eslintrc.js"],
  overrides: [
    {
      files: ["**/*.spec.ts"],
      rules: {
        "testing-library/no-node-access": "error",
        "testing-library/prefer-user-event": "error",
        "testing-library/prefer-user-event-setup": "error",
        "testing-library/no-render-in-lifecycle": "error",
        "no-restricted-syntax": ["error", ...restrictedSyntax],
      },
    },
  ],
};
