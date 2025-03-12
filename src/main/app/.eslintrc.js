/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
// Configured using https://github.com/angular-eslint/angular-eslint/blob/main/docs/CONFIGURING_ESLINTRC.md
module.exports = {
  root: true,
  ignorePatterns: ["dist", "coverage"],
  parserOptions: {
    ecmaVersion: 2020,
  },
  overrides: [
    {
      files: ["*.ts"],
      parserOptions: {
        project: "tsconfig.json",
        tsconfigRootDir: __dirname,
        sourceType: "module",
      },
      extends: [
        "eslint:recommended",
        "plugin:@typescript-eslint/recommended",
        "plugin:@angular-eslint/recommended",
        "plugin:prettier/recommended",
      ],
      rules: {
        /**
         * Any TypeScript source code (NOT TEMPLATE) related rules you wish to use/reconfigure over and above the
         * recommended set provided by the @angular-eslint project would go here.
         */
        "@angular-eslint/component-class-suffix": [
          "off",
          {
            suffixes: ["Component", "Page", "Dialog"],
          },
        ],
        "@typescript-eslint/no-explicit-any": "warn",
      },
    },
    {
      files: ["*.html"],
      extends: [
        "plugin:@angular-eslint/template/recommended",
        "plugin:@angular-eslint/template/accessibility",
        "plugin:prettier/recommended"
      ],
      rules: {
        /**
         * Any template/HTML related rules you wish to use/reconfigure over and above the
         * recommended set provided by the @angular-eslint project would go here.
         */
      },
    },
  ],
};
