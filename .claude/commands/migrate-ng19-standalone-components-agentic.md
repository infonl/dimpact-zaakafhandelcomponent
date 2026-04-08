# Agentic Standalone Migration Pipeline

You are the **orchestrator** for a fully automated Angular 19 standalone migration pipeline.

When invoked (with or without a component name as `$ARGUMENTS`), run the pipeline below in order.
If `$ARGUMENTS` names a specific component, skip Stage 1 and use that component directly.

**Working directory for all shell commands:** `src/main/app/` inside the repo root
**Migration rules source of truth:** `.claude/commands/migrate-ng19-standalone-components.md`
**Collaboration claims branch:** `chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me`

> **Every agent spawned in this pipeline MUST read these two files before doing any work:**
> 1. `AGENTS.md` — shared project-wide rules (no `any`, no `NO_ERRORS_SCHEMA`, SPDX headers, commit conventions, etc.)
> 2. `.claude/commands/migrate-ng19-standalone-components.md` — migration-specific rules and patterns
>
> Rules in `AGENTS.md` take precedence and apply to all code and specs produced.

---

## ⛔ Human Gates — NEVER skip, NEVER auto-proceed past these

| Gate | Condition | Action |
|---|---|---|
| **G-1** | Before `npm run dev` browser verify | _"Please verify in browser (`npm run dev`). All good?"_ — **stop and wait** |
| **G-2** | Before `gh pr create` | Show PR title + body as markdown — **stop and wait for approval** |

---

## Stage 1 — Target Picker

> Skip if `$ARGUMENTS` already names a component.

Launch a **general-purpose** agent:

```
Read AGENTS.md.
Read the migration plan at .claude/commands/migrate-ng19-standalone-components.md.
Run: git show origin/chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me:migration-claims.md
Run from repo root: gh pr list --limit 20 --json title,headRefName

Your task: identify the single easiest next component to migrate.

Rules:
- Not already claimed or done (check claims file)
- Not in shared/material-form-builder/
- Not a routing module
- Not already standalone: true

Candidate pool — run from repo root:
  grep -rl "standalone: false" src/main/app/src/app --include="*.ts" | grep -v "spec.ts"

Score each candidate (lower = easier):
  +1 per non-standalone import in its declaring module
  +1 per injected service
  +2 if it has non-standalone child components in its template
  +3 if it uses MAT_DIALOG_DATA

Return ONLY:
COMPONENT_PATH: <relative path from repo root>
COMPONENT_CLASS: <ClassName>
DECLARING_MODULE: <relative path from repo root>
RATIONALE: <one sentence>
```

Confirm the target with the user before proceeding.

---

## Stage 2 — Claim Agent

Launch a **general-purpose** agent (substitute `{COMPONENT_CLASS}`):

```
Repo root: /Users/marcel.evers/_PROJECTS/_INFO/DIMPACT/dimpact-zaakafhandelcomponent
Claims branch: chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me

Use git worktree — never git checkout:
1. git fetch origin chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me
2. git worktree add /tmp/zac-claims origin/chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me
3. In /tmp/zac-claims/migration-claims.md: find the ## Marcel section (or create it); add `- [ ] {COMPONENT_CLASS}` under it.
4. cd /tmp/zac-claims && git add migration-claims.md && git commit -m "chore: claim {COMPONENT_CLASS} for standalone migration"
5. git push origin HEAD:chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me
6. git worktree remove /tmp/zac-claims

Do NOT touch the main working tree. Do NOT stash anything.

Return: CLAIMED: yes
```

---

## Stage 3 — Work Branch

Run directly:

```bash
git checkout main && git pull origin main
git checkout -b temp/standalone-{COMPONENT_CLASS_KEBAB}
```

> Each parallel run MUST use a unique branch name based on the component class (kebab-case).
> Example: `TakenVrijgevenDialogComponent` → `temp/standalone-taken-vrijgeven-dialog`.

---

## Stage 4 — Spec Writer + Reviewer (combined)

Launch a **general-purpose** agent (substitute paths):

