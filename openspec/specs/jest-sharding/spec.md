## ADDED Requirements

### Requirement: Jest transform cache is stored in a predictable project-relative path
The Jest configuration SHALL set `cacheDirectory` to `<rootDir>/.jest-cache` so that the transform cache lives inside the project directory and can be persisted by CI caching tools.

#### Scenario: Transform cache is written to project directory
- **WHEN** Jest runs in `src/main/app/`
- **THEN** compiled transform artifacts are written to `src/main/app/.jest-cache/`

#### Scenario: Transform cache directory is excluded from version control
- **WHEN** a developer runs `git status` after Jest has run
- **THEN** the `.jest-cache/` directory does not appear as an untracked file

### Requirement: Jest coverage output is written to a predictable path
The Jest configuration SHALL set `coverageDirectory` to `"coverage"` so that CI steps that upload coverage reports can reference a stable path.

#### Scenario: Coverage report is written to expected directory
- **WHEN** Jest runs with `--coverage`
- **THEN** the coverage report is written to `src/main/app/coverage/`

### Requirement: `test:ci` npm script is a valid Jest invocation
The `test:ci` script in `package.json` SHALL be a valid Jest CLI invocation that runs with coverage and force-exits after completion.

#### Scenario: `test:ci` runs all tests with coverage
- **WHEN** a developer runs `npm run test:ci`
- **THEN** Jest runs all test suites with coverage enabled and exits when done

#### Scenario: `test:ci` accepts additional Jest CLI arguments
- **WHEN** a developer runs `npm run test:ci -- --shard=1/3`
- **THEN** Jest runs shard 1 of 3 with coverage enabled

### Requirement: `test:shard` npm script exists for running a subset of tests
The `package.json` SHALL expose a `test:shard` script so developers can run a specific shard locally.

#### Scenario: Developer runs a specific shard locally
- **WHEN** a developer runs `npm run test:shard -- --shard=1/3`
- **THEN** Jest runs only the test files assigned to shard 1 of 3

### Requirement: Jest transform cache is persisted between CI runs
The CI `run-unit-tests` job SHALL cache `src/main/app/.jest-cache/` using `actions/cache` so that Angular template compilation is skipped for unchanged files on subsequent runs.

#### Scenario: Warm cache skips recompilation of unchanged files
- **WHEN** the `run-unit-tests` CI job runs and a prior cache exists for the same OS and dependency configuration
- **THEN** the `.jest-cache/` directory is restored before Jest runs
- **THEN** Jest recompiles only files that changed since the cached run

#### Scenario: Cache is always saved after a run
- **WHEN** the `run-unit-tests` CI job completes
- **THEN** the updated `.jest-cache/` is saved under a key that includes `github.run_id`
- **THEN** the next run can restore this cache via the restore-key fallback

#### Scenario: First run on a new branch seeds from main branch cache
- **WHEN** a PR branch runs `run-unit-tests` for the first time
- **THEN** `actions/cache` falls back to the most recent cache from the same OS and dependency hash
- **THEN** Jest benefits from the main branch cache on the first PR run
