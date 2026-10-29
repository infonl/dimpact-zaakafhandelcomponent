## 1. Preparation (on Angular 20)

- [ ] 1.1 Set `typeCheckHostBindings` to `true` in `tsconfig.json`
- [ ] 1.2 Fix the host bindings it rejects

## 2. Angular 21 and Jest 30

- [ ] 2.1 `ng update @angular/core@21 @angular/cli@21 @angular/material@21 --force` and review the migrations;
  `--force` because `ngx-bootstrap`, `@angular-builders/jest` and `jest-preset-angular` still require Angular 20
  until 2.2 and 2.3
- [ ] 2.2 Update `@angular-eslint/*` and `ngx-bootstrap` to 21
- [ ] 2.3 Update `@angular-builders/jest` to 21, `jest` and `@types/jest` to 30, `jest-preset-angular` to 16 and
  `jsdom` to 26 or later
- [ ] 2.4 Fix the specs that fail on the new versions

## 3. Verify

- [ ] 3.1 `npm ci` without peer dependency conflicts
- [ ] 3.2 `tsc --project .`, `npm run lint` and `npm test` pass
- [ ] 3.3 Overlays (dialogs, menus, selects, tooltips, datepickers) stack and look as before
- [ ] 3.4 Every BPMN task form works as before
- [ ] 3.5 The production build's initial total is close to that of `main`
