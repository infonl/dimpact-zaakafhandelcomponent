---
name: sprint-hero-page
description: Sprint hero tasks for the ZAC team. Use when the user asks to update the sprint hero page in Confluence - the version tables PodiumD componenten (latest, PodiumD releases, our TEST versions), Supporting componenten (latest, our version) and Externe API's (KVK, BRP - latest, our version on TEST), or the Samenvatting (Dutch sprint summary of component versions and security fixes). The version table tasks also update their table in the Confluence page.
---

# Sprint hero page

Each task fills one section of the sprint hero page in Confluence. Pick the task the user asks for and follow its page exactly.

| Task (= Confluence heading) | Page |
|---|---|
| PodiumD componenten | [podiumd-componenten.md](podiumd-componenten.md) |
| Supporting componenten | [supporting-componenten.md](supporting-componenten.md) |
| Externe API's | [externe-apis.md](externe-apis.md) |
| Security issues in component repositories | [security-issues.md](security-issues.md) |
| Samenvatting | [samenvatting.md](samenvatting.md) |

## Before you start

If the user did not name a task, ask which one: PodiumD componenten, Supporting componenten, Externe API's, Security issues in component repositories or Samenvatting.

Check the prerequisites yourself; when one is missing, ask the user to fix it and give the exact command. Continue once they confirm.

- PodiumD componenten, Supporting componenten and Samenvatting read the PRIVATE repo `infonl/dimpact-provisioning`. Check with `gh api repos/infonl/dimpact-provisioning --jq .full_name`.
  - Not logged in: ask the user to run `gh auth login -h github.com -w` (choose SSH, then Skip for the key upload).
  - 404/403: the token has no access to that repo, or awaits infonl org approval. Ask the user to fix the token. Never guess those values.
- Externe API's needs no GitHub login.
- PodiumD componenten, Supporting componenten and Externe API's get Latest from the Python scripts in `scripts/python/dependencies/`. They need `requests` and `beautifulsoup4`: check with `python3 -c "import requests, bs4"`. If that fails, ask the user to run `pip3 install -r scripts/python/requirements.txt`, or to activate the virtual environment of `scripts/python/init-pyenv.sh` before starting Claude Code.
- Security issues reads the code-scanning alerts of `infonl/dimpact-zaakafhandelcomponent` and `infonl/webdav-servlet`, which needs write access to both. Check with `gh api "repos/infonl/<repo>/code-scanning/alerts?per_page=1" --jq length`; a 403/404 means the token lacks access.
- Supporting componenten, Externe API's and Samenvatting read the ZAC repo via `git show origin/main:<file>`.
  - Check the session runs in the ZAC repo: `git remote get-url origin` must point to `infonl/dimpact-zaakafhandelcomponent`. If not, ask the user to start Claude Code in their ZAC checkout.
  - Run `git fetch origin main` first, so `origin/main` is current. It only updates the remote ref; it never touches the user's branch or working tree.
- For the table tasks: read the current table from the Confluence page (step 1 of [confluence.md](confluence.md)), so you can report what changed. It is only for comparing: fetch every value live from the sources on the task page and never copy a value from it. When the page cannot be read (no API token and `acli` not logged in), ask the user to paste the table instead (optional) and, for PodiumD componenten, which PodiumD releases to show; the task then gives the table without updating Confluence.
- Updating the table in Confluence needs an Atlassian API token: `ATLASSIAN_EMAIL` and `ATLASSIAN_API_TOKEN`, or the macOS Keychain item `atlassian-api-token`. Without one, the table task still gives the table; the user pastes it as before.
- The output is only rendered (copyable into Confluence) in the VS Code extension or desktop app. You cannot detect where the session runs, so mention this once in every run.

## Rules for every task

- Read-only, except as listed below. The user's `gh` login may have full rights: run only the read calls the task page lists. Never push, edit files, open PRs or touch other repos in any other way. When done, tell the user they can log out (`gh auth logout -h github.com`).
- The only write: updating the table of the current task in Confluence with `hero_page_tables.py apply` (see [confluence.md](confluence.md)), only after the user confirmed its change list. Never write to Confluence or GitHub in any other way.
- Always state where the values came from (tag, branch, file or PRs) in one line, so the user can verify them.

## Output for the table tasks

The Samenvatting and Security issues have their own output rules on their page.

- ONE rendered markdown table, NOT in a code block, so it can be copied straight into Confluence.
- A first column with the component name (the row names of the task page), then the version columns of that table, rows in the fixed order of the task page. Fill ALL version columns with current values, including Our Version on TEST.
- Below the table, max 4 short lines (the source line and the logout reminder do not count): what changed compared to the table on the page, remarks on the page that are now outdated, and values that could not be verified.
- Then update the table in Confluence: follow [confluence.md](confluence.md).

## Tooling notes

- `gh` on macOS needs the sandbox disabled, otherwise the keychain is blocked and gh fails with `x509: OSStatus -26276`.
- Do not assume `yq`, `helm` or PyYAML are installed. Read YAML paths by indentation with `grep`/`awk`.
- `git fetch` over SSH also needs the sandbox disabled; otherwise it fails with `This proxy requires authentication` and `origin/main` stays stale.
- Run the Python scripts with `python3 <path>`, not as `./<path>`: the shebang of `versions.py` names `python`, which does not exist outside a virtual environment on macOS. Run them with the sandbox disabled: they need the network, and `supporting_componenten.py` reads the `gh` token from the keychain.
- The anonymous GitHub API allows only 60 requests/hour. Use `gh api` or the sources named on the task page instead of paging through GitHub tags.
