# Generic TDD Standalone Migration Plan

## Context
149 components remain `standalone: false` after the Angular CLI schematic run.
We migrate one component at a time using TDD, in order of complexity (fewest deps first).

**Progress: 1/149 done**

---

## Rules (always apply)

| Rule | Detail |
|---|---|
| **Skip ATOS form builder — explicit** | Do NOT migrate any file in `src/main/app/src/app/shared/material-form-builder/` (entire subtree). Do NOT migrate any component whose `.ts` file imports from that subtree or extends a class that does. The excluded files are listed below. |
| **Skip routing** | Do not touch any `*-routing.module.ts` or router config files. |
| **No `any` — everywhere** | Zero use of `any` in all files: component `.ts`, spec `.ts`, and any helper touched. Use explicit types, generics, or `unknown`. |
| **Fix TS errors only in touched files** | Only fix TypeScript errors in files you actually modified in this session. Do not fix pre-existing errors in untouched files. |
| **Access modifiers in component `.ts`** | All class members (methods, fields, getters, computed signals) must have an explicit access modifier: use `protected` as the default for anything used in the template; use `private` for anything only used internally within the class; use `public` only when required by an interface or called from outside the class. |
| **No access modifiers in spec files** | Spec files are plain functions — no class members to annotate. But still: no `any`. |
| **SPDX header** | Every modified or new file needs the header with `SPDX-FileCopyrightText: 2026 INFO.nl` + `SPDX-License-Identifier: EUPL-1.2+`. For **existing files**: only add `2026 INFO.nl` if `INFO.nl` is completely absent; if it already appears (any year), leave unchanged. |

### ATOS Form Builder — Excluded Files (do not touch)

All files under:
```
src/main/app/src/app/shared/material-form-builder/
```
Specifically the builder classes extending `AbstractFormFieldBuilder`:
- `model/abstract-form-field-builder.ts`
- `model/abstract-file-form-field-builder.ts`
- `model/abstract-choices-form-field-builder.ts`
- `form-components/textarea/textarea-form-field-builder.ts`
- `form-components/readonly/readonly-form-field-builder.ts`
- `form-components/medewerker-groep/medewerker-groep-field-builder.ts`
- `form-components/input/input-form-field-builder.ts`
- `form-components/html-editor/html-editor-form-field-builder.ts`
- `form-components/hidden/hidden-form-field-builder.ts`
- `form-components/documenten-lijst/documenten-lijst-field-builder.ts`
- `form-components/date/date-form-field-builder.ts`
- `form-components/checkbox/checkbox-form-field-builder.ts`
- `form-components/divider/divider-form-field-builder.ts`
- `form-components/message/message-form-field-builder.ts`
- `form-components/heading/heading-form-field-builder.ts`

And all components under `shared/material-form-builder/form-components/` and `shared/material-form-builder/form/` — even those that don't extend a builder — because they are part of the ATOS abstraction being phased out.

---

## Per-Component Migration Steps

### 1. SELECT
- Pick the component with the fewest template dependencies that is not yet standalone.
- Exclude: ATOS form builder chain, routing components, already-standalone components.
- Prefer: no dependency on other `standalone: false` components in its template.

### 2. READ
- Read `<component>.component.ts`
- Read `<component>.component.html`
- Read the module(s) that declare it — to know which imports it relies on via the module.
- Read `<component>.component.less` only if styles affect testable behavior.

### 3. IDENTIFY IMPORTS NEEDED
From the template, list every Angular feature used:
- Directives: `@if`, `@for` → none needed (built-in); `*ngIf`, `*ngFor` → `CommonModule` or `NgIf`/`NgFor`
- Material components → specific `Mat*Module` or standalone Mat imports
- Pipes: `translate` → `TranslateModule`; custom pipes → import them directly
- Child components used in template → they must be standalone first (or mocked in tests)

### 4. WRITE SPEC (TDD — before migration)
- Create `<component>.component.spec.ts` if it does not exist; extend it if it does.
- Follow the same testing style as `loading.component.spec.ts` (canonical reference), but note the TestBed difference:
  - **Before migration (non-standalone component)**: `TestBed.configureTestingModule({ declarations: [Component], imports: [...] })`
  - **After migration (standalone component)**: `TestBed.configureTestingModule({ imports: [Component, ...] })` — this is what `loading.component.spec.ts` uses.
  - `NoopAnimationsModule` for Material
  - `TranslateModule.forRoot()` if template uses `translate`
  - Use `MatHarness` classes for Material component interaction
  - Use `fixture.componentRef.setInput()` for inputs
