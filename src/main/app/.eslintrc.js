/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
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
        "plugin:@angular-eslint/recommended",
        "eslint:recommended",
        "plugin:@typescript-eslint/recommended",
        "plugin:prettier/recommended",
      ],
      rules: {
        "@angular-eslint/component-class-suffix": [
          "off",
          {
            suffixes: ["Component", "Page", "Dialog"],
          },
        ],
        "@typescript-eslint/no-explicit-any": "off",
      },
    },
  ],
};
