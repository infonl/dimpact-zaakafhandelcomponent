# Update the sprint hero page with Claude Code

The `sprint-hero-page` skill fills the version sections and the summary of the sprint hero page in Confluence. It reads from GitHub, Docker Hub, release notes, download pages and the ZAC repo, among other sources. It changes nothing except refreshing your local `origin/main` ref and, after your yes, the version table on the hero page and the Renovate cap pull requests; it never touches your branch or your working tree.

## What you need

- Claude Code in the VS Code extension or the desktop app, started in your ZAC checkout. In a plain terminal the table shows as raw text and does not paste well.
- `gh`, logged in with `gh auth login -h github.com -w` (choose SSH, then Skip). Externe API's needs no login. The other tasks need:
  - PodiumD componenten and Supporting componenten: read access to `infonl/dimpact-provisioning`.
  - Security issues: write access to the ZAC repo and `infonl/webdav-servlet`, which GitHub requires to read code-scanning alerts.
  - Samenvatting: read access to `infonl/dimpact-provisioning` and `infonl/webdav-servlet`, plus write access to the ZAC repo, which GitHub requires to read its code-scanning alerts.
- Python 3 with the packages of `scripts/python/requirements.txt`, for PodiumD componenten, Supporting componenten and Externe API's. Their Latest column comes from `scripts/python/dependencies/versions.py` and `supporting_componenten.py`.
- If access is missing, the skill stops and tells you what to do.

## Usage

Type `/sprint-hero-page <task>`, or just `/sprint-hero-page` and it asks which one:

- `PodiumD componenten`: Latest, the PodiumD releases and our TEST version
- `Supporting componenten`: Latest and our TEST version
- `Externe API's`: KVK and BRP, Latest and our TEST version
- `Security issues in component repositories`: Open, New and Closed code-scanning alerts per severity for ZAC and WebDAV
- `Samenvatting`: at most 3 bullets on components and security in this sprint; the WebDAV bullet only appears when there is a WebDAV fix (asks for the sprint start date)

For the table tasks the skill reads the current table from the hero page itself, so it can report what changed and which remarks on the page are outdated. For PodiumD componenten the page also sets which PodiumD releases become columns.

## Put the result in Confluence

- For the three version tables, the skill then also updates that table on the hero page. Without a link it finds the newest "Sprint <n> - Hero" page and asks you to confirm it.
- It shows every cell that changes and writes only after your yes: the version columns, "Last checked" (today, only when the whole row was verified) and, in Supporting componenten and Externe API's, the colour of "Our Version on TEST" according to the legend. It changes nothing else, and Confluence keeps the previous version in the page history.
- Still by hand: the Remarks / Stories (the skill names the outdated ones) and the colours in the PodiumD table (ask Rovo, see the help panel above that table).
- Writing needs an Atlassian API token: `ATLASSIAN_EMAIL` and `ATLASSIAN_API_TOKEN`, or the macOS Keychain item `atlassian-api-token`. Without one, copy the table Claude returns into the page and let Rovo update the cell colours and "Last checked" (see the instruction with each table).

## Raise the Renovate caps

After PodiumD componenten and Supporting componenten, the skill checks the `allowedVersions` caps in `renovate.json` of the ZAC repo and the provisioning repo against the newest PodiumD release. When a component of that release is above its cap, it offers one pull request per repo that raises the cap to the next minor of that version (PodiumD 4.3.0 gives `< 4.4.0`, the style of the other caps), so Renovate opens the update. It asks for the hero ticket of this sprint (`Solves PZ-XXXX`) and creates the pull requests only after your yes; merging stays with you. It raises the caps of the Docker images (PodiumD components, Redis, PABC) and of the Helm charts that install them (versions from the PodiumD `Chart.yaml`), so a new image comes with the chart PodiumD uses for it; never Open Archiefbeheer or the Bitnami Redis chart.

- Log out when you are done: `gh auth logout -h github.com`. The skill reminds you.

## Good to know

- "Our Version on TEST" comes from the pins in the repos (the provisioning repo, the ZAC repo and the Dimpact `monitoring-logging` chart), not from the running cluster. The Samenvatting does check whether the provisioning workflow ran after the merge.
- A PodiumD release without a tag comes from its feature branch and can still change. The skill says so.
- The skill states which sources it used. If in doubt, check there.

## Maintenance

- The skill lives in `.claude/skills/sprint-hero-page/`: `SKILL.md` (prerequisites and general rules) plus one page per task. This README is for people; Claude does not use it.
- Do not put version numbers in it. The skill always reads them live; fixed versions go stale and send Claude the wrong way.
- When a source changes (another repo, another file, a new row on the page), update that task's page and run the task once to check.
