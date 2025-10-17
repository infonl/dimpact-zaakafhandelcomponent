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
  "--format progress-bar", // Load custom formatter
  "--format json:reports/e2e-report.json",
  // '--format node_modules/cucumber-pretty' // Load custom formatter
].join(" ");

module.exports = {
  default: common,
};
