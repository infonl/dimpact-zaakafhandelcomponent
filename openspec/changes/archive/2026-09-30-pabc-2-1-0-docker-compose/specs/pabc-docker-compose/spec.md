## Purpose

Defines which PABC version the local Docker Compose stack runs, and how PABC is configured in it. The integration tests use this stack too.

## ADDED Requirements

### Requirement: Docker Compose runs PABC 2.1.0-prerelease
The `docker-compose.yaml` `pabc-migrations` and `pabc-api` services SHALL use the `ghcr.io/platform-autorisatie-beheer-component/pabc-migrations` and `ghcr.io/platform-autorisatie-beheer-component/pabc-api` images at tag `2.1.0-prerelease`, each pinned by its image digest. The `docker-compose.arm64-override.yaml` `pabc-migrations` and `pabc-api` services SHALL use `ghcr.io/infonl/pabc-migrations` and `ghcr.io/infonl/pabc-api` at tag `2.1.0-prerelease-arm64`, each pinned by its image digest. `DEPENDENCIES.md` SHALL list `2.1.0-prerelease` for both images.

#### Scenario: Stack starts on the new PABC version
- **WHEN** a developer runs `./start-docker-compose.sh` on an amd64 machine from a clean state
- **THEN** `pabc-migrations` completes successfully and loads `pabc-mapping-data.json`
- **AND** `pabc-api` starts, answers on host port 8006, and runs image version `2.1.0-prerelease`

#### Scenario: Stack starts on the new PABC version on arm64
- **WHEN** a developer runs `./start-docker-compose.sh` on an arm64 machine from a clean state
- **THEN** `pabc-migrations` and `pabc-api` run the `linux/arm64` `2.1.0-prerelease-arm64` images, and `pabc-api` answers on host port 8006

#### Scenario: Integration tests pass against the new PABC version
- **WHEN** the integration tests run with `./gradlew itest`
- **THEN** they start `pabc-api` `2.1.0-prerelease`, and all tests that call PABC pass

### Requirement: Technical Keycloak realm roles are not imported as functional roles
The `pabc-api` service in `docker-compose.yaml` SHALL configure the Keycloak realm roles `offline_access`, `uma_authorization` and `default-roles-zaakafhandelcomponent` as excluded roles. When a functioneel beheerder imports functional roles from Keycloak in the PABC UI, PABC SHALL skip these roles.

#### Scenario: Importing functional roles from Keycloak
- **WHEN** a functioneel beheerder imports the Keycloak realm roles as functional roles in the local PABC
- **THEN** the realm's other roles are imported
- **AND** `offline_access`, `uma_authorization` and `default-roles-zaakafhandelcomponent` are not
