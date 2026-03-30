# Generic TDD Standalone Migration Plan

**Progress: 34 done — 118 remaining** (2026-03-26)
Re-verify: `grep -rl "standalone: false" src/app --include="*.ts" | grep -v "spec.ts" | wc -l` (from `src/main/app/`)

---

## ⛔ Hard Gates — NEVER skip, NEVER auto-proceed

| Gate | When | Action |
|---|---|---|
| **B-9 → B-10** | Baseline spec is green | Say _"Baseline green (N tests). OK to migrate?"_ — **stop and wait** |
| **B-17** | After lint passes | Say _"Add another component to this branch, or PR now?"_ — **stop and wait** |
| **C-21** | After commit | Say _"Please verify in browser. All good?"_ — **stop and wait** |
| **C-22** | After browser OK | Show PR title + body as markdown — **stop and wait** |

These gates exist because the user explicitly asked for them and has corrected skipping them multiple times. Problem-solving mode is not an excuse to skip them. If a step fails (e.g. baseline red), fix it — do not jump past the gate.

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
| **No `NO_ERRORS_SCHEMA`** | Never use `NO_ERRORS_SCHEMA` in specs. Use real imports so the compiler catches missing declarations. Only acceptable as a temporary last resort when the declared type is impossible to import. |
| **No `querySelectorAll` in specs** | Do not use `querySelectorAll` / `querySelector` to assert on Material components; use harnesses instead. Allowed only for plain HTML elements (`p`, `h3`, custom components) that have no harness. |

---

## Steps

### Phase A — Start branch (once per PR)

| # | Step | Gate |
|---|---|---|
| 0 | **Read claims** ⚠️ ALWAYS EXECUTE — never skip, never rely on memory — run `git show origin/chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me:migration-claims.md` and read the output; note every component already claimed or done by any teammate; do NOT propose any of these as a target | — |
| 1 | **Analyse** — pull `main`; check open PRs (`gh pr list`) for module files already touched; pick next fewest-deps component(s) from the queue; exclude ATOS, routing, already-standalone, and anything claimed in step 0; present choice with rationale | **Ask user to confirm first target** |
| 2 | **Branch** — `git checkout -b temp/standalone-migration` fresh from `main` | — |
| 3 | **Claim** — `git checkout -b claims-update origin/chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me`; ask user to name the batch (`## {name}`) where this migration falls under in `migration-claims.md`; commit + push to `origin/chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me`; `git checkout temp/standalone-migration` | — |

### Phase B — Per-component loop (repeat until PR)

| # | Step | Gate |
|---|---|---|
| 4 | **Read** — component `.ts`, `.html`, declaring module | — |
| 5 | **Identify imports** — list every directive/component/pipe/module the template needs | — |
| 6 | **Analyse template** — produce `# \| Behaviour \| ✅/❌` checklist; ≥90% must be covered | **No `it()` until checklist is done** |
| 7 | **Fix pre-existing TS errors** in component `.ts` only (ViewChild `!`, uninitialised fields, nullables) | — |
| 8 | **Write spec** — `TestBed` with `imports: [Component, NoopAnimationsModule, TranslateModule.forRoot()]`; harnesses over raw DOM; bracket notation for protected access; `describe(ClassName.name, ...)` | — |
| 9 | **Run tests** — baseline must be green: `ng test --test-path-pattern="<name>.spec"` | **Fix until green; never proceed on red** |
| 10 | **Ask permission to migrate** — _"Baseline green (N tests). OK to migrate?"_ | **Wait for user** |
| 11 | **Migrate** — `standalone: true`, add `imports[]`, apply access modifiers | — |
| 12 | **Clean module** — remove from `declarations[]`; keep in `exports[]` only if used externally | — |
| 13 | **Fix new TS errors** introduced by migration only | — |
| 14 | **Run tests** — must still pass | **Fix until green** |
| 15 | **Lint** — `npm run lint` from `src/main/app/` | **Fix before continuing** |
| 16 | **Tick off claim** — `git checkout claims-update`, mark component `[x]` in `migration-claims.md`, commit + push to `origin/chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me`, `git checkout temp/standalone-migration` | — |
| 17 | **Stop or continue?** — assess conflict risk: list which module files this branch has already touched; flag if any open PR on `main` touches the same files; present recommendation, then ask _"Add another component to this branch, or PR now?"_ | **Wait for user decision** |
| 18 | → if **continue**: **claim first** — `git checkout claims-update`, add next component under `## Marcel` in `migration-claims.md`, commit + push, `git checkout temp/standalone-migration`; then go to step 4 | — |
| 19 | → if **stop**: proceed to Phase C | — |

