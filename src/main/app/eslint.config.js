/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
// Configured using https://github.com/angular-eslint/angular-eslint/blob/main/docs/CONFIGURING_FLAT_CONFIG.md
const eslint = require("@eslint/js");
const { defineConfig } = require("eslint/config");
const angular = require("angular-eslint");
const testingLibrary = require("eslint-plugin-testing-library");
const tseslint = require("typescript-eslint");

// Stylelint only sees .less files, so this catches ::ng-deep in inline component styles.
const noNgDeep = {
  selector:
    ":matches(Literal[value=/::ng-deep/], TemplateElement[value.raw=/::ng-deep/])",
  message:
    "Avoid ::ng-deep, Angular deprecated it. Style through a CSS custom property, a global stylesheet or the Angular Material theming API instead.",
};

const typeScriptLanguageOptions = {
  parserOptions: {
    project: "tsconfig.json",
    tsconfigRootDir: __dirname,
    sourceType: "module",
  },
};

module.exports = defineConfig(
  {
    ignores: ["dist/**", "coverage/**"],
  },
  {
    files: ["**/*.ts"],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      angular.configs.tsRecommended,
    ],
    languageOptions: typeScriptLanguageOptions,
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
      "@angular-eslint/prefer-inject": "error",
      "@angular-eslint/prefer-signals": [
        "error",
        {
          preferInputSignals: true,
          preferQuerySignals: false,
          preferReadonlySignalProperties: false,
        },
      ],
      "@angular-eslint/prefer-output-emitter-ref": "error",
      "@typescript-eslint/no-unused-vars": [
        "error",
        {
          args: "after-used",
          argsIgnorePattern: "^_",
          varsIgnorePattern: "^_",
          caughtErrorsIgnorePattern: "^_",
        },
      ],
      "no-restricted-imports": [
        "error",
        {
          paths: [
            {
              name: "@angular/core",
              importNames: ["NO_ERRORS_SCHEMA"],
              message:
                "Import the real dependencies instead, so the compiler catches a missing declaration.",
            },
            {
              name: "@angular/core",
              importNames: ["OnChanges", "SimpleChange", "SimpleChanges"],
              message:
                "React to an input with computed() or effect() instead of ngOnChanges.",
            },
          ],
        },
      ],
      "no-restricted-properties": [
        "error",
        {
          property: "ngOnChanges",
          message:
            "React to an input with computed() or effect() instead of ngOnChanges.",
        },
      ],
      // Rejects a member named ngOnChanges, also in specs: the spec block replaces no-restricted-syntax with
      // warnings, while no block touches this rule. Together with the OnChanges import ban above.
      "@typescript-eslint/naming-convention": [
        "error",
        {
          selector: "memberLike",
          filter: { regex: "^ngOnChanges$", match: true },
          format: null,
          custom: { regex: "^ngOnChanges$", match: false },
        },
      ],
      "no-restricted-syntax": ["error", noNgDeep],
      // Jira ticket references belong in commit messages and pull request descriptions, not in code.
      // ESLint has no rule with a custom message for comment contents, so this one reports the matched term.
      "no-warning-comments": [
        "error",
        { terms: ["pz-", "drt-"], location: "anywhere" },
      ],
    },
  },
  {
    files: ["**/*.spec.ts"],
    extends: [testingLibrary.configs["flat/angular"]],
    languageOptions: typeScriptLanguageOptions,
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
        noNgDeep,
        {
          selector:
            "CallExpression[callee.object.name='By'][callee.property.name='css']",
          message:
            "Use Testing Library instead of By.css: query by role or label, e.g. screen.getByRole('button', { name: '…' }). See https://testing-library.com/docs/queries/about/#priority",
        },
        {
          selector: "MemberExpression[property.name=/^querySelector(All)?$/]",
          message:
            "Use Testing Library instead of querySelector: query by role or label, e.g. screen.getByRole('row', { name: '…' }) with within(). See https://testing-library.com/docs/queries/about/#priority",
        },
        {
          selector:
            "TSAsExpression > TSAsExpression[typeAnnotation.type='TSUnknownKeyword'] > ObjectExpression.expression",
          message:
            "Use fromPartial<T>({ … }) from src/test-helpers instead of casting an object literal through unknown: it keeps the fields you do write type-checked.",
        },
      ],
    },
  },
  {
    files: ["**/*.html"],
    extends: [
      angular.configs.templateRecommended,
      angular.configs.templateAccessibility,
    ],
    rules: {
      "@angular-eslint/template/prefer-control-flow": "error",
      "@angular-eslint/template/prefer-at-else": "error",
      "@angular-eslint/template/eqeqeq": "error",
      "@angular-eslint/template/no-autofocus": "error",
      "@angular-eslint/template/label-has-associated-control": "error",
      "@angular-eslint/template/click-events-have-key-events": "error",
      "@angular-eslint/template/interactive-supports-focus": "error",
      "@angular-eslint/template/elements-content": "error",
    },
  },
);
