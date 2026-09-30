## Context

`docker-compose.yaml` pins the upstream PABC images (`linux/amd64` only) by tag and digest. `docker-compose.arm64-override.yaml` swaps them for arm64 builds that INFO publishes under `ghcr.io/infonl/pabc-*` with a `-arm64` tag suffix. Renovate tracks both sets of images, plus the versions in `DEPENDENCIES.md`.

Changes in PABC `2.1.0-prerelease` compared with `2.0.0-prerelease` (release notes and `compare/v2.0.0-prerelease...v2.1.0-prerelease`):
- A text filter in the admin UI.
- A startup pre-fill of applications and application roles in `pabc-migrations`. It only runs when `PREFILL_PATH` is set.
- The optional setting `KeycloakAdmin__ExcludedRoles__N` on `pabc-api`.
- No changes to the PABC OpenAPI spec, the dataset schema, or the database migrations that ZAC depends on.

## Goals / Non-Goals

**Goals:**
- The local stack, on both amd64 and arm64, and the integration tests run PABC `2.1.0-prerelease`.

**Non-Goals:**
- Using the PABC pre-fill.
- Configuring PABC outside Docker Compose. The `ExcludedRoles` values apply to the local stack only, and ZAC's Helm chart and other environments are unchanged.

## Decisions

**Pin the new images by digest, as today.** Digests of the multi-platform indexes:
- `pabc-migrations:2.1.0-prerelease`: `sha256:e444442e21bc85b99f5a39825f84bc2c73523b4867d9712e17f83807527e6641`
- `pabc-api:2.1.0-prerelease`: `sha256:632dde894d86a9f1aa2dff0224fa5a67010e2d16c894a7898320f2ce313c2df9`

Keep `platform: linux/amd64`. Upstream still publishes only amd64 plus an attestation manifest.

**Configure `KeycloakAdmin__ExcludedRoles__0..2` on `pabc-api`** with `offline_access`, `uma_authorization` and `default-roles-zaakafhandelcomponent`. Without this setting, the functional role import in the local PABC offers the Keycloak technical roles as functional roles. The release notes recommend excluding them, using `default-roles-<REALM_NAME>` for the default role. For the `zaakafhandelcomponent` realm, that name is `default-roles-zaakafhandelcomponent`.
- Alternative: leave the setting out. It is optional, but then the local functional role import offers technical roles that are never meant to become functional roles. Rejected.

**Upgrade the arm64 override to `ghcr.io/infonl/pabc-*:2.1.0-prerelease-arm64`**, pinned by digest. Both are single-platform `linux/arm64` manifests:
- `pabc-migrations:2.1.0-prerelease-arm64`: `sha256:66cc8e376cb2581c03e8e81e80cf9ceaaa7f76158f1889ce3b77124f11a9cc6c`
- `pabc-api:2.1.0-prerelease-arm64`: `sha256:708df0e425f2fe035ff770292e632a025b1e96b4121214f265f398d72992a934`

The override only replaces `image` and `platform`, so arm64 machines get the new `ExcludedRoles` environment from `docker-compose.yaml` too.
- Alternative: remove the PABC entries from the override, so arm64 machines run the amd64 images under emulation. Rejected: the override exists because emulation is slow or unreliable for these images.

**Do not set `PREFILL_PATH`.** `pabc-migrations` already runs with `JSON_DATASET_PATH`, which replaces all data with the full dataset, including applications and application roles. A pre-fill would add nothing.

## Risks / Trade-offs

- [Risk] The infonl arm64 images are built separately from upstream and could differ from the amd64 build. Mitigation: they are built from the same `v2.1.0-prerelease` tag, and task 3.1 starts the stack on arm64.
- [Risk] A prerelease can have regressions that the release notes do not mention. Mitigation: the full integration test suite runs against the new images in CI. Rollback is reverting the image lines.

## Migration Plan

No data migration. PABC data in the local stack is re-seeded from `pabc-mapping-data.json` on every start. Rollback: revert the commit.
