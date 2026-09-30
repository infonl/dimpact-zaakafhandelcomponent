## Why

The local Docker Compose stack, which the integration tests also use, runs PABC `2.0.0-prerelease`. PABC `2.1.0-prerelease` came out on 2026-09-24. Upgrading lets local development and the integration tests use the new PABC version.

## What Changes

- Upgrade the `pabc-migrations` and `pabc-api` images in `docker-compose.yaml` from `2.0.0-prerelease` to `2.1.0-prerelease`, pinned by digest.
- Configure the new optional `KeycloakAdmin__ExcludedRoles__N` setting on the Docker Compose `pabc-api` service only, with the values `offline_access`, `uma_authorization` and `default-roles-zaakafhandelcomponent`, so these technical Keycloak roles are not imported as functional roles. The values follow the `default-roles-<REALM_NAME>` pattern from PABC's release notes. The setting does not apply to any other environment.
- Update the PABC versions in `DEPENDENCIES.md`.
- Upgrade the arm64 override (`docker-compose.arm64-override.yaml`) from `ghcr.io/infonl/pabc-*:2.0.0-prerelease-arm64` to `2.1.0-prerelease-arm64`, pinned by digest, so arm64 machines run the same PABC version.
- The new startup pre-fill of applications and application roles (`PREFILL_PATH`) is not used. The local stack already loads the full dataset through `JSON_DATASET_PATH`, which includes the applications and application roles.

## Capabilities

### New Capabilities
- `pabc-docker-compose`: which PABC version the local Docker Compose stack runs, and how PABC is configured in it.

### Modified Capabilities
<!-- none -->

## Impact

- `docker-compose.yaml` and `docker-compose.arm64-override.yaml`: `pabc-migrations` and `pabc-api` services.
- `DEPENDENCIES.md`: PABC versions.
- Integration tests: run against PABC `2.1.0-prerelease`, because they start the Docker Compose stack. The PABC OpenAPI spec and the dataset schema did not change between `2.0.0-prerelease` and `2.1.0-prerelease`, so the PABC client in ZAC and `pabc-mapping-data.json` need no changes.
- ZAC application code and Helm charts: no changes.