### Phase C — Ship (once per PR)

| # | Step | Gate |
|---|---|---|
| 20 | **Commit** — update plan first (add `## Completed` entries, `## Next Target`, progress counter, new patterns/gotchas); include updated plan MD in same commit | **Never auto-commit** |
| 21 | **Functional test** — ask _"Please verify in browser (`npm run dev`). All good?"_ | **Wait for user go-ahead** |
| 22 | **PR draft** — propose title + body as markdown; wait for approval | **Wait for user** |
| 23 | **Rename branch** — ask for Jira ticket; `git branch -m temp/standalone-migration chore/PZ-XXXXX--FE--Angular-v19-migration--<names>` | **Wait for user approval** |
| 24 | **Push + open PR** — `git push -u origin <branch>`; `gh pr create` with approved title + body | — |
| 25 | **Sync plan to collaboration branch** — if the plan MD changed in this PR: `git checkout claims-update`, `git show <work-branch>:.claude/commands/migrate-ng19-standalone-components.md > .claude/commands/migrate-ng19-standalone-components.md`, commit + push to `origin/chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me`, `git checkout <work-branch>` | — |
| 26 | **Next batch?** — _"PR open. Start next branch?"_ → if yes, go to step 1 | **Wait for user** |

### Spec conventions
- Service mocking priority: **1)** real service + `jest.spyOn` **2)** `let mock: Pick<Service, 'method'>` + `useValue: mock` **3)** inline `useValue: { ... } satisfies Pick<...>`
- Never `useValue: { prop }` without a type annotation
- `WritableSignal` in mocks → `signal(value)`, not `jest.fn()`
- TanStack Query → `provideQueryClient(testQueryClient)` from `setupJest.ts`
- Describe-scope order: `fixture` → `loader` → services → mocks; inject services **before** `createComponent`
- `describe(ClassName.name, ...)` — always use class name reference, not string literal
- **No trivial smoke tests** — never add `it("should create", () => expect(component).toBeTruthy())`. Every test must assert meaningful behaviour.
- **`isDisabled()` exception** — `MatButtonHarness.isDisabled()` is unreliable for `[disabled]` *bindings* in Angular Material 19 — use `nativeElement.querySelector(...).disabled` only in that case.
- **Partial test fixtures** — never use bare `as unknown as T` for test object literals. Preferred: a named factory at the top of the spec — `const makeX = (fields: Partial<T> = {}): T => ({ ...defaults, ...fields }) as Partial<T> as unknown as T` — the intermediate `as Partial<T>` validates the object literal's property names; `as unknown as T` forces assignment. When a factory would be used only once, inline is acceptable: `{ ...fields } as Partial<T> as unknown as T`. For invalid-union-value tests (error branches), cast only the offending field: `makeX({ type: "UNKNOWN" as T["type"] })`.

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

### ✅ `shared/table-zoek-filters/date-range-filter/date-range-filter.component.ts` (2026-03-23) — PR #5565
- `imports: [NgIf, ReactiveFormsModule, MatFormFieldModule, MatDatepickerModule, MatNativeDateModule, MatIconModule]`
- **Fix**: `floatLabel="never"` removed from template (not a valid `FloatLabelType`); `FormControl<Date | null>` for nullable date controls
- **Fix**: `@Input({ required: true }) range!: DatumRange`, `@Input({ required: true }) label!: string`

