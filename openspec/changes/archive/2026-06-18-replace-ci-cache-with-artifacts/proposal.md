## Why

The `build-test-deploy.yml` workflow uses `actions/cache/save` and `actions/cache/restore` to pass temporary build outputs (Gradle build dir, generated Java clients, frontend artefacts, the ZAC JAR, and the Docker image tar) between jobs. The GitHub Actions cache is designed for reusing dependencies across runs, not for handing intermediate outputs between jobs of a single run. Misusing it for run-scoped artefacts pollutes the cache, can collide or evict legitimate dependency caches, never auto-expires per run, and provides no clear linkage to a specific workflow run. GitHub's recommended mechanism for sharing files between jobs in the same run is workflow artifacts.

## What Changes

- Replace each `actions/cache/save` step that stores a run-scoped build output with `actions/upload-artifact`.
- Replace each matching `actions/cache/restore` step in downstream jobs with `actions/download-artifact`.
- Covers the artefacts passed between jobs: Gradle build dir (`build`), generated Java clients (`src/generated`), frontend artefacts (`src/main/app/node_modules`, `src/main/app/src/generated/types`), the built ZAC JAR (`target/zaakafhandelcomponent.jar`), and the Docker image tar (`docker-image.tar`).
- Keep run-scoping by deriving artifact names from the run (no manual cache-key composition needed; artifacts are already scoped to the run).
- Leave genuine dependency caching untouched: `setup-java` Maven cache, `setup-gradle` caching, and the Docker Buildx `type=gha` build cache remain as-is.

## Capabilities

### New Capabilities
- `ci-cross-job-artifacts`: Defines how the build-test-deploy workflow passes run-scoped intermediate build outputs between jobs using GitHub workflow artifacts rather than the GitHub Actions cache.

### Modified Capabilities
<!-- none: no existing spec covers CI artifact passing -->

## Impact

- `.github/workflows/build-test-deploy.yml` — `build`, `run-unit-tests`, `build-docker-image-and-run-itests`, and `push-docker-image` jobs.
- No application code changes.
- Dependency action versions: adds `actions/upload-artifact` and `actions/download-artifact`; removes run-scoped uses of `actions/cache/save` and `actions/cache/restore`.
