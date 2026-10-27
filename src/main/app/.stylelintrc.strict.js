/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
// Applied to changed stylesheets only, by the "Lint Changed Stylesheets" workflow. The rule below
// is a warning in .stylelintrc.js so that a whole-project `npm run lint` stays green on the
// existing stylesheets; here it is an error, so a stylesheet you touch has to meet the standard
// before it can be merged.
const baseConfig = require("./.stylelintrc.js");

const ruleName = "selector-pseudo-element-disallowed-list";
const [disallowedPseudoElements, options] = baseConfig.rules[ruleName];

module.exports = {
  extends: ["./.stylelintrc.js"],
  rules: {
    [ruleName]: [disallowedPseudoElements, { ...options, severity: "error" }],
  },
};
