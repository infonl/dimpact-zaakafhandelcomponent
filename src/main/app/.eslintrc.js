/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
// Configured using https://github.com/angular-eslint/angular-eslint/blob/main/docs/CONFIGURING_ESLINTRC.md
module.exports = {
  ignorePatterns: ["dist/**", "coverage/**"],
  parserOptions: {
    ecmaVersion: 2020,
  },
  overrides: [
    {
      files: ["**/*.ts"],
      parserOptions: {
        project: "tsconfig.json",
        tsconfigRootDir: __dirname,
        sourceType: "module",
      },
      extends: [
        "eslint:recommended",
        "plugin:@typescript-eslint/recommended",
        "plugin:@angular-eslint/recommended",
        // "plugin:prettier/recommended",
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
        "@angular-eslint/use-lifecycle-interface": "error",
        "@angular-eslint/prefer-standalone": "warn",
        "@angular-eslint/prefer-inject": "warn",
        "@typescript-eslint/no-unused-vars": [
          "error",
          {
            args: "after-used",
            argsIgnorePattern: "^_",
            varsIgnorePattern: "^_",
            caughtErrorsIgnorePattern: "^_",
          },
        ],
      },
    },
    {
      files: ["**/*.spec.ts"],
      parserOptions: {
        project: "tsconfig.json",
        tsconfigRootDir: __dirname,
        sourceType: "module",
      },
      extends: ["plugin:testing-library/angular"],
      rules: {
        "testing-library/no-debugging-utils": "error",
        "testing-library/no-test-id-queries": "error",
        "testing-library/prefer-explicit-assert": [
          "error",
          { assertion: "toBeInTheDocument" },
        ],
        "testing-library/prefer-user-event": "warn",
        "testing-library/prefer-user-event-setup": "warn",
        "testing-library/no-node-access": "warn",
        "testing-library/no-render-in-lifecycle": "warn",
        "testing-library/render-result-naming-convention": "off",
        // testing-library/no-node-access only sees specs that import @testing-library/*,
        // so it cannot reach the specs that query the DOM through Angular's TestBed
        // instead. These two selectors cover that gap.
        "no-restricted-syntax": [
          "warn",
          {
            selector:
              "CallExpression[callee.object.name='By'][callee.property.name='css']",
            message:
              "Use Testing Library instead of By.css: query by role or label, e.g. screen.getByRole('button', { name: '…' }). See https://testing-library.com/docs/queries/about/#priority",
          },
          {
            selector:
              "MemberExpression[property.name=/^querySelector(All)?$/]",
            message:
              "Use Testing Library instead of querySelector: query by role or label, e.g. screen.getByRole('row', { name: '…' }) with within(). See https://testing-library.com/docs/queries/about/#priority",
          },
        ],
      },
    },
    {
      files: ["**/*.html"],
      extends: [
        "plugin:@angular-eslint/template/recommended",
        "plugin:@angular-eslint/template/accessibility",
        "plugin:prettier/recommended",
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