```
Read AGENTS.md.
Read the migration plan at .claude/commands/migrate-ng19-standalone-components.md.
Read:
- {COMPONENT_TS_PATH}
- {COMPONENT_HTML_PATH} (if exists)
- {DECLARING_MODULE_PATH}

TASK: Write a spec file at {COMPONENT_SPEC_PATH}, then self-review it.

--- WRITING RULES ---
1. TestBed: declarations: [{COMPONENT_CLASS}], imports: [NoopAnimationsModule, TranslateModule.forRoot()]
   Component is still standalone: false — use declarations[], NOT imports[].
   Add standalone child component imports to imports[] as needed.
2. Mock services with `TestBed.inject()` + `jest.spyOn()`. Add `provideHttpClient()` for ZacHttpClient-based services, `provideRouter([])` for Router-based services. NEVER use `{ provide: Service, useValue: mockObject }` for `providedIn: 'root'` services. For `Observable<never>` returns (DELETE/PUT 204): use `of(undefined) as never`.
3. Every test asserts meaningful behaviour — no it("should create", ...).
4. Named factory helpers:
     const makeX = (fields: Partial<T> = {}): T =>
       ({ ...defaults, ...fields }) as Partial<T> as unknown as T
   Never use bare `as unknown as T` on an object literal.
5. Protected/private members via bracket notation: component["member"].
6. SPDX: new spec files get `2026 INFO.nl` only — never copy the component's Atos/prior-year header.
7. Cover ≥90% of template behaviours. Do NOT add a checklist comment block to the spec.
8. No `any`, no `as any`, no NO_ERRORS_SCHEMA, no querySelectorAll for Material components (use harnesses).
9. No `: void` return type annotations — remove any existing ones in files you touch.
10. describe(ClassName.name, ...) — class name reference, not a string literal.

--- SELF-REVIEW CHECKLIST ---
After writing, verify every item:
[ ] No `any` / `as any` / eslint-disable no-explicit-any
[ ] No `: void` return type annotations (remove any found in touched files)
[ ] describe(ClassName.name, ...) — class reference
[ ] No trivial smoke tests
[ ] No NO_ERRORS_SCHEMA
[ ] No querySelectorAll for Material components
[ ] Factory helpers use `as Partial<T> as unknown as T` (never bare `as unknown as T`)
[ ] Protected/private members via bracket notation
[ ] SPDX header correct (new file: `2026 INFO.nl` only)
[ ] ≥90% template behaviours covered (no comment block)
[ ] declarations[] used (not imports[])
[ ] Services mocked via TestBed.inject() + jest.spyOn() — NOT useValue: mockObject (unless MAT_DIALOG_DATA or similar token)
[ ] provideHttpClient() added if any service uses ZacHttpClient
[ ] provideRouter([]) added if any service uses Router

Fix any violations before returning.

Return: SPEC_WRITTEN: yes | SPEC_PATH: {path} | SELF_REVIEW: PASS
```

---

## Stage 5 — Baseline Tests

Run directly from repo root:

```bash
cd src/main/app && npx ng test --test-path-pattern="{COMPONENT_SPEC_STEM}.spec" --watch=false 2>&1
```

**If red:** launch a **general-purpose** agent with the error output to fix the spec only (no component source changes). Re-run. Max 3 iterations — if still red, stop and report to user.

Proceed automatically once green.

---

## Stage 6 — Migration + Review (combined)

Launch a **general-purpose** agent (substitute paths):

```
Read AGENTS.md.
Read the migration plan at .claude/commands/migrate-ng19-standalone-components.md.
Read:
- {COMPONENT_TS_PATH}
- {COMPONENT_HTML_PATH}
- {DECLARING_MODULE_PATH}
- {COMPONENT_SPEC_PATH}

TASK: Migrate {COMPONENT_CLASS} to standalone, then self-review.

--- MIGRATION STEPS ---
1. {COMPONENT_TS_PATH}:
   - standalone: true
   - imports[] covering all template directives/components/pipes
   - Access modifiers: protected (template-visible), private (internal), public only for @Input/@Output
   - @Input({ required: true }) field!: Type for required; @Input() field?: Type for optional
   - Fix TS errors only in files you touch
   - SPDX: add 2026 INFO.nl only if INFO.nl completely absent from the existing header

2. {DECLARING_MODULE_PATH}:
   - Move from declarations[] to imports[]
   - Keep in exports[] if it was already there

3. {COMPONENT_SPEC_PATH}:
   - Change declarations: [{COMPONENT_CLASS}] to imports: [{COMPONENT_CLASS}]
   - Remove modules now provided by the standalone component

4. Run from repo root:
   cd src/main/app && npx ng test --test-path-pattern="{COMPONENT_SPEC_STEM}.spec" --watch=false
   Fix and re-run if red. Max 3 iterations.

--- SELF-REVIEW CHECKLIST ---
[ ] standalone: true present
[ ] imports[] covers all template dependencies
[ ] No `any` / `as any` in any touched file
[ ] No `: void` return type annotations in any touched file
[ ] Access modifiers correct
[ ] @Input({ required: true }) for required inputs
[ ] SPDX correct
[ ] Module cleaned (declarations → imports)
[ ] Spec uses imports[] not declarations[]
[ ] No NO_ERRORS_SCHEMA in spec
[ ] No querySelectorAll for Material in spec
[ ] Tests green

Fix any violations before returning.

Return:
MIGRATION_DONE: yes
IMPORTS_ADDED: <list>
ACCESS_FIXES: <list>
MODULE_CLEANED: yes/no
TESTS_GREEN: yes (N tests)
SELF_REVIEW: PASS
```

---

## Stage 7 — Lint

Run directly from repo root:

```bash
cd src/main/app && npm run lint 2>&1 | grep -E "ERROR|error" | grep -v "node_modules"
```

