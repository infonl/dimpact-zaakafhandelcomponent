## ADDED Requirements

### Requirement: Jest uses explicit worker count
The Jest configuration SHALL set `maxWorkers: 4` so that tests run with 4 parallel workers on both 4-vCPU CI runners and local developer machines.

#### Scenario: Local test run uses 4 workers
- **WHEN** a developer runs `npm run test:report` locally
- **THEN** Jest uses 4 workers (visible in Jest output as `Workers: 4`)

#### Scenario: CI test run uses 4 workers
- **WHEN** the CI runner executes Jest with the project's `jest.config.js`
- **THEN** Jest uses exactly 4 workers regardless of the host CPU count

### Requirement: Frontend tests run in shards in CI
The CI workflow SHALL split the 205-spec frontend test suite into 3 parallel shards using Jest's `--shard=<index>/<total>` flag.

#### Scenario: Three shards run in parallel
- **WHEN** the `run-frontend-unit-tests` CI job runs
- **THEN** three job instances execute concurrently, each running `npx jest --coverage --shard=N/3 --config jest.config.js`
- **THEN** all three shards complete in approximately the same wall-clock time (within 2× of each other)

#### Scenario: Each shard collects partial coverage
- **WHEN** a shard finishes
- **THEN** it uploads its lcov coverage report to Codecov with the `frontendunittests` flag
- **THEN** Codecov merges the partial uploads for the same commit

#### Scenario: A shard failure does not block coverage upload
- **WHEN** one shard fails its tests
- **THEN** coverage from the remaining shards is still uploaded (via `if: ${{ !cancelled() }}` condition)

### Requirement: `test:ci` npm script is valid
The `test:ci` script in `package.json` SHALL be a valid Jest CLI invocation usable for running a specific shard with coverage.

#### Scenario: `test:ci` script accepts shard argument
- **WHEN** a developer runs `npm run test:ci -- --shard=1/3`
- **THEN** Jest runs shard 1 of 3 with coverage enabled and exits with code 0 if all tests pass

### Requirement: `test:shard` npm script exists for local sharding
The `package.json` SHALL expose a `test:shard` script so developers can reproduce a specific shard locally.

#### Scenario: Developer runs a specific shard locally
- **WHEN** a developer runs `npm run test:shard -- 1 3`
- **THEN** Jest runs shard 1 of 3 with coverage and exits with the test result code

### Requirement: Backend unit tests run independently of frontend shards
The CI `run-backend-unit-tests` job SHALL run only `./gradlew test jacocoTestReport` without the `npmRunTestCoverage` task.

#### Scenario: Backend tests do not wait for frontend shards
- **WHEN** the CI pipeline triggers
- **THEN** `run-backend-unit-tests` and `run-frontend-unit-tests` run concurrently
- **THEN** neither job depends on the other completing first
