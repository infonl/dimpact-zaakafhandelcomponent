# Samenvatting

A short sprint summary of component versions and security, in Dutch, for a non-technical audience. Not about functional changes.

## Sprint period

Ask the user for the sprint start date. If they leave it open, use the last 14 days and say so.

## Sources

Read-only, via `gh`. Repos: `infonl/dimpact-zaakafhandelcomponent` (ZAC), `infonl/dimpact-provisioning` (TEST), `infonl/webdav-servlet` (WebDAV).

1. Componenten: merged PRs in `infonl/dimpact-provisioning` in the sprint (`gh pr list --state merged --search "merged:>=<start>"`), including Renovate PRs. Each `update <image> docker tag to vX` or `upgrade <component> to X` is a new version on TEST.
2. Beveiliging ZAC:
   - Fixed code-scanning alerts: `gh api "repos/infonl/dimpact-zaakafhandelcomponent/code-scanning/alerts?state=fixed&per_page=100&page=<n>&sort=updated"`, keep `fixed_at >= <start>`. Count unique `rule.id` per `rule.security_severity_level` (one CVE found in several jars counts once).
   - Merged ZAC PRs in the sprint with `fix(helm)`, `security`, `CVE` or `vulnerab` in the title, for container hardening.
3. Beveiliging WebDAV: merged PRs in `infonl/webdav-servlet` in the sprint, except Renovate/chore. Read each fix PR body (`gh pr view`), and check whether ZAC already uses the release containing it (`webdav-servlet` in `git show origin/main:gradle/libs.versions.toml`, against `gh release list -R infonl/webdav-servlet`).

## Output

Plain rendered text, NOT in a code block, so the user can paste it into Confluence. Title `**Sprintoverzicht: componenten en beveiliging**`, then max 3 bullets, each max 22 words, in this order:

1. **Componenten:** `TEST draait nu op <component> <versie>, …`. Priority: Keycloak, Open Formulieren, PABC, Open Zaak, Open Klant, Open Notificaties, Open Object, Open Archiefbeheer. Leave out monitoring (Grafana, Loki, Tempo, Fluent-bit, monitoring chart) and infrastructure (nginx, Redis, BRP mock) unless nothing else changed.
2. **Beveiliging ZAC:** `<n> kritieke en <n> hoge kwetsbaarheden opgelost; <container hardening in one phrase>`. Skip a part that has nothing.
3. **Beveiliging WebDAV:** only if there is a WebDAV fix. Say whether ZAC already uses the new version.

Shape:

**Sprintoverzicht: componenten en beveiliging**

- **Componenten:** TEST draait nu op `<component> <versie>`, `<component> <versie>`, …
- **Beveiliging ZAC:** `<n>` kritieke en `<n>` hoge kwetsbaarheden opgelost; `<container hardening>`.
- **Beveiliging WebDAV:** `<wat er is verbeterd>` (preventief, if hardening); ZAC gebruikt de nieuwe versie `<versie>` al.

## Do not overstate

- Read the PR body before calling something a fixed leak. Hardening (the risk was already blocked elsewhere) is "extra controle (preventief)", not "lek gedicht".
- Below the summary, in one line for the user only (not for the page): how many critical/high fixes were in build or test tooling, not in production.
