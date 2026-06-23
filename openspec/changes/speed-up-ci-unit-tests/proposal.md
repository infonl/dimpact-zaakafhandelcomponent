## Why

The `run-unit-tests` job in `.github/workflows/build-test-deploy.yml` now takes more than 11 minutes — longer than the integration tests — making it the critical-path bottleneck of every pull-request and main build. The job runs the backend (`test`) and frontend (`npmRunTestCoverage`) suites in a single Gradle invocation, so they execute one after the other, and the backend suite runs in a single JVM fork that ignores the spare cores on the GitHub runner. Shortening this job directly shortens the wall-clock time of the whole pipeline.

## What Changes

- Split the single `run-unit-tests` job into two jobs that run concurrently: one for the backend unit tests and one for the frontend unit tests. Each downloads only the artefacts it needs.
- Enable parallel execution of the backend unit tests within their job so they use all available runner cores instead of a single JVM fork.
- Keep the existing test-result and coverage uploads (Codecov, `publish-unit-test-result-action`) working, splitting them across the two jobs by suite.
- Update downstream job dependencies (`push-docker-image`) and the `paths-filter`/`merge_group` gating so both new jobs are correctly required and skipped under the same conditions as the old job.

## Capabilities

### New Capabilities
- `ci-parallel-unit-tests`: the CI pipeline runs backend and frontend unit tests as separate concurrent jobs, and runs the backend suite with multi-fork parallelism, so total unit-test wall-clock time is bounded by the slower suite rather than their sum.

### Modified Capabilities
<!-- No existing spec's requirements change. ci-cross-job-artifacts still holds: the new jobs consume the same run-scoped artifacts via download-artifact. -->

## Impact

- `.github/workflows/build-test-deploy.yml` — `run-unit-tests` job replaced by two jobs; `push-docker-image` `needs:` list updated.
- `build.gradle.kts` — backend `test` JVM test suite gains parallel-fork configuration (e.g. `maxParallelForks`).
- No production/runtime code changes. The `ci-cross-job-artifacts` capability is preserved: both new jobs still retrieve build outputs via `actions/download-artifact`.
