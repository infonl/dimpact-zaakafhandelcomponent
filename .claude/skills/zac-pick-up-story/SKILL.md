---
name: zac-pick-up-story
description: "Pick up a ZAC Jira story or bug end to end, specs first: read the PZ ticket and its subtasks, put them in progress, challenge the plan, write the OpenSpec change and open it as a draft PR for the team to review before any code is written. Also the map of the whole ZAC story flow (specs, review, build, review, proof). Use in this repository when the user wants to pick up, start or work on a PZ ticket, e.g. \"/zac-pick-up-story PZ-1234\", \"pick up PZ-1234\", \"start on this story\", or asks what the next step on a story is."
---

# zac-pick-up-story

The team agrees on the behaviour before anyone writes code. So picking up a story ends with a PR that holds
only the OpenSpec change, and a stop. The code comes after the team has reviewed the specs.

## The story flow

| Step | Who | Skill | Result |
|------|-----|-------|--------|
| 1. Pick up | author | `zac-pick-up-story` (this one) | ticket in progress, plan challenged |
| 2. Write the specs | author | `zac-write-specs` | `docs(openspec): ...` draft PR |
| 3. Review the specs | team | `zac-review-specs` | agreed behaviour |
| 4. Build | author | `zac-apply-specs` | code PRs stacked on the specs PR, docs PR last |
| 5. Review the code | team | `zac-review-pr` | reviewed stack |
| 6. Try it out | anyone | `zac-proof` | a test guide on the local stack, or Playwright proof |

`zac-create-pr` sets how every PR in the stack is opened and how review comments are handled. `zac-grilling`
stress-tests a plan at any step.

When the user asks for the next step on a story, find where it is (`gh pr list --search PZ-XXX --state all`,
the stack, the review state) and point to the skill for that step.

## 1. Read the story

1. `acli jira workitem view PZ-XXX --fields '*all' --json`: description, acceptance criteria, comments, linked
   issues, subtasks. The plain output glues lines together; use `--json`. When the description is thin, read the
   parent too. Not logged in: ask the user to run `! acli jira auth login --web`. Use `acli`, not the Atlassian
   MCP: the MCP is not allowed on the dimpact domain.
2. Read every subtask (e.g. "Gebruikershandleiding bijwerken", "Design maken") up front, so its work is part of
   the plan and the PR split, not an afterthought.
3. Session title: the key plus the Jira title, e.g. `PZ-1234 Zaakspecifieke autorisatie verwijderen`. If you
   cannot rename the session yourself, give the user `/rename PZ-1234 <title>`.

## 2. Put it in progress

Right away, move the story and the subtasks the user starts now (not one that waits for design) to
"In development" and assign them to the user:

```bash
acli jira workitem transition --key PZ-XXX --status "In development" --yes
acli jira workitem assign --key PZ-XXX --assignee "@me" --yes
```

A transition can take a few seconds to show in `view`; check again before you retry.

## 3. Challenge before writing anything

If the story is unclear, or contradicts the code, ask before going on. One question at a time, question first.

Look at the change through each lens, with the code open (`file:line`), not from memory:

- **Security**: authorisation (OPA policies in `src/main/resources/policies/`, `PolicyService`), input
  validation, personal data (AVG) in logs or responses (`docs/development/logging.md`).
- **Current patterns**: how ZAC already does the same thing. No new pattern, helper or library when one exists;
  a new one needs an ADR (`docs/solution-architecture/decisions/README.md`).
- **Correctness**: edge cases, error handling, REST/JSON backwards compatibility, running Flowable instances,
  existing data (Flyway), the Solr index.
- **Maintainability**: the smallest change that solves the story.

Don't add business rules the acceptance criteria don't ask for, not even ones that seem obviously right ("you
cannot remove yourself"). Ask the user, or draft an extra acceptance criterion for the ticket: the team often
knows why the rule is wrong.

For an action that takes something away (unlink, revoke, delete), check every flow that grants the same thing
(add, grant, assign), and make them consistent in one go: same lock, same reindex, same order.

Say it when something in the story is unnecessary, risky or better solved differently. For a larger or riskier
story, offer to run `zac-grilling` on the plan before writing the specs.

## 4. Write the specs and open the specs PR

1. Branch from a fresh `origin/main`: `feature/PZ-XXX-openspec-<short-name>`.
2. Use `zac-write-specs` to write and validate the OpenSpec change.
3. Use `zac-create-pr` to open it as a draft: title `docs(openspec): <what the story does>`, footer
   `Solves PZ-XXX`. Make it the bottom of a `gh stack` (`gh stack init --base main <branch>`), so the code PRs
   can stack on it later.
4. In the PR description, list the questions the team must answer, if any.

## 5. Stop

Do not start on the code. Tell the user:
- the specs PR link, and the open questions for the team;
- that a teammate can review it with `/zac-review-specs <PR number>`;
- that once the specs are agreed, `/zac-apply-specs <PR number>` builds the code on top of it.

Building before the specs are agreed only when the user says so explicitly, e.g. for a tiny bug fix. Even then
the specs PR stays the bottom of the stack.
