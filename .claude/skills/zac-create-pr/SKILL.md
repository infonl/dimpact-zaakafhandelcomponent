---
name: zac-create-pr
description: "Open ZAC pull requests the team way: draft, a stack per story (OpenSpec PR, code PRs, docs PR) via gh stack, a short plain-English description with the Solves PZ-XXX footer, and a Copilot review loop until only nits remain. Also handles review feedback on those PRs. Use in this repository when the user wants to open, create or draft a PR, write or rewrite a PR title or description, push a stack, or respond to review comments on their PR."
---

# zac-create-pr

A PR description tells a reviewer what changed and why, in a few plain sentences. Readers are often not native
English speakers and not always developers.

## Before opening

1. `git fetch origin` and check the branch holds only its own commits: `git log --oneline <parent>..HEAD`, where
   `<parent>` is `origin/main` for the bottom PR and the branch below it for an upper layer of a stack.
2. Branch name: `feature/PZ-XXX-description` (CLAUDE.md). Never `renovate/`.
3. Backend changes: `./gradlew spotlessApply detektApply`, then `./gradlew detekt`, from the repo root. Frontend
   changes: `./gradlew spotlessApply` too (local prettier passes things CI's spotless rejects), and
   `cd src/main/app && npm run lint`. Only say a check passed when it ran: check the exit code and the
   `BUILD SUCCESSFUL` line, and don't filter the output so far that an error line disappears.
4. A REST path, payload or validation change updates `scripts/test-data/` in the same PR (CLAUDE.md).
5. Open as **draft**: `gh pr create --draft`, or `gh stack submit --auto` for a stack (without `--auto` an
   interactive run opens new PRs as ready for review). Draft until CI is green and the
   Copilot loop below is done; the developer marks it ready.

## One story, one stack

A story goes up as a native GitHub stack, even a small one:
[stacked PRs](https://docs.github.com/en/pull-requests/get-started/about-stacked-prs) via the `gh stack` extension
(`gh extension install github/gh-stack`), not separate PRs whose base is the previous branch.

1. **OpenSpec PR**: only `openspec/changes/<change>/`, titled `docs(openspec): ...`. The team agrees on the
   behaviour before anyone reads code. The `zac-pick-up-story` skill opens this one and stops.
2. **Code PRs**: one or more, split logically (one per screen, flow or layer). Each carries its own tests and
   i18n texts. A Java-to-Kotlin conversion is its own PR below the PR that changes the behaviour.
3. **Docs PR**: last; the manual updates (`docs/manuals/`) and, once the code is in, the archive of the
   OpenSpec change.

Commands:
- Adopt existing branches: `gh stack init --base main <b1> <b2> ...`, then `gh stack submit --auto` (new PRs
  open as drafts). `--auto` fills each new PR with the PR template as is, so set the real title and
  description right after: `gh pr edit <n> --title "<title>" --body-file <file>`.
- Add a layer from the top branch: `gh stack add <branch>`. Keep in sync with `gh stack sync` / `gh stack rebase`.
- Merge bottom-up. Merging a PR in the middle merges everything below it.

### CI on the upper layers

Some workflows only run for PRs whose base is `main`: `lint-changed-specs.yml` (strict lint of changed Angular
specs) and `helm-chart-testing.yml`. An upper layer gets them only once the layer below is merged. Until then,
run them yourself for a layer that changes Angular specs or the Helm chart:
`cd src/main/app && npx eslint -c eslint.strict-specs.config.js <changed .spec.ts files>`, and say in the PR
that the Helm checks still have to run.

### Fixing a PR lower in the stack

Push the fix only to that PR's branch; do not restack the branches above it. A restack gives every commit above
a new SHA, so every PR above reruns its full CI for nothing. Restack only when a PR above needs the fix,
conflicts with its base, or is next to merge.

## The title

Conventional Commits, `<type>[optional scope]: <description>`: one short line on what the change does for the
user. Example: `feat: allow a brondatum in the past`.

## The description

Follow `.github/pull_request_template.md`. Exactly these parts, in this order:

1. **What**: one or two sentences on what changes, in the words of the user or the ticket.
2. **Why**: one sentence, only when "What" does not make it obvious.
3. **Notes for the reviewer**: at most three short bullets, only what a reviewer must know (e.g. "No backend
   change: the backend already checks this."). Leave it out when there is nothing.
4. The footer `Solves PZ-XXX`.

Nothing else. The diff shows the files, names and tests.

Style:
- Short sentences, simple words, one idea per sentence.
- Describe behaviour, not code: "The date can now be in the past", not "Replaced `Validators.min(today)`".
- No file paths, class or function names, i18n keys or commands. Library names (TanStack Query) are fine.
- No "Summary" or "Test plan" headings, no checklists, no emoji.

```
A recordmanager can now set the brondatum to a date in the past, to correct it later.
The date cannot be before the end date of the zaak.

- Only the frontend changes. The backend already checked this rule.

Solves PZ-12608
```

## After opening: the Copilot loop

Keep at it for every PR in the stack until done. The waiting and reading go to a **`zac-researcher`** subagent
(Haiku): it watches CI and the Copilot review of each PR and returns the verdict, the failing checks and the new
comments with their `file:line`. Judging and fixing comments, and every reply, stay with you, on the main model.


1. Request the first Copilot review unless one is running or done (drafts are not always reviewed
   automatically): `gh pr edit <n> --add-reviewer @copilot`. Wait for it and for CI.
2. Fix valid Copilot comments; reply briefly to the ones you reject, with the reason.
3. Push, and re-request a Copilot review.
4. Repeat until CI is green and Copilot's remaining comments are nits.

Read the whole review body, not only the open inline comments. The verdict ("Approval recommended", "Changes
recommended", "Needs a closer look") and "Previously missed" notes are only in the body. "0 open findings" is not
done. The loop ends at "Approval recommended", or when every remaining point is declined with a documented
reason (e.g. an accepted risk in the design), so Copilot stops raising it.

### Reading CI

`gh pr checks` hides cancelled duplicates that GitHub still shows as red. Use
`gh pr view <n> --json statusCheckRollup` and list everything that is not SUCCESS, SKIPPED or NEUTRAL. Compare
with the required checks of the `main` ruleset (`gh api repos/{owner}/{repo}/rules/branches/main`).

- After `gh stack push` every workflow runs twice on the same commit and one copy is cancelled; `helm-status`
  then reports a failure. Re-run the cancelled runs on the head commit
  (`gh api "repos/{owner}/{repo}/actions/runs?head_sha=<sha>"`, then `gh run rerun <id>`).
- "Sorry, this diff is temporarily unavailable due to heavy server load" is a GitHub outage. Re-run the job.
- `codecov/project` is not required. It can stay pending after a partial re-run; say so instead of waiting.

## Human review comments

Comments from teammates are a conversation the developer owns:

- Draft the reply and the fix, and show both to the developer first. Post only after they approve.
- Replies are a few words: "Agree, fixed", "Applied", or one sentence with the reason when you disagree.
- A comment that changes the agreed behaviour goes into the OpenSpec PR first, then into the code.
- Never resolve a teammate's thread; the reviewer does that.
