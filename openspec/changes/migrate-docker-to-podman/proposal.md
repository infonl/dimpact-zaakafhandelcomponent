## Why

Docker Desktop requires a paid subscription for commercial use at our organization's scale, and several municipalities and integrators consuming ZAC prefer a daemonless, rootless container runtime for security and licensing reasons. Podman is a drop-in, OCI-compliant replacement for Docker that removes the licensing dependency and the always-on root daemon, without requiring changes to the underlying Dockerfiles or Compose service definitions. Migrating now, while the local dev stack, integration tests, and CI images all still assume Docker, avoids compounding the migration cost as more services are added to `docker-compose.yaml`.

## What Changes

- Replace local developer tooling instructions and scripts (`start-docker-compose.sh`, `stop-docker-compose.sh`, `start-e2e-with-local-env.sh`, `start-it-with-local-env.sh`, `scripts/docker-compose/setup-linux.sh`) to use Podman and `podman compose` instead of Docker and `docker compose`.
- Replace `docker build` in `scripts/docker/build-docker-image.sh` with `podman build`, keeping the same `Dockerfile` and build args.
- Update the Gradle `buildDockerImage` task and related task naming/docs to invoke the Podman-based build script.
- Reconfigure TestContainers (used by `itest` via `ComposeContainer`) to talk to the Podman socket instead of the Docker socket, including handling Ryuk (resource reaper) which does not work with rootless Podman by default and must be disabled with an equivalent, documented cleanup strategy.
- Update CI workflows (`build-test-deploy.yml`, `trivy-docker-image-scan.yml`, `dependency-security-scan.yml`, `publish-nvd-database-cache.yml`, `helm-chart-testing.yml`) to install Podman on GitHub-hosted runners and replace `docker/setup-buildx-action`, `docker/build-push-action`, and `docker/login-action` with Podman-native equivalents (`podman build`, `podman push`, `podman login`).
- Rename Docker-specific references in documentation (`docs/development/*.md`, `README.md`, `CONTRIBUTING.md`) and file names (`.dockerignore` → keep as-is since Podman honors it; `docker-compose.yaml` filename kept for compatibility with `podman compose` auto-discovery) to reflect Podman as the supported runtime, while noting Docker remains usable as an unsupported fallback for contributors who prefer it.
- **BREAKING**: The Gradle `buildDockerImage` task and CI pipelines will no longer support Docker-only environments without Podman installed; contributors and CI runners must have Podman (and `podman-compose` or the `podman compose` plugin) available.

## Capabilities

### New Capabilities
- `container-runtime`: Defines the requirements for which container engine and compose tooling ZAC's build, local development, and test infrastructure use (Podman as the primary/required runtime), including image build, compose orchestration, and TestContainers integration behavior.

### Modified Capabilities
- `open-formulieren-docker-compose`: Scenario steps that invoke `docker compose --profile ... up` change to the Podman-equivalent command; underlying service definitions, profiles, and behavior are unchanged.

## Impact

- **Scripts**: `scripts/docker/build-docker-image.sh`, `start-docker-compose.sh`, `stop-docker-compose.sh`, `start-e2e-with-local-env.sh`, `start-it-with-local-env.sh`, `scripts/docker-compose/setup-linux.sh`.
- **Build**: `build.gradle.kts` (`buildDockerImage` task and its dependents, e.g. integration test setup).
- **Compose files**: `docker-compose.yaml`, `docker-compose.arm64-override.yaml`.
- **Tests**: `src/itest/kotlin/nl/info/zac/itest/config/ZacItestProjectConfig.kt` (TestContainers `ComposeContainer` and Ryuk handling), other itest classes that assume a Docker socket.
- **CI**: `.github/workflows/build-test-deploy.yml`, `.github/workflows/trivy-docker-image-scan.yml`, `.github/workflows/dependency-security-scan.yml`, `.github/workflows/publish-nvd-database-cache.yml`, `.github/workflows/helm-chart-testing.yml`.
- **Docs**: `docs/development/INSTALL.md`, `docs/development/installDockerCompose.md`, `docs/development/testing.md`, `README.md`, `CONTRIBUTING.md`, `.env.example` (if it documents `DOCKER_HOST`).
- **No changes** to `charts/` (Kubernetes Helm charts already run under whatever container runtime the cluster's CRI uses, independent of local Docker/Podman).
