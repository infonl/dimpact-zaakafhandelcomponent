---
name: zac-review-specs
description: "Review the OpenSpec change (proposal, design, tasks, delta specs) in a ZAC pull request before any code exists, against its PZ Jira story and the current code, and draft review comments. Use in this repository when the user wants to review, check or grill the specs of a PR, e.g. \"/zac-review-specs 8016\", \"check the specs in PR 7975\", or pastes a PR link that adds or changes `openspec/changes/<change>/`. For a PR with code changes, use zac-review-pr."
---

# zac-review-specs

I review the OpenSpec change in a PR and draft review comments. The user posts them. I never post.

Specs are checked **before** code exists, so the question is not "does the code do this" but "will a developer
who follows only this change build the right thing, and nothing else".

## Never post anything

- Never run `gh pr review`, `gh pr comment`, or `gh api` with POST/PATCH/PUT. Never approve, resolve or react.
- Only post when the user says "post it" for a specific draft in the same turn.
- No AI attribution in drafts.

## 1. Gather

1. `gh pr view <n> --json title,body,headRefName,headRefOid,baseRefName,files` and `gh pr diff <n>`.
2. Temp dir: `T=${CLAUDE_JOB_DIR:+$CLAUDE_JOB_DIR/tmp}; T=${T:-$(mktemp -d)}`.
   Fetch the head: `git fetch -fq origin pull/<n>/head:pr<n>` (`-f`: the author may have force-pushed). Never
   switch branches in the user's checkout.
3. Find the change: the `openspec/changes/<change>/` folder the diff adds or edits. Write each artifact to
   `$T` (`git show "pr<n>:<path>" > "$T/<file>"`, quote the argument: in zsh `$B:src/...` breaks) and read them
   one by one, in full: `proposal.md`, `design.md`, `tasks.md` and every `specs/<capability>/spec.md`.
4. Find the ticket: `PZ-XXX` from title, body or branch. `acli jira workitem view PZ-XXX --fields
   summary,description,status,comment,parent,subtasks --json` (the plain output glues the AC lines together).
   If the description is thin, fetch the parent (`... --fields parent --json | jq -r .fields.parent.key`). If
   `acli` is not logged in, ask the user to run `! acli jira auth login --web`. No ticket → say so and check
   against the PR body only.
5. Baseline: for every capability the change modifies, read `openspec/specs/<capability>/spec.md` on the base
   branch. Also find other open changes for the same capability
   (`git ls-tree -r --name-only origin/<base> openspec/changes/ | grep -v archive/ | grep "specs/<capability>/"`)
   and read them too: an open change may already be in the code without being in `openspec/specs/`, so it is part
   of the baseline. Check that the two changes do not clash on archive.
6. Existing review comments: `gh api --paginate repos/{owner}/{repo}/pulls/<n>/comments` and
   `gh pr view <n> --comments`. I do not repeat them. A comment the head already fixes goes under **Considered
   and dropped**.

## 2. Check, one section at a time

Go through every section. Each one ends up in the output, also when it finds nothing.

### A. Complete and valid
- Validate in a worktree: `git worktree add "$T/pr<n>" pr<n>`, then in it
  `npx -y -p node@22 -p @fission-ai/openspec@latest openspec validate <change> --strict` (the CLI needs
  Node ≥ 22; ignore `EBADENGINE` noise). Afterwards `git worktree remove "$T/pr<n>"` and `git branch -D pr<n>`.
  Report errors, not the run. Skip this step when a CI check on the PR already runs `openspec validate`
  (`gh pr checks <n>`); then report its result instead.
- All four artifacts present. `proposal.md` has Why, What Changes, Capabilities, Impact.
- Every requirement uses SHALL/MUST and has at least one `#### Scenario:` with WHEN/THEN.
- `MODIFIED` requirements copy the **whole** baseline requirement, header identical to the baseline, not just the
  changed lines (a partial copy silently deletes the rest on archive). `REMOVED` has Reason and Migration.
- Capabilities in `proposal.md` match the `specs/` folders: no capability listed without a delta, no delta
  without a listing. New vs modified matches whether `openspec/specs/<capability>/` exists on base.
- Unhappy paths have scenarios: no right / wrong role, empty or missing data, external system error, the user
  without a read role, the second click. Not every one applies; flag the ones that clearly do.
- `tasks.md`: each task names what to verify and how; tests are tasks; there is a task for
  `spotlessApply detektApply` and for `scripts/test-data/` if a REST path or payload changes (CLAUDE.md rule).
  Do not ask for an `openspec validate` task: validation belongs in the review (above) or CI, not in `tasks.md`.
  Tasks do not contradict design.md.

### B. Matches the story
- Number every acceptance criterion of the ticket (and of the subtasks this PR covers) as `AC1`, `AC2`, ….
- Map each AC to the requirement/scenario that covers it. An uncovered AC is an **Issue**. A non-functional AC
  (performance, "simple improvements") that only design or tasks cover is ⚠️, not ❌: ask whether it needs a
  measurable requirement.
