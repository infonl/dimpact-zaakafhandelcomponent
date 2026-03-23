# Generic TDD Standalone Migration Plan

**Progress: 14 done — 140 remaining** (2026-03-23)
Re-verify: `grep -rl "standalone: false" src/app --include="*.ts" | grep -v "spec.ts" | wc -l` (from `src/main/app/`)

---

## Rules

| Rule | Detail |
|---|---|
| **Skip ATOS form builder** | Do NOT touch anything under `shared/material-form-builder/` or any component that imports from it. |
| **Skip routing** | Do not touch `*-routing.module.ts`. |
| **No `any`** | No `any`, `as any`, or `eslint-disable no-explicit-any` anywhere. Use explicit types or `unknown`. |
| **TS errors: touched files only** | Fix errors only in files you modified. Don't cascade. |
| **Methods: `protected` by default** | All methods are `protected`. Never `public` just because a spec calls it. |
| **Fields: `protected` or `private`** | `protected` if used in the template; `private` if internal only. |
| **`public` only when forced** | `@Input`, `@Output`, or inherited `public` from a parent class (e.g. `AdminComponent.utilService`). |
| **Bracket notation in specs** | Access protected members via `component["member"]()` — never promote to `public` for a spec. |
| **`@Input()` with `!`** | Must use `@Input({ required: true }) field!: Type`. Optional inputs use `?`. |
| **SPDX header** | Add `2026 INFO.nl` only if `INFO.nl` is completely absent from the header. |

---

## Steps

| # | Step | Gate |
|---|---|---|
| 1 | **Branch** — confirm on `temp/standalone-migration` (create from `main` if needed) | — |
| 2 | **Select** — pick fewest-deps non-standalone component; exclude ATOS, routing, already-standalone | **Ask user to confirm target** |
| 3 | **Read** — component `.ts`, `.html`, declaring module | — |
| 4 | **Identify imports** — list every directive/component/pipe/module the template needs | — |
| 5 | **Analyse template** — produce `# \| Behaviour \| ✅/❌` checklist; ≥90% must be covered | **No `it()` until checklist is done** |
| 6 | **Fix pre-existing TS errors** in component `.ts` only (ViewChild `!`, uninitialised fields, nullables) | — |
| 7 | **Write spec** — `TestBed` with `imports: [Component, NoopAnimationsModule, TranslateModule.forRoot()]`; harnesses over raw DOM; bracket notation for protected access | — |
| 8 | **Run tests** — baseline must be green: `ng test --test-path-pattern="<name>.spec"` | **Fix until green; never proceed on red** |
| 9 | **Ask permission to migrate** — _"Baseline green (N tests). OK to migrate?"_ | **Wait for user** |
| 10 | **Migrate** — `standalone: true`, add `imports[]`, apply access modifiers | — |
| 11 | **Clean module** — remove from `declarations[]`; keep in `exports[]` only if used externally | — |
| 12 | **Fix new TS errors** introduced by migration only | — |
| 13 | **Run tests** — must still pass | **Fix until green** |
| 14 | **Lint** — `bash scripts/lint-changed-files.sh` | **Fix before continuing** |
| 15 | **Ask permission for next component** — _"Done with X. Pick next?"_ | **Wait for user** |
| 16 | **Commit** — only on explicit user instruction; run `./gradlew spotlessAppApply` first | **Never auto-commit** |
| 17 | **Functional test** — user verifies in browser (`npm run dev`) | **Wait for user go-ahead** |
| 18 | **Rename + PR** — ask for Jira ticket; `git branch -m temp/standalone-migration chore/PZ-XXXXX--FE--Angular-v19-migration--<name(s)>`; push + open PR | — |

### Spec conventions
- Service mocking priority: **1)** real service + `jest.spyOn` **2)** `let mock: Pick<Service, 'method'>` + `useValue: mock` **3)** inline `useValue: { ... } satisfies Pick<...>`
- Never `useValue: { prop }` without a type annotation
- `WritableSignal` in mocks → `signal(value)`, not `jest.fn()`
- TanStack Query → `provideQueryClient(testQueryClient)` from `setupJest.ts`
- Describe-scope order: `fixture` → `loader` → services → mocks; inject services **before** `createComponent`

### PR body template
```
FE - Angular v19 migration to standalone components - <ComponentName>

- <ComponentName> made standalone
- spec added/updated
- migration plan updated

Solves PZ-XXXXX
```

---

## Order Strategy
1. **Presentational** — pure `@Input`/`@Output`, no child non-standalone deps
2. **Service-dependent** — one or two services, no non-standalone children
3. **Composite** — uses other components (migrate leaves first)
4. **Complex / dialogs** — `MAT_DIALOG_DATA`, complex service graphs
5. **Last** — SharedModule, CoreModule themselves

