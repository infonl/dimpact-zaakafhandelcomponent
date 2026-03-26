# Generic TDD Standalone Migration Plan

**Progress: 20 done — 134 remaining** (2026-03-24)
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
| **No `NO_ERRORS_SCHEMA`** | Never use `NO_ERRORS_SCHEMA` in specs. Use real imports so the compiler catches missing declarations. Only acceptable as a temporary last resort when the declared type is impossible to import. |
| **No `querySelectorAll` in specs** | Do not use `querySelectorAll` / `querySelector` to assert on Material components; use harnesses instead. Allowed only for plain HTML elements (`p`, `h3`, custom components) that have no harness. |

---

## Steps

### Phase A — Start branch (once per PR)

| # | Step | Gate |
|---|---|---|
| 1 | **Analyse** — pull `main`; check open PRs (`gh pr list`) for module files already touched; pick next fewest-deps component(s) from the queue; exclude ATOS, routing, already-standalone; present choice with rationale | **Ask user to confirm first target** |
| 2 | **Branch** — `git checkout -b temp/standalone-migration` fresh from `main` | — |
| 3 | **Claim** — `git checkout -b claims-update origin/chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me`; add batch under `## Marcel` in `migration-claims.md`; commit + push to `origin/chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me`; `git checkout temp/standalone-migration` | — |

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
| 18 | → if **continue**: go to step 4 with next component | — |
| 19 | → if **stop**: proceed to Phase C | — |

### Phase C — Ship (once per PR)

| # | Step | Gate |
|---|---|---|
| 20 | **Commit** — update plan first: bump progress counter, set `## Next Target`, add any new patterns/gotchas to `## Accumulated Patterns` (grouped by theme); include updated plan MD in same commit | **Never auto-commit** |
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

## Next Target
TBD — `/admin` lazy-loading complete. Pick next module.

---

## Accumulated Patterns

### Dialog
- `fixture.debugElement.injector.get(MatDialog)` — standalone component with `MatDialogModule` gets its own injector instance, not the root injector
- `TestBed.inject(MAT_DIALOG_DATA)` to get a typed reference to dialog data in specs
- `MatDialogRef` spy with `afterClosed: () => of(true)` to simulate confirm-dialog close
- `private readonly dialog = inject(MatDialog)` as field initializer — remove from constructor
- `setup()` helper keeps `beforeEach` light when the same `TestBed` config is needed across multiple `describe` blocks
- `Subject<void>` as the observable lets tests control next/error timing precisely

### Shell / composite
- `TestBed.overrideComponent(Shell, { remove: { imports: [RealChild] }, add: { imports: [StubChild] } })` to isolate a shell from full child service graphs
- Stub: `@Component({ selector: 'app-xyz', template: '', standalone: true, inputs: ['...'], outputs: ['...'] })` — must match every `@Input`/`@Output` of the real component

### TanStack Query
- `injectIsMutating` / `injectIsFetching` use async scheduler → `beforeEach(() => notifyManager.setScheduler((fn) => fn()))` + `afterEach` restore; import `notifyManager` from `@tanstack/query-core`
- In-flight query cancelled by `testQueryClient.clear()` → add `.catch(() => {})` on `query.fetch()`
- `WritableSignal` in mocks → `signal(value)`, never `jest.fn()`
- `provideQueryClient(testQueryClient)` in `TestBed.configureTestingModule`

### Service mocks
- Named `Pick<Service, 'method'>` mock + `TestBed.inject` for type-safe assertion access
- `useValue: { ... } satisfies Pick<Service, 'method'>` for inline mocks — never omit type annotation
- `EMPTY` when a service method just needs to complete without emitting

### Directives
- Test host must be standalone and import the directive directly
- `fakeAsync` + `tick(0)` to flush `setTimeout` in `ngOnInit`
- Directive used by a non-standalone component in the same module → still needs module `imports[]` even if not re-exported
- `By.directive(MatTooltip)` to assert directive presence — raw attribute selectors don't work after Angular processes them

### Angular Material
- `MatButtonHarness.isDisabled()` unreliable for `[disabled]` *binding* — use `nativeElement.querySelector(...).disabled` instead
- `MatButtonHarness.with({ text: /regex/ })` to find a button by translated text
- `By.directive(ChildComponent)` to query all instances including those inside table cells
- `*matNoDataRow` does not render synchronously — assert empty-state via component state, not the DOM

### Forms / inputs
- `@Input({ required: true }) field!: Type`; optional inputs use `?`
- `floatLabel="never"` is not a valid `FloatLabelType` — remove from template
- `FormControl<Date | null>` for nullable date controls; `FormControl<string | undefined>` for optional strings
- `component["formControl"].setValue(...)` + `dispatchEvent(new Event("blur"))` to trigger output without touching the DOM input
- `formControl.value ?? undefined` to avoid assigning `null` to `string | undefined`

### Router
- `provideRouter([])` instead of `RouterModule.forRoot([])` in standalone specs
- `satisfies Pick<ActivatedRoute, "data">` for typed route mocks

### Drag & drop
- `CdkDragDrop` — call the handler method directly: `component["move"]({ previousIndex, currentIndex, container: { data } } as unknown as CdkDragDrop<...>)`
- `import type { CdkDragDrop }` when used only as a type cast — avoids "declared but never read"

### Generic components
- Full type union in fixture: `ComponentFixture<StaticTextComponent<string | number | null | undefined>>`

### Spec setup
- Split `beforeEach` into `async` (TestBed config + spies) and `fakeAsync` (createComponent + `tick(0)`) when `ngAfterViewInit` emits synchronously; use `delay(0)` on all mock observables
- Mock hidden HTTP endpoints called by imported child components, or test-level HTTP errors will fire
- Baseline spec uses `declarations[]`; after migration switch to `imports: [Component]` — Material modules drop from the spec (component brings them)
- `@Output()` EventEmitters may be `public` — specs can subscribe directly

### ATOS exception
- `edit-input` is a thin wrapper around ATOS and was migrated to unblock `referentie-tabel`; the exclusion covers the `material-form-builder/` subtree itself, not wrappers around it

### Lazy-loading a module → routes (completed for `/admin`, 2026-03-24)
- Create `<module>.routes.ts` with `ROUTES: Routes` (children only, no wrapper path)
- `app-routing.module.ts`: `{ path: "admin", loadChildren: () => import("./admin/admin.routes").then(m => m.ADMIN_ROUTES) }`
- Remove `AdminModule` from `AppModule.imports`; delete `admin.module.ts` + `admin-routing.module.ts`
