# ci-parallel-unit-tests Specification

## ADDED Requirements

### Requirement: Backend and frontend unit tests run as separate concurrent jobs

The `build-test-deploy` workflow SHALL run the backend unit tests and the frontend unit tests in two separate jobs that can execute concurrently, instead of a single job that runs both suites sequentially. Each job SHALL download only the run-scoped artefacts it needs to run its suite.

#### Scenario: Backend and frontend jobs run in parallel

- **WHEN** the workflow reaches the unit-test stage after the `build` job completes
- **THEN** a backend unit-test job and a frontend unit-test job both start, each depending only on `build` (and `paths-filter`)
- **AND** neither job waits for the other to finish before starting

#### Scenario: Each job downloads only the artefacts it needs

- **WHEN** the backend unit-test job runs
- **THEN** it downloads the Gradle build output and generated Java clients, and does not require the frontend artefacts to be extracted before running the backend suite
- **WHEN** the frontend unit-test job runs
- **THEN** it downloads and extracts the frontend artefacts (`node_modules` and generated types) needed to run the frontend suite

### Requirement: Backend unit tests run with multi-fork parallelism

The backend `test` JVM test suite SHALL be configured to run in multiple parallel JVM forks so that it uses the available CPU cores on the CI runner rather than a single fork.

#### Scenario: Test suite uses more than one fork on a multi-core runner

- **WHEN** the backend `test` task runs on a runner with multiple CPU cores
- **THEN** the suite executes across multiple JVM forks (more than one) determined from the available processors

#### Scenario: Parallel execution does not change test outcomes

- **WHEN** the backend unit tests run in parallel forks
- **THEN** the same set of tests run and produce the same pass/fail result as a single-fork run

### Requirement: Test results and coverage are reported per suite

The split jobs SHALL preserve the existing test-result publication and coverage uploads, with each job reporting its own suite. Backend coverage SHALL be uploaded under the `backendunittests` Codecov flag and frontend coverage under the `frontendunittests` flag, and JUnit/Jest test results SHALL continue to be published. Both jobs SHALL upload their suite's results to Codecov test analytics (`report_type: "test_results"`) so that test analytics covers both the backend and frontend suites.

#### Scenario: Backend job reports backend results and coverage

- **WHEN** the backend unit-test job completes
- **THEN** it publishes the backend JUnit test results
- **AND** uploads the backend JaCoCo coverage report to Codecov with the `backendunittests` flag
- **AND** uploads the backend test results to Codecov test analytics (`report_type: "test_results"`)

#### Scenario: Frontend job reports frontend results and coverage

- **WHEN** the frontend unit-test job completes
- **THEN** it publishes the frontend Jest test results
- **AND** uploads the frontend coverage report to Codecov with the `frontendunittests` flag
- **AND** uploads the frontend test results to Codecov test analytics (`report_type: "test_results"`)

### Requirement: Gating and downstream dependencies match the previous single job

Both unit-test jobs SHALL be gated by the same conditions that previously controlled `run-unit-tests` (the `paths-filter` result, `merge_group` exclusion, and `main`/`hotfix` overrides), and downstream jobs SHALL require both unit-test jobs to succeed where they previously required `run-unit-tests`.

#### Scenario: Jobs skip under the same conditions as before

- **WHEN** the workflow runs in a situation where the old `run-unit-tests` job would have been skipped (for example a `merge_group` event, or a change that does not require a build)
- **THEN** both the backend and frontend unit-test jobs are skipped under the same condition

#### Scenario: Push job requires both unit-test jobs

- **WHEN** the `push-docker-image` job is evaluated
- **THEN** its `needs` include both the backend and frontend unit-test jobs in place of the former single `run-unit-tests` job
