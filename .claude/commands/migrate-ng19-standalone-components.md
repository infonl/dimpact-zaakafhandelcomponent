# Generic TDD Standalone Migration Plan

**Progress: 64 remaining** (2026-04-15)
Re-verify: `grep -rl "standalone: false" src/app --include="*.ts" | grep -v "spec.ts" | grep -v "material-form-builder" | wc -l` (from `src/main/app/`)

---

## ⛔ Hard Gates — NEVER skip, NEVER auto-proceed

| Gate | When | Action |
|---|---|---|
| **B-10 → B-11** | Baseline spec is green | Say _"Baseline green (N tests). OK to migrate?"_ — **stop and wait** |
| **B-18** | After lint passes | Say _"Add another component to this branch, or PR now?"_ — **stop and wait** |
| **C-22** | After commit | Say _"Please verify in browser. All good?"_ — **stop and wait** |
| **C-23** | After browser OK | Show PR title + body as markdown — **stop and wait** |

These gates exist because the user explicitly asked for them and has corrected skipping them multiple times. Problem-solving mode is not an excuse to skip them. If a step fails (e.g. baseline red), fix it — do not jump past the gate.

---

## Pattern Index

| Pattern | Detail |
|---|---|
| Service mocking (`TestBed.inject` + `jest.spyOn`) | See Spec Conventions → Service mocking below |
| `MatDialog` in standalone | `fixture.debugElement.injector.get(MatDialog)` — standalone gets its own injector, not root |
| Shell isolation (`TestBed.overrideComponent`) | `TestBed.overrideComponent(Shell, { remove: { imports: [Real] }, add: { imports: [Stub] } })` |
| Partial test fixtures (`makeX` factory) | See Spec Conventions → bullets below |
| TanStack Query scheduler (loading spinner tests) | `notifyManager.setScheduler((fn) => fn())` in `beforeEach`; restore in `afterEach`; import from `@tanstack/query-core` |
| Abstract base class | Abstract base: `@Component({ template: '', standalone: true })`, no `imports[]`; each subclass declares its own |
| Direct class instantiation (no TestBed) | Zero-service components: `new Component(mockService)` — skip TestBed entirely |
| `CdkDragDrop` direct method call | `component["move"]({ previousIndex, currentIndex, container: { data } } as unknown as CdkDragDrop<...>)` |
| `fakeAsync` + `tick(0)` for `setTimeout` in `ngOnInit` | Wrap `createComponent` + `fixture.detectChanges` in `fakeAsync`; call `tick(0)` after |
| Async + fakeAsync split `beforeEach` (child HTTP calls) | First `beforeEach` is `async` (TestBed + spies); second is `fakeAsync` (create + `tick(0)`); use `delay(0)` on all mock observables |
| `By.directive` for directive presence | `fixture.debugElement.query(By.directive(MatTooltip))` — raw attribute selectors don't work after Angular processes them |
| Cherry-pick an unmerged dep before migrating | `git cherry-pick <sha>` onto work branch when a dep PR hasn't merged to `main` yet |

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
| **SPDX header (existing files)** | Add `2026 INFO.nl` only if `INFO.nl` is completely absent from the header. If `INFO.nl` already appears (any year), leave unchanged. |
| **SPDX header (new spec files)** | New `.spec.ts` files get `2026 INFO.nl` only — never copy the component's `Atos`/prior-year header. |
| **No `NO_ERRORS_SCHEMA`** | Never use `NO_ERRORS_SCHEMA` in specs. Use real imports so the compiler catches missing declarations. Only acceptable as a temporary last resort when the declared type is impossible to import. |
| **No `querySelectorAll` in specs** | Do not use `querySelectorAll` / `querySelector` to assert on Material components; use harnesses instead. Allowed only for plain HTML elements (`p`, `h3`, custom components) that have no harness. |
| **No `: void` return types** | Never write `: void` on methods (component `.ts` and spec `.ts`). TypeScript infers `void` — the annotation is redundant. Remove any existing `: void` annotations in files you touch. |

---

## Steps

### Phase A — Start branch (once per PR)

