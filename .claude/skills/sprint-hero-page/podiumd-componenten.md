# PodiumD componenten

Rows (fixed order): Open Formulieren, Open Archiefbeheer, Open Object, Open Klant, Open Notificaties, Keycloak, Open Zaak.

Columns: Latest version | one column per PodiumD release in the user's table (on 2026-10-12: PodiumD 4.8, 4.9, 4.10) | Our Version on TEST. If the user pasted no table, ask which PodiumD releases to show.

## 1. Latest version

Docker Hub: `https://hub.docker.com/v2/repositories/<image>/tags?page_size=100&ordering=last_updated`. Keep only `X.Y.Z` tags and take the highest by number (not the first in the list).

- Open Formulieren: `openformulieren/open-forms`
- Open Archiefbeheer: `maykinmedia/open-archiefbeheer`
- Open Object: `maykinmedia/open-object`
- Open Klant: `maykinmedia/open-klant`
- Open Notificaties: `openzaak/open-notificaties`
- Open Zaak: `openzaak/open-zaak`
- Keycloak: `gh api repos/keycloak/keycloak/releases/latest`

## 2. PodiumD releases

Public repo `Dimpact-Samenwerking/helm-charts`, file `charts/podiumd/values.yaml`, read via `https://raw.githubusercontent.com/Dimpact-Samenwerking/helm-charts/<tag-or-branch>/charts/podiumd/values.yaml`.

- Per PodiumD release `4.N`: use the highest `podiumd-4.N.x` tag. If there is no tag yet, use branch `feature/podiumd-4.N.0` and say the column is unreleased and can still change.
- Per top-level section, take the uncommented `tag:` that belongs to the app image itself; strip `@sha256…`. Sections hold more images (nginx, redis, exporters, config-cli, …), so pair each `tag:` with its `repository:` and never just take the first tag.
- Sections: `openformulieren` (the image is pinned here, not under `openforms`), `openarchiefbeheer`, `openklant`, `opennotificaties`, `openzaak`, `keycloak` (4.9+ uses the YAML anchor `&keycloakImageVersion`; its value is the version).
- Open Object: `-` while the release still uses `objecten` with image `maykinmedia/objects-api`; the `objecten` tag once that section uses image `maykinmedia/open-object`.

## 3. Our Version on TEST

PRIVATE repo `infonl/dimpact-provisioning`, branch `main`, via `gh api repos/infonl/dimpact-provisioning/contents/<path> --jq .content | base64 -d`.

- Default image pin in `.github/workflows/azure-provision-<x>.yml` for `open-formulieren`, `archiefbeheer`, `open-object`, `open-klant`, `open-notificaties`, `open-zaak`. Line: `<name>_docker_image_override: ${{ inputs.tag || '<VERSION>@sha256…' }}`.
- Keycloak: the `FROM` line in `containers/keycloak/Dockerfile`.
- These are the pins for the next deploy; a manual run with a tag can override them.
