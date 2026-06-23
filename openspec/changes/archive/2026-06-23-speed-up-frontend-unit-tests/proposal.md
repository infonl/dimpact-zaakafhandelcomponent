## Why

Frontend unit tests take ~11 minutes in CI, blocking fast feedback. With 205 spec files and no parallelization configured, Jest runs on system defaults and coverage collection adds overhead. Sharding tests across parallel CI jobs and tuning Jest worker count will cut wall-clock time significantly.

## What Changes

- Add Jest worker count (`maxWorkers`) configuration to `jest.config.js` for consistent parallelization locally and in CI
- Split frontend unit tests across multiple parallel CI jobs using Jest `--shard` support
- Aggregate sharded coverage reports in CI and upload them correctly to Codecov
- Fix the broken `test:ci` npm script (currently `npx jest -t` with no argument)
- Add a `test:shard` npm script for running a specific shard locally

## Capabilities

### New Capabilities

- `jest-sharding`: Run frontend Jest tests in parallel shards (e.g., 3 shards) in CI, merging coverage results before upload

### Modified Capabilities

- (none — no existing specs affected)

## Impact

- `src/main/app/jest.config.js` — add `maxWorkers`, `coverageDirectory` settings
- `src/main/app/package.json` — fix `test:ci`, add `test:shard` script
- `.github/workflows/build-test-deploy.yml` — replace single `npmRunTestCoverage` Gradle call with matrix-based sharded jobs + merge step
- `build.gradle.kts` — may need a new `npmRunTestShard` task accepting shard index/total parameters
