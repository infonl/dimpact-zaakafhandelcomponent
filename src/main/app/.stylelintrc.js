/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
module.exports = {
  customSyntax: "postcss-less",
  rules: {
    // A warning, not an error: existing stylesheets still use ::ng-deep. .stylelintrc.strict.js
    // makes it an error on the stylesheets a pull request changes.
    "selector-pseudo-element-disallowed-list": [
      ["ng-deep"],
      {
        severity: "warning",
        message:
          "Avoid ::ng-deep, Angular deprecated it. Style through a CSS custom property, a global stylesheet or the Angular Material theming API instead.",
      },
    ],
  },
};