### ✅ `admin/parameters/parameters.component.ts` (2026-03-23) — PR #5565
- `imports: [NgIf, NgFor, RouterLink, TranslateModule, MatSidenavModule, MatCardModule, MatTableModule, MatSortModule, MatFormFieldModule, MatSelectModule, MatIconModule, MatButtonModule, SideNavComponent, ToggleFilterComponent, DateRangeFilterComponent, ReadMoreComponent, DatumPipe, EmptyPipe]`
- **Pattern**: `provideRouter([])` instead of `RouterModule.forRoot([])` in standalone spec
- **Pattern**: Three describe blocks: unit tests for `applyFilter`, unit tests for compare functions, TestBed render tests

### ✅ `shared/notification-dialog/notification-dialog.component.ts` (2026-03-23) — PR #5567
- `imports: [MatDialogContent, MatDialogActions, MatButtonModule, TranslateModule]`
- **Pattern**: `TestBed.inject(MAT_DIALOG_DATA)` to get a typed reference to dialog data for use in assertions

### ✅ `shared/table-zoek-filters/tekst-filter/tekst-filter.component.ts` (2026-03-23) — PR #5567
- `imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatIconModule]`
- **Fix**: `@Input() value: string` → `@Input() value?: string`; `FormControl<string>` → `FormControl<string | undefined>`; `formControl.value` assigned with `?? undefined` to avoid `null`
- **Pattern**: `component["formControl"].setValue(...)` + `dispatchEvent(new Event("blur"))` to trigger `change()` without going through the DOM input

### ✅ `shared/confirm-dialog/confirm-dialog.component.ts` (2026-03-23) — PR #5567
- `imports: [NgIf, MatToolbarModule, MatDialogTitle, MatDialogContent, MatDialogActions, MatDividerModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, TranslateModule]`
- **Pattern**: `setup()` helper function keeps `beforeEach` light when the same `TestBed` config is needed across multiple `describe` blocks
- **Pattern**: `Subject<void>` as the observable lets tests control next/error timing precisely
- **Note**: `confirm-dialog.component.less` has a pre-existing ESLint parse error (no LESS parser configured); `autofocus` attribute on confirm button is a pre-existing `no-autofocus` lint violation — both are in untouched files

### ✅ `admin/parameters-select-process-model-method/parameters-select-process-model-method.component.ts` (2026-03-24)
- `imports: [MatStepperModule, MatIconModule, MatButtonModule, TranslateModule, MaterialFormBuilderModule]`
- **Pattern**: `MatButtonHarness.with({ text: /regex/ })` to find button by translated text; `nativeElement.disabled` for `[disabled]` binding (harness `isDisabled()` unreliable)
- **Pattern**: `satisfies Pick<ActivatedRoute, "data">` for typed route mock

### ✅ `admin/parameters-edit-bpmn/parameters-edit-bpmn.component.ts` (2026-03-24)
- `imports: [NgIf, NgFor, ReactiveFormsModule, MatStepperModule, MatIconModule, MatButtonModule, MatCardModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatSelectModule, MatSlideToggleModule, MatTableModule, TranslateModule, MaterialFormBuilderModule, StaticTextComponent]`
- **Pattern**: `private readonly dialog = inject(MatDialog)` (field initializer) — removed from constructor
- **Pattern**: `fixture.debugElement.injector.get(MatDialog)` for standalone component with `MatDialogModule`
- **Pattern**: `MatDialogRef` spy with `afterClosed: () => of(true)` to simulate confirm dialog close

### ✅ `admin/parameters-edit-shell/parameters-edit-shell.component.ts` (2026-03-24)
- `imports: [NgSwitch, NgSwitchCase, NgSwitchDefault, MatSidenavModule, MatProgressSpinnerModule, SideNavComponent, ParameterSelectProcessModelMethodComponent, ParametersEditCmmnComponent, ParametersEditBpmnComponent]`
- **Pattern**: `TestBed.overrideComponent(ShellComponent, { remove: { imports: [RealChild] }, add: { imports: [StubChild] } })` to isolate shell from full child service graphs
- **Pattern**: Stub components: `@Component({ selector: 'app-xyz', template: '', standalone: true, inputs: ['...'], outputs: ['...'] })` — must match all `@Input`/`@Output` of the real component to avoid binding errors

