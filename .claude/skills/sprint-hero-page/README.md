# Update the sprint hero page with Claude Code

The `sprint-hero-page` skill fills the version sections and the summary of the sprint hero page in Confluence. It reads from GitHub, Docker Hub, release notes, download pages and the ZAC repo, among other sources. It changes nothing except refreshing your local `origin/main` ref; it never touches remote repositories, your branch or your working tree.

## What you need

- Claude Code in the VS Code extension or the desktop app, started in your ZAC checkout. In a plain terminal the table shows as raw text and does not paste well.
- `gh`, logged in with `gh auth login -h github.com -w` (choose SSH, then Skip). Externe API's needs no login. The other tasks need:
  - PodiumD componenten and Supporting componenten: read access to `infonl/dimpact-provisioning`.
  - Samenvatting: read access to `infonl/dimpact-provisioning` and `infonl/webdav-servlet`, plus write access to the ZAC repo, which GitHub requires to read its code-scanning alerts.
- If access is missing, the skill stops and tells you what to do.

## Usage

Type `/sprint-hero-page <task>`, or just `/sprint-hero-page` and it asks which one:

- `PodiumD componenten`: Latest, the PodiumD releases and our TEST version
- `Supporting componenten`: Latest and our TEST version
- `Externe API's`: KVK and BRP, Latest and our TEST version
- `Samenvatting`: at most 3 bullets on components and security in this sprint; the WebDAV bullet only appears when there is a WebDAV fix (asks for the sprint start date)

Tip for the table tasks: paste the current table from Confluence. The skill then reports what changed and which remarks on the page are outdated. For PodiumD componenten the pasted table also sets which PodiumD releases become columns; without it, the skill asks which releases to show.

## Put the result in Confluence

- Copy the table Claude returns and paste it over the version columns. It contains only the version columns, in the order of the page.
- Then let Rovo update the cell colours and "Last checked" (see the instruction with each table).
- Log out when you are done: `gh auth logout -h github.com`. The skill reminds you.

## Good to know

- "Our Version on TEST" comes from the pins in the repos (the provisioning repo, the ZAC repo and the Dimpact `monitoring-logging` chart), not from the running cluster. The Samenvatting does check whether the provisioning workflow ran after the merge.
- A PodiumD release without a tag comes from its feature branch and can still change. The skill says so.
- The skill states which sources it used. If in doubt, check there.

## Maintenance

- The skill lives in `.claude/skills/sprint-hero-page/`: `SKILL.md` (prerequisites and general rules) plus one page per task. This README is for people; Claude does not use it.
- Do not put version numbers in it. The skill always reads them live; fixed versions go stale and send Claude the wrong way.
- When a source changes (another repo, another file, a new row on the page), update that task's page and run the task once to check.
