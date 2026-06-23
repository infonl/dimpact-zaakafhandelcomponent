# ci-cross-job-artifacts Specification (delta)

## MODIFIED Requirements

### Requirement: Run-scoped build outputs are shared between jobs via workflow artifacts

The build-test-deploy workflow SHALL use GitHub workflow artifacts (`actions/upload-artifact` / `actions/download-artifact`) to pass run-scoped intermediate build outputs between jobs of the same workflow run. It SHALL NOT use the GitHub Actions cache (`actions/cache/save` / `actions/cache/restore`) for these run-scoped outputs.

The run-scoped outputs in scope are:
- the Gradle build directory (`build`),
- the generated Java clients (`src/generated`),
- the frontend artefacts (`src/main/app/node_modules`, `src/main/app/src/generated/types`),
- the built ZAC JAR (`target/zaakafhandelcomponent.jar`),
- the Docker image tar (`docker-image.tar`).

#### Scenario: Producing job uploads outputs as artifacts

- **WHEN** the `build` job finishes building the Gradle outputs, generated Java clients, frontend artefacts, and the ZAC JAR
- **THEN** each output is uploaded with `actions/upload-artifact` under a distinct artifact name
- **AND** no `actions/cache/save` step is used to store these outputs

#### Scenario: Consuming job downloads outputs as artifacts

- **WHEN** a downstream job (`run-backend-unit-tests`, `run-frontend-unit-tests`, `build-docker-image-and-run-itests`, or `push-docker-image`) needs an output produced by an earlier job
- **THEN** it retrieves the output with `actions/download-artifact` using the matching artifact name
- **AND** no `actions/cache/restore` step is used to retrieve these outputs

#### Scenario: Frontend artefacts retain dotfiles and executable permissions

- **WHEN** the frontend artefacts (`node_modules` and the generated types) are passed from `build` to `run-frontend-unit-tests`
- **THEN** they are archived with `tar` before upload and extracted after download
- **AND** the downloaded tree retains hidden entries such as `node_modules/.bin` and the executable bit on its CLI shims, so the `npmRun*` Gradle tasks can run

#### Scenario: Docker image tar passed to the push job

- **WHEN** the `build-docker-image-and-run-itests` job saves the Docker image tar on `main` or a `hotfix/*` branch
- **THEN** it uploads `docker-image.tar` as a workflow artifact
- **AND** the `push-docker-image` job downloads that artifact and loads the image
