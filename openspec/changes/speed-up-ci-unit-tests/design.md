## Context

The `run-unit-tests` job in `.github/workflows/build-test-deploy.yml` runs a single Gradle invocation:

```
./gradlew -x processResources -x classes -x generateJavaClients -x generateJsonSchema2Pojo \
  -x generateOpenApiSpec -x npmInstall test npmRunTestCoverage jacocoTestReport --info
```

Two facts make this the pipeline bottleneck (>11 min, longer than the integration tests):

1. **Serial suites.** `test` (backend, Kotlin/JUnit Jupiter) and `npmRunTestCoverage` (frontend, Jest) run one after the other. Gradle executes tasks within a single project serially; `org.gradle.parallel` is not set, and even if it were it only parallelizes across projects, not tasks within this single-project build. So wall-clock ≈ backend + frontend.
2. **Single-fork backend.** The `test` JVM test suite (`build.gradle.kts`, `testing { suites { getByName<JvmTestSuite>("test") }`) has no `maxParallelForks`, so the whole backend suite runs in one JVM fork and leaves the GitHub runner's other cores idle. `ubuntu-24.04` standard runners have 4 vCPUs.

The build already passes run-scoped outputs between jobs via `actions/upload-artifact` / `download-artifact` (capability `ci-cross-job-artifacts`): `gradle-build`, `generated-java-clients`, `frontend-artefacts`, `zac-jar`. This change builds on that and must not break it.

## Goals / Non-Goals

**Goals:**
- Reduce unit-test wall-clock so it is bounded by the slower suite, not the sum.
- Use the runner's spare cores for the backend suite.
- Preserve all existing reporting: JUnit + Jest result publication and the three Codecov uploads (`backendunittests`, `frontendunittests`, plus the `report_type: test_results` upload).
- Keep gating identical to today (`paths-filter`, `merge_group` exclusion, `main`/`hotfix` overrides) and keep `push-docker-image` correctly gated.

**Non-Goals:**
- No change to which tests exist or to production code.
- Not introducing larger/self-hosted runners (kept as a future option).
- Not changing the integration-test or build jobs.
- Not changing dependency caching (`setup-java` Maven cache, Gradle setup cache).

## Decisions

### Decision 1: Split `run-unit-tests` into `run-backend-unit-tests` and `run-frontend-unit-tests`

Two jobs, each `needs: [build, paths-filter]`, running concurrently.

- **Backend job**: downloads `gradle-build` + `generated-java-clients`; runs `./gradlew ... test jacocoTestReport`; publishes backend JUnit results (`build/test-results/test/**/*.xml`); uploads JaCoCo report (`backendunittests` flag) and the `test_results` report.
- **Frontend job**: downloads + extracts `frontend-artefacts`; runs `./gradlew ... npmRunTestCoverage` (or `npm run test:report` directly); publishes Jest results (`src/main/app/reports/*.xml`); uploads frontend coverage (`frontendunittests` flag).

Wall-clock becomes `max(backend, frontend)` instead of `backend + frontend`.

*Why over alternatives:* Running both suites in one job with Gradle `--parallel` does not help — it is a single-project build, so the two tasks still run serially. Separate jobs is the only way to truly overlap them on GitHub Actions, and it also lets each job download a smaller artefact set (the backend job no longer needs to extract `node_modules`, the frontend job no longer needs the Gradle build dir).

*Cost:* Each job pays its own setup (checkout, JDK, Gradle, artefact download). This fixed overhead is small relative to the multi-minute suites and is hidden because the jobs overlap.

### Decision 2: Enable multi-fork parallelism for the backend `test` suite

In `build.gradle.kts`, configure the `test` task:

```kotlin
getByName<JvmTestSuite>("test") {
    useJUnitJupiter()
    targets.all {
        testTask.configure {
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        }
    }
}
```

`maxParallelForks` runs multiple JVM forks, each running a subset of test classes — this is process-level isolation, the safest form of test parallelism (no shared in-JVM state). Start with `processors / 2` (≈2 forks on a 4-vCPU runner) to balance fork count against each fork's memory under the 4g heap, then tune.

*Why over alternatives:* JUnit Jupiter's in-JVM parallel execution (`junit.jupiter.execution.parallel.enabled`) shares one JVM and is fragile against tests that touch shared statics/MockK state; `maxParallelForks` isolates per process and needs no per-test annotations. The two can be combined later if needed.

*Local impact:* Faster local `./gradlew test` too; bounded by `maxParallelForks` so it does not starve developer machines.

### Decision 3: Reporting split, gating preserved

- The `publish-unit-test-result-action` step is duplicated per job with suite-specific `check_name` (e.g. `backend-unit-test-results`, `frontend-unit-test-results`) and file globs, so each suite still surfaces a check.
- Both jobs copy the existing `if:` gate verbatim from `run-unit-tests`.
- `push-docker-image.needs` replaces `run-unit-tests` with `[run-backend-unit-tests, run-frontend-unit-tests]`.

## Risks / Trade-offs

- **Flaky tests under parallel forks** (order/shared-resource assumptions surface) → Start at `processors / 2`, run the suite a few times on a branch before merge; `maxParallelForks` isolates per process which avoids most in-JVM shared-state issues. Fall back to 1 fork if instability appears.
- **Per-fork memory pressure** under the existing `-Xmx4g` Gradle JVM args → Conservative fork count (≈2); monitor for OOM in CI and lower if needed.
- **Codecov sees two uploads instead of one** → Flags (`backendunittests`, `frontendunittests`) are already distinct per suite today, so coverage merging is unaffected. Codecov test analytics needs the Jest results too, so the `report_type: "test_results"` upload is duplicated in both jobs (each uploading its own suite's results); Codecov aggregates them.
- **Two checks instead of one** in PR UI → Acceptable and arguably clearer; branch-protection required-checks list must be updated to reference the new job/check names.
- **Slight increase in total runner-minutes** (two setups) → Accepted; the goal is wall-clock latency, and the overlap keeps added latency near zero.

## Migration Plan

1. Add `maxParallelForks` to the backend `test` suite in `build.gradle.kts`; verify `./gradlew test` passes locally.
2. Replace the `run-unit-tests` job with the two new jobs; update `push-docker-image.needs`.
3. Update the repository branch-protection required status checks to the new job/check names.
4. Validate on a PR: confirm both jobs run in parallel, results + coverage appear, and total unit-test wall-clock dropped.
5. Rollback: revert the workflow and `build.gradle.kts` changes (single commit/PR) — no data or runtime state involved.

## Open Questions

- Optimal `maxParallelForks` value for the 4-vCPU runner given `-Xmx4g` — settle empirically (2 vs 3) after the first runs.
