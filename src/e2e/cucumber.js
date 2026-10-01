/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
// cucumber.js
let common = [
  "features/**/*.feature", // Specify our feature files
  "--require-module ts-node/register", // Load TypeScript module
  "--require support/worlds/*.ts", // Load support files
  "--require step-definitions/**/*.ts", // Load step definitions
  // The progress formatter writes no line breaks, so GitHub Actions shows nothing until the run ends.
  process.env.CI ? "--format pretty" : "--format progress-bar",
  "--format json:reports/e2e-report.json", // JSON report consumed by the HTML reporter
].join(" ");

module.exports = {
  default: common,
};