---

## Completed

### ✅ `shared/version/version.component.ts` (2026-03-16)
- `imports: [NgIf, MatChipsModule, MatTooltipModule, MatIconModule, MatCardModule, DatumPipe, TranslateModule]`
- **Pattern**: Named `Pick<Service, 'method'>` mock + `TestBed.inject` for typesafe assertions; `EMPTY` when service just needs to complete

### ✅ `shared/table-zoek-filters/toggle-filter/toggle-filter.component.ts` (2026-03-16)
- `imports: [MatButtonModule, MatIconModule, NgSwitch, NgSwitchCase]`
- **Pattern**: Button click via `querySelector("button").click()`; `@Output()` EventEmitter is `public` so tests subscribe directly

### ✅ `shared/read-more/read-more.component.ts` (2026-03-12)
- `imports: [NgIf, MatTooltipModule]`
- **Pattern**: `By.directive(MatTooltip)` to assert directive presence — raw attribute selectors don't work after Angular processes them

### ✅ `core/loading/loading.component.ts` (2026-03-11) — canonical spec reference
- `imports: [MatProgressBarModule]`
- **Gotcha**: `injectIsMutating/IsFetching` use async scheduler → wrap in `describe` with `beforeEach(() => notifyManager.setScheduler((fn) => fn()))` + `afterEach` restore; import from `@tanstack/query-core`
- **Gotcha**: In-flight query cancelled by `testQueryClient.clear()` → add `.catch(() => {})` on `query.fetch()`
- **Pattern**: `WritableSignal` mocked with real `signal()`; `UtilService` inline `useValue: { loading: signal(false) } satisfies Pick<...>`; `provideQueryClient(testQueryClient)`

### ✅ `shared/material/narrow-checkbox.directive.ts` (2026-03-17)
- `imports: []` — attribute directive, no template deps
- **Pattern**: Test host becomes standalone and imports the directive directly

### ✅ `shared/export-button/export-button.component.ts` (2026-03-17)
- `imports: [MatButtonModule, MatIconModule, TranslateModule]`
- **Pattern**: Baseline uses `declarations[]` → after migration switch to `imports: [Component]`; Material modules drop from spec (component brings them)

### ✅ `shared/navigation/back-button.directive.ts` (2026-03-17)
- `imports: []` — host listener directive
- **Pattern**: TestHost standalone + imports directive; `Pick<NavigationService, 'back'>` mock

### ✅ `shared/directives/outside-click.directive.ts` (2026-03-17)
- `imports: []`; kept in module `imports` (used by `EditInputComponent`) but NOT exported
- **Pattern**: `fakeAsync` + `tick(0)` to flush `setTimeout` in `ngOnInit`
- **Pattern**: Directive used by another non-standalone component in the same module → still needs module `imports` even without re-exporting

### ✅ `shared/static-text/static-text.component.ts` (2026-03-17)
- `imports: [NgIf, NgClass, MatIconModule, TranslateModule, ReadMoreComponent, EmptyPipe]`
- **Pattern**: Generic components need full type union in `ComponentFixture<StaticTextComponent<string | number | null | undefined>>`

### ✅ `admin/mailtemplates/mailtemplates.component.ts` (2026-03-19)
- `imports: [NgIf, NgFor, MatSidenavModule, MatTableModule, MatSortModule, MatCardModule, MatIconModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatDialogModule, RouterModule, TranslateModule, SideNavComponent, ReadMoreComponent]`
- **Pattern**: Use `fixture.debugElement.injector.get(MatDialog)` — standalone component with `MatDialogModule` gets its own injector instance, not root

### ✅ `admin/inrichtingscheck/inrichtingscheck.component.ts` (2026-03-19)
- `imports: [NgIf, MatSidenavModule, MatCardModule, MatExpansionModule, MatIconModule, MatTableModule, MatSortModule, MatFormFieldModule, MatInputModule, MatButtonModule, TranslateModule, DatumPipe, SideNavComponent, ToggleFilterComponent, VersionComponent, ReadMoreComponent]`
- **Pattern**: Split `beforeEach` into `async` (TestBed + spies) and `fakeAsync` (create + `tick(0)`) — needed when `ngAfterViewInit` emits synchronously; use `delay(0)` on all mock observables
- **Pattern**: Mock hidden endpoints called by child components (`VersionComponent.readBuildInformatie`) or HTTP errors occur
- **Pattern**: `MatButtonHarness.isDisabled()` unreliable for `[disabled]` binding in Angular Material 19 — use native `querySelector("[mat-raised-button]").disabled`