| # | Step | Gate |
|---|---|---|
| 0 | **Read claims** ⚠️ ALWAYS EXECUTE — never skip, never rely on memory — run `git show origin/chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me:migration-claims.md` and read the output; note every component already claimed or done by any teammate; do NOT propose any of these as a target | — |
| 1 | **Analyse** — pull `main`; check open PRs (`gh pr list`) for module files already touched; pick next fewest-deps component(s) from the queue; exclude ATOS, routing, already-standalone, and anything claimed in step 0; present choice with rationale | **Ask user to confirm first target** |
| 2 | **Branch** — `git checkout -b temp/standalone-migration` fresh from `main` | — |
| 3 | **Claim** — `git fetch origin <claims-branch> && git worktree add /tmp/zac-claims origin/<claims-branch>`; ask user to name the batch (`## {name}`); edit `/tmp/zac-claims/migration-claims.md` to add components; `cd /tmp/zac-claims && git add migration-claims.md && git commit -m "chore: claim ..." && git push origin HEAD:<claims-branch>`; `git worktree remove /tmp/zac-claims` (where `<claims-branch>` = `chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me`) | — |

### Phase B — Per-component loop (repeat until PR)

| # | Step | Gate |
|---|---|---|
| 4 | **Read** — component `.ts`, `.html`, declaring module | — |
| 5 | **Identify imports** — list every directive/component/pipe/module the template needs | — |
| 6 | **Analyse template** — produce `# \| Behaviour \| ✅/❌` checklist; ≥90% must be covered | **No `it()` until checklist is done** |
| 7 | **Fix pre-existing TS errors** in component `.ts` only (ViewChild `!`, uninitialised fields, nullables) | — |
| 8 | **Log pre-existing bugs** — while reading the component, note any logic bugs you spot (error paths not handled, null-checks that always evaluate the same way, duplicate subscriptions, etc.). Do NOT fix them. Add them to `## Known Pre-existing Bugs` at the bottom of this file so they can be addressed in a separate ticket. | — |
| 9 | **Write spec** — `TestBed` with `imports: [Component, NoopAnimationsModule, TranslateModule.forRoot()]`; harnesses over raw DOM; bracket notation for protected access; `describe(ClassName.name, ...)` | — |
| 10 | **Run tests** — baseline must be green: `ng test --test-path-pattern="<name>.spec"` | **Fix until green; never proceed on red** |
| 11 | **Ask permission to migrate** — _"Baseline green (N tests). OK to migrate?"_ | **Wait for user** |
| 12 | **Migrate** — `standalone: true`, add `imports[]`, apply access modifiers | — |
| 13 | **Clean module** — remove from `declarations[]`; keep in `exports[]` only if used externally | — |
| 14 | **Fix new TS errors** introduced by migration only | — |
| 15 | **Run tests** — must still pass | **Fix until green** |
| 16 | **Lint** — `npm run lint` from `src/main/app/` | **Fix before continuing** |
| 17 | **Tick off claim** — worktree pattern: `git fetch origin <claims-branch> && git worktree add /tmp/zac-claims origin/<claims-branch>`; mark `[x]` in `/tmp/zac-claims/migration-claims.md`; commit + push; `git worktree remove /tmp/zac-claims` | — |
| 18 | **Stop or continue?** — assess conflict risk: list which module files this branch has already touched; flag if any open PR on `main` touches the same files; present recommendation, then ask _"Add another component to this branch, or PR now?"_ | **Wait for user decision** |
| 19 | → if **continue**: **claim first** — worktree pattern: add next component under `## Marcel` in `/tmp/zac-claims/migration-claims.md`, commit + push, remove worktree; then go to step 4 | — |
| 20 | → if **stop**: proceed to Phase C | — |

### Phase C — Ship (once per PR)

| # | Step | Gate |
|---|---|---|
| 21 | **Commit** — update plan first (add `## Completed` entries, `## Next Target`, progress counter, new patterns/gotchas); include updated plan MD in same commit | **Never auto-commit** |
| 22 | **Functional test** — ask _"Please verify in browser (`npm run dev`). All good?"_ | **Wait for user go-ahead** |
| 23 | **PR draft** — propose title + body as markdown; wait for approval | **Wait for user** |
| 24 | **Rename branch** — ask for Jira ticket; `git branch -m temp/standalone-migration chore/PZ-XXXXX--FE--Angular-v19-migration--<names>` | **Wait for user approval** |
| 25 | **Push + open PR** — `git push -u origin <branch>`; `gh pr create` with approved title + body | — |
| 26 | **Sync plan to collaboration branch** — if the plan MD changed in this PR: `git fetch origin chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me && git worktree add /tmp/zac-claims origin/chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me`; copy updated plan: `git show <work-branch>:.claude/commands/migrate-ng19-standalone-components.md > /tmp/zac-claims/.claude/commands/migrate-ng19-standalone-components.md`; `cd /tmp/zac-claims && git add .claude/commands/migrate-ng19-standalone-components.md && git commit -m "chore: sync migration plan" && git push origin HEAD:chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me`; `git worktree remove /tmp/zac-claims` | — |
| 27 | **Next batch?** — _"PR open. Start next branch?"_ → if yes, go to step 1 | **Wait for user** |

