## Context

ZAC's local dev stack, integration tests, and CI all currently assume the Docker CLI and a Docker daemon socket:

- `docker-compose.yaml` / `docker-compose.arm64-override.yaml` are started via `docker compose` in `start-docker-compose.sh` and friends.
- `scripts/docker/build-docker-image.sh` runs `docker build`, invoked from the Gradle `buildDockerImage` task (`build.gradle.kts:834`).
- `src/itest/.../ZacItestProjectConfig.kt` uses TestContainers' `ComposeContainer` to bring up `docker-compose.yaml` under the `itest` profile, and already has a `TESTCONTAINERS_RYUK_DISABLED` escape hatch (`ZacItestProjectConfig.kt:114`) for environments where the Ryuk resource-reaper container can't run.
- CI (`build-test-deploy.yml`) uses `docker/setup-buildx-action`, `docker/build-push-action`, and `docker/login-action` on GitHub-hosted runners, which ship Docker preinstalled but not Podman.

Podman aims for CLI and (via `podman machine`/system service) socket compatibility with Docker, which is what makes this migration tractable without rewriting Compose files or TestContainers usage from scratch. The main friction points are: rootless Ryuk, the `docker compose` vs `podman compose` split, and GitHub Actions runners not having Podman preinstalled.

## Goals / Non-Goals

**Goals:**
- Make Podman the primary, documented, and CI-enforced container runtime for building the ZAC image, running the local Compose stack, and running integration tests.
- Keep `docker-compose.yaml` as the single source of truth for service definitions (no fork into a separate Podman-only compose file).
- Keep the Dockerfile unchanged; Podman builds OCI images from the same `Dockerfile` with no syntax changes needed.
- Preserve current itest behavior (profiles, healthchecks, container lifecycle) under Podman.

**Non-Goals:**
- Do not migrate Kubernetes/Helm chart deployment (`charts/`) — that runs on a cluster's own CRI (containerd/CRI-O) and is unaffected by the local build/dev runtime choice.
- Do not support running both Docker and Podman as equally-supported first-class runtimes long-term; Docker becomes a best-effort, undocumented fallback.
- Do not change any application-level behavior, image contents, or Compose service topology.

## Decisions

### 1. Use Podman's Docker-API-compatible socket rather than reconfiguring TestContainers
Podman can expose a Docker-API-compatible socket (`podman system service`, or automatically via `podman machine` on macOS/Windows). We point TestContainers and the Docker CLI compatibility layer at this socket via the standard `DOCKER_HOST` environment variable, instead of switching to a Podman-specific TestContainers module. This keeps `ComposeContainer` and all existing itest code unchanged.
- **Alternative considered**: Use `testcontainers.properties` with a Podman-specific strategy or the `org.testcontainers:podman` compatibility notes. Rejected because `DOCKER_HOST` + socket is simpler, requires no new dependency, and is Podman's own recommended integration path.

### 2. Disable Ryuk for Podman by default, reusing the existing escape hatch
Rootless Podman does not support the privileged container Ryuk needs to run reliably. Rather than adding new configuration, we reuse the existing `TESTCONTAINERS_RYUK_DISABLED` environment variable (`ZacItestProjectConfig.kt:114`) and set it by default in the Podman-based scripts/CI. Container cleanup already falls back to an explicit `docker compose down`-equivalent (`ZacItestProjectConfig.kt:254`), which we update to invoke `podman compose down`.
- **Alternative considered**: Run Podman rootful (as root) so Ryuk works unmodified. Rejected: rootful Podman reintroduces the root-daemon security trade-off we're migrating away from.

### 3. Use the native `podman compose` command, not the standalone `podman-compose` Python tool
`podman compose` (built into recent Podman versions) shells out to an installed `docker-compose`/`podman-compose` binary or uses Podman's internal compose provider, and behaves closer to `docker compose` in profile/health-check semantics than the older, less-maintained `podman-compose` Python project.
- **Alternative considered**: `podman-compose` (Python). Rejected due to known gaps in `depends_on: condition: service_healthy` support, which several services in `docker-compose.yaml` (e.g. `openformulieren-database`) rely on.

