## Why

[PZ-13011](https://dimpact.atlassian.net/browse/PZ-13011): Angular 20 stops receiving security updates at the end
of November 2026. The ZAC frontend has to move to Angular 21 so that production does not run an unsupported version.

## What Changes

- **Preparation, on Angular 20**: enable `typeCheckHostBindings`, the Angular 21 default, and fix the host bindings
  it rejects.
- **Angular 21**: `@angular/*`, `@angular/cli`, `@angular/material` and `@angular/cdk` to 21, with the `ng update`
  migrations. `@angular-eslint/*` and `ngx-bootstrap` move along to their 21 releases.
- **Jest 30**: `@angular-builders/jest` 21 requires Jest 30, so `jest`, `@types/jest`, `jest-preset-angular` and
  `jsdom` move with it. Specs that break on the new versions are fixed.
- `@formio/angular` already supports Angular 21 since its upgrade to 11 (PZ-13012).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. Users see no change: screens, dialogs, menus, selects, datepickers and BPMN task forms work as before.

## Impact

- **Frontend**: `src/main/app/package.json`, `package-lock.json`, `angular.json`, `tsconfig.json`, Jest config and
  the code and specs the migrations or new versions touch.
- **Backend, database, Solr, OPA, Helm charts**: no change.
