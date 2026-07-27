## 1. Spike: verify Podman compatibility

- [x] 1.1 Install Podman locally (Linux and macOS via `podman machine`) and run `docker-compose.yaml` unmodified via `podman compose up` for the default profile — **done on macOS (Apple Silicon)**: `podman-machine-default` (applehv, 8GiB/5 CPU after bumping from the default 2GiB) started, `./start-docker-compose.sh -e` brought up all 16 default-profile services unmodified via the `podman compose` → external `docker-compose` v5.3.1 provider path, all reaching `Started`/`Healthy`; `./stop-docker-compose.sh` tore it down cleanly. Not yet run on Linux.
- [x] 1.2 Verify inter-service hostnames (`openzaak-nginx`, `objecten-api.local`, etc.) resolve correctly under Podman's rootless networking — confirmed: `openzaak-nginx` resolves `openzaak.local` directly, `keycloak`/`pabc-api` resolve `keycloak-database`/`pabc-database` via the `.dns.podman` search domain; the published `openzaak-nginx` port answered `HTTP 200` from the host
- [x] 1.3 Run `docker-compose.arm64-override.yaml` under `podman machine` on Apple Silicon and confirm services start — confirmed: `DOCKER_USE_ARM64_CONTAINERS=true ./start-docker-compose.sh -e` brought up the same 16 services, all `Started`/`Healthy`
- [x] 1.4 Confirm `podman compose` supports `depends_on: condition: service_healthy` as used by `openformulieren-database` and other healthcheck-gated services — confirmed: observed the exact `Healthy` → dependent-`Starting` cascade (e.g. `openklant-database` Healthy → `openklant.local` Starting; `pabc-database` Healthy → `pabc-migrations` → `pabc-api`; `openzaak-database` Healthy → `openzaak.local` Healthy → `openzaak-nginx`) in both the default and ARM64-override runs. `openformulieren` profile itself not tested (requires 1Password-sourced secrets not available in this session).
- [x] 1.5 Document minimum required Podman version based on spike findings — validated with **Podman 6.0.2** (Homebrew) and its external **docker-compose v5.3.1** provider (`podman compose` shells out to `/usr/local/bin/docker-compose`, confirming design.md decision #3). The default `podman machine` allocation of **2GiB RAM was judged too small** for this stack (Solr + Keycloak + 4× Postgres + Open Zaak + Open Klant + PABC, etc.) and was bumped to **8GiB / 5 CPU** before testing — not stress-tested at 2GiB, so this is a recommendation, not a proven floor. Recommend documenting an 8GiB+ machine as the minimum in `docs/development/installDockerCompose.md`.

## 2. Image build

- [x] 2.1 Update `scripts/docker/build-docker-image.sh` to invoke `podman build` instead of `docker build`, keeping the same build args (`versionNumber`, `branchName`, `commitHash`) and tag
- [x] 2.2 Update the Gradle `buildDockerImage` task in `build.gradle.kts` (and any task naming/comments referencing Docker) to reflect the Podman-based script
- [ ] 2.3 Verify `./gradlew buildDockerImage` produces a working image using only Podman (no Docker installed) — needs a real build run; not executed in this session

## 3. Local development scripts

- [x] 3.1 Update `start-docker-compose.sh` to invoke `podman compose up` (including the ARM64 override selection logic)
- [x] 3.2 Update `stop-docker-compose.sh` to invoke `podman compose down`
- [x] 3.3 Update `start-e2e-with-local-env.sh` and `start-it-with-local-env.sh` to use Podman/`podman compose` (`start-e2e-with-local-env.sh` needed no change — it has no direct engine invocation)
- [x] 3.4 Update `scripts/docker-compose/setup-linux.sh` for Podman-specific setup steps (e.g. rootless socket activation)

## 4. Integration tests (TestContainers)

- [x] 4.1 Configure `DOCKER_HOST` (or `testcontainers.properties`) so TestContainers connects to the Podman-exposed Docker-API-compatible socket — documented in `docs/development/testing.md`; TestContainers honors `DOCKER_HOST` natively, no code change needed
- [x] 4.2 Default `TESTCONTAINERS_RYUK_DISABLED=true` when running under Podman in `ZacItestProjectConfig.kt`, and document why (rootless Podman can't run privileged Ryuk)
- [x] 4.3 Update the explicit Compose teardown path (`ZacItestProjectConfig.kt:254`) — this goes through TestContainers' API (`.stop()`), not a literal CLI string, so no command text to change; added a comment noting the `podman-docker` compatibility shim `ComposeContainer` relies on for its underlying `docker compose` executable lookup
- [ ] 4.4 Run `./gradlew itest` end-to-end against Podman and confirm all integration tests pass — needs a real Podman machine/CI run; not executed in this session

## 5. CI workflows

- [x] 5.1 Add a Podman installation step to `.github/workflows/trivy-docker-image-scan.yml` and switch its image build step to `podman build`
- [x] 5.2 Add a Podman installation step to `.github/workflows/build-test-deploy.yml`; replace `docker/setup-buildx-action`, `docker/build-push-action`, and `docker/login-action` with `podman build`, `podman login`, and `podman push` steps (note: lost the `type=gha` Buildx layer cache — plain `podman build` has no equivalent; and `docker push --all-tags` was replaced with a loop since Podman has no bulk-push flag)
- [x] 5.3 Update `.github/workflows/dependency-security-scan.yml` and `.github/workflows/publish-nvd-database-cache.yml` to use Podman where they build or reference the ZAC image
- [x] 5.4 Update `.github/workflows/helm-chart-testing.yml` if it depends on a local Docker image build — determined no change needed: it only uses `docker/login-action` to avoid Docker Hub rate limiting for the `kind` cluster it creates, and `kind` requires Docker (Podman provider is experimental/unsupported by `helm/kind-action`); this matches the design's non-goal of leaving Kubernetes/Helm chart testing on its own CRI
- [ ] 5.5 Run the full CI pipeline on a branch and confirm build, itest, and scan jobs all pass under Podman — requires pushing a branch and observing real CI; not done in this session

## 6. Documentation

- [x] 6.1 Update `docs/development/INSTALL.md` and `docs/development/installDockerCompose.md` to document Podman installation and usage as the supported runtime
- [x] 6.2 Update `docs/development/testing.md` with Podman-specific TestContainers/Ryuk notes
- [x] 6.3 Update `README.md` and `CONTRIBUTING.md` references to Docker, noting Docker as an unsupported fallback (`CONTRIBUTING.md` had no Docker-specific references to update)
- [x] 6.4 Update `.env.example` if it documents `DOCKER_HOST` or other Docker-specific variables (updated comments; left `host.docker.internal`, `DOCKER_USE_ARM64_CONTAINERS`, and `DOCKER_COMPOSE_*` variable names unchanged as they are plain identifiers/host-file entries that work identically under Podman)

## 7. Rollout

- [ ] 7.1 Announce the migration and required local tooling change to contributors — a human/communication action, not a code change
- [ ] 7.2 Merge script, build, and itest changes first; confirm stability before touching CI — a merge-sequencing decision for the team, not something to do inside a single change
- [ ] 7.3 Merge CI workflow changes once local/itest changes are proven stable — same as 7.2
- [ ] 7.4 Remove Docker-specific fallback documentation after the deprecation window — deferred until after the deprecation window has actually passed