- Cover at minimum:
  - All `@Input()` variations that change rendered output
  - All user interactions (`click`, form input) that emit `@Output()` or call service methods
  - All conditional rendering branches (`@if`, `*ngIf`)
  - Service calls: verify they are called with correct args (use `jest.fn()` or spy)

#### Service mocking — priority order (use the first that fits)

| Priority | When to use | Pattern |
|---|---|---|
| **1. Real service + spy** | Service has few/no deps, or is `providedIn: 'root'` and its deps are already provided | `jest.spyOn(service, 'method').mockReturnValue(...)` after `TestBed.inject()` |
| **2. Named `useValue` mock** | Service has many deps OR only a subset of its members is used | `let mock: Pick<Service, 'member'>` at describe scope; `mock = { member: ... }` in `beforeEach`; `{ provide: Service, useValue: mock }` in providers |
| **3. Inline `useValue` with `satisfies`** | Only one property/method, no need to reference the mock variable elsewhere | `useValue: { prop: value } satisfies Pick<Service, 'prop'>` inline in providers |

**Never**: plain `useValue: { prop: value }` without a type annotation — TypeScript won't catch shape mismatches.

#### Signals vs spies
- For `WritableSignal` properties: use `signal(initialValue)` in the mock — **not** `jest.fn()`. Signals are reactive; a jest mock breaks change detection.
- For methods: use `jest.spyOn()` or `jest.fn()`.

#### QueryClient in specs
- **Always** use `provideQueryClient(testQueryClient)` from `setupJest.ts` — **never** `new QueryClient()` or `{ provide: QueryClient, useValue: ... }`.
- `testQueryClient` is cleared automatically after each test via `afterEach` in `setupJest.ts`.
- Access the client in tests directly via the imported `testQueryClient` reference (no need to `TestBed.inject(QueryClient)`).

#### Spec readability — guiding principle
**Extract when repetition obscures intent. Keep structure flat when nesting adds no information.**

| Rule | Example |
|---|---|
| Extract repeated boilerplate into a helper | `const progressBarMode = async () => loader.getHarness(...).then(b => b.getMode())` — keeps `expect` lines readable |
| Promote a lone `it` out of its `describe` | A `describe` with one `it` and no `beforeEach` adds no value — flatten it |
| Scope variables to where they're used | Declare `ngZone` inside the nested `describe`, not at top level |
| Inline when a variable is referenced only once | Use `satisfies` inline instead of a named mock variable nobody else needs |

Readability beats brevity when they conflict.

#### Describe-scope variable layout (canonical order)
```typescript
describe(MyComponent.name, () => {
  let fixture: ComponentFixture<MyComponent>;
  let loader: HarnessLoader;           // if using harnesses
  let myService: MyService;            // injected services
  let myServiceMock: Pick<MyService, 'method' | 'prop'>; // useValue mocks

  beforeEach(async () => {
    myServiceMock = { ... };           // fresh mock per test

    await TestBed.configureTestingModule({ ... }).compileComponents();

    myService = TestBed.inject(MyService);  // inject BEFORE createComponent
    // ... other injects

    fixture = TestBed.createComponent(MyComponent);
    loader  = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });
```

### 5. RUN TESTS — must PASS
```bash
cd src/main/app && ng test --test-path-pattern="<component-name>.spec"
```
Tests must pass on the **current non-standalone** code. This is the baseline.

> ⚠️ Use `ng test --test-path-pattern=` (kebab-case), NOT `npm test -- --testPathPattern` (which fails). The `ng` builder is `@angular-builders/jest:run`.

### 5.5 GATE — convince yourself before continuing
**Do NOT proceed to step 6 until:**
- All tests in the spec are green (0 failures, 0 errors)
- The test output shows the expected number of passing tests
- No "Test suite failed to run" errors (these are compile/config errors, not test failures — fix them first)

If any test fails or the suite errors out, fix the spec until it is fully green. Only a fully green baseline proves the spec is valid and that the migration didn't silently break anything.

