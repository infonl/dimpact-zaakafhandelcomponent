## 1. Spike: verify Podman compatibility

- [x] 1.1 Install Podman locally (Linux and macOS via `podman machine`) and run `docker-compose.yaml` unmodified via `podman compose up` for the default profile — **done on macOS (Apple Silicon)**: `podman-machine-default` (applehv, 8GiB/5 CPU after bumping from the default 2GiB) started, `./start-docker-compose.sh -e` brought up all 16 default-profile services unmodified via the `podman compose` → external `docker-compose` v5.3.1 provider path, all reaching `Started`/`Healthy`; `./stop-docker-compose.sh` tore it down cleanly. Not yet run on Linux.
- [x] 1.2 Verify inter-service hostnames (`openzaak-nginx`, `objecten-api.local`, etc.) resolve correctly under Podman's rootless networking — confirmed: `openzaak-nginx` resolves `openzaak.local` directly, `keycloak`/`pabc-api` resolve `keycloak-database`/`pabc-database` via the `.dns.podman` search domain; the published `openzaak-nginx` port answered `HTTP 200` from the host
- [x] 1.3 Run `docker-compose.arm64-override.yaml` under `podman machine` on Apple Silicon and confirm services start — confirmed: `DOCKER_USE_ARM64_CONTAINERS=true ./start-docker-compose.sh -e` brought up the same 16 services, all `Started`/`Healthy`
- [x] 1.4 Confirm `podman compose` supports `depends_on: condition: service_healthy` as used by `openformulieren-database` and other healthcheck-gated services — confirmed: observed the exact `Healthy` → dependent-`Starting` cascade (e.g. `openklant-database` Healthy → `openklant.local` Starting; `pabc-database` Healthy → `pabc-migrations` → `pabc-api`; `openzaak-database` Healthy → `openzaak.local` Healthy → `openzaak-nginx`) in both the default and ARM64-override runs. `openformulieren` profile itself not tested (requires 1Password-sourced secrets not available in this session).
- [x] 1.5 Document minimum required Podman version based on spike findings — validated with **Podman 6.0.2** (Homebrew) and its external **docker-compose v5.3.1** provider (`podman compose` shells out to `/usr/local/bin/docker-compose`, confirming design.md decision #3). The default `podman machine` allocation of 2GiB RAM was too small; **8GiB was enough for the idle default-profile stack but not for the full itest run under load (see 4.4) — 16GiB/8 CPU is the validated recommendation**, now documented in `docs/development/installDockerCompose.md`.

## 2. Image build

- [x] 2.1 Update `scripts/docker/build-docker-image.sh` to invoke `podman build` instead of `docker build`, keeping the same build args (`versionNumber`, `branchName`, `commitHash`) and tag
- [x] 2.2 Update the Gradle `buildDockerImage` task in `build.gradle.kts` (and any task naming/comments referencing Docker) to reflect the Podman-based script
- [x] 2.3 Verify `./gradlew buildDockerImage` produces a working image using only Podman (no Docker installed) — **confirmed with a real build**: `podman build` ran all 29 Dockerfile steps and tagged `ghcr.io/infonl/zaakafhandelcomponent:dev` successfully via the migrated script

## 3. Local development scripts

- [x] 3.1 Update `start-docker-compose.sh` to invoke `podman compose up` (including the ARM64 override selection logic)
- [x] 3.2 Update `stop-docker-compose.sh` to invoke `podman compose down`
- [x] 3.3 Update `start-e2e-with-local-env.sh` and `start-it-with-local-env.sh` to use Podman/`podman compose` (`start-e2e-with-local-env.sh` needed no change — it has no direct engine invocation)
- [x] 3.4 Update `scripts/docker-compose/setup-linux.sh` for Podman-specific setup steps (e.g. rootless socket activation)

## 4. Integration tests (TestContainers)

- [x] 4.1 Configure `DOCKER_HOST` (or `testcontainers.properties`) so TestContainers connects to the Podman-exposed Docker-API-compatible socket — `start-it-with-local-env.sh` now auto-detects and exports it (`podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}'` on macOS/Windows, `/run/user/$(id -u)/podman/podman.sock` on Linux) if not already set; confirmed working end-to-end
- [x] 4.2 ~~Default `TESTCONTAINERS_RYUK_DISABLED=true` when running under Podman in `ZacItestProjectConfig.kt`~~ — **superseded by a real finding**: a value computed in Kotlin can't influence TestContainers' own Ryuk-launch decision, which is made independently by reading the literal `TESTCONTAINERS_RYUK_DISABLED` OS environment variable before the JVM starts. The Kotlin auto-detect was reverted (it produced a misleading "disabled" log message while Ryuk still tried and failed to launch). Fixed for real in `start-it-with-local-env.sh`, which now unconditionally exports `TESTCONTAINERS_RYUK_DISABLED=true` before invoking Gradle, and in the CI workflow (already done in task 5.2).
- [x] 4.3 Update the explicit Compose teardown path (`ZacItestProjectConfig.kt:254`) — confirmed this goes through TestContainers' API (`.stop()`), not a literal CLI string. **Real consequence found**: `skipContainerCleanup` (driven by `TESTCONTAINERS_RYUK_DISABLED`) gates both Ryuk *and* ZAC's own explicit teardown call in the existing code, so under Podman — where Ryuk must always be disabled — the Compose stack is never auto-stopped after a local `itest` run. Documented this in the script's help text and comments rather than silently changing existing teardown-skip semantics; developers must run `./stop-docker-compose.sh` manually afterwards.
- [x] 4.4 Run `./gradlew itest` end-to-end against Podman and confirm all integration tests pass — **done for real, multiple times**, and it now passes cleanly: **326/326 tests, `BUILD SUCCESSFUL`**, using the actual `./start-it-with-local-env.sh` script fresh (no manual env exports). Getting there surfaced three real, previously-unknown blockers, all now fixed:
  - **Stale Gradle daemon**: a daemon started before `DOCKER_HOST`/`TESTCONTAINERS_RYUK_DISABLED` were exported keeps its original environment and silently breaks TestContainers. `./gradlew --stop` before a Podman itest run avoids this; worth a callout in `docs/development/testing.md`.
  - **Rootless Podman can't bind privileged host ports** (<1024) by default — `podman machine`'s VM ships with `net.ipv4.ip_unprivileged_port_start=1024`, so publishing `greenmail`'s SMTP (25) and IMAP (143) ports failed the *entire* `podman compose up -d` (one failing service aborts the whole stack). Fixed by lowering the sysctl inside the machine/host (`sudo sysctl -w net.ipv4.ip_unprivileged_port_start=25`, persisted via `/etc/sysctl.d/`). This needs doing on Linux hosts too, not just the macOS/Windows VM — added to `docs/development/installDockerCompose.md`.
  - **Rootless Podman containers don't get `NET_BIND_SERVICE` by default** — even after the host-level fix above, the `greenmail` process itself (running as non-root inside the container) got `Permission denied` binding port 25. Fixed with a minimal, additive `cap_add: [NET_BIND_SERVICE]` on the `greenmail` service in `docker-compose.yaml` (harmless under Docker, which grants this by default).
  - Resource sizing (see 1.5): 8GiB was enough for the stack to reach `Healthy` idle, but the full itest run under load produced scattered `Connection reset`/timeout failures (37/292) consistent with resource pressure, not a Podman defect; bumping to 16GiB/8 CPU resolved it completely (326/326 passing, repeatable).

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