- Map each requirement back to an AC. A requirement no AC asks for is a **Question**: in scope? Should the
  ticket say so?
- Check the terms: role names, page names, Dutch domain words and texts shown to the user match the ticket and
  the existing UI exactly (grep `src/main/app/src/assets/i18n/nl.json` for user-facing text).
- The PR title is `docs(openspec): ...` and the PR body names the ticket (`Solves PZ-XXX`).

### C. True to the code
Specs describe the system. Every claim about existing code must hold. I check, with `file:line`:
- Every class, function, endpoint, path, role, right, policy rule, table or config key the change names exists
  and does what the spec says (grep `src/main/kotlin`, `src/main/app/src`, `src/main/resources/policies`,
  `src/main/resources/schemas`).
- The baseline behaviour the spec says "stays the same" really is the current behaviour.
- The Impact list is complete: grep for every caller of every function, endpoint or type whose signature or
  behaviour changes, including Flowable bindings and tests. A caller the proposal misses is an Issue.
- Expected status codes and errors match what sibling code and its itests really return (e.g. JAX-RS gives 404,
  not 400, for an enum query parameter it cannot parse, unless a `ParamConverter` exists).
- Rights: compare with how the same check is done elsewhere (OPA policy files, `PolicyService`, the toolbar or
  the REST endpoint guard). A spec that checks a different right than the sibling code is an Issue.

### D. Unusual patterns
Look for where the plan departs from how ZAC already does the same thing:
- A new helper, service, table, library or pattern while an existing one does the job. Find the existing one
  and link it.
- A change of framework, layer, integration or cross-cutting convention: check
  `docs/solution-architecture/decisions/README.md`. Conflicts with an accepted ADR are an **Issue**; a new
  decision without a new ADR task is an **Issue**.
- A design that touches Java code without a separate conversion PR (CLAUDE.md rule).
- A Flyway migration, Solr schema change, Flowable model change or REST contract change that the design does
  not call out, or that has no rollback / reindex / data migration task.
- Changes to Open Zaak, Open Notificaties or other external systems: out of our control, must be solved in ZAC.
- Logging of personal data (BSN, names, addresses): see `docs/development/logging.md`.
- Design choices without a "why", and alternatives not mentioned that look simpler. These are **Discussion**.

### E. Clear to read
Only objective problems: a requirement that can be read two ways, a scenario whose THEN cannot be tested, two
artifacts that contradict each other, a term used for two things. Not wording taste.

## Prove it before drafting

Every Issue cites what I checked: the AC text, the baseline requirement, or the code at `file:line`. If I could
not confirm it, it becomes a Question, never a claim. Fewer, correct comments beat many weak ones. At most 3
Minor and 3 Discussion items, the most useful ones.

## Writing the drafts

Write as the user would: short, simple sentences for non-native English readers, friendly and direct, no "Great
work!", no "Nit:". Say what is wrong and why it matters in one or two sentences; suggest the fix only when short.
A question is fine when unsure.

Good: "AC3 asks for a coordinator to see the button too, but no scenario covers the coordinator. Can you add one?"
Bad: "It may be worth considering whether the current set of scenarios sufficiently addresses the full matrix of
roles enumerated in the acceptance criteria..."

## Links

- PZ numbers: `[PZ-1234](https://dimpact.atlassian.net/browse/PZ-1234)`.
- PRs: `[#1234](https://github.com/infonl/dimpact-zaakafhandelcomponent/pull/1234)`.
- Lines in the diff: `https://github.com/<owner>/<repo>/pull/<n>/changes#diff-<sha256 of path>R<line>`;
  hash with `printf '%s' "<path>" | shasum -a 256 | cut -d' ' -f1`. Build every link with a command and paste
  it whole; never guess or retype.
- Existing code: `https://github.com/<owner>/<repo>/blob/<sha>/<path>#L<a>-L<b>`, with the base branch sha
  (`git rev-parse origin/<base>`). A specs-only PR does not change code, so base and head show the same code.

## Output

1. **Ticket**: one or two sentences on what the story asks, with its status.
2. **AC coverage** table: `AC#` | criterion (short) | covered by (requirement/scenario link) | ✅ / ❌ / ⚠️.
3. **Verdict**: **Blockers**, **Some issues** or **Looks good**.
4. **Checks**: one line per section A–E with what I checked and the result, "n/a" with a reason if it does not
   apply.
5. **Considered and dropped**: candidate concerns that did not hold, one line each with why.
6. If there are drafts, an overview table:

   | # | File:line | Severity | Issue |
   |---|-----------|----------|-------|

   Severity is **Blocker**, **Issue**, **Question**, **Minor** or **Discussion**.
7. The drafts, one per row, as a quote block ready to copy, headed by the linked `file:line`.

End by asking which drafts to keep, change or drop. Do not post.

## After the review

Once the team agrees on the specs, the author builds the code with the `zac-apply-specs` skill, stacked on top of
this PR. A spec that changes during the build goes back into this PR first, so the specs stay the source of truth.