---

### Spec conventions

#### Service mocking — ALWAYS use `TestBed.inject()` + `jest.spyOn`
**Never use `{ provide: Service, useValue: mockObject }`** for services that are `providedIn: 'root'` (which is nearly all services in this project).

**Correct pattern:**
```typescript
providers: [provideHttpClient(), provideRouter([])],  // add what the service tree needs
// ...
service = TestBed.inject(MyService);
jest.spyOn(service, "myMethod").mockReturnValue(of(result));
// expectations:
expect(service.myMethod).toHaveBeenCalledWith(expected);
```

**For `Observable<never>` return types** (e.g. DELETE/PUT endpoints that return 204):
```typescript
jest.spyOn(service, "deleteItem").mockReturnValue(of(undefined) as never);
// `of(undefined) as never` emits once (triggers subscribe callbacks) AND satisfies the type
```

**For property-only services** (no methods to spy on — e.g. `FoutAfhandelingService.foutmelding`):
```typescript
foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
foutAfhandelingService.foutmelding = "Test fout";  // set directly before createComponent
```

**Common providers needed:**
- `provideHttpClient()` — for any service using `ZacHttpClient`
- `provideRouter([])` — for any service using `Router` (e.g. `UtilService`, `FoutAfhandelingService`)
- `provideQueryClient(testQueryClient)` — for TanStack Query (import `testQueryClient` from `setupJest.ts`)

**`useValue` is only acceptable** for Angular built-in tokens (`MAT_DIALOG_DATA`, `LOCALE_ID`) or complex dependencies you cannot inject differently.

---

- `WritableSignal` in mocks → `signal(value)`, not `jest.fn()`
- TanStack Query → `provideQueryClient(testQueryClient)` from `setupJest.ts`
- Describe-scope order: `fixture` → `loader` → services; inject services **before** `createComponent`
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

## Next Target
TBD — run step 0 (claims check) at start of next session.

---

## Pre-existing Bugs — Fix When You Touch the File

Do **not** go hunting for these in files you are not already migrating. Only fix them when the component is part of the current migration batch.

| Pattern | Why it is wrong | Fix |
|---|---|---|
| `ngOnChanges` re-assigns `@Input` from `changes.x?.currentValue` | Angular sets `@Input` fields before calling `ngOnChanges`. Re-assigning is redundant when the input _did_ change, and **destructive** (sets to `undefined`) when it _did not_. Only the side-effect call (e.g. `loadIndicaties()`) belongs in `ngOnChanges`. | Remove the re-assignment lines; keep only the side-effect call. Drop `SimpleChanges` from the parameter if it is no longer used. |

---

## Known Pre-existing Bugs

Bugs spotted during migration but **not fixed** — standalone migration scope only. Raise separate tickets for these.

| Component | Bug | Found in batch |
|---|---|---|
| `ZoekopdrachtComponent` | `clearActief()` sets `actieveZoekopdracht = null` then immediately checks `if (emit && this.actieveZoekopdracht)` — always `false`. The `@Output() zoekopdracht` never emits when clearing; consumers are never notified. Output type should also include `null`. | batch-4 |
| `SignaleringenSettingsComponent` | `changed()` calls `setLoading(true)` but only calls `setLoading(false)` in the success callback of `put()`. On HTTP error the loading spinner stays stuck forever. Should use `finalize(() => setLoading(false))`. | batch-4 |
| `FoutAfhandelingComponent` | Template uses `serverErrorTexts \| async` twice (in `@if` and `*ngFor`), creating two separate HTTP subscriptions and triggering duplicate requests. Should use `shareReplay(1)` or an `@let` binding. | batch-4 |
