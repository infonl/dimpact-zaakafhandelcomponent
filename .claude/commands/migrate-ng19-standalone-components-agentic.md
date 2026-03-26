# Agentic Standalone Migration Pipeline

You are the **orchestrator** for a fully automated Angular 19 standalone migration pipeline.

When invoked (with or without a component name as `$ARGUMENTS`), run the pipeline below in order.
If `$ARGUMENTS` names a specific component, skip Stage 1 and use that component directly.

**Working directory for all shell commands:** `src/main/app/`
**Migration rules source of truth:** `.claude/commands/migrate-ng19-standalone-components.md`
**Collaboration claims branch:** `chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me`

---

## ⛔ Human Gates — NEVER skip, NEVER auto-proceed past these

| Gate | Condition | Action |
|---|---|---|
| **G-1** | After spec baseline is green | Say _"Baseline green (N tests). OK to migrate?"_ — **stop and wait** |
| **G-2** | After browser request | Say _"Please verify in browser (`npm run dev`). All good?"_ — **stop and wait** |
| **G-3** | Before `gh pr create` | Show PR title + body as markdown — **stop and wait for approval** |

---

## Stage 1 — Target Picker

> Skip if `$ARGUMENTS` already names a component.

Launch a **general-purpose** agent with this prompt:

```
Read the migration plan at .claude/commands/migrate-ng19-standalone-components.md.
Read the current claims file: run `git show origin/chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me:migration-claims.md`
List all open PRs: run `gh pr list --limit 20 --json title,files` from the repo root.

Your task: identify the single easiest next component to migrate.

Easiest = fewest template dependencies + no non-standalone children + not already claimed/done + not in shared/material-form-builder/.

Candidate pool: components with `standalone: false` in src/main/app/src/app/ (excluding spec.ts).
Run: grep -rl "standalone: false" src/main/app/src/app --include="*.ts" | grep -v "spec.ts"

For each candidate compute a score (lower = easier):
- +1 per import in its declaring module that is not yet standalone
- +1 per injected service
- +2 if it has child components
- +3 if it uses MAT_DIALOG_DATA

Return ONLY:
COMPONENT_PATH: <relative path from repo root>
COMPONENT_CLASS: <ClassName>
DECLARING_MODULE: <relative path from repo root>
RATIONALE: <one sentence>
```

Parse the agent output. Confirm the target to the user before proceeding.

---

## Stage 2 — Claim Agent

Launch a **general-purpose** agent with this prompt (fill in `{COMPONENT_CLASS}`):

```
Working directory: repo root.

1. Run: git checkout chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me
2. Read migration-claims.md.
3. Find the ## Marcel section (or create it if absent).
4. Add a new unchecked entry: `- [ ] {COMPONENT_CLASS}` under ## Marcel.
5. Commit: git add migration-claims.md && git commit -m "chore: claim {COMPONENT_CLASS} for standalone migration"
6. Push: git push origin chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me
7. Checkout back to the previous branch (store it before switching).

Return: CLAIMED: yes | BRANCH_RESTORED: yes
```

---

## Stage 3 — Work Branch

Run directly (no subagent needed):

```bash
git checkout main && git pull origin main
git checkout -b temp/standalone-migration
```

---

## Stage 4 — Spec Writer

Launch a **general-purpose** agent with this prompt (fill in paths):