### 4. Install Podman explicitly on GitHub-hosted runners rather than switching to self-hosted runners
GitHub-hosted `ubuntu-latest` runners have Docker preinstalled but not Podman. We add a Podman installation step (`apt-get install -y podman` or the official setup action) to the affected workflows rather than moving to self-hosted/Podman-preinstalled runners, to keep CI infra changes minimal.
- **Alternative considered**: Self-hosted runners with Podman baked in. Rejected as out of scope — larger infra investment than this migration warrants.

### 5. Replace `docker/*-action` GitHub Actions with direct Podman CLI steps
`docker/setup-buildx-action`, `docker/build-push-action`, and `docker/login-action` have no official Podman equivalents. We replace them with plain `run:` steps invoking `podman build`, `podman login`, and `podman push`, since Podman's CLI already covers this without needing a dedicated action.

## Risks / Trade-offs

- **[Risk]** Rootless Podman's networking (slirp4netns/pasta) can behave differently from Docker's bridge networking for inter-container hostnames used across `docker-compose.yaml` (e.g. `openzaak-nginx`, `objecten-api.local`). → **Mitigation**: Verify all inter-service hostnames resolve under `podman compose` in a spike before rolling out to CI; keep the same user-defined network names/aliases already in `docker-compose.yaml`.
- **[Risk]** Disabling Ryuk removes automatic orphaned-container cleanup, which could leak containers on CI runners between itest runs. → **Mitigation**: Rely on the existing explicit teardown path (`ZacItestProjectConfig.kt:254`) and ensure CI runners are ephemeral (already the case for GitHub-hosted runners).
- **[Risk]** Contributors on Docker Desktop who don't migrate immediately lose "supported" status and may hit undocumented breakage. → **Mitigation**: Keep Docker working as an unsupported fallback for one release cycle; document the switch clearly in `docs/development/INSTALL.md` and `CONTRIBUTING.md`.
- **[Risk]** `docker-compose.arm64-override.yaml` may rely on Docker-specific image selection behavior on Apple Silicon. → **Mitigation**: Explicitly test the ARM64 override under Podman on macOS (`podman machine`) as part of the migration tasks.

## Migration Plan

1. Spike Podman locally (macOS + Linux) against the existing `docker-compose.yaml` unmodified, to confirm compose/networking/healthcheck compatibility before touching scripts.
2. Update build scripts (`scripts/docker/build-docker-image.sh`, `start-docker-compose.sh`, `stop-docker-compose.sh`, etc.) and the Gradle `buildDockerImage` task to use Podman, gated behind a spike sign-off.
3. Update `ZacItestProjectConfig.kt` and itest CI job to point `DOCKER_HOST` at the Podman socket and default `TESTCONTAINERS_RYUK_DISABLED=true` under Podman.
4. Update CI workflows one at a time, starting with the lowest-risk (`trivy-docker-image-scan.yml`) before `build-test-deploy.yml`.
5. Update documentation and `CONTRIBUTING.md` to reflect Podman as the supported runtime; keep a short "using Docker instead" note.
6. **Rollback**: Every script/workflow change is scoped to swapping the CLI binary and socket target; if Podman proves incompatible for a given step, revert that step's commit independently — Compose file contents and Dockerfile remain untouched throughout, so Docker keeps working as a fallback at every stage.

## Open Questions

- Should `podman machine` (macOS/Windows) resource limits (CPU/memory) be standardized and documented, given the existing stack's resource footprint (Solr, Postgres, Keycloak, etc. all running concurrently)?
- Do we need a CI matrix job that still runs Docker temporarily during the deprecation window, to catch regressions for contributors who haven't switched yet?
- Is `podman compose` available/stable enough on the exact Podman version we plan to pin in CI, or do we need to pin a minimum Podman version in documentation?
