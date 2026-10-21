# PodiumD componenten

Rows (fixed order): Open Formulieren, Open Archiefbeheer, Open Object, Open Klant, Open Notificaties, Keycloak, Open Zaak.

Columns: Latest version | one column per PodiumD release in the user's table | Our Version on TEST. If the user pasted no table, ask which PodiumD releases to show.

## 1. Latest version

Docker Hub: start with `https://hub.docker.com/v2/repositories/<image>/tags?page_size=100&ordering=last_updated` and follow each response's `next` URL until it is null. Keep only `X.Y.Z` tags from all pages and take the highest by number.

- Open Formulieren: `openformulieren/open-forms`
- Open Archiefbeheer: `maykinmedia/open-archiefbeheer`
- Open Object: `maykinmedia/open-object`
- Open Klant: `maykinmedia/open-klant`
- Open Notificaties: `openzaak/open-notificaties`
- Open Zaak: `openzaak/open-zaak`
- Keycloak: `gh api repos/keycloak/keycloak/releases/latest`

## 2. PodiumD releases

Public repo `Dimpact-Samenwerking/helm-charts`, file `charts/podiumd/values.yaml`.

- List all tags via `gh api --paginate "repos/Dimpact-Samenwerking/helm-charts/tags?per_page=100" --jq '.[].name'` and all branches via `gh api --paginate "repos/Dimpact-Samenwerking/helm-charts/branches?per_page=100" --jq '.[].name'`.
- Read the selected ref via `https://raw.githubusercontent.com/Dimpact-Samenwerking/helm-charts/<tag-or-branch>/charts/podiumd/values.yaml`.

- Per PodiumD release `X.Y` (from the user's table): use the highest tag that matches exactly `^podiumd-X\.Y\.[0-9]+$`, compared by number (`sort -V`). Ignore tags with a suffix, such as `…-snapshot`. If there is no such tag yet, use the branch named exactly `feature/podiumd-X.Y.0` (ignore `feature/podiumd-X.Y.0-*` and other variants) and say the column is unreleased and can still change. If that exact branch does not exist either, show `?` and say so.
- Per top-level section, the app image is the `image:` block directly under the section (2-space indent). Take its uncommented `tag:`, whether or not that block has a `repository:` line (the subchart often sets it); strip `@sha256…`. Ignore deeper nested `image:` blocks (nginx, redis, exporters, keycloak-config-cli, …).
- Sections: `openformulieren` (the image is pinned here, not under `openforms`), `openarchiefbeheer`, `openklant`, `opennotificaties`, `openzaak`, `keycloak` (may use the YAML anchor `&keycloakImageVersion`; its value is the version). Never use the keycloak-config-cli tag, even when its comments mention a Keycloak version.
- Open Object: `-` unless the `objecten` image block has `repository: maykinmedia/open-object`; then its tag.

## 3. Our Version on TEST

PRIVATE repo `infonl/dimpact-provisioning`, branch `main`, via `gh api repos/infonl/dimpact-provisioning/contents/<path> --jq .content | base64 -d`.

- Default image pin in `.github/workflows/azure-provision-<x>.yml` for `open-formulieren`, `archiefbeheer`, `open-object`, `open-klant`, `open-notificaties`, `open-zaak`. Grep for `_docker_image_override:`; the line is `<prefix>_docker_image_override: ${{ inputs.tag || '<VERSION>@sha256…' }}`, with prefixes `openformulieren`, `openarchiefbeheer`, `openobject`, `openklant`, `opennotificaties`, `openzaak`.
- Keycloak: the version in the last `FROM` line of `containers/keycloak/Dockerfile` (the first one is the builder stage).
- These are the pins for the next deploy; a manual run with a tag can override them.
