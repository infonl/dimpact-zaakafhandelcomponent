/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
module.exports = {
  customSyntax: "postcss-less",
  rules: {
    "selector-pseudo-element-disallowed-list": [
      ["ng-deep"],
      {
        message:
          "Avoid ::ng-deep, Angular deprecated it. Style through a CSS custom property, a global stylesheet or the Angular Material theming API instead.",
      },
    ],
  },
};
