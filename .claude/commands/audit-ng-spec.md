# Angular Spec Auditor

Independently audit an Angular component spec file against the project's migration guidelines.

**Usage:** `/audit-ng-spec $ARGUMENTS`
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
- [ ] No `querySelectorAll` / `querySelector` for Material components (use harnesses; plain HTML elements are OK)
- [ ] SPDX header: new file → `2026 INFO.nl` only; existing file → `INFO.nl` added only if completely absent from the existing line

**Spec structure**
- [ ] `describe(ClassName.name, ...)` — class name reference, not a string literal
- [ ] `declarations: [Component]` used (component is `standalone: false`), OR `imports: [Component]` if already standalone
- [ ] Services mocked via `TestBed.inject()` + `jest.spyOn()` — NOT `useValue: mockObject` (exception: `MAT_DIALOG_DATA`, `LOCALE_ID`, and other Angular built-in tokens)
- [ ] `provideHttpClient()` present if any service uses `ZacHttpClient`
- [ ] `provideRouter([])` present if any service uses `Router`
- [ ] Factory helpers use `as Partial<T> as unknown as T` — never bare `as unknown as T` on an object literal

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
