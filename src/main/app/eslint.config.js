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
const dateTimeFrozenFiles = require("./eslint.date-time-frozen.js");

// Stylelint only sees .less files, so this catches ::ng-deep in inline component styles.
const noNgDeep = {
  selector:
    ":matches(Literal[value=/::ng-deep/], TemplateElement[value.raw=/::ng-deep/])",
  message:
    "Avoid ::ng-deep, Angular deprecated it. Style through a CSS custom property, a global stylesheet or the Angular Material theming API instead.",
};

// alterMoment() makes the app send dates with the local offset, so a UTC string from Date never shows what is sent.
const noNativeDateIsoString = {
  selector:
    "CallExpression[callee.property.name='toISOString'][callee.object.type='NewExpression'][callee.object.callee.name='Date']",
  message:
    'Write the date as a literal with its offset, e.g. "2024-01-01T00:00:00.000+01:00", or use moment().toISOString(): the app sends dates with the local offset, new Date().toISOString() gives UTC.',
};

// The template rule requires `dataSource:` on every matCellDef, but only the component's imports make it typed:
// without TypedMatCellDefDirective the binding matches no directive, ngc stays silent and the row is any again.
const typedMatCellDefImport = {
  selector:
    "Program:has(ImportDeclaration[source.value='@angular/material/table'] ImportSpecifier[imported.name=/^(MatTableModule|MatCellDef)$/]):not(:has(ImportSpecifier[imported.name='TypedMatCellDefDirective']))",
  message:
    "Import TypedMatCellDefDirective next to MatTableModule or MatCellDef and add it to the component's imports. Without it the dataSource of *matCellDef binds to nothing and the row is typed any.",
};

// Date and time handling lives in src/app/date-time, see ADR-0015. The files in eslint.date-time-frozen.js broke
// these rules before they were added; that list may only shrink.
const DATE_TIME_MODULE = "src/app/date-time/**";
const dateTimeRestrictedImportPaths = [
  {
    name: "moment",
    message:
      "Handle dates in src/app/date-time and import from its index.ts, e.g. toApiDate() or fromApiDate().",
  },
  {
    name: "@angular/material-moment-adapter",
    message:
      "Use provideZacDateAdapter() or provideFilterDateFormats() from src/app/date-time.",
  },
];
const dateTimeRestrictedImportPatterns = [
  {
    group: ["**/date-time/*", "!**/date-time/index"],
    message: "Import src/app/date-time through its index.ts.",
  },
];
const dateTimeRestrictedSyntax = [
  {
    selector: "CallExpression[callee.property.name='toISOString']",
    message:
      "Send a date as toApiDate(value) from src/app/date-time: toISOString() turns a picked date into a UTC instant of another day.",
  },
  {
    selector:
      "CallExpression[callee.object.name='Date'][callee.property.name='parse']",
    message: "Read a date with fromApiDate(value) from src/app/date-time.",
  },
];

const restrictedImportPaths = [
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
];
const restrictedSyntax = [noNgDeep, typedMatCellDefImport];

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
      "@angular-eslint/prefer-on-push-component-change-detection": "error",
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
          paths: [...restrictedImportPaths, ...dateTimeRestrictedImportPaths],
          patterns: dateTimeRestrictedImportPatterns,
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
      "no-restricted-syntax": [
        "error",
        ...restrictedSyntax,
        ...dateTimeRestrictedSyntax,
      ],
      // Jira ticket references belong in commit messages and pull request descriptions, not in code.
      // ESLint has no rule with a custom message for comment contents, so this one reports the matched term.
      "no-warning-comments": [
        "error",
        { terms: ["pz-", "drt-"], location: "anywhere" },
      ],
    },
  },
  {
    files: [DATE_TIME_MODULE, ...dateTimeFrozenFiles],
    rules: {
      "no-restricted-imports": ["error", { paths: restrictedImportPaths }],
      "no-restricted-syntax": [
        "error",
        ...restrictedSyntax,
        noNativeDateIsoString,
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
        noNativeDateIsoString,
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
      "no-restricted-syntax": [
        "error",
        {
          selector:
            "Template:has(> TextAttribute[name='matCellDef']):not(:has(> BoundAttribute[name='matCellDefDataSource']))",
          message:
            'Pass the table data source to the cell, e.g. *matCellDef="let row; dataSource: rows()", and import TypedMatCellDefDirective. Without it the row is typed any and the compiler cannot check the fields the cell reads.',
        },
      ],
    },
  },
);
