# PodiumD componenten versions columns

Update the version columns of the PodiumD components table on the sprint hero page. Read-only: never push, edit or open PRs anywhere.

Rows (fixed order): Open Formulieren, Open Archiefbeheer, Open Object, Open Klant, Open Notificaties, Keycloak, Open Zaak, Office Add-in.

## Output

ONE rendered markdown table (not in a code block) with ONLY these 5 columns: Latest version | PodiumD 4.8 | PodiumD 4.9 | PodiumD 4.10 | Our Version on TEST. Rows in the fixed order, no component-name column, so the user can paste it straight into the page. Below it, max 3 short lines: what changed since the previous table (if the user pasted one), which tags/branch were used, and which values are unverified.

## 1. Latest version

Public GitHub API, no auth needed.

- Use `/tags?per_page=100&page=1..5`, keep only `X.Y.Z` tags, sort numerically. The API sorts alphabetically, so never take the first tag. Maykin repos have no GitHub releases.
- Repos: `open-formulieren/open-forms`, `maykinmedia/open-archiefbeheer`, `maykinmedia/open-object` (`objects-api` redirects there), `maykinmedia/open-klant`, `open-zaak/open-notificaties`, `open-zaak/open-zaak`, `keycloak/keycloak` (`/releases`, skip `nightly`).
- Office Add-in: latest non-prerelease `vX.Y.Z` from `infonl/zgw-office-addin` `/releases` (new numbering, e.g. 0.12.0). Ignore the old `0.9.x` tags and the `zgw-office-addin-0.0.x` chart tags.

## 2. PodiumD 4.8 / 4.9 / 4.10

Public repo `Dimpact-Samenwerking/helm-charts`, file `charts/podiumd/values.yaml`.

- Use the highest `podiumd-4.8.x` and `podiumd-4.9.x` tag. If there is no `podiumd-4.10.x` tag yet, use branch `feature/podiumd-4.10.0` and say it is unreleased. Report which tags/branch you used.
- Image version = the `tag:` under each top-level section; strip `@sha256…`. Sections: `openformulieren` (NOT `openforms`; not pinned there, the subchart default is wrong), `openarchiefbeheer`, `openklant`, `opennotificaties`, `openzaak`, `keycloak` (4.9+ uses YAML anchor `&keycloakImageVersion`), `zgw-office-addin`.
- Open Object: `-` while that version still uses `objecten`/`objecttypen` (`maykinmedia/objects-api`); the version once section `objecten` uses `maykinmedia/open-object`.

## 3. Our Version on TEST

PRIVATE repo `infonl/dimpact-provisioning`, branch `main`, via `gh api`. Needs a token with Contents read on that repo.

- Default image pin in `.github/workflows/azure-provision-<x>.yml`, line `<name>_docker_image_override: ${{ inputs.tag || '<VERSION>@sha256…' }}`, for `open-formulieren`, `archiefbeheer`, `open-object`, `open-klant`, `open-notificaties`, `open-zaak`.
- Keycloak: the `FROM` line in `containers/keycloak/Dockerfile`.
- Office Add-in: the workflow deploys `latest`, so it cannot be determined. Keep the value from the user's previous table and mark it unverified.
- If gh returns 404/403: STOP and tell the user (token lacks access to the repo, or awaits infonl org approval). Never guess TEST values.
- These are the pins for the next deploy; a manual run with a tag can override them.

## Tooling notes

- `gh` on macOS needs the sandbox disabled, otherwise the keychain is blocked and gh fails with `x509: OSStatus -26276`.
- Network: `api.github.com`, `raw.githubusercontent.com`.
