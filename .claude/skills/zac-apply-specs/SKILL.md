---
name: zac-apply-specs
description: "Build the code for an agreed OpenSpec change of a ZAC PZ story, as logically split draft PRs stacked with gh stack on top of the specs PR, with a docs PR last. Use in this repository when the specs of a story are reviewed and the user wants to implement, build or apply them, e.g. \"/zac-apply-specs 8016\", \"build PZ-1234 now the specs are approved\", \"implement the openspec change\", or continue the code stack of a story."
---

# zac-apply-specs

Turns the agreed OpenSpec change into code: one or more code PRs stacked on the specs PR, and a docs PR on top.
The specs are the source of truth. The code follows them, and when the code shows they are wrong, the specs
change first.

## Subagents

- **`zac-researcher`** (Haiku): the review state and open threads of the specs PR, CI status, callers and
  patterns around the code to change.
- **`zac-implementer`** (Sonnet): one per code PR, in that PR's branch, with the tasks of its group, the change
  folder and the branch name. Run them one after another: each PR builds on the one below.
- Planning the stack, judging a blocker or a spec change, and the final check of each PR stay with you: read the
  diff of every PR yourself, and run its tests again before you push.

Check every `file:line`, quote and finding a subagent returns before it goes into a spec, a comment or a PR:
cheaper models sometimes invent them. Delegate only work that reads many files or runs long; a few quick lookups
are cheaper inline.

## 1. Find the change and check it is agreed

1. From the input (specs PR number, `PZ-XXX`, or change name): `gh pr view <n> --json
   number,title,state,reviewDecision,headRefName,isDraft` and the change folder it adds under
   `openspec/changes/<change>/`.
2. Read the ticket again with `acli` (acceptance criteria, subtasks) and every artifact of the change in full.
3. Check the team agreed: an approval, or no open review thread on the specs PR
   (`gh api --paginate repos/{owner}/{repo}/pulls/<n>/comments`). Open threads or no review yet: tell the user
   and ask whether to go on anyway. Never decide that yourself.

## 2. Plan the stack

Take the PR split from `design.md` and the groups in `tasks.md`. Show it to the user before building:

```
#1 docs(openspec): ...            (exists)
#2 refactor: convert <package> to Kotlin   (only when Java code changes; no functional change)
#3 feat(<scope>): <backend part>
#4 feat(<scope>): <frontend part>
#5 docs: update the gebruikershandleiding and archive the <change> change
```

The top PR is always there: it archives the OpenSpec change, and holds the manual updates when the story needs
them. Without manual updates it is `docs(openspec): archive the <change> change`. Create it with the rest of
the stack, so the stack cannot be merged without updating `openspec/specs/`.

- Split by what a reviewer can read in one go: per layer, screen or flow. Each PR builds, passes its tests and
  carries its own tests and i18n texts.
- Java that must change is first converted in a PR of its own with `migrate-java-to-kotlin`, with no
  functional change (CLAUDE.md).
- Branches: `feature/PZ-XXX-<what this PR does>`. Stack them with `gh stack`: check out the specs branch, then
  `gh stack add <branch>` per layer (`zac-create-pr` has the commands).

## 3. Build each PR

Per PR, the tasks of its group in `tasks.md`, following `openspec-apply-change`:

- Read the code around the change first and follow its patterns and `.claude/rules/` (Kotlin, Kotlin tests,
  Angular, Angular specs). Constructor injection, lambda logging, no narrating comments, full variable names,
  SPDX headers (add `, <year> INFO.nl` only when `INFO.nl` is missing).
- Write the test first where it fits: the scenario of the spec becomes the test name, so the test documents the
  behaviour.
- A REST path, payload or validation change: regenerate (`./gradlew generateOpenApiSpec`), update the frontend
  types, and update `scripts/test-data/` in the same PR.
- Check off a task in `tasks.md` (`- [ ]` → `- [x]`) only when its verification ran and passed, in the PR that
  did the task.
- Before each push: `./gradlew spotlessApply detektApply`, `./gradlew detekt`, and the tests of the code you
  touched (`./gradlew test --tests "..."`, `cd src/main/app && npm test -- <spec>`). Report what ran with its
  result; never claim a check you did not run.

When a task turns out wrong or impossible, or the code needs behaviour the specs don't describe: stop, explain
it, and propose the spec change. After the user agrees, update the specs PR first (`zac-write-specs`), then
continue. Don't let code and specs drift apart silently.

## 4. Open and review

1. Open each PR as a draft with `zac-create-pr` (`gh stack submit --auto`), and run the Copilot loop.
2. Offer to review the stack with `zac-review-pr` before asking the team, PR by PR.
3. Offer to try it out with `zac-proof`: a test guide with a localhost link, the user and the zaak to use.
4. Move the subtasks you finished to the next status the team uses (ask which, once).

## 5. Docs and archive

- The top PR updates the manual under `docs/manuals/` when the story or a subtask asks for it. Describe the
  behaviour in the words of the manual, in Dutch.
- The top PR also archives the change, with `openspec-archive-change` (`openspec archive <change> --yes`):
  it moves the change to `openspec/changes/archive/<date>-<change>/` and merges the delta specs into
  `openspec/specs/`. Check the merged specs read well, and that `openspec validate --specs --strict` passes.
- Make the archive commit when the code PRs are approved, after the last `tasks.md` checkmark. When the change
  changes below it after that (a spec fix, a checkmark), drop the archive commit and archive again, so the
  archived copy and `openspec/specs/` match the final change.
- The stack merges bottom-up, so the archive lands together with the code it describes.

## 6. Hand back

Tell the user: the stack with links, bottom to top; what ran and passed per PR; open questions; and the next
step (team review with `zac-review-pr`, a test round with `zac-proof`). Merging is the team's call, bottom-up.
