---
name: zac-review-pr
description: "Review a ZAC (zaakafhandelcomponent) PR or branch (the changes since a fixed point) on three axes: Standards, Spec, and Regression. Builds the scope from the linked PZ Jira story and subtasks (via acli) and the story's OpenSpec change, checks the ZAC standards, ADRs and PR conventions, and treats every change to existing behaviour outside that scope as a possible regression, even when all tests pass. Use in this repository when the user wants to review a PR, a branch, work-in-progress changes, or asks to \"review since X\"."
---

Three-axis review of the diff between `HEAD` and a fixed point (by default the base branch of the PR):

- **Standards**: does the code conform to this repo's documented coding standards?
- **Spec**: does the code faithfully implement the scope (the Jira story/subtasks and the OpenSpec change)?
- **Regression**: does the diff change existing behaviour that the scope does not ask to change, or break behaviour CI used to prove?

The **scope rule** below is the core of this skill. Tests are never the yardstick for regression: whoever changes the code wrongly can change the tests along with it, and CI stays green. The yardstick is the scope.

Regression is evidenced two ways: a CI-based check (tests that used to pass and now don't) and a functional check (the behaviour delta of the production code, judged against the scope, whether or not any test covers it). All four sub-agents (Standards, Spec, Regression (CI), Regression (functional)) run **in parallel** so they don't pollute each other's context. This skill then reconciles their findings against existing PR feedback, verifies what survives, and aggregates the result under three headings.

## The scope rule

The **scope** is what this PR is allowed to change: the Jira story (or bug), the subtask(s) this PR covers, and the OpenSpec change that belongs to the story. Step 2 builds it as a numbered list of scope items (`S1`, `S2`, …), each with its source.

Every behaviour change in the diff falls into exactly one class:

| Behaviour change | Class | Axis |
|---|---|---|
| Traceable to a scope item | In scope; check that it is implemented correctly | Spec |
| New behaviour outside the scope, existing behaviour untouched (a new endpoint, field, or option nobody asked for) | Scope creep | Spec |
| Existing behaviour changed or removed, and no scope item asks for it | Out-of-scope change: judge it below | Regression |

Judge every out-of-scope change into one of three verdicts. Do not report all of them as hard regressions:

- **Regression**: the change breaks or worsens behaviour that a user, a caller, or a stored dataset relied on. This is a hard finding.
- **Unmentioned but correct**: the change looks right (it fixes a real bug, makes the behaviour consistent with its neighbours, or matches a documented business rule or an `openspec/specs/` requirement as it stood at the fixed point), but the scope does not mention it. Report it so the user knows about it, say why it looks correct, and say that the ticket or OpenSpec change should mention it. It is not a regression.
- **Question for the author**: you cannot tell whether the change is intended, usually because the ticket is vague or has few acceptance criteria. Ask it as a question ("Not in PZ-XXX: intended?") with the before → after behaviour. It is not a hard regression.

Tests never prove a change is in scope. A test whose expected value the diff changed must itself trace to a scope item. If it doesn't, the code and the test were **bent together**, and the change is judged like any other out-of-scope change. This skill uses the term "bent together" for that case everywhere.

## Process

### 1. Pin the fixed point

Whatever the user said is the fixed point (a commit SHA, branch name, tag, `main`, `HEAD~5`, a PR number, etc.). For a PR number, use the PR's base branch. If they didn't specify one, use the base branch of the PR for the current branch; if there is no PR, ask. If the user names a PR whose head branch is not the current branch, ask them to check it out; do not switch branches yourself.

**Never compare against a local branch.** A local `main` is usually behind `origin/main`. When the PR branch has merged a newer `main`, the merge-base with the stale local `main` lies before that merge, and the diff then contains every change that reached `main` since: hundreds of files that are not this PR, plus the Jira keys of other PRs in the commit messages. So:

1. Run `git fetch origin` first. A fetch is enough: the review needs the remote refs, not a local branch that is up to date, so do not `pull` and do not change the user's checkout.
2. When the fixed point is a branch name, use its remote ref: `main` becomes `origin/main`, the base branch `<base>` becomes `origin/<base>`. Keep the plain branch name (`<base-branch>`) for the CI lookups in step 4, which work on branch names. A commit SHA or tag is used as is.
3. Capture the diff command once: `git diff <fixed-point>...HEAD` (three-dot, so the comparison is against the merge-base). Note the PR's own commits via `git log --no-merges <fixed-point>..HEAD --oneline`; `--no-merges` keeps the merge commits from the base branch out.
4. Confirm the fixed point resolves (`git rev-parse <fixed-point>`) and the diff is non-empty.
5. **Check the diff against the PR.** If there is a PR, compare `git diff --name-only <fixed-point>...HEAD` with `gh pr diff <number> --name-only`. They must list the same files. If they differ, the fixed point is wrong (a stale ref, or local commits not pushed yet): stop and report the difference to the user instead of reviewing the wrong diff. Local commits that are not pushed yet are the one expected difference; name them.

A bad ref, an empty diff, or a diff that does not match the PR should fail here, not inside four parallel sub-agents.

Also identify the PR for the current `HEAD`, if one exists (`gh pr view --json number,url,baseRefName,body`). Its number is needed for reconciling against existing feedback (step 5) and for the Regression (CI) sub-agent (step 4). Its base branch tells you whether this is a stacked PR (step 2).

### 2. Build the scope

Build the scope yourself from Jira and OpenSpec. Do not wait for the user to hand you a spec.

1. **Find the Jira key(s).** Take them from the branch name (`feature/PZ-XXX-…`, `docs/PZ-XXX-…`, `fix/PZ-XXX-…`), the `Solves PZ-XXX` footer in the PR body, and the PR's own commit messages (the `--no-merges` list from step 1). Collect every distinct key; most are `PZ-`, but other project keys such as `DRT-` occur.
2. **Fetch each ticket** with `acli jira workitem view <KEY> --fields "summary,issuetype,parent,status,description,issuelinks"`. This repo's tickets live on dimpact.atlassian.net, so always use `acli`, not the Atlassian MCP.
   - The issue type is shown in Dutch (`Subtaak`, `Story`, `Bug`, …). If the ticket is a **subtask**, also fetch its `parent`: the story or bug holds the acceptance criteria. The subtask narrows which part of them this PR covers.
   - If the ticket is a **story or bug**, list its subtasks with `acli jira workitem search --jql "parent = <KEY>"` and fetch each one. Mark which subtasks this diff covers; the others are out of this PR's scope, not missing.
   - Read the summaries of linked issues. A linked bug or story can widen the scope ("fixes PZ-YYY too"), but only if the link or ticket text says so.
3. **Find the OpenSpec change of the story.** A change lives in `openspec/changes/<change>/` while it is open and moves to `openspec/changes/archive/<YYYY-MM-DD>-<change>/` when it is archived. Most changes do not mention their Jira key, so search in this order:
   1. The diff itself: `git diff --name-status <fixed-point>...HEAD -- openspec/`. A PR often adds or archives its own change; that change is the one.
   2. The Jira keys: `grep -rlE "<KEY>" openspec/changes` for the story and every subtask.
   3. The names: match the change directory names (without the date prefix) against the branch description and the ticket summary, and read the `proposal.md` of every candidate to confirm it describes this story.
   Read `proposal.md`, `design.md`, `tasks.md`, and the delta specs under `specs/<capability>/spec.md`. Each requirement and scenario is a scope item. OpenSpec files carry no SPDX header by design (`CONTRIBUTING.md`, _Exemption: OpenSpec files_); that is not a finding.
4. **Read the baseline.** The capability specs under `openspec/specs/<capability>/spec.md` describe the existing behaviour. Read them **at the fixed point** (`git show <fixed-point>:openspec/specs/<capability>/spec.md`): archiving a change merges its delta specs into `openspec/specs/`, so at `HEAD` they may already describe the new behaviour. Behaviour described in the baseline that the diff changes, without a delta spec or ticket asking for it, is an out-of-scope change. So is a change to `openspec/specs/` that no archived change in the diff explains. The manuals under `docs/manuals/` also describe existing user-facing behaviour and can serve as baseline.
5. Also use any spec path the user passed as an argument.
6. **Write the scope list**: number every acceptance criterion, requirement, and scenario as `S1`, `S2`, … with its source (`PZ-XXX description`, `openspec/changes/<change>/specs/<capability>/spec.md`). List the baseline capability specs separately, unnumbered: they are not scope. Note which ones belong to this PR. Every sub-agent gets this list.
7. **Stacked PRs.** If the PR's base branch is not `main`, scope items implemented in the base branch are out of scope for this diff. They are not "missing". Also list the PRs stacked *on top of* this one (`gh pr list --base <this-branch>`): step 6 checks every finding against them.
8. Only if there is no Jira key and no OpenSpec change, ask the user where the scope is. If there is none, the Spec sub-agent skips with "no scope available". Both Regression sub-agents still run, but judge every changed behaviour and every rewritten assertion as a Question for the author, unless the trace shows something actually breaks: nothing can put it in scope.
9. **Java-to-Kotlin conversion PRs.** A PR that converts Java to Kotlin (title like `refactor: convert … to Kotlin`, made with the `migrate-java-to-kotlin` skill) has one scope item: `S1` "convert the code without functional changes; REST paths and JSON payloads stay the same" (`CLAUDE.md`, _Convert Java to Kotlin in a separate pull request_). Every behaviour delta is then out of scope and judged as a Regression or a Question for the author, never as Unmentioned but correct. For the tests the same rule applies: test titles and test logic stay as they were at the fixed point; only stubs, mocks and calls change to fit the Kotlin code. A changed test title, assertion or expected value is bent together. Check that the signaleringen, events and policy calls of the converted code are unchanged.

### 3. Identify the standards sources

In this repo, the standards sources are:

- `CLAUDE.md`: architecture rules (constructor injection, lambda logging, TanStack Query), SPDX headers, readable names, no narrating comments, behaviour documented in tests instead of comments, `https` dummy URLs, Java-to-Kotlin conversion in a separate PR, conventional commits, and branch names.
- `CONTRIBUTING.md`: language, coding conventions, SPDX rules for new and modified files (including the OpenSpec exemption), TypeScript conventions, and conventional commits.
- The path-scoped rule files in `.claude/rules/`, applied only to the changed files their `paths:` frontmatter matches. At the time of writing: `kotlin.md` (`src/**/*.kt`, `src/main/java/**/*.java`, `build.gradle.kts`), `kotlin-tests.md` (`src/test/**/*.kt`, `src/itest/**/*.kt`), `angular.md` (`src/main/app/src/**/*.ts`, `src/main/app/src/**/*.html`), and `angular-specs.md` (`src/main/app/src/**/*.spec.ts`). Read the frontmatter instead of trusting this list. More than one file can apply: a Kotlin test gets both `kotlin.md` and `kotlin-tests.md`, an Angular spec both `angular.md` and `angular-specs.md`.
- The guides in `docs/development/` that describe conventions for the changed code, such as `logging.md` (logging and GDPR/AVG), `paging.md` (REST paging), `testing.md`, `endToEndTypeSafety.md`, `bpmn.md`, and `cmmn.md`. Read only the ones that match what the diff touches.
- `docs/development/linting-strategy.md`: says what the linters and CI checks already enforce: ESLint, the strict TypeScript build (`strict`, `strictTemplates`), the strict Testing Library rules on every spec file a PR touches, detekt with type information, and the i18n key check in `translations.spec.ts`. Its table _Conventions enforced by linters_ is the list to skip. Do not report anything a linter or CI check enforces.

- The ADRs in `docs/solution-architecture/decisions/`: read the index in `README.md` and the ADRs that match what the diff touches. An ADR with status `accepted` is binding: a diff that conflicts with it is a hard violation; cite the ADR number and the decision. A diff that makes an architectural decision itself (adds or replaces a framework, library or external service, or changes a layer, pattern, integration or cross-cutting convention) without adding a new ADR, or an ADR that supersedes the old one, is also a finding (`CLAUDE.md`, _Architecture Decision Records_; conventions in `.claude/rules/adr.md`).
- `docs/development/architectureRules.md`: the ArchUnit rules for the backend layers. The tests enforce them, so do not report a violation, but do report a diff that adds a new frozen violation to the freeze store instead of fixing it.

There is no `CODING_STANDARDS.md` in this repo.

Two ZAC rules always apply on top of the files above, because they make a PR hard to review:

- **Java-to-Kotlin conversion belongs in its own PR.** When a feature or bug PR converts a Java file to Kotlin, or changes a Java file, that is a hard Standards violation, even when the conversion itself is correct. The conversion must be done in a separate PR without functional changes, and the feature PR builds on it; never in the feature PR itself, because a conversion mixed with a functional change adds complexity to the PR: the reviewer can no longer tell which lines are a pure translation and which change behaviour. Recognise it by a Java file deleted next to a new Kotlin file with the same class name (`git diff -M --name-status` shows it as a rename or as `D` + `A`), or by any change under `src/main/java/`. The fix in the finding: move the conversion to its own PR with the `migrate-java-to-kotlin` skill and base this branch on it. This rule does not apply to a conversion PR itself (step 2, item 9).
- **PR metadata.** Check that the PR body has the footer `Solves PZ-XXX` with the right Jira key, and that the branch name follows `feature/PZ-XXX-description` (`CLAUDE.md`, _Git branch conventions_; `renovate/` is reserved for Renovate). The PR title format is enforced by `check-for-conventional-commits.yml`: do not report it.

On top of whatever the repo documents, the Standards axis always carries the **smell baseline** below: a fixed set of Fowler code smells (_Refactoring_, ch.3) that applies even when a repo documents nothing. Two rules bind it:

- **The repo overrides.** A documented repo standard always wins; where it endorses something the baseline would flag, suppress the smell.
- **Always a judgement call.** Each smell is a labelled heuristic ("possible Feature Envy"), never a hard violation. Like any standard here, skip anything `linting-strategy.md` says a linter enforces.

Each smell reads *what it is* → *how to fix*; match it against the diff:

- **Mysterious Name**: a function, variable, or type whose name doesn't reveal what it does or holds. → rename it; if no honest name comes, the design's murky.
- **Duplicated Code**: the same logic shape appears in more than one hunk or file in the change. → extract the shared shape, call it from both.
- **Feature Envy**: a method that reaches into another object's data more than its own. → move the method onto the data it envies.
- **Data Clumps**: the same few fields or params keep travelling together (a type wanting to be born). → bundle them into one type, pass that.
- **Primitive Obsession**: a primitive or string standing in for a domain concept that deserves its own type. → give the concept its own small type.
- **Repeated Switches**: the same `switch`/`if`-cascade on the same type recurs across the change. → replace with polymorphism, or one map both sites share.
- **Shotgun Surgery**: one logical change forces scattered edits across many files in the diff. → gather what changes together into one module.
- **Divergent Change**: one file or module is edited for several unrelated reasons. → split so each module changes for one reason.
- **Speculative Generality**: abstraction, parameters, or hooks added for needs the scope doesn't have. → delete it; inline back until a real need shows.
- **Message Chains**: long `a.b().c().d()` navigation the caller shouldn't depend on. → hide the walk behind one method on the first object.
- **Middle Man**: a class or function that mostly just delegates onward. → cut it, call the real target direct.
- **Refused Bequest**: a subclass or implementer that ignores or overrides most of what it inherits. → drop the inheritance, use composition.

### 4. Spawn all four sub-agents in parallel

Paste **the scope rule** (the section above) and **the scope list** from step 2 into the Spec and both Regression prompts. The sub-agents have no other access to them.

**Standards sub-agent prompt** should include:

- The full diff command and commit list.
- The list of standards-source files you found in step 3, including the ADRs that match the diff, **plus the two ZAC rules (Java-to-Kotlin in its own PR, PR metadata) and the smell baseline from step 3** pasted in full, and the PR body and branch name.
- The brief: "Report, per file/hunk where relevant, (a) every place the diff violates a documented standard: cite the standard (file + the rule); and (b) any baseline smell you spot: name it and quote the hunk. Distinguish hard violations from judgement calls: documented-standard breaches can be hard, but baseline smells are always judgement calls, and a documented repo standard overrides the baseline. Skip anything `docs/development/linting-strategy.md` says a linter enforces. Under 400 words."

**Spec sub-agent prompt** should include:

- The diff command and commit list.
- The scope rule and the scope list, with the fetched ticket texts and OpenSpec files.
- The brief: "Report: (a) scope items for this PR that are missing or partial; (b) scope creep: *new* behaviour that no scope item asks for, while existing behaviour stays untouched; (c) scope items that look implemented but where the implementation looks wrong. Cite the scope item (`S#` and its source line) for each finding. Changes to *existing* behaviour outside the scope belong to the Regression axis: do not report them here. Under 400 words."

If there is no scope, skip the Spec sub-agent and note this in the final report.

**Regression (CI) sub-agent prompt** should include:

- The diff command, commit list, the fixed-point ref, and the plain base branch name (`<base-branch>`).
- The PR number found in step 1, if any. It may be absent (see the fallback below).
- The scope rule and the scope list.
- The brief, in this order:

  1. **Locate CI for the current HEAD.** The ZAC tests run in these workflows: `build-test-deploy.yml` (jobs `backend-unit-tests`, `frontend-unit-tests` in 2 shards, and `itest-shard` in 3 shards; the job `paths-filter` skips them when the PR changes nothing that needs a build, which is not a green run), `run-e2e-on-kind.yml` (`Playwright Tests on kind`), and `lint-changed-specs.yml` (the strict Testing Library rules on the spec files the PR touches). `run-e2e.yml` only runs on a schedule and by hand, so it says nothing about this PR. If a PR number exists: `gh pr checks <number>`, then drill into any failing test-reporting job with `gh run view <run-id> --log-failed` or the check-run's annotations (`gh api repos/<owner>/<repo>/check-runs/<id>/annotations`). If no PR exists, don't stop: CI is tied to the commit, not the PR, so `gh api repos/<owner>/<repo>/commits/<sha>/check-runs` works with no PR at all. If tests run across a job matrix (OS/version/shards), check every job for this commit and de-duplicate by test name.

  2. **Rule out pre-existing/flaky failures before calling anything a regression.** For every currently-failing test, check whether the *same test* also fails on the latest completed CI run for the base branch itself (e.g. `gh run list --branch <base-branch> -L 3`, or `gh api repos/<owner>/<repo>/commits/$(git rev-parse <fixed-point>)/check-runs`; `gh` takes the plain branch name `main`, not `origin/main`). If it fails there too, this diff didn't cause it. Exclude it from the regression list, but still name it once as "pre-existing failure on `<base-branch>`, unrelated to this diff".

  3. **For every test that fails now but not on the base branch**, classify it:
     - Get the diff's changed-file list *with rename detection*: `git diff -M --name-status <fixed-point>...HEAD`. Check the failing test's file against it under both its current and (if renamed) its pre-rename path.
     - A failing test in a file absent from that list, whose imports/fixtures/helpers/config are *also* absent from it, is a regression candidate. If the test file itself is untouched but imports a fixture/helper/config file that *is* in the diff, it's still a regression candidate; attribute the cause to that shared file.
     - A failing test in a file the diff did touch needs two separate checks: (i) did the *file* exist before this diff (`git show <fixed-point>:<path>`)? A file new to this diff is a feature-completeness gap, not a regression; exclude it, it belongs to the Spec axis. (ii) did the diff edit *that specific test's own body*? Diff just that test's block between the fixed point and HEAD. Only a pre-existing test, itself untouched by the diff, that now fails, is a regression candidate. A pre-existing test whose own body the diff changed goes to step 4.

  4. **Check every rewritten assertion against the scope, failing or passing.** For every pre-existing test whose expected value the diff changed, find the scope item that asks for the new value. If one does, it is an intended behaviour change: cite the `S#`. If none does, report it as **bent together**: the code and the test were changed to agree with each other, and nothing in the scope asks for it. Give the old and new expected value and the production hunk it follows, and judge it per the scope rule (Regression, Unmentioned but correct, or Question for the author).

  5. **Scan for coverage regressions.** Report every test the diff newly skips, disables, or deletes (`@Disabled`, `@Ignore`, `.skip(`, `xit(`, `xdescribe(`, a removed test block) that previously ran, unless a scope item removes the behaviour it tested. Nothing "fails" in CI for these.

  6. **Cite causes honestly.** For every regression candidate, quote the actual assertion/exception from the CI log or annotation. Only name a causing diff hunk if you can trace a concrete link: a symbol, field, or exception message from the failure that literally appears in a hunk. If you can't, report "cause not identified" rather than inventing a plausible-sounding hunk.

  7. **Group by root cause, not by test.** If one broken function fails 15 tests, report it once with the list of affected tests.

  Report each root-cause finding as a concrete input → wrong outcome at `file:line`. If CI is green, say so plainly ("no regressions detected in CI"), and still report steps 4 and 5. If no CI run exists yet for the current HEAD, say so plainly and recommend the user run the project's test suite and share the results. Do not run it yourself. Under 400 words.

**Regression (functional) sub-agent prompt** should include:

- The diff command and commit list.
- The scope rule and the scope list.
- Full repo access, not just the diff: this sub-agent has to read callers, shared services, and framework/library source outside the changed hunks.
- The brief, in this order:

  1. **Derive the behaviour delta from the production code alone.** Do not read the tests in this step: a test changed alongside the code proves nothing. For every changed production hunk, read the code before (`git show <fixed-point>:<path>`) and after, and write down what it did before and what it does now, as behaviour a user or caller can observe: a changed condition, default, filter, query, sort order, permission or policy rule, REST field or payload, mapping, error handling, event or signalering, migration, or a removed branch. Number each delta `D1`, `D2`, …. Pure refactors with identical behaviour get no delta; say so once.
  2. **Match every delta to the scope.** Per delta: in scope (cite `S#`), new behaviour outside scope (leave it to the Spec axis, just note it), or existing behaviour changed or removed outside scope. Read the whole ticket text, all subtasks, and the OpenSpec change before you call a delta out of scope.
  3. **Imagine the mistake.** For every out-of-scope delta, and for in-scope deltas that touch shared code, assume the edit was a mistake and ask what breaks. Ask the same about plausible wrong edits nearby: an inverted condition, a dropped null check, a changed default, a filter that now excludes a case, a policy rule that now grants or denies more. Then:
     - `grep` the whole repo for the callers of every changed symbol and list each call site as `file:line`. A symbol with no callers outside its own tests is dead code, not a regression risk.
     - **Follow the contracts a name search does not find.** In ZAC much of the coupling runs through serialised names and data, not through Kotlin symbols, so a `grep` on the changed class or function misses the consumer on the other side. For every delta that touches one of the boundaries below, search for the consumers by the *serialised name* (the JSON property, policy field, index field, variable name, or column), and read them:

       | Boundary | Where the change shows | Where the consumers are |
       |---|---|---|
       | ZAC REST API → frontend | `Rest*` models and REST services under `src/main/kotlin/nl/info/zac/app/` (models in `app/<domain>/model/`) | The generated types in `src/main/app/src/generated/types/zac-openapi-types.d.ts` and every frontend use of the property name under `src/main/app/src/`. JSON names follow the Kotlin property, but JSON-B strips an `is` prefix and `@JsonbProperty` overrides it: check the real JSON name. |
       | Policy input and output (OPA) | `src/main/kotlin/nl/info/zac/policy/input/`, `policy/output/`, `PolicyService.kt` | The Rego rules in `src/main/resources/policies/*.rego` that read the input field, and the `Rest*Rechten` models and frontend checks that read the output field. |
       | Stored JSON | Models stored as JSON, such as the search parameters (`RestZoekParameters`, `FilterParameters`) saved in `Zoekopdracht` | Existing rows in the database: a renamed or removed field breaks the deserialisation of data saved before this PR. |
       | Database | `src/main/resources/schemas/` (Flyway) and JPA entities | Existing data, other entities and native queries that use the column, and whether the migration runs on a populated database. |
       | Solr index | The zoekobjecten in `src/main/kotlin/nl/info/zac/search/model/zoekobject/` and their field names | Queries, facets, and sorting in `nl/info/zac/search/`, the frontend filters, and the need to reindex existing documents (`scripts/solr/`). |
       | Flowable | Process and case variables (`ZaakVariabelenService`, delegates), the CMMN model in `src/main/resources/cmmn/`, and BPMN process definitions (uploaded by beheerders and stored in the database, not in the repo) | Every reader of the variable name, and running case and process instances started before this PR that still hold the old variables. |
       | External APIs | Generated clients from `src/main/resources/api-specs/` (ZGW, Open Klant, BRP, KVK, BAG, PABC, …) | What the external system accepts or returns: a changed request field or a newly required value can break calls in production, while the tests mock the client. |
       | Events and notifications | Screen events, signaleringen (`nl/info/zac/signalering/`), notifications (`nl/info/zac/notification/`), and the handling of Open Notificaties | The observers and frontend listeners that react to the event type or its payload. |
       | Test data scripts | REST paths, JSON payloads and REST validation | The Python scripts in `scripts/test-data/`, which call the ZAC REST API with hand-written JSON. No test covers them, so a REST change without a matching script change breaks them silently (`CLAUDE.md`, _Update the test data scripts when a REST API changes_). |
       | Configuration | New, renamed or removed environment variables and system properties, `src/main/resources/wildfly/configure-wildfly.cli` | `.env.example`, the Helm chart in `charts/` (values and templates), and the kind stack in `scripts/kind/`. An existing installation that does not set a newly required variable fails at start-up. |
       | Translations | i18n keys used in templates and components | `src/main/app/src/assets/i18n/nl.json` and `en.json`. `translations.spec.ts` checks that the keys exist; it does not check that a renamed or removed key is no longer used in a dynamically built key (`'prefix.' + value`). |
       | Form.io forms and task data | Names of task and form fields, and the data a task stores | The Form.io form definitions that read those names. Like BPMN, they are uploaded by beheerders and stored in the database, not in the repo, so a `grep` finds no consumer. |

       Data that already exists in production (database rows, stored searches, Solr documents, running process instances) has no test: no test runs against data saved before this PR. A delta that changes how such data is read is a regression candidate even when every test passes.
     - **Read the actual contract** of anything the diff now routes through, instead of trusting its name or the commit message. If the diff sends previously-separate code paths into a shared handler, framework callback, or global service (an `onError`, an interceptor, a shared cache, a lifecycle hook), open its real source, including inside `node_modules`/vendor code or the framework itself, and cite the lines establishing (a) exactly when it fires and (b) everything it does as a side effect, not only the one thing the commit message advertises.
     - **Walk each side effect forward to a concrete scenario**: name the actual screen, dialog, REST endpoint, task, or stored data, and state plainly what the user sees, loses, or has done to them. When several call sites share one cause, use a compact "Spot → what the user sees" table.
  4. **Compare before vs. after honestly, not just after vs. ideal.** If the diff replaced a worse behaviour with a better one that still costs something, say that plainly. A net improvement with a smaller new cost is a different finding from a pure regression.
  5. **Judge every out-of-scope delta** with the three verdicts of the scope rule: Regression, Unmentioned but correct (say why it looks correct), or Question for the author (say what the ticket leaves open). For a vague ticket, prefer the question over the regression unless the trace in step 3 shows something actually breaks.
  6. **Treat tests as a signal, not as evidence.** Now read the tests the diff touched, and flag false test confidence in three forms:
     - *Bent together*: a test whose expected value the diff changed to follow an out-of-scope delta. Quote the old and new assertion and link it to its `D#`. This is the strongest regression signal this skill knows: a green test that was edited to agree with the new code.
     - *Self-referential assertion*: a changed expected value that nothing independent confirms: no scope item, no fixture recording a real external system's response, no hand-computed value, no documented business rule. Diff just that assertion's line between the fixed point and HEAD to confirm it changed here.
     - *Bypassed mechanism*: a test that appears to cover the changed path but exercises a stub, mock, or bare setup that bypasses the exact mechanism being changed.
     Also report out-of-scope deltas that no test covers at all.
  7. **Rate the confidence of every Regression verdict:**
     - **Critical**: every link in the causal chain is confirmed by reading code, this repo's and any library's, with no assumption gaps.
     - **Significant (conditional)**: the mechanism is real, but at least one link only holds under something a running app would confirm (focus state, mount/unmount lifecycle, timing, a feature flag, specific zaaktype configuration). Name that link and say what would confirm or rule it out.
     - **Minor**: real, but a pre-existing rough edge the diff touches rather than causes.
  8. **Tie every suggested follow-up to a specific finding by number.**

  Report using this structure: **Verified facts** (each with a `file:line` citation), then a **Behaviour delta** table (`D#` | `file:line` | before → after | scope `S#` or "none"), then **Regressions** under Critical / Significant / Minor, then **Unmentioned but correct**, then **Questions for the author**, then **False test confidence**, then **Suggested follow-up**. No word cap, but every claim needs a citation; an uncited claim is a hedge, not a finding.

### 5. Reconcile against existing PR feedback

Fetch all existing PR feedback before presenting anything. On GitHub that takes three calls, because each returns a different kind:

- `gh pr view <number> --comments`: the general conversation comments.
- `gh api --paginate repos/{owner}/{repo}/pulls/<number>/reviews`: the review summaries (approve / request changes, with their body text).
- `gh api --paginate repos/{owner}/{repo}/pulls/<number>/comments`: the inline comments on specific lines, including bot findings such as Copilot's. Most findings live here.

- Drop any candidate finding that's already been raised, matching on substance rather than exact wording: a re-run must not repeat what's already on the PR. This applies to all four sub-reports.
- If the same underlying issue shows up on more than one axis, or in both Regression sub-reports (e.g. a bent-together test from the CI sub-agent and a traced delta describing the same change), keep it once, filed under whichever states it more precisely.
- If a PR comment or the PR body explains an out-of-scope change ("also fixes X because Y"), that explanation does not put it in scope, but it can move the verdict from Question for the author to Unmentioned but correct. Mention the comment.
- If a PR comment claims a finding was already fixed, don't take that at face value: check step 6.
- If nothing survives this pass, say so plainly ("no new findings") instead of padding the report.

### 6. Verify each survivor

A confident wrong finding costs the author more to disprove than a missed nit costs to live with, so nothing reaches the report unverified:

- State every survivor at a specific `file:line`: a Regression, Standards, or Spec finding as concrete *inputs → wrong outcome*, an Unmentioned but correct change or a Question for the author as *before → after* behaviour. If a finding can't be pinned down that precisely, drop it.
- **Verify every out-of-scope claim against the scope sources.** Re-read the ticket, parent story, subtasks, linked issues, and OpenSpec change before you report that a delta is out of scope. A delta that turns out to be in scope is dropped from Regression.
- **Verify every "Unmentioned but correct" verdict**: cite why it is correct (the bug it fixes, the neighbouring behaviour it matches, the business rule, or the `openspec/specs/` requirement at the fixed point). Without that citation it becomes a Question for the author.
- Don't settle a disputed claim by re-reading the diff. Where a finding asserts something about behaviour ("no test would catch this," "this branch is unreachable," "this still works if X is removed"), propose a mutation test instead of arguing it: name the exact change (revert the fix, delete the binding, flip the boolean, remove the guard) at `file:line`, and the exact test command to run. **Do not apply the mutation or run tests yourself.** Hand it to the user and ask them to run it and report the result. Until they do, label the finding "unverified, mutation test proposed". If the suite still passes with the mutation, the claim is false; drop it. If it fails, keep the finding and cite the test that caught it.
- This also applies to any PR comment claiming a finding is already fixed (step 5): check the current code at the `file:line` the comment references, and if the comment references a commit SHA, confirm that commit exists in the branch's history (`git cat-file -t <sha>`). A "fixed in <sha>" reply where the SHA doesn't resolve, or where the current code still shows the bug, is a false resolution: keep the finding open and note the discrepancy.
- **Stacked PRs: check the layers above.** Before you report a finding on a PR that has PRs stacked on top of it (step 2, item 7), check whether a higher layer already fixes, deletes, or duplicates the code the finding is about (`gh pr diff <number>` of each higher PR). If it does, do not report it as an open finding: mention it once with the number of the PR that handles it.
- Regression (CI) failures are evidenced by a real CI run, which satisfies this verification *provided the baseline check ran*. A "CI failed" finding with no baseline check behind it is a hedge: drop it.
- Regression (functional) survivors are verified by tracing the full causal chain end-to-end and re-opening each cited `file:line`, including any library/vendor source, to confirm it says what the finding claims. Don't accept a link that rests only on a function's name, its doc-comment, or the commit message. A finding rated Critical that turns out to rest on a runtime condition is re-labelled Significant (conditional), not dropped and not silently kept at Critical.

### 7. Aggregate

Present the four sub-reports under three headings: `## Standards`, `## Spec`, and `## Regression`. Put the scope list (`S#` with source) above them, so the reader can check every citation.

Order the Regression section by verdict, combining both Regression sub-reports and deduplicated per step 5:

1. `### Regressions`: hard findings, by Critical / Significant / Minor, plus CI regressions.
2. `### Unmentioned but correct`: out-of-scope changes that look right, each with why, and a note that the ticket or OpenSpec change should mention them.
3. `### Questions for the author`: out-of-scope changes whose intent is unclear.
4. `### False test confidence`: bent-together tests, self-referential assertions, bypassed mechanisms, untested out-of-scope deltas, and coverage regressions, unless the same issue is already listed above.

Present findings verbatim or lightly cleaned. Do **not** merge or rerank findings across the three headings, because they are deliberately separate (see _Why three axes_).

End with a one-line summary: total findings per axis (for Regression, the count per verdict), and the worst issue _within each axis_ (if any). Don't pick a single winner across axes.

### 8. Offer to place the findings as PR comments

When the review is done and there is a PR, end the report with one plain line saying that you can place the findings as comments on the PR once the user has read the report and tells you which ones. Do **not** ask with `AskUserQuestion` and do not offer a menu of choices: the user first reads the report in full, then tells you which findings to post. Never post without that instruction: posting notifies the author and cannot be taken back.

When the user names the findings to post:

- Place each comment at the exact `file:line` where the finding was found, as an inline review comment on the right side of the diff at the current `HEAD` commit. Use `start_line` and `line` for a finding that spans a range. Check first that each line lies inside a diff hunk; a finding outside the diff goes in the review body instead.
- Post all comments as **one** review (`gh api -X POST repos/{owner}/{repo}/pulls/<number>/reviews` with `event: "COMMENT"`), so the author gets one notification.
- Write each comment so it stands on its own: what is wrong, the standard or scope item it breaks (file + rule, or `S#`), and the fix. Do not mention the review axes or sub-agents.
- Report the URL of the review to the user.

## Why three axes

A change can pass some axes and fail others:

- Code that follows every standard but implements the wrong thing → **Standards pass, Spec fail.**
- Code that does exactly what the issue asked but breaks the project's conventions → **Spec pass, Standards fail.**
- Code that implements the scope and follows every standard, but also changes behaviour nobody asked to change → **Standards pass, Spec pass, Regression fail.**

Reporting them separately stops one axis from masking the others.

## Why the scope is the yardstick, not the tests

CI-green is not proof of no regression: it only proves that no *existing, still-run, unedited* test caught one. A change can pass Standards, pass Spec, and stay CI-green while still breaking real behaviour, when:

- Someone changes the code wrongly and updates the test to match. The test still exercises the changed line and still passes; it just grades the code against itself. Only an independent answer catches this, and the scope (the ticket and the OpenSpec change) is that answer: if nobody asked for the new behaviour, the edited test is a warning, not a proof.
- The diff funnels previously-isolated code paths into a shared handler whose full side effects (closing a dialog, reloading the page, resetting a display) were never checked against every new caller. No assertion fails anywhere, because no test exercised the newly-shared path in a realistic composition.
- The tests that appear to cover the changed path run it through a stub or bare setup that bypasses the exact mechanism being changed.

This is why Regression runs as two sub-agents: the CI check catches a previously-passing assertion that now fails, and the functional check derives the behaviour delta from the production code, judges it against the scope, and catches the behaviour that broke *without* any assertion moving. That kind is often the more damaging one, because it reaches production believing itself covered.

Not every out-of-scope change is a regression, though. A review that calls every unmentioned fix a regression teaches the author to ignore it. That is why out-of-scope changes get three verdicts: a hard Regression only when something breaks, Unmentioned but correct when the change is right but undocumented, and a Question for the author when the scope is too vague to judge.