### ✅ `admin/mailtemplate/mailtemplate.component.ts` (2026-03-19)
- Already `standalone: true`; `imports: [ReactiveFormsModule, MatSidenavModule, MatCardModule, MatButtonModule, RouterModule, TranslateModule, SideNavComponent, MaterialFormBuilderModule]`
- Note: imports ATOS `MaterialFormBuilderModule` — already migrated, exclusion applies only to future migrations

### ✅ `shared/edit/edit-input/edit-input.component.ts` (2026-03-19) — ATOS exception
- `imports: [NgIf, MatIconModule, MatButtonModule, TranslateModule, EmptyPipe, OutsideClickDirective, MaterialFormBuilderModule]`
- **Exception**: thin wrapper around ATOS, not ATOS internals; migrating unblocks `referentie-tabel`

### ✅ `admin/referentie-tabel/referentie-tabel.component.ts` (2026-03-19)
- `imports: [NgIf, DragDropModule, MatSidenavModule, MatCardModule, MatTableModule, MatIconModule, MatButtonModule, TranslateModule, SideNavComponent, EditInputComponent]`
- **Pattern**: `By.directive(EditInputComponent)` to query all instances incl. inside table cells
- **Pattern**: `CdkDragDrop` tested by calling `component["moveTabelWaarde"]({ previousIndex, currentIndex, container: { data } } as unknown as CdkDragDrop<...>)` directly
- **Pattern**: `import type { CdkDragDrop }` when used only as type cast — avoids "declared but never read"
- **Pattern**: `*matNoDataRow` does not render synchronously — test empty-state via component state instead

---

### ✅ `shared/table-zoek-filters/date-range-filter/date-range-filter.component.ts` (2026-03-23)
- `imports: [NgIf, ReactiveFormsModule, MatFormFieldModule, MatDatepickerModule, MatNativeDateModule, MatIconModule]`
- **Pre-existing fix**: `@Input({ required: true })` on `range!`/`label!`; `FormControl<Date | null>` instead of `FormControl<Date>`
- **Pre-existing fix**: removed invalid `floatLabel="never"` from template (not in `FloatLabelType`)
- **Pattern**: Component brings its own `MatNativeDateModule` (date adapter) — host TestBed only needs `[DateRangeFilterComponent, NoopAnimationsModule]`

### ✅ `admin/parameters/parameters.component.ts` (2026-03-23)
- `imports: [NgIf, NgFor, RouterLink, TranslateModule, MatSidenavModule, MatCardModule, MatTableModule, MatSortModule, MatFormFieldModule, MatSelectModule, MatIconModule, MatButtonModule, SideNavComponent, ToggleFilterComponent, DateRangeFilterComponent, ReadMoreComponent, DatumPipe, EmptyPipe]`
- **Blocker**: `DateRangeFilterComponent` was `standalone: false` — migrated it first
- **Pattern**: existing pure unit tests (`new Component(...)`) stay as-is; added separate TestBed `describe(ParametersComponent.name, ...)` for render tests
- **Pattern**: `describe(ClassName.name, ...)` and `` describe(`${ClassName.name} suffix`, ...) `` for describe labels

---

## Next Target
`admin/parameters-edit-select-process-definition/parameters-edit-select-process-definition.component.ts`

---

## Intermediate Goal: Lazy-load `/admin`

**Progress: 14/18** — all components below must be `standalone: true` before `admin.module.ts` can be dissolved into `admin.routes.ts`.

| Component | Status |
|---|---|
| `shared/abstract-view/view-component.ts` | ✅ |
| `admin/admin/admin.component` | ✅ |
| `admin/parameters-edit-cmmn/smart-documents-form/smart-documents-form-item.component` | ✅ |
| `admin/parameters-edit-cmmn/smart-documents-form/smart-documents-form.component` | ✅ |
| `admin/parameters-edit-cmmn/parameters-edit-cmmn.component` | ✅ |
| `admin/groep-signaleringen/groep-signaleringen.component` | ✅ |
| `admin/mailtemplates/mailtemplates.component` | ✅ |
| `admin/mailtemplate/mailtemplate.component` | ✅ |
| `admin/process-definitions/process-definitions.component` | ✅ (open PR) |
| `admin/referentie-tabellen/referentie-tabellen.component` | ✅ |
| `admin/referentie-tabel/referentie-tabel.component` | ✅ |
| `admin/inrichtingscheck/inrichtingscheck.component` | ✅ |
| `admin/parameters/parameters.component` | ✅ |
| `admin/parameters-edit-select-process-definition/parameters-edit-select-process-definition.component` | ⬜ |
| `admin/parameters-edit-bpmn/parameters-edit-bpmn.component` | ⬜ |
| `admin/parameters-edit-wrapper/parameters-edit-wrapper.component` | ⬜ |
| **Replace `admin.module.ts` → `admin.routes.ts` + wire `loadChildren`** | ⬜ |
