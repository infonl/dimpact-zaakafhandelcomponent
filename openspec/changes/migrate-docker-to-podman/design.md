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

### 2. Disable Ryuk for Podman by exporting the real OS environment variable before the JVM starts
Rootless Podman does not support the privileged container Ryuk needs to run reliably — **confirmed empirically**: without `TESTCONTAINERS_RYUK_DISABLED=true` set as a real process environment variable, `ComposeContainer.start()` throws `ContainerLaunchException: Container startup failed for image testcontainers/ryuk:0.14.0` and the whole itest run fails. Critically, TestContainers reads this env var itself, independently of any application code — a value computed at runtime inside `ZacItestProjectConfig.kt` (e.g. by inspecting `DOCKER_HOST`) has no effect on whether TestContainers attempts to launch Ryuk; it can only affect ZAC's own teardown logic. The fix is to export the real env var before Gradle/the JVM starts — `start-it-with-local-env.sh` does this unconditionally now, and the CI workflow exports it via `$GITHUB_ENV` before running `itest`.
- **Known side effect** (existing code, not introduced by this migration): the same flag also gates ZAC's own explicit Compose teardown in `afterProject()`, so under Podman the Compose stack is never auto-stopped after a local `itest` run — run `./stop-docker-compose.sh` manually afterwards.
- **Also confirmed**: Gradle daemons cache their environment at startup and don't pick up new environment variables from later invocations. Run `./gradlew --stop` before an itest run if `DOCKER_HOST`/`TESTCONTAINERS_RYUK_DISABLED` changed, or a stale daemon will silently ignore them.
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

- **[Risk, confirmed harmless]** Rootless Podman's networking (slirp4netns/pasta) can behave differently from Docker's bridge networking for inter-container hostnames used across `docker-compose.yaml` (e.g. `openzaak-nginx`, `objecten-api.local`). → **Verified**: all inter-service hostnames resolved correctly in a real run (direct service name and via the `.dns.podman` search domain); no mitigation needed beyond what's already in `docker-compose.yaml`.
- **[Risk, confirmed and mitigated]** Disabling Ryuk removes automatic orphaned-container cleanup — under Podman this also disables ZAC's own explicit teardown (see Decision #2), so local Compose stacks persist after `itest` runs. → **Mitigation**: documented in `start-it-with-local-env.sh`'s help text and `docs/development/testing.md`; CI runners are ephemeral so this doesn't leak resources there.
- **[Risk]** Contributors on Docker Desktop who don't migrate immediately lose "supported" status and may hit undocumented breakage. → **Mitigation**: Keep Docker working as an unsupported fallback for one release cycle; document the switch clearly in `docs/development/INSTALL.md` and `CONTRIBUTING.md`.
- **[Risk, confirmed harmless]** `docker-compose.arm64-override.yaml` may rely on Docker-specific image selection behavior on Apple Silicon. → **Verified**: identical clean startup under `podman machine` on Apple Silicon with `DOCKER_USE_ARM64_CONTAINERS=true`.
- **[Risk, confirmed and fixed]** Rootless Podman's VM/host defaults `net.ipv4.ip_unprivileged_port_start` to 1024, so publishing any privileged host port (<1024) fails — and since Compose aborts the *entire* `up -d` on any single service failure, this took down the whole `itest` profile via `greenmail`'s SMTP (25) and IMAP (143) port mappings, even though the itest suite never uses those ports directly (only GreenMail's non-privileged API port). → **Mitigation**: lower the sysctl (`sudo sysctl -w net.ipv4.ip_unprivileged_port_start=25`, persisted via `/etc/sysctl.d/`) on the Podman machine/host — documented in `docs/development/installDockerCompose.md`. Applies on Linux hosts directly, not just the macOS/Windows VM.
- **[Risk, confirmed and fixed]** Rootless Podman containers don't get `CAP_NET_BIND_SERVICE` by default the way Docker containers typically do, so a non-root process binding a privileged port *inside* the container also fails, independently of the host-level sysctl above. → **Mitigation**: added `cap_add: [NET_BIND_SERVICE]` to the `greenmail` service in `docker-compose.yaml` — a minimal, additive change, harmless under Docker.
- **[Risk, confirmed and sized]** The default `podman machine` allocation is far too small for this stack under real load (not just idle). 8GiB was enough to reach `Healthy` at idle but produced ~13% scattered test failures (`Connection reset`/timeouts) under the full itest suite's load. → **Mitigation**: 16GiB/8 CPU is the validated minimum; documented in `docs/development/installDockerCompose.md`.

## Migration Plan

1. Spike Podman locally (macOS + Linux) against the existing `docker-compose.yaml` unmodified, to confirm compose/networking/healthcheck compatibility before touching scripts.
2. Update build scripts (`scripts/docker/build-docker-image.sh`, `start-docker-compose.sh`, `stop-docker-compose.sh`, etc.) and the Gradle `buildDockerImage` task to use Podman, gated behind a spike sign-off.
3. Update `ZacItestProjectConfig.kt` and itest CI job to point `DOCKER_HOST` at the Podman socket and default `TESTCONTAINERS_RYUK_DISABLED=true` under Podman.
4. Update CI workflows one at a time, starting with the lowest-risk (`trivy-docker-image-scan.yml`) before `build-test-deploy.yml`.
5. Update documentation and `CONTRIBUTING.md` to reflect Podman as the supported runtime; keep a short "using Docker instead" note.
6. **Rollback**: Every script/workflow change is scoped to swapping the CLI binary and socket target; if Podman proves incompatible for a given step, revert that step's commit independently — Compose file contents and Dockerfile remain untouched throughout, so Docker keeps working as a fallback at every stage.

## Open Questions

- ~~Should `podman machine` (macOS/Windows) resource limits (CPU/memory) be standardized and documented?~~ **Resolved**: yes, 16GiB/8 CPU, documented in `docs/development/installDockerCompose.md`.
- ~~Is `podman compose` available/stable enough on the exact Podman version we plan to pin in CI?~~ **Resolved**: validated with Podman 6.0.2 + external docker-compose v5.3.1 provider; the full itest suite (326 tests) passes reliably.
- Do we need a CI matrix job that still runs Docker temporarily during the deprecation window, to catch regressions for contributors who haven't switched yet? (Still open — a team/process decision.)
- Should the Linux setup docs also call out the `net.ipv4.ip_unprivileged_port_start` sysctl fix, or should `setup-linux.sh` set it automatically? (Currently documented as a manual step; not yet tested on real Linux hardware in this migration.)
