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

### Requirement: `test:report` npm script is the single coverage run
The `test:report` script in `package.json` SHALL run the suite through the Angular Jest builder with coverage enabled. Invoking Jest directly bypasses the builder, which is what compiles the Angular templates, so `package.json` SHALL NOT offer a script that calls the Jest CLI itself. The script does not pin `maxWorkers`; Jest uses its default worker count.

#### Scenario: `test:report` runs all tests with coverage
- **WHEN** a developer runs `npm run test:report`
- **THEN** Jest runs all test suites with coverage enabled and exits when done

#### Scenario: `test:report` accepts additional Jest CLI arguments
- **WHEN** a developer runs `npm run test:report -- <extra jest args>`
- **THEN** the extra arguments are passed through to the Jest CLI

### Requirement: Coverage is collected with the Babel provider
The Jest configuration SHALL leave `coverageProvider` at its default (`babel`). The V8 provider instruments through source maps, which both slows the suite down and reports different figures for the same code.

#### Scenario: Switching to the V8 provider is rejected
- **WHEN** `coverageProvider` is set to `"v8"`
- **THEN** the suite takes more than twice as long and the reported line coverage drops by roughly eighteen percentage points without any code change

### Requirement: The frontend unit tests run as two CI shards
The CI `frontend-unit-tests` job SHALL run Jest as a two-way matrix, each job passing `--shard=<index>/2`, so that the two halves run on separate runners.

#### Scenario: Each shard runs half of the suite
- **WHEN** the `frontend-unit-tests` job runs for shard `<index>`
- **THEN** Jest runs only the test suites that `--shard=<index>/2` selects
- **AND** the union of the shards covers every test suite exactly once

#### Scenario: Coverage of both shards is merged
- **WHEN** both shards have uploaded their coverage report under the `frontendunittests` flag
- **THEN** Codecov merges the partial reports into one frontend coverage report for the commit

### Requirement: Jest transform cache is persisted between CI runs
The CI `frontend-unit-tests` job SHALL cache `src/main/app/.jest-cache/` using `actions/cache` so that Angular template compilation is skipped for unchanged files on subsequent runs. Every shard SHALL use a cache key of its own, because a shard only ever compiles the files of its own half of the suite.

#### Scenario: Warm cache skips recompilation of unchanged files
- **WHEN** the `frontend-unit-tests` CI job runs and a prior cache exists for the same OS, shard and dependency configuration
- **THEN** the `.jest-cache/` directory is restored before Jest runs
- **THEN** Jest recompiles only files that changed since the cached run

#### Scenario: Cache is always saved after a run
- **WHEN** the `frontend-unit-tests` CI job completes
- **THEN** the updated `.jest-cache/` is saved under a key that includes the shard index and `github.run_id`
- **THEN** the next run can restore this cache via the restore-key fallback

#### Scenario: First run on a new branch seeds from main branch cache
- **WHEN** a PR branch runs `frontend-unit-tests` for the first time
- **THEN** `actions/cache` falls back to the most recent cache from the same OS, shard and dependency hash
- **THEN** Jest benefits from the main branch cache on the first PR run
