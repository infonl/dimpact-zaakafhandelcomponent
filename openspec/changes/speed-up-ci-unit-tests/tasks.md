## 1. Backend test parallelism (build.gradle.kts)

- [x] 1.1 In the `testing { suites { getByName<JvmTestSuite>("test") } }` block, configure the test task with `maxParallelForks` derived from available processors (start with `(availableProcessors / 2).coerceAtLeast(1)`)
- [x] 1.2 Run `./gradlew test` locally and confirm all backend unit tests still pass with multiple forks
- [x] 1.3 Run `./gradlew spotlessApply detektApply` and verify the build config is clean

## 2. Split the unit-test workflow job (.github/workflows/build-test-deploy.yml)

- [x] 2.1 Add a `run-backend-unit-tests` job: `needs: [build, paths-filter]`, copy the existing `run-unit-tests` `if:` gate, set up JDK + Gradle, download `gradle-build` and `generated-java-clients` artefacts
- [x] 2.2 In `run-backend-unit-tests`, run Gradle with the existing `-x` exclusions for `test jacocoTestReport` (drop `npmRunTestCoverage` and the frontend-only exclusions)
- [x] 2.3 Add a `run-frontend-unit-tests` job: `needs: [build, paths-filter]`, same `if:` gate, set up JDK + Gradle, download and extract `frontend-artefacts`
- [x] 2.4 In `run-frontend-unit-tests`, run `npmRunTestCoverage` (with `npmInstall` excluded since artefacts are restored)
- [x] 2.5 Remove the old `run-unit-tests` job

## 3. Preserve reporting

- [x] 3.1 In `run-backend-unit-tests`, publish backend JUnit results via `publish-unit-test-result-action` (`check_name: backend-unit-test-results`, files `build/test-results/test/**/*.xml`)
- [x] 3.2 In `run-backend-unit-tests`, upload JaCoCo coverage to Codecov with the `backendunittests` flag, and the `report_type: "test_results"` upload
- [x] 3.3 In `run-frontend-unit-tests`, publish Jest results via `publish-unit-test-result-action` (`check_name: frontend-unit-test-results`, files `src/main/app/reports/*.xml`)
- [x] 3.4 In `run-frontend-unit-tests`, upload frontend coverage to Codecov with the `frontendunittests` flag, and the frontend `report_type: "test_results"` upload

## 4. Update dependencies and gating

- [x] 4.1 In `push-docker-image`, replace `run-unit-tests` in `needs:` with `run-backend-unit-tests` and `run-frontend-unit-tests`
- [x] 4.2 Confirm both new jobs use the same `if:` condition as the removed job so they skip identically (merge_group, paths-filter, main/hotfix)
- [ ] 4.3 Update the repository branch-protection required status checks to reference the new job/check names

## 5. Validate

- [ ] 5.1 Open a PR and confirm `run-backend-unit-tests` and `run-frontend-unit-tests` start and run concurrently
- [ ] 5.2 Confirm backend and frontend test results and both Codecov coverage flags appear as before
- [ ] 5.3 Confirm the total unit-test wall-clock time is meaningfully lower than the previous single job; tune `maxParallelForks` if needed
- [ ] 5.4 Confirm `push-docker-image` and downstream jobs still gate correctly on `main`