Fix errors only in touched files. Ignore pre-existing errors in untouched files.

---

## Stage 8 — Tick Claim

Launch a **general-purpose** agent:

```
Repo root: /Users/marcel.evers/_PROJECTS/_INFO/DIMPACT/dimpact-zaakafhandelcomponent
Claims branch: chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me

Use git worktree — never git checkout:
1. git fetch origin chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me
2. git worktree add /tmp/zac-claims origin/chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me
3. In /tmp/zac-claims/migration-claims.md: change `- [ ] {COMPONENT_CLASS}` to `- [x] {COMPONENT_CLASS}`
4. cd /tmp/zac-claims && git add migration-claims.md && git commit -m "chore: mark {COMPONENT_CLASS} done in migration claims"
5. git push origin HEAD:chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me
6. git worktree remove /tmp/zac-claims

Do NOT touch the main working tree. Do NOT stash anything.

Return: TICKED: yes
```

---

## Stage 9 — Commit

Stage and commit directly (do not auto-commit without showing the stat first):

```bash
git add {COMPONENT_TS_PATH} {COMPONENT_HTML_PATH} {COMPONENT_SPEC_PATH} {DECLARING_MODULE_PATH}
git diff --staged --stat
git commit -m "chore: FE - Angular v19 migration to standalone - {COMPONENT_CLASS}

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Stage 10 — Stats Updater

Run directly:

```bash
cd src/main/app && grep -rl "standalone: false" src/app --include="*.ts" | grep -v "spec.ts" | grep -v "material-form-builder" | wc -l
```

Update `.claude/commands/migrate-ng19-standalone-components.md`:
- Replace the `**Progress: N remaining**` line with the new count and today's date.
- Update `## Next Target` to `TBD — run step 0 (claims check) at start of next session.`

```bash
git add .claude/commands/migrate-ng19-standalone-components.md
git commit -m "chore: update migration plan after {COMPONENT_CLASS}

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## ⛔ Gate G-1 — Browser Verify

_"Please verify in browser (`npm run dev`). All good?"_ — **stop and wait.**

---

## Stage 11 — PR

⛔ **Gate G-2** — show the following and wait for approval:

```
**Title:** chore: FE - Angular v19 migration to standalone - {COMPONENT_CLASS}

**Body:**
FE - Angular v19 migration to standalone components - {COMPONENT_CLASS}

- {COMPONENT_CLASS} made standalone
- spec added
- migration plan updated

Solves PZ-XXXXX
```

Ask for Jira ticket if unknown. After approval, launch a **general-purpose** agent:

```
Repo root: /Users/marcel.evers/_PROJECTS/_INFO/DIMPACT/dimpact-zaakafhandelcomponent

1. git branch -m temp/standalone-{COMPONENT_CLASS_KEBAB} chore/{JIRA}--FE--Angular-v19-migration--{COMPONENT_CLASS_KEBAB}
2. git push -u origin chore/{JIRA}--FE--Angular-v19-migration--{COMPONENT_CLASS_KEBAB}
3. gh pr create --title "{APPROVED_TITLE}" --body "$(cat <<'EOF'
{APPROVED_BODY}
EOF
)"

Return: PR_URL: <url>
```

---

## Pipeline Summary

```
Stage  1   Target Picker        → component chosen (skip if named)
Stage  2   Claim Agent          → claimed via git worktree (no stash)
Stage  3   Work Branch          → temp/standalone-{component-kebab} from main
Stage  4   Spec Writer+Reviewer → spec written and self-reviewed
Stage  5   Baseline Tests       → green ✅ (auto-proceed)
Stage  6   Migration+Reviewer   → standalone: true, tests green, self-reviewed
Stage  7   Lint                 → 0 errors in touched files
Stage  8   Tick Claim           → [x] via git worktree (no stash)
Stage  9   Commit               → files committed
Stage 10   Stats Updater        → plan MD progress counter updated
         ⛔ G-1: browser verify
Stage 11   PR                   → branch renamed, PR created
         ⛔ G-2: PR title/body approval
```

**Human gates: 2** (browser verify · PR approval).
Everything else runs autonomously.

---

## Parallelism

To migrate multiple components simultaneously, launch this skill in separate Claude Code sessions, each with a different target:

```
Session 1: /migrate-ng19-standalone-components-agentic ComponentA
Session 2: /migrate-ng19-standalone-components-agentic ComponentB
Session 3: /migrate-ng19-standalone-components-agentic ComponentC
```

Each session gets its own branch (`temp/standalone-component-a`, `temp/standalone-component-b`, etc.) and its own claim worktree at `/tmp/zac-claims`. Since worktrees are ephemeral (added and removed per stage), sessions do not interfere.

**Conflict rules:**
- Components that share a **module file** must NOT run in parallel — they would produce merge conflicts in the module.
- Claims are serialised automatically by git push ordering — no coordination needed.
- Each PR is independent and targets `main`.
