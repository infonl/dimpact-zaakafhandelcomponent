## 1. Docker Compose

- [x] 1.1 In `docker-compose.yaml`, set `pabc-migrations` to `ghcr.io/platform-autorisatie-beheer-component/pabc-migrations:2.1.0-prerelease@sha256:e444442e21bc85b99f5a39825f84bc2c73523b4867d9712e17f83807527e6641`. Verify with `docker compose config | grep pabc-migrations:`.
- [x] 1.2 In `docker-compose.yaml`, set `pabc-api` to `ghcr.io/platform-autorisatie-beheer-component/pabc-api:2.1.0-prerelease@sha256:632dde894d86a9f1aa2dff0224fa5a67010e2d16c894a7898320f2ce313c2df9`. Verify with `docker compose config | grep pabc-api:`.
- [x] 1.3 Add `KeycloakAdmin__ExcludedRoles__0: "offline_access"`, `KeycloakAdmin__ExcludedRoles__1: "uma_authorization"` and `KeycloakAdmin__ExcludedRoles__2: "default-roles-zac"` to the `pabc-api` environment. Verify with `docker compose config`, which should show the three variables.
- [x] 1.4 In `docker-compose.arm64-override.yaml`, set `pabc-migrations` to `ghcr.io/infonl/pabc-migrations:2.1.0-prerelease-arm64@sha256:66cc8e376cb2581c03e8e81e80cf9ceaaa7f76158f1889ce3b77124f11a9cc6c` and `pabc-api` to `ghcr.io/infonl/pabc-api:2.1.0-prerelease-arm64@sha256:708df0e425f2fe035ff770292e632a025b1e96b4121214f265f398d72992a934`. Verify with `docker compose -f docker-compose.yaml -f docker-compose.arm64-override.yaml config | grep pabc-`.

## 2. Documentation

- [x] 2.1 Update `pabc-migrations` and `pabc-api` in `DEPENDENCIES.md` to `2.1.0-prerelease`. Verify that the Renovate regex in `renovate.json` still matches the lines.
- [x] 2.2 Add the current year to the SPDX header of each changed file that has one, following the CLAUDE.md rules. Verify with `git diff`.

## 3. Verification

- [x] 3.1 Start the stack from a clean state with `./stop-docker-compose.sh` followed by `./start-docker-compose.sh`, on an arm64 machine (which uses the override) and, through CI, on amd64. Verify that `pabc-migrations` exits with code 0 and that `curl -s -o /dev/null -w '%{http_code}' http://localhost:8006/health` (or the equivalent PABC health endpoint) returns 200.
- [x] 3.2 In the local PABC UI, import the functional roles from Keycloak. Verify that `offline_access`, `uma_authorization` and `default-roles-zac` are not offered or imported.
- [x] 3.3 Build the ZAC Docker image with `./gradlew buildDockerImage`, then run `./gradlew itest`. Verify that all integration tests pass, including the PABC-dependent ones such as `SearchRestServiceLeesrechtTest`.
