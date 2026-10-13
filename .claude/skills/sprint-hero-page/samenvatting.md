# Samenvatting

A short sprint summary of component versions and security, in Dutch, for a non-technical audience. Not about functional changes.

## Sprint period

Ask the user for the sprint start date. If they leave it open, use the last 14 days and say so.

## Sources

Read-only, via `gh`. Repos: `infonl/dimpact-zaakafhandelcomponent` (ZAC), `infonl/dimpact-provisioning` (TEST), `infonl/webdav-servlet` (WebDAV).

1. Componenten: merged PRs in `infonl/dimpact-provisioning` in the sprint (`gh pr list -R infonl/dimpact-provisioning --state merged --search "merged:>=<start>" --limit 1000`), including Renovate PRs. Each `update <image> docker tag to vX` or `upgrade <component> to X` changes the version TEST gets on its next deploy. The provisioning workflows only run manually, so check per component whether it was deployed: the last successful run of `azure-provision-<x>.yml` (`gh run list -R infonl/dimpact-provisioning -w azure-provision-<x>.yml --status success --limit 1 --json createdAt`) must be later than the PR's `mergedAt`.
2. Beveiliging ZAC:
   - Fixed code-scanning alerts: `gh api "repos/infonl/dimpact-zaakafhandelcomponent/code-scanning/alerts?state=fixed&per_page=100&page=<n>&sort=updated"`, keep `fixed_at >= <start>`. Count unique `rule.id` per `rule.security_severity_level` (one CVE found in several jars counts once).
   - Merged ZAC PRs in the sprint with `fix(helm)`, `security`, `CVE` or `vulnerab` in the title, for container hardening (`gh pr list -R infonl/dimpact-zaakafhandelcomponent --state merged --search "merged:>=<start>" --limit 1000`).
3. Beveiliging WebDAV: merged PRs in `infonl/webdav-servlet` in the sprint, except Renovate/chore (`gh pr list -R infonl/webdav-servlet --state merged --search "merged:>=<start>" --limit 1000`). Read each fix PR's body and merge commit (`gh pr view <number> -R infonl/webdav-servlet --json body,mergeCommit`). Check whether ZAC already contains it: take ZAC's pinned version (`webdav-servlet` in `git show origin/main:gradle/libs.versions.toml`) and run `gh api repos/infonl/webdav-servlet/compare/<merge-commit>...v<zac-version> --jq .status`. `ahead` or `identical` = included; `behind` or `diverged` = not yet.

## Output

Plain rendered text, NOT in a code block, so the user can paste it into Confluence. Title `**Sprintoverzicht: componenten en beveiliging**`, then max 3 bullets, each max 22 words, in this order:

1. **Componenten:** deployed components: `TEST is bijgewerkt naar <component> <versie>, …`; merged but not yet deployed: `klaar voor de volgende TEST-deployment: <component> <versie>, …`. Priority: Keycloak, Open Formulieren, PABC, Open Zaak, Open Klant, Open Notificaties, Open Object, Open Archiefbeheer. Leave out monitoring (Grafana, Loki, Tempo, Fluent-bit, monitoring chart) and infrastructure (nginx, Redis, BRP mock) unless nothing else changed.
2. **Beveiliging ZAC:** `<n> kritieke en <n> hoge kwetsbaarheden opgelost; <container hardening in one phrase>`. Skip a part that has nothing.
3. **Beveiliging WebDAV:** only if there is a WebDAV fix. Say whether ZAC already contains it (from the compare check), not just whether the version number is higher.

Shape:

**Sprintoverzicht: componenten en beveiliging**

- **Componenten:** TEST is bijgewerkt naar `<component> <versie>`, …; klaar voor de volgende TEST-deployment: `<component> <versie>` (only if any)
- **Beveiliging ZAC:** `<n>` kritieke en `<n>` hoge kwetsbaarheden opgelost; `<container hardening>`.
- **Beveiliging WebDAV:** `<wat er is verbeterd>` (preventief, if hardening); ZAC gebruikt de nieuwe versie `<versie>` al.

## Do not overstate

- Read the PR body before calling something a fixed leak. Hardening (the risk was already blocked elsewhere) is "extra controle (preventief)", not "lek gedicht".
- Below the summary, in one line for the user only (not for the page): how many critical/high fixes were in build or test tooling, not in production.
