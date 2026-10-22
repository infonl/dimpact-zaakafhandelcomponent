# Migrate the next ngx-translate batch

Do the next step of the ngx-translate v18 migration: replace `TranslateModule` with the standalone `TranslatePipe` and/or `TranslateDirective` in one batch of source files, or, when no batch is left, report that the v18 bump is next.

**Usage:** `/migrate-ngx-translate-batch $ARGUMENTS`
`$ARGUMENTS` = the Jira subtask as pasted from Jira: its key (e.g. `PZ-13153`) and its title (e.g. `Replace TranslateModule with TranslatePipe/TranslateDirective in zaken (25 files)`). The batch is the one whose name appears in the title (see the table in Step 1); use the key in the branch name and PR body.
Without `$ARGUMENTS`, work out the next step as described in Step 1 and ask the user for the Jira key before delivering the branch name and PR text.

Temporary: delete this command once ngx-translate v18 is merged.

---

## Background

ngx-translate v18 removes `TranslateModule`. On v17, `TranslatePipe` and `TranslateDirective` are already standalone and `app.config.ts` already uses `provideTranslateService()`, so every batch is a behaviour-neutral change that ships on its own, branched from `main`. The specs were migrated first (PZ-13152); they use `provideTranslateService()` in `providers`.

## Step 1 — Find the next step

Batches, in this order (folders under `src/main/app/src/app/`):

| # | Batch | Folders | Jira | Status |
|---|---|---|---|---|
| 0 | specs | all `*.spec.ts` | PZ-13152 | done (#7565) |
| 1 | zaken | `zaken` | PZ-13153 | migrated, PR pending |
| 2 | shared | `shared` | | |
| 3 | admin | `admin` | | |
| 4 | klanten, zoeken, dashboard, taken, informatie-objecten | `klanten` `zoeken` `dashboard` `taken` `informatie-objecten` | | |
| 5 | remaining areas | every other folder, plus files directly in `src/app/` | | |

The Status column is a convenience; the grep and the open-PR check below decide. After finishing a batch, fill in its Jira key and set its status to "migrated, PR pending" (the user updates it to "done (#PR)" after the merge).

1. Make sure you work on an up-to-date `main` (`git fetch` and compare; do not switch branches yourself — if the working tree is not on a fresh `main`, tell the user and stop).
2. Count the remaining non-spec files per folder:
   `cd src/main/app/src/app && grep -rl "TranslateModule" --include='*.ts' . | grep -v '\.spec\.ts' | sed 's|^\./||' | awk -F/ 'NF==1{print "(root)"; next}{print $1}' | sort | uniq -c | sort -rn`
3. List open PRs for this migration so a batch that is already in review is skipped:
   `gh pr list --state open --search "TranslateModule in:title" --json number,title,headRefName`
   If `gh` fails, use the public API: `curl -s "https://api.github.com/repos/infonl/dimpact-zaakafhandelcomponent/pulls?state=open&per_page=100"` and filter titles on `TranslateModule`.
4. With `$ARGUMENTS`, the batch named in the subtask title is the one to do; warn the user if it has no files left or already has an open PR, and stop. Without `$ARGUMENTS`, the next step is the first batch in the table that still has files AND has no open PR.
   - Batches with files but an open PR: mention them as "in review".
   - No batch left with files: all source files are migrated → go to **Step 4**.
5. Report the overview (files left per batch, which are in review, which one is next) before starting.

## Step 2 — Migrate the batch

Per file in the batch:
1. Read the template: the sibling `.html`, or the inline `template:`.
2. `| translate` in the template → import `TranslatePipe`.
3. The directive (`translate="key"`, `[translate]="…"`, a bare `translate` attribute, `[translateParams]`) → import `TranslateDirective`. A pipe expression that only contains the word "translate" is not the directive; check each hit.
4. Both → import both. Neither → remove the import entirely.
5. Replace `TranslateModule` in the component `imports` array and in the `@ngx-translate/core` import statement. Import nothing unused: Angular warns about unused standalone imports (NG8113).

Hard rules:
- The user handles all git: no commit, branch, stash, reset, checkout or push. Read-only git is fine.
- Stay inside the batch's folders. Anything beyond the mechanical swap is reported, not done.
- No `any`, `as any`, or `eslint-disable` of `no-explicit-any`. No narrating comments.
- SPDX: in a modified file, add `, <current year> INFO.nl` only if `INFO.nl` is completely absent from the header.
- Do not touch specs unless one breaks. A touched spec is checked by CI with `.eslintrc.strict-specs.js`, where every Testing Library warning is an error, so the whole spec must then pass it: `getByRole` > `getByLabelText` > `getByText`, `userEvent.setup()`, `fromPartial` from `src/test-helpers`. Never swap one banned query for another (`By.css`, `querySelector`, `getByTestId`). See `.claude/rules/angular-specs.md` and `.claude/commands/audit-ng-spec-skill.md`.

## Step 3 — Verify (in `src/main/app`)

1. The count from Step 1 shows 0 files for the batch's folders.
2. From the repo root: `./gradlew spotlessAppApply` (outside the sandbox). Spotless formats the app with Prettier plus the organize-imports plugin, so it also sorts imports, which a plain `npx prettier --write` does not; never use plain Prettier, its config differs and it reformats untouched code. Review what it changed: only the batch files may differ. `spotlessApp` depends on `npmRunBuild`, so it rebuilds `src/main/app/dist/`, the local WildFly deployment on :8080 — tell the user it was rebuilt.
3. `npx tsc --noEmit -p tsconfig.json` → 0 errors (the strict gate; `tsconfig.app.json` is lenient).
4. `npx ng lint` → 0 errors.
5. `npx ng build --output-path "$TMPDIR/ng-build-check"` outside the sandbox → 0 errors and no NG8113 warning in a batch file. Never build to the default `dist/`: that is the local WildFly deployment.
6. `npx ng test` (full suite, outside the sandbox — watchman is blocked inside it). Use `ng test`, not raw jest. A jest worker that dies with SIGSEGV is the environment; rerun that suite alone.
7. Only if a spec was edited: `ESLINT_USE_FLAT_CONFIG=false npx eslint -c .eslintrc.strict-specs.js <spec>` → 0 errors.

Then deliver:
- A table: file → `TranslatePipe` / `TranslateDirective` / both / removed.
- The result of each check.
- Branch name (the user creates it): `refactor/PZ-XXXXX--ngx-translate-v18-migration--translate-pipe--<batch slug>`
- PR title: `refactor(app): <batch #> of 5 - replace TranslateModule with TranslatePipe/TranslateDirective in <batch name>` (batch # from the table in Step 1, so reviewers see how far the migration is)
- PR body in a fenced block: at most 3 short paragraphs (what and why, effect, one line of test proof) plus `Solves PZ-XXXXX`. No headings, no attribution line.
- Fill `PZ-XXXXX` with the key from `$ARGUMENTS`; without it, ask the user instead of leaving the placeholder.

## Step 4 — All batches done: the v18 bump

When no source file imports `TranslateModule` any more (and no batch PR is still open), report that the last subtask is next: upgrade `@ngx-translate/core` and `@ngx-translate/http-loader` to v18 (exact versions, no `^`/`~`), which closes Renovate PRs #6260 and #6261, and add a `no-restricted-imports` rule for `TranslateModule`. Do not start it without the user's go-ahead. Remind the user to delete this command in that PR.
