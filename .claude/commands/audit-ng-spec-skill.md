# Angular Spec Auditor

Independently audit an Angular component spec file against the project's migration guidelines.

**Usage:** `/audit-ng-spec-skill $ARGUMENTS`
`$ARGUMENTS` = path to the spec file (relative from repo root), e.g. `src/main/app/src/app/taken/taken-vrijgeven-dialog/taken-vrijgeven-dialog.component.spec.ts`

If `$ARGUMENTS` is omitted, ask the user for the spec path before proceeding.

---

You did NOT write this spec. Treat it with completely fresh eyes.

**Step 1 — Read context**

1. Read `AGENTS.md`.
2. Read `.claude/commands/migrate-ng19-standalone-components.md` — pay special attention to the Rules table and Spec Conventions section.
3. Read the spec at `{SPEC_PATH}`.
4. Derive the component path from the spec path (same directory, `.component.ts` instead of `.component.spec.ts`), read it.
5. Derive the template path (`.component.html`), read it if it exists.

**Step 2 — Audit**

For each item, mark **PASS** or **FAIL** with a one-line reason and the exact line(s) involved.

**Rules**
- [ ] No `any` / `as any` / `eslint-disable no-explicit-any`
- [ ] No `: void` return type annotations anywhere in the spec
- [ ] No trivial smoke tests (`it("should create", ...)` or similar)
- [ ] No `NO_ERRORS_SCHEMA`
- [ ] **DOM query preference order**: harness → `querySelector`/`querySelectorAll` (plain HTML / custom elements) → `By.directive` — **never `By.css`**. `By.css('[input="value"]')` matches on DOM attributes that Angular never writes for `@Input` bindings → silent false positives. For custom components with `@Input` checks use `debugElement.queryAll(de => de.name === "tag-name")` + `.componentInstance.prop`.
- [ ] No `querySelectorAll` / `querySelector` for Material components (use harnesses; plain HTML elements are OK)
- [ ] **Variable naming**: no single-letter or abbreviated names (`el`, `f`, `res`, `btn`). Use full descriptive names (`element`, `fixtureRef`, `result`, `button`).
- [ ] SPDX header: new file → `2026 INFO.nl` only; existing file → `INFO.nl` added only if completely absent from the existing line

**Spec structure**
- [ ] `describe(ClassName.name, ...)` — class name reference, not a string literal
- [ ] `declarations: [Component]` used (component is `standalone: false`), OR `imports: [Component]` if already standalone
- [ ] **Services should use `TestBed.inject()` + `jest.spyOn()` — avoid `useValue: mockObject`.**
  - **Reason: type safety.** `useValue` accepts any object — typos, missing methods, wrong return types all silently pass. `jest.spyOn` is checked against the real service type, so the compiler catches mismatches.
  - Strongly preferred pattern:
    ```typescript
    providers: [provideHttpClient(), provideRouter([]), MyService],
    // ...
    myService = TestBed.inject(MyService);
    jest.spyOn(myService, "someMethod").mockReturnValue(of(result));
    ```
  - `useValue` is acceptable when genuinely difficult to avoid (e.g. a service with a deeply complex DI tree that cannot be satisfied without significant setup). Flag it with a comment explaining why.
  - **Always acceptable exceptions**: Angular built-in injection tokens (`MAT_DIALOG_DATA`, `LOCALE_ID`, `APP_BASE_HREF`, etc.) where no real class exists to inject.
  - `TranslateService` → use `TranslateModule.forRoot()` in `imports`, then `TestBed.inject(TranslateService)` + `jest.spyOn`.
  - Services using `ZacHttpClient` → add `provideHttpClient()` to providers.
  - Services using `Router` → add `provideRouter([])` to providers.
- [ ] `provideHttpClient()` present if any service in the tree uses `ZacHttpClient`
- [ ] `provideRouter([])` present if any service in the tree uses `Router`
- [ ] Factory helpers use `fromPartial<T>(obj)` — never bare `{ ... } as unknown as T` on an object literal (non-literal re-casts like `mockVar as unknown as T` are OK)

**Coverage**
- [ ] Every test asserts meaningful behaviour
- [ ] ≥90% of template behaviours covered (buttons, conditionals, outputs, form states)
- [ ] Protected/private members accessed via bracket notation: `component["member"]`

**Step 3 — Fix**

For each FAIL: edit the spec file with the correct fix. Do NOT touch any component source files.

After all fixes are applied, re-read the spec to confirm no violations remain.

**Step 4 — Report**

```
AUDIT: PASS                         (if nothing needed fixing)
AUDIT: PASS (N issues fixed)        (if fixes were applied)

Issues fixed:
- Line X: <description of fix>
- ...
```

If a violation cannot be auto-fixed (e.g. missing test coverage that requires understanding domain logic), list it as a **manual action required** item and do not mark the audit PASS until the user addresses it.