```
Read the migration plan at .claude/commands/migrate-ng19-standalone-components.md — pay close attention to:
- Spec conventions section
- Partial test fixtures rule
- No trivial smoke tests rule
- No NO_ERRORS_SCHEMA rule
- No querySelectorAll for Material components
- describe(ClassName.name, ...) rule
- Access modifiers (bracket notation for protected members)

Read these files:
- {COMPONENT_TS_PATH}
- {COMPONENT_HTML_PATH} (if it exists)
- {DECLARING_MODULE_PATH}

Your task: write a spec file for {COMPONENT_CLASS} at {COMPONENT_SPEC_PATH}.

Requirements:
1. TestBed with `imports: [declarations: [{COMPONENT_CLASS}], imports: [NoopAnimationsModule, TranslateModule.forRoot()]]`
   NOTE: component is still standalone: false at this point — use declarations[], NOT imports[].
   Add any required child imports (standalone components used by the template) in the imports[] array.
2. Providers: provideRouter([]) if RouterLink/ActivatedRoute used; mock services with Pick<Service, 'method'> pattern.
3. Every test must assert meaningful behaviour — no `it("should create", ...)`.
4. Use named factory helpers: `const makeX = (fields: Partial<T> = {}): T => ({ ...defaults, ...fields }) as Partial<T> as unknown as T` — the intermediate `as Partial<T>` validates property names; never use bare `as unknown as T` on an object literal.
5. Use `component["member"]` bracket notation for protected/private members.
6. SPDX header: `2026 INFO.nl` (only if INFO.nl absent from existing header).
7. Cover ≥90% of template behaviours from this checklist:
   - Produce a `# | Behaviour | Covered` table in a comment block at the top of the spec.

Return: SPEC_WRITTEN: yes | SPEC_PATH: {path} | BEHAVIOURS_COVERED: N/M
```

---

## Stage 5 — Spec Quality Reviewer

Launch an **Explore** agent with this prompt:

```
Read the migration plan at .claude/commands/migrate-ng19-standalone-components.md.
Read the spec file at {COMPONENT_SPEC_PATH}.
Read the component at {COMPONENT_TS_PATH}.

Review the spec against every rule in the plan. Check specifically:
[ ] No `any` / `as any` / eslint-disable no-explicit-any
[ ] describe(ClassName.name, ...) — class name reference, not string
[ ] No trivial smoke tests (no `it("should create", ...)`)
[ ] No NO_ERRORS_SCHEMA
[ ] No querySelectorAll for Material components (harnesses only)
[ ] Partial fixtures use named factory with Partial<T> parameter
[ ] Protected/private members accessed via bracket notation
[ ] SPDX header present and correct
[ ] Every test asserts meaningful behaviour
[ ] ≥90% template behaviours covered
[ ] TestBed uses declarations[] (not imports[]) since component is not yet standalone
[ ] Service mocks typed with Pick<Service, 'method'> or satisfies Pick<...>

Return:
REVIEW: PASS or FAIL
ISSUES: bullet list of every violation found (empty if PASS)
```

**If FAIL:** send issues back to Stage 4 (re-launch Spec Writer with the issue list appended). Repeat until PASS. Max 3 iterations — if still failing after 3, stop and report to user.

---

## Stage 6 — Run Baseline Tests

Run directly:

```bash
cd src/main/app && npx ng test --test-path-pattern="{COMPONENT_SPEC_STEM}.spec" --watchAll=false
```

**If red:** launch a **general-purpose** agent:

```
The spec at {COMPONENT_SPEC_PATH} has failing tests. Error output:
{ERROR_OUTPUT}

Read the spec, the component .ts, and the component .html.
Fix ONLY the spec — do not touch the component source yet.
Rules: no `any`, no NO_ERRORS_SCHEMA, no querySelectorAll for Material.
Return: FIXED: yes | CHANGES: bullet list of what you changed
```

Re-run tests. Repeat until green. Max 3 iterations.

**→ Human Gate G-1**: _"Baseline green (N tests). OK to migrate?"_ — stop and wait.

---

## Stage 7 — Migration Agent

Launch a **general-purpose** agent with this prompt:

```
Read the migration plan at .claude/commands/migrate-ng19-standalone-components.md.
Read:
- {COMPONENT_TS_PATH}
- {COMPONENT_HTML_PATH}
- {DECLARING_MODULE_PATH}
- {COMPONENT_SPEC_PATH}

Your task: migrate {COMPONENT_CLASS} to standalone.