### 6. MIGRATE COMPONENT
In `<component>.component.ts`:
- Change `standalone: false` → `standalone: true`
- Add `imports: [...]` array with everything identified in step 3
- Remove constructor injection in favour of `inject()` where the existing code already uses it (don't refactor if not needed)
- Fix any TS errors introduced

### 7. CLEAN UP MODULE
In the module that declared this component (e.g. `core.module.ts`, `shared.module.ts`, feature module):
- Remove the component from `declarations: []`
- If the component was also in `exports: []`, move it there only if other modules import it — otherwise remove entirely
- If the module's `declarations` array becomes empty, flag it for future pruning (do not delete the module yet)

### 8. FIX TS ERRORS
- Check all files touched in steps 6 and 7 compile cleanly
- No `any`, no implicit types

### 9. RUN TESTS — must still PASS
```bash
ng test --test-path-pattern="<component-name>.spec"
```

### 10. VERIFY (type check + lint changed files)
```bash
# From repo root:
bash scripts/lint-changed-files.sh
```
`scripts/lint-changed-files.sh` (already exists at repo root):
- Compares against `main` by default; pass a branch name to override: `bash scripts/lint-changed-files.sh my-branch`
- Runs `npm run lint` (full ESLint) + `tsc --noEmit` filtered to changed files only
- Skips spec files in the type-check pass (they are linted)
- Ignores pre-existing TS errors in untouched files — only fails on errors **in changed files**
- Also picks up untracked new files automatically

### 11. COMMIT on temp branch — only when user asks
Work is done on `temp/standalone-migration` (branched from `main`).

> ⚠️ **Never commit without explicit user instruction.** Do not auto-commit after spotless, after tests pass, or after any other step. Wait for the user to say "commit" or "make the branch".

First, apply Spotless formatting (from repo root):
```bash
./gradlew spotlessAppApply
```
> `clean` is not needed — Spotless works on source files directly and Gradle caches the config.

Then stage and commit:
```bash
git add <touched files>
git commit -m "chore(app): Angular v19 migration to standalone components - <Component name>"
```

### 12. FUNCTIONAL TESTING — by the user
Before renaming/PR, the user verifies the migrated component works correctly in the running app:
- Start the app (`npm run dev` in `src/main/app/`)
- Exercise the component manually in the browser
- Confirm no visual regressions, no console errors
- Give Claude the go-ahead when done

### 13. RENAME branch + ask for Jira ticket
Once functional testing passes, ask the user for the Jira ticket number, then rename:
```bash
git branch -m temp/standalone-migration chore/PZ-XXXXX--FE--Angular v19-migration--<component-name(s)>
```
Branch name format: `chore/PZ-XXXXX--FE--Angular v19-migration--<kebab-case-component-name(s)>`
- Multiple components in one branch: separate names with `--` e.g. `chore/PZ-12345--FE--Angular v19-migration--export-button--static-text`
- User provides the `PZ-XXXXX` ticket number
- After rename, offer to push and open a PR
- PR title format: `chore(app): Angular v19 migration to standalone components - <Component name>`
- PR description format:
  ```
  FE - Angular v19 migration to standalone components - <ComponentName>

  - <ComponentName> made standalone
  - spec added/updated
  - migration plan updated

  Solves PZ-XXXXX
  ```

---

## Definition of Done (per component)
- [ ] `standalone: true` in component decorator
- [ ] `imports: []` array is complete and minimal
- [ ] Component removed from module `declarations`
- [ ] All TS errors resolved
- [ ] Spec exists and covers 90%+ of behavior
- [ ] Tests pass
- [ ] Lint passes

---

## Order Strategy
1. **No-dep presentational** — pure `@Input`/`@Output`, one or two Material imports, no child non-standalone components
2. **Service-dependent** — inject one or two services, still no child non-standalone components
3. **Composite** — uses other components in template (migrate leaves first)
4. **Complex / dialogs** — `MAT_DIALOG_DATA`, complex service graphs
5. **Last** — SharedModule, CoreModule themselves (after all their components are standalone)

## Completed

### ✅ `shared/version/version.component.ts` (2026-03-16)
- `imports: [NgIf, MatChipsModule, MatTooltipModule, MatIconModule, MatCardModule, DatumPipe, TranslateModule]`
- Removed from `shared.module.ts` declarations, moved to imports + kept in exports
- Spec: 3 tests (default chip layout, verbose card layout, service called on init)
- **Pattern**: Named `Pick<Service, 'method'>` mock + `TestBed.inject(Service)` after configure for typesafe assertions; `EMPTY` when service just needs to complete without emitting

### ✅ `shared/table-zoek-filters/toggle-filter/toggle-filter.component.ts` (2026-03-16)
- `imports: [MatButtonModule, MatIconModule, NgSwitch, NgSwitchCase]`
- Removed from `shared.module.ts` declarations, moved to imports + kept in exports
- Spec: 4 tests (default icon, 3 cycle transitions + emit); icon rendering tests dropped (trust `ngSwitch`)
- **Pattern**: Button click via `fixture.nativeElement.querySelector("button").click()`; `@Output()` EventEmitter is `public` so tests can subscribe to it

### ✅ `shared/read-more/read-more.component.ts` (2026-03-12)
- `imports: [NgIf, MatTooltipModule]`
- Removed from `shared.module.ts` `declarations`, moved to `imports` + kept in `exports`
- Spec: 3 tests (full text, truncated + tooltip, undefined) — uses `By.directive(MatTooltip)` to assert tooltip presence
- **Pattern**: `By.directive()` is the right way to assert directives in tests (raw attribute selectors don't work after Angular processes them)

### ✅ `core/loading/loading.component.ts` (2026-03-11)
- `imports: [MatProgressBarModule]`
- Removed from `core.module.ts` `declarations`, moved to `imports` + kept in `exports`
- Spec: `loading.component.spec.ts` — 4 tests (idle, loading via UtilService, mutating, fetching) — **use as canonical spec reference**
- **Gotcha**: `ng test --test-path-pattern=` (not `npm test -- --testPathPattern`)
- **Gotcha**: `injectIsMutating()` / `injectIsFetching()` use `notifyManager` async scheduler. Group both under `describe("when TanStack Query is active")` with shared `beforeEach(() => notifyManager.setScheduler((fn) => fn()))` and `afterEach` to restore. Import `notifyManager` from `@tanstack/query-core`.
- **Gotcha**: In-flight query cancelled by `testQueryClient.clear()` in setupJest's global `afterEach` throws `CancelledError`. Fix: `.catch(() => {})` on `query.fetch()`.
- **Gotcha**: After migration, spec uses `imports: [LoadingComponent]` (not `declarations`); `MatProgressBarModule` drops from spec as the standalone component brings it.
- **Pattern**: `WritableSignal` mocked with real `signal()`, not jest; `UtilService` provided inline via `useValue: { loading: signal(false) } satisfies Pick<UtilService, 'loading'>`; `QueryClient` via `provideQueryClient(testQueryClient)`.

**Progress: 9/149 done**

### ✅ `shared/material/narrow-checkbox.directive.ts` (2026-03-17)
- `imports: []` — no template dependencies (attribute directive only)
- Removed from `shared.module.ts` declarations, moved to imports + kept in exports
- Spec: 1 test (class added to host element); `TestHostComponent` made standalone with `imports: [ZacNarrowMatCheckboxDirective]`
- **Pattern**: Attribute directives with no template deps need no `imports` array; test host becomes standalone and imports the directive

### ✅ `shared/export-button/export-button.component.ts` (2026-03-17)
- `imports: [MatButtonModule, MatIconModule, TranslateModule]`
- Removed from `shared.module.ts` declarations, moved to imports + kept in exports
- Fixed pre-existing TS2564: added `!` to both `@Input()` properties (selector already enforces required inputs)
- Spec: 2 tests (render icon, click triggers export + download); `Pick<Service, 'method'>` mocks with `useValue`
- **Pattern**: Spec baseline uses `declarations` → after migration switch to `imports: [Component]` and drop the Material module imports (component brings them)

### ✅ `shared/navigation/back-button.directive.ts` (2026-03-17)
- `imports: []` — no template dependencies (host listener directive only)
- Removed from `shared.module.ts` declarations, moved to imports + kept in exports
- Spec rewritten: replaced `jest.autoMockOn()` + `RouterTestingModule` + untyped mock with `TestHostComponent` pattern + `Pick<NavigationService, 'back'>` mock
- **Pattern**: Same TestHost pattern as `narrow-checkbox` — host component standalone, imports directive, TestBed uses `imports: [TestHostComponent]`

### ✅ `shared/directives/outside-click.directive.ts` (2026-03-17)
- `imports: []` — no template dependencies
- Removed from `shared.module.ts` declarations, added to imports (needed by `EditInputComponent` in same module), NOT in exports (not used outside SharedModule)
- Spec: 3 tests (outside click emits, inside click silent, overlay suppresses); `fakeAsync` + `tick(0)` to flush the `setTimeout` in `ngOnInit`
- **Pattern**: When a directive is used by another non-standalone component in the same module, it still needs to be in module `imports` even though it doesn't need re-exporting

### ✅ `shared/static-text/static-text.component.ts` (2026-03-17)
- `imports: [NgIf, NgClass, MatIconModule, TranslateModule, ReadMoreComponent, EmptyPipe]`
- Removed from `shared.module.ts` declarations, added to imports + kept in exports
- Spec: 5 tests (label, empty pipe dash, value, read-more, icon click); generic typed as `StaticTextComponent<string | number | null | undefined>`
- **Pattern**: Generic components need the full type union in `ComponentFixture<...>` — `string` alone is too narrow

### ✅ `admin/mailtemplates/mailtemplates.component.ts` (2026-03-19)
- `imports: [NgIf, NgFor, MatSidenavModule, MatTableModule, MatSortModule, MatCardModule, MatIconModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatDialogModule, RouterModule, TranslateModule, SideNavComponent, ReadMoreComponent]`
- Removed from `admin.module.ts` declarations (not added to imports — router handles standalone components directly)
- Spec: 5 tests (init/load, table row, disabled state, dialog open, delete confirmed)
- **Pattern**: For standalone components importing `MatDialogModule`, use `fixture.debugElement.injector.get(MatDialog)` to spy on `open` — the component's own injector provides a separate `MatDialog` instance from `TestBed`'s root injector
- **Pattern**: Real services + `jest.spyOn` after `TestBed.inject()` (not `useValue`); `provideHttpClient()` + `provideHttpClientTesting()` for HTTP-dependent services

## Next Target
TBD — remaining `shared.module.ts` declarations: `EditInputComponent`, `DateRangeFilterComponent`, `FacetFilterComponent`, `TekstFilterComponent`, `ConfirmDialogComponent`, `DialogComponent`, `ColumnPickerComponent`, `DocumentViewerComponent`, `NotificationDialogComponent`, `BesluitIndicatiesComponent`, `PersoonIndicatiesComponent`, `ZaakIndicatiesComponent`, `ZaakdataComponent`, `SideNavComponent`

---

## Intermediate Goal: Lazy-load the `/admin` route

**Progress: 10/18 done** (2 components deleted in PR #5535, total reduced from 20 to 18)

Make the entire `/admin` section lazy-loaded by converting `admin.module.ts` into a standalone route config (`admin.routes.ts`) and wiring it up with `loadChildren` in the app routing.

**All components below must be `standalone: true` before the module can be dissolved.**

| Component | Status |
|---|---|
| `shared/abstract-view/view-component.ts` | ✅ done |
| `admin/admin/admin.component` | ✅ done |
| `admin/parameters-edit-cmmn/smart-documents-form/smart-documents-form-item.component` | ✅ done |
| `admin/parameters-edit-cmmn/smart-documents-form/smart-documents-form.component` | ✅ done |
| `admin/parameters-edit-cmmn/parameters-edit-cmmn.component` | ✅ done |
| `admin/formio-formulieren/formio-formulieren.component` | ✅ deleted (open PR) |
| `admin/formulier-definities/formulier-definities.component` | ✅ deleted (PR #5535) |
| `admin/formulier-definitie-edit/formulier-definitie-edit.component` | ✅ deleted (PR #5535) |
| `admin/groep-signaleringen/groep-signaleringen.component` | ✅ done |
| `admin/mailtemplates/mailtemplates.component` | ✅ done |
| `admin/mailtemplate/mailtemplate.component` | ⬜ pending |
| `admin/process-definitions/process-definitions.component` | ✅ done (open PR) |
| `admin/referentie-tabellen/referentie-tabellen.component` | ✅ done |
| `admin/referentie-tabel/referentie-tabel.component` | ⬜ pending |
| `admin/inrichtingscheck/inrichtingscheck.component` | ⬜ pending |
| `admin/parameters/parameters.component` | ⬜ pending |
| `admin/parameters-edit-select-process-definition/parameters-edit-select-process-definition.component` | ⬜ pending |
| `admin/parameters-edit-bpmn/parameters-edit-bpmn.component` | ⬜ pending |
| `admin/parameters-edit-wrapper/parameters-edit-wrapper.component` | ⬜ pending |
| **Replace `admin.module.ts` with `admin.routes.ts` + wire `loadChildren` in app routing** | ⬜ pending |
