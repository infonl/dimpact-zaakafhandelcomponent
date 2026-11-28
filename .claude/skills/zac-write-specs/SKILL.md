---
name: zac-write-specs
description: "Write the OpenSpec change (proposal, design, delta specs, tasks) for a ZAC PZ story, grounded in the ticket and the current code, and validate it, so the team can review the behaviour before code exists. Called by zac-pick-up-story; also use on its own in this repository when the user wants to write, draft or update the specs or OpenSpec change for a PZ ticket, or fold review feedback back into a specs PR."
---

# zac-write-specs

Writes the OpenSpec change for a story. The bar: a developer who reads only this change builds the right thing,
and nothing else. The `zac-review-specs` skill checks exactly that, so this skill writes to its checklist.

This skill wraps `openspec-propose` with the ZAC rules below. It writes planning files only, never code.

## Subagents

- **`zac-researcher`** (Haiku), several in parallel when the questions are independent: the baseline specs and
  open changes per capability, every caller of each function, endpoint or type that changes (for Impact), the
  `file:line` of every name the change will mention, the Dutch UI texts in `nl.json`, how sibling code checks
  the same right.
- Writing the proposal, design, specs and tasks stays with you, on the main model.

Check every `file:line`, quote and finding a subagent returns before it goes into a spec, a comment or a PR:
cheaper models sometimes invent them. Delegate only work that reads many files or runs long; a few quick lookups
are cheaper inline.

## Inputs

- The ticket, its parent and subtasks, read with `acli` (see `zac-pick-up-story`). Number the acceptance criteria
  `AC1`, `AC2`, … and keep that list next to you: every AC must end up in a scenario.
- The outcome of the challenge step: decisions taken, questions still open.

## 1. Know the baseline

- For each capability the story touches, read `openspec/specs/<capability>/spec.md` on `origin/main`.
- Read other open changes for the same capability (`ls openspec/changes/`, skip `archive/`): an open change may
  already be in the code but not yet in `openspec/specs/`, so it is part of the baseline. Make sure the two
  changes do not clash on archive.
- Read the code the story touches, and write down `file:line` for every class, endpoint, right, policy rule,
  table or config key you are going to name. Every claim about existing code must hold.

### Unspecced code: explore first

When no capability in `openspec/specs/` describes the behaviour the story changes, record that behaviour before
writing the change:

1. Tell the user the area has no specs yet, and which part you will describe.
2. Explore with `openspec-explore` (thinking mode, no code changes), with `zac-researcher` for the lookups:
   what the code does today, read from the code, the unit and integration tests, `docs/manuals/` and the UI
   texts in `nl.json`. Only the behaviour the story touches, not the whole module.
3. Write it down as requirements and scenarios of a new capability, every claim with its `file:line`. Where the
   code and the manual disagree, the code is the baseline; list the difference as a question for the team.
4. In the change, the new capability holds both: the current behaviour as `ADDED` requirements and the change
   of the story. `proposal.md` lists which requirements describe existing behaviour and which are new, so
   reviewers check both: "is this how it works today?" and "is the change right?".

For a large unspecced area, propose a separate baseline-only specs PR first, and ask the user.

## 2. Create the change

The CLI needs Node ≥ 22. If `openspec` is not on the path, use
`npx -y -p node@22 -p @fission-ai/openspec@latest openspec <command>` (ignore `EBADENGINE` noise).

Follow `openspec-propose`: `openspec new change "<kebab-case-name>"`, then the artifacts in the order
`openspec status --change <name> --json` gives. Name the change after the behaviour, not the ticket.

## 3. ZAC rules per artifact

**proposal.md**: Why, What Changes, Capabilities, Impact.
- Name the ticket (`PZ-XXX`) in Why.
- Capabilities match the `specs/` folders one to one; new vs modified matches whether
  `openspec/specs/<capability>/` exists on `origin/main`.
- Impact lists every caller of every function, endpoint or type whose behaviour or signature changes, found by
  grep, including Flowable bindings, frontend callers and tests.
- Out of scope: say what the story does not do, so reviewers don't have to ask.

**specs/<capability>/spec.md**:
- Every requirement uses SHALL/MUST and has at least one `#### Scenario:` with WHEN/THEN.
- `MODIFIED` copies the **whole** baseline requirement with an identical header, not just the changed lines: a
  partial copy silently deletes the rest on archive. `REMOVED` has a Reason and a Migration.
- Every AC maps to a scenario. A requirement no AC asks for is a question for the team: list it in the PR
  description instead of quietly adding it. Don't invent business rules.
- Cover the unhappy paths that clearly apply: no right or the wrong role, a user with only a read role, empty or
  missing data, an error from an external system (Open Zaak, BRP, KVK, SmartDocuments), the second click.
- Use the exact role names, page names and Dutch texts of the ticket and the UI; check user-facing texts in
  `src/main/app/src/assets/i18n/nl.json`.
- Rights: check them the same way sibling code does (OPA policy, `PolicyService`, the REST guard, the frontend).
- Status codes and errors: what sibling endpoints and their itests really return.

**design.md**: the how, and the why of each choice.
- Follow existing ZAC patterns; name the existing helper or service you reuse. A new framework, library,
  integration or layer needs an ADR (`docs/solution-architecture/decisions/README.md`); an `accepted` ADR is
  binding.
- Call out explicitly: Flyway migrations (and existing data), Solr fields (and reindex), Flowable model or
  variable changes (and running instances), REST contract changes, new configuration (`.env.example`, Helm,
  kind).
- Java code that must change is converted to Kotlin first in its own PR (`migrate-java-to-kotlin`).
- No changes to Open Zaak, Open Notificaties or other external systems: solve it in ZAC.
- No personal data in logs (`docs/development/logging.md`).
- Name the PR split: which code PRs, in which order, then the docs PR.
- Alternatives you rejected, in one line each.

**tasks.md**: grouped per planned PR, small enough to check off one by one.
- Each task says what to verify and how (`./gradlew test --tests "..."`, an itest, a step in the local stack).
- Tests are tasks. So are `./gradlew spotlessApply detektApply` + `./gradlew detekt`, the
  `scripts/test-data/` scripts when a REST path or payload changes, the SPDX headers of touched files, and the
  manual update when a subtask asks for it.
- A task to try it out in the local kind stack, with the user and the steps (`zac-proof` can guide it).
- The last task: archive the change in the top PR of the stack (`zac-apply-specs`, step 5).
- Don't add an `openspec validate` task; validation happens below and in review.
- No task contradicts design.md.

OpenSpec files carry no SPDX header (CONTRIBUTING.md exemption). Don't add one.

## 4. Check it yourself

1. `openspec validate <change> --strict`; fix every error.
2. Walk the `zac-review-specs` checklist (sections A to E) over your own change and fix what it finds. Every
   `file:line` you cite must be one you opened.
3. Keep it short. Specs that a reviewer can read in ten minutes get reviewed; long ones get skimmed.

## 5. Hand back

Show the user the AC → scenario table and the open questions for the team. Commit with
`docs(openspec): <what the story does>`. Opening the PR is `zac-create-pr`'s job (via `zac-pick-up-story`).

When the team's review changes the agreed behaviour, update the change in the specs PR, validate again, and
push only to that branch (don't restack the code PRs above it unless they need it).