Steps:
1. In {COMPONENT_TS_PATH}:
   - Set standalone: true
   - Add imports[] covering every directive/component/pipe used in the template
   - Apply access modifiers: protected for template-visible, private for internal, public only for @Input/@Output or inherited public
   - Fix @Input fields: @Input({ required: true }) field!: Type (required ones), @Input() field?: Type (optional)
   - Fix any TS errors introduced (only in files you touch)
   - SPDX: add 2026 INFO.nl only if INFO.nl is completely absent

2. In {DECLARING_MODULE_PATH}:
   - Move {COMPONENT_CLASS} from declarations[] to imports[]
   - Keep in exports[] if it was there before

3. In {COMPONENT_SPEC_PATH}:
   - Change declarations: [{COMPONENT_CLASS}] to imports: [{COMPONENT_CLASS}]
   - Remove any Material/Angular modules that the component now provides itself

4. Run: cd src/main/app && npx ng test --test-path-pattern="{COMPONENT_SPEC_STEM}.spec" --watchAll=false
   If red, fix and re-run. Max 3 iterations.

Rules (hard):
- NO `any`, `as any`, eslint-disable no-explicit-any — anywhere
- Do NOT touch files outside the 3 listed above plus any TS errors in directly imported files
- Do NOT touch routing modules
- Do NOT touch shared/material-form-builder/

Return:
MIGRATION_DONE: yes
IMPORTS_ADDED: comma-separated list
ACCESS_FIXES: bullet list
MODULE_CLEANED: yes/no
TESTS_GREEN: yes (N tests)
```

---

## Stage 8 — Migration Quality Reviewer

Launch an **Explore** agent with this prompt:

```
Read the migration plan at .claude/commands/migrate-ng19-standalone-components.md.
Read:
- {COMPONENT_TS_PATH}
- {COMPONENT_SPEC_PATH}
- {DECLARING_MODULE_PATH}

Review the migration against every rule in the plan:
[ ] standalone: true present
[ ] imports[] covers all template dependencies
[ ] No `any` / `as any` anywhere in touched files
[ ] Access modifiers correct (protected template, private internal, public only forced)
[ ] @Input({ required: true }) field!: Type for required inputs
[ ] SPDX header correct (2026 INFO.nl only if INFO.nl was absent)
[ ] Module: component moved from declarations[] to imports[] (stays in exports[] if needed)
[ ] Spec: component in imports[] (not declarations[])
[ ] No NO_ERRORS_SCHEMA in spec
[ ] No querySelectorAll for Material components in spec

Return:
REVIEW: PASS or FAIL
ISSUES: bullet list of every violation (empty if PASS)
```

**If FAIL:** send issues back to Stage 7 migration agent with the issue list. Repeat until PASS. Max 3 iterations.

---

## Stage 9 — Lint

Run directly:

```bash
cd src/main/app && npm run lint
```

If errors only in touched files — fix inline. If errors in untouched files — ignore (pre-existing).

---

## Stage 10 — Tick Claim

Launch a **general-purpose** agent:

```
1. git checkout chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me
2. In migration-claims.md, find `- [ ] {COMPONENT_CLASS}` and change to `- [x] {COMPONENT_CLASS}`.
3. git add migration-claims.md && git commit -m "chore: mark {COMPONENT_CLASS} done in migration claims"
4. git push origin chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me
5. git checkout temp/standalone-migration

