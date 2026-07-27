## ADDED Requirements

### Requirement: Podman is the required runtime for building the ZAC image
The ZAC container image SHALL be built using Podman (`podman build`) from the existing `Dockerfile`, invoked via `scripts/docker/build-docker-image.sh` and the Gradle `buildDockerImage` task, without requiring changes to the `Dockerfile` itself.

#### Scenario: Building the image via Gradle
- **WHEN** a developer or CI job runs `./gradlew buildDockerImage`
- **THEN** the script invokes `podman build` with the same build args (`versionNumber`, `branchName`, `commitHash`) and produces a tagged image

#### Scenario: Docker is not required to build the image
- **WHEN** a machine has Podman installed but not Docker
- **THEN** `./gradlew buildDockerImage` completes successfully

### Requirement: Local development Compose stack runs under Podman
The local development stack defined in `docker-compose.yaml` and `docker-compose.arm64-override.yaml` SHALL be started, stopped, and managed using `podman compose`, invoked via `start-docker-compose.sh`, `stop-docker-compose.sh`, and related helper scripts, without requiring changes to the Compose service definitions.

#### Scenario: Starting the default stack
- **WHEN** a developer runs `start-docker-compose.sh`
- **THEN** the script invokes `podman compose up` (with the ARM64 override on Apple Silicon) and all default-profile services start and pass their healthchecks

#### Scenario: Stopping the stack
- **WHEN** a developer runs `stop-docker-compose.sh`
- **THEN** the script invokes `podman compose down` and all running containers for the stack are removed

### Requirement: Integration tests run TestContainers against the Podman socket
Integration tests (`src/itest`) SHALL run TestContainers' `ComposeContainer` against a Podman-exposed, Docker-API-compatible socket, with `TESTCONTAINERS_RYUK_DISABLED` set to `true` by default when running under Podman, since rootless Podman does not support the privileged Ryuk resource-reaper container.

#### Scenario: Integration tests run without a Docker daemon present
- **WHEN** `./gradlew itest` runs on a machine with only Podman installed and `DOCKER_HOST` pointed at the Podman socket
- **THEN** `ComposeContainer` starts the `itest` profile services and all integration tests execute normally

#### Scenario: Ryuk is skipped under Podman
- **WHEN** integration tests run with `TESTCONTAINERS_RYUK_DISABLED=true` under Podman
- **THEN** no Ryuk container is started, and the existing explicit teardown path stops and removes the Compose stack after the test run completes

### Requirement: CI pipelines build and scan images using Podman
GitHub Actions workflows that build, push, or scan the ZAC container image (`build-test-deploy.yml`, `trivy-docker-image-scan.yml`, `dependency-security-scan.yml`, `publish-nvd-database-cache.yml`) SHALL install Podman on the runner and use direct `podman` CLI invocations (`podman build`, `podman login`, `podman push`) instead of the `docker/setup-buildx-action`, `docker/build-push-action`, and `docker/login-action` GitHub Actions.

#### Scenario: CI builds and pushes the ZAC image with Podman
- **WHEN** the `build-test-deploy.yml` workflow runs on a GitHub-hosted runner
- **THEN** the workflow installs Podman, builds the image with `podman build`, authenticates with `podman login`, and pushes the image with `podman push`

#### Scenario: Image vulnerability scanning still runs
- **WHEN** the `trivy-docker-image-scan.yml` workflow runs against a Podman-built image
- **THEN** Trivy scans the image successfully and reports vulnerabilities as before