### ✅ `zoeken/zoek-object/zoek-object-link/zoek-object-link.component.ts` (2026-03-26)
- Zero services; pure switch logic + `@HostListener` — fastest possible spec: direct class instantiation, 11 tests, no TestBed
- **Cherry-pick pattern**: when a dependency PR (e.g. indicaties standalone) hasn't merged to `main` yet, `git cherry-pick <commit>` onto the work branch before migrating the dependent component
- **Partial test fixtures**: use named factory helpers `const makeX = (fields: Partial<T>): T => fields as unknown as T`; for inline (single use) prefer `{ ...fields } as Partial<T> as unknown as T`; for invalid-union tests cast only the field: `makeX({ type: "UNKNOWN" as T["type"] })`

### ✅ `indicaties` cluster (2026-03-26) — `IndicatiesComponent` (base) + `BesluitIndicatiesComponent` + `PersoonIndicatiesComponent` + `ZaakIndicatiesComponent`
- `imports: [CommonModule, MaterialModule, TranslateModule]` (all 3 concrete subclasses share the same template and same import set)
- **Inheritance pattern**: Abstract base `@Component({ template: '', standalone: true })` needs no `imports[]`; each subclass declares its own `imports[]` covering the shared template's directives/pipes
- **Access modifiers**: `indicaties` and `Layout` fields on the base class → `protected`; `loadIndicaties()` in subclasses → `private`; `@Input()` fields remain `public`
- **Spec pattern**: Instantiate component class directly with `new Component(mockService)` — no `TestBed.createComponent` needed when testing pure logic; use `component["indicaties"]` (bracket notation) to access the protected field
- **NgModule cleanup**: Standalone components move from `declarations[]` → `imports[]` in `shared.module.ts`; remain in `exports[]` so consuming modules (`zoeken`, `zaken`, `informatie-objecten`) continue to receive them via `SharedModule`
- **Downstream spec fix**: Other specs that had the component in `declarations[]` need it moved to `imports[]` (e.g., `zaak-view.component.spec.ts`)

### ✅ `fout-afhandeling/dialog/fout-dialog.component.ts` + `actie-onmogelijk-dialog.component.ts` (2026-03-26) — Dax Batch 2
- `imports: [MatToolbarModule, MatDialogTitle, MatDialogContent, MatDialogActions, MatDividerModule, MatButtonModule, MatIconModule, TranslateModule]` (both components, identical import set)
- **Access modifiers**: `dialogRef` → `private`; `close()` → `protected`
- **Module cleanup**: removed from `declarations[]`, added to `imports[]` in `FoutAfhandelingModule`
- **Pattern**: same as `ConfirmDialogComponent` / `NotificationDialogComponent` — `Pick<MatDialogRef<T>, 'close'>` mock; `MAT_DIALOG_DATA` via `useValue`; button clicks via Angular Material harnesses (e.g. `MatButtonHarness`), not `nativeElement.querySelector`, per Rules above

### ✅ `zoeken/zoek-object/zoek-object/zoek-object-component.ts` + `zaak-zoek-object` + `taak-zoek-object` + `document-zoek-object` (2026-03-26) — Marcel Batch 6
- Abstract base: `standalone: true`, no `imports[]` — same inheritance pattern as `indicaties` cluster
- Concrete subclasses: `imports: [ZoekObjectLinkComponent, StaticTextComponent, DatumPipe, TranslateModule]` (identical set)
- **Fix**: `@Input() taak: TaakZoekObject` / `@Input() document: DocumentZoekObject` → `@Input({ required: true }) taak!` / `document!` (pre-existing TS errors)
- **Spec pattern**: baseline uses `declarations[]` + explicit child imports; after migration switch to `imports: [Component]`; child imports drop (component brings them)
- **Spec pattern**: `ZoekObjectLinkComponent` uses `RouterLink` → `provideRouter([])` needed in test providers
- **Spec pattern**: `ZoekObjectLinkComponent` calls `zoekObject.type.toLowerCase()` → fixture must include `type` field
- **Spec pattern**: multiple `toContain` label assertions → `toEqual(expect.arrayContaining([...]))` for conciseness