Return: TICKED: yes
```

---

## Stage 11 — Commit

Run directly (never auto-commit — orchestrator stages the files, then asks user to confirm):

Stage files:
```bash
git add {COMPONENT_TS_PATH} {COMPONENT_HTML_PATH} {COMPONENT_SPEC_PATH} {DECLARING_MODULE_PATH} .claude/commands/migrate-ng19-standalone-components.md
```

Show `git diff --staged --stat` to user, then commit:
```bash
git commit -m "chore: FE - Angular v19 migration to standalone - {COMPONENT_CLASS} (#ticket)"
```

---

## Stage 12 — Human Gate G-2 (Browser Verify)

_"Please verify in browser (`npm run dev`). All good?"_ — **stop and wait.**

---

## Stage 13 — PR Agent

**→ Human Gate G-3:** show the following as markdown and wait for approval:

```
**Title:** chore: FE - Angular v19 migration to standalone - {COMPONENT_CLASS} (#PZ-XXXXX)

**Body:**
FE - Angular v19 migration to standalone components - {COMPONENT_CLASS}

- {COMPONENT_CLASS} made standalone
- spec added
- migration plan updated

Solves PZ-XXXXX
```

Ask user for Jira ticket if not already known.

After approval, launch a **general-purpose** agent:

```
1. Rename branch: git branch -m temp/standalone-migration chore/{JIRA}--FE--Angular-v19-migration--{COMPONENT_CLASS_KEBAB}
2. Push: git push -u origin chore/{JIRA}--FE--Angular-v19-migration--{COMPONENT_CLASS_KEBAB}
3. Create PR:
   gh pr create \
     --title "{APPROVED_TITLE}" \
     --body "{APPROVED_BODY}"
Return: PR_URL: <url>
```

---

## Stage 14 — Claims Checker

Launch an **Explore** agent:

```
Run: git show origin/chore/angular-19-migration--collaboration-claims-list--no-merging_keep_me:migration-claims.md
Verify that `- [x] {COMPONENT_CLASS}` is present.

Return: CLAIM_VERIFIED: yes | ENTRY_FOUND: the exact line
```

---

## Stage 15 — Stats Updater

Launch a **general-purpose** agent:

```
Read .claude/commands/migrate-ng19-standalone-components.md.
Run from src/main/app/: grep -rl "standalone: false" src/app --include="*.ts" | grep -v "spec.ts" | wc -l
This gives you the new REMAINING count.
DONE = 152 - REMAINING  (152 = total at migration start).

Update the progress line at the top of the plan:
  **Progress: {OLD_DONE} done — {OLD_REMAINING} remaining** → **Progress: {NEW_DONE} done — {NEW_REMAINING} remaining**

Also add a ✅ entry to the ## Completed section:
### ✅ `{COMPONENT_RELATIVE_PATH}` ({DATE})
- `imports: [{IMPORTS_LIST}]`
- Access modifiers: {ACCESS_FIXES_SUMMARY}
- **Pattern**: {any notable pattern worth recording}

Update ## Next Target to the next logical component.

Commit the updated plan to the work branch:
git add .claude/commands/migrate-ng19-standalone-components.md
git commit -m "chore: update migration plan after {COMPONENT_CLASS}"

Return: STATS_UPDATED: yes | NEW_PROGRESS: {NEW_DONE} done — {NEW_REMAINING} remaining
```

---

## Pipeline Summary

```
Stage  1  Target Picker          → component chosen
Stage  2  Claim Agent            → component claimed on collab branch
Stage  3  Work Branch            → temp/standalone-migration created
Stage  4  Spec Writer            → spec file created
Stage  5  Spec Reviewer          → spec quality verified (loop until PASS)
Stage  6  Baseline Tests         → green ✅
         ⛔ G-1: user approval to migrate
Stage  7  Migration Agent        → standalone: true, tests green
Stage  8  Migration Reviewer     → migration quality verified (loop until PASS)
Stage  9  Lint                   → clean
Stage 10  Tick Claim             → [x] on collab branch
Stage 11  Commit                 → files committed
         ⛔ G-2: browser verify
Stage 12  (wait)
Stage 13  PR Agent               → branch renamed, PR created
         ⛔ G-3: PR title/body approval
Stage 14  Claims Checker         → [x] verified
Stage 15  Stats Updater          → plan MD updated, progress counter correct
```

Human gates remaining: **3** (migrate approval · browser verify · PR approval).
Everything else runs autonomously.
