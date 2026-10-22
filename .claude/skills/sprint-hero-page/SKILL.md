---
name: sprint-hero-page
description: Sprint hero tasks for the ZAC team. Use when the user asks to update the sprint hero page in Confluence - the version tables PodiumD componenten (latest, PodiumD releases, our TEST versions), Supporting componenten (latest, our version) and Externe API's (KVK, BRP - latest, our version on TEST), or the Samenvatting (Dutch sprint summary of component versions and security fixes).
---

# Sprint hero page

Each task fills one section of the sprint hero page in Confluence. Pick the task the user asks for and follow its page exactly.

| Task (= Confluence heading) | Page |
|---|---|
| PodiumD componenten | [podiumd-componenten.md](podiumd-componenten.md) |
| Supporting componenten | [supporting-componenten.md](supporting-componenten.md) |
| Externe API's | [externe-apis.md](externe-apis.md) |
| Samenvatting | [samenvatting.md](samenvatting.md) |

## Before you start

If the user did not name a task, ask which one: PodiumD componenten, Supporting componenten, Externe API's or Samenvatting.

Check the prerequisites yourself; when one is missing, ask the user to fix it and give the exact command. Continue once they confirm.

- PodiumD componenten, Supporting componenten and Samenvatting read the PRIVATE repo `infonl/dimpact-provisioning`. Check with `gh api repos/infonl/dimpact-provisioning --jq .full_name`.
  - Not logged in: ask the user to run `gh auth login -h github.com -w` (choose SSH, then Skip for the key upload).
  - 404/403: the token has no access to that repo, or awaits infonl org approval. Ask the user to fix the token. Never guess those values.
- Externe API's needs no login.
- Supporting componenten, Externe API's and Samenvatting read the ZAC repo via `git show origin/main:<file>`.
  - Check the session runs in the ZAC repo: `git remote get-url origin` must point to `infonl/dimpact-zaakafhandelcomponent`. If not, ask the user to start Claude Code in their ZAC checkout.
  - Run `git fetch origin main` first, so `origin/main` is current. It only updates the remote ref; it never touches the user's branch or working tree.
- For the table tasks: ask the user to paste the current table from Confluence if they did not, so you can report what changed. It is optional; continue without it if they say so. The pasted table is only for comparing: fetch every value live from the sources on the task page and never copy a value from it.
- The output is only rendered (copyable into Confluence) in the VS Code extension or desktop app. You cannot detect where the session runs, so mention this once in every run.

## Rules for every task

- Read-only. The user's `gh` login may have full rights: run only the read calls the task page lists. Never push, edit files, open PRs or touch other repos. When done, tell the user they can log out (`gh auth logout -h github.com`).
- Always state where the values came from (tag, branch, file or PRs) in one line, so the user can verify them.

## Output for the table tasks

The Samenvatting has its own output rules on its page.

- ONE rendered markdown table, NOT in a code block, so it can be copied straight into Confluence.
- ONLY the version columns of that table, rows in the fixed order of the task page, no component-name column. Fill ALL of them with current values, including Our Version on TEST.
- Below the table, max 4 short lines (the source line and the logout reminder do not count): what changed compared to the table the user pasted (if any), remarks in the pasted table that are now outdated, and values that could not be verified.

## Tooling notes

- `gh` on macOS needs the sandbox disabled, otherwise the keychain is blocked and gh fails with `x509: OSStatus -26276`.
- Do not assume `yq`, `helm` or PyYAML are installed. Read YAML paths by indentation with `grep`/`awk`.
- `git fetch` over SSH also needs the sandbox disabled; otherwise it fails with `This proxy requires authentication` and `origin/main` stays stale.
- The anonymous GitHub API allows only 60 requests/hour. Use `gh api` or the sources named on the task page instead of paging through GitHub tags.