### ✅ `shared/table-zoek-filters/facet-filter/facet-filter.component.ts` (2026-03-26)
- `imports: [NgFor, ReactiveFormsModule, MatFormFieldModule, MatSelectModule, TranslateModule]`
- Access modifiers: `selected`, `getFilters()`, `isVertaalbaar()`, `change()` → `protected`; `VERTAALBARE_FACETTEN` → `protected Record<string, string>`
- **Pattern**: `MatSelectHarness` + `(await select.host()).getAttribute("id")` to assert on Material component attributes without querySelector

### ✅ `shared/material/narrow-checkbox.directive.ts` (fix) + `zoeken/zoek/filters/multi-facet-filter/multi-facet-filter.component.ts` (2026-03-26) — Marcel Batch 7
- `ZacNarrowMatCheckboxDirective`: prior PR removed `standalone: false` but never added `standalone: true`; also already moved to `imports[]`/`exports[]` in `shared.module.ts` — only fix needed was adding explicit `standalone: true`
- `MultiFacetFilterComponent` `imports: [NgIf, NgFor, LowerCasePipe, ReactiveFormsModule, MatCardModule, MatCheckboxModule, MatIconModule, TranslateModule, ZacNarrowMatCheckboxDirective, ReadMoreComponent]`
- **Access modifiers**: `VERTAALBARE_FACETTEN` `public` → `protected`; `formGroup`/`inverse` → `protected`; `selected` → `private`; `checkboxChange`/`isVertaalbaar`/`invert` → `protected`
- **Spec pattern**: `setup()` helper outside `describe` + `beforeEach` only configures TestBed; avoids re-calling `ngOnInit` across tests
- **Spec pattern**: `makeFilter` default param `= {}` avoids redundant `makeFilter({ values: [] })` at call sites
- **Spec pattern**: `toEqual(["ZAAK"])` replaces `toHaveLength(1)` + `toContain` + `not.toContain` triple

---

## Next Target
`zoeken.module.ts` remaining `declarations`: `ZaakBetrokkeneFilterComponent` (116 lines) → `KlantZoekDialog` → `ZoekComponent` (294 lines, most complex). `KlantZoekDialog` blocked on `KlantZoekComponent` being non-standalone.

---

## Intermediate Goal: Lazy-load `/admin` ✅ DONE (2026-03-24)

**Progress: 18/18**

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
| `admin/process-definitions/process-definitions.component` | ✅ |
| `admin/referentie-tabellen/referentie-tabellen.component` | ✅ |
| `admin/referentie-tabel/referentie-tabel.component` | ✅ |
| `admin/inrichtingscheck/inrichtingscheck.component` | ✅ |
| `admin/parameters/parameters.component` | ✅ |
| `admin/parameters-select-process-model-method/parameters-select-process-model-method.component` | ✅ |
| `admin/parameters-edit-bpmn/parameters-edit-bpmn.component` | ✅ |
| `admin/parameters-edit-shell/parameters-edit-shell.component` | ✅ |
| **Replace `admin.module.ts` → `admin.routes.ts` + wire `loadChildren`** | ✅ |

### ✅ Lazy-load `/admin` (2026-03-24)
- Created `admin/admin.routes.ts` with `ADMIN_ROUTES: Routes` (children only, no `"admin"` wrapper)
- Added `{ path: "admin", loadChildren: () => import("./admin/admin.routes").then(m => m.ADMIN_ROUTES) }` to `app-routing.module.ts`
- Removed `AdminModule` from `AppModule.imports`
- Deleted `admin.module.ts` and `admin-routing.module.ts`
- Build clean; lint 0 errors

---

## Pre-existing Bugs — Fix When You Touch the File

Do **not** go hunting for these in files you are not already migrating. Only fix them when the component is part of the current migration batch.

| Pattern | Why it is wrong | Fix |
|---|---|---|
| `ngOnChanges` re-assigns `@Input` from `changes.x?.currentValue` | Angular sets `@Input` fields before calling `ngOnChanges`. Re-assigning is redundant when the input _did_ change, and **destructive** (sets to `undefined`) when it _did not_. Only the side-effect call (e.g. `loadIndicaties()`) belongs in `ngOnChanges`. | Remove the re-assignment lines; keep only the side-effect call. Drop `SimpleChanges` from the parameter if it is no longer used. |
