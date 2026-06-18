## Context

`build-test-deploy.yml` currently abuses the GitHub Actions cache to hand intermediate build outputs from the `build` job to downstream jobs within the same run:

| Output | Producer | Consumer(s) | Current key |
|---|---|---|---|
| `build` (Gradle build dir) | `build` | `run-unit-tests`, `build-docker-image-and-run-itests` | `build-gradle-artefacts-<repo>-<ref>-<run_number>` |
| `src/generated` (Java clients) | `build` | `run-unit-tests` | `generated-java-clients-<repo>-<ref>-<run_number>` |
| `src/main/app/node_modules` + `src/main/app/src/generated/types` | `build` | `run-unit-tests` | `build-frontend-artefacts-<repo>-<ref>-<run_number>` |
| `target/zaakafhandelcomponent.jar` | `build` | `build-docker-image-and-run-itests` | `zac-jar-<repo>-<ref>-<run_number>` |
| `docker-image.tar` | `build-docker-image-and-run-itests` | `push-docker-image` | `docker-image-<repo>-<ref>-<run_number>` |

Every key already embeds `${{ github.run_number }}`, confirming these are run-scoped and not meant to be reused across runs — exactly the workflow-artifact use case. The cache also separately holds genuine dependency caches (`setup-java` Maven, `setup-gradle`, Docker Buildx `type=gha`), which must stay.

## Goals / Non-Goals

**Goals:**
- Pass the five run-scoped outputs between jobs via `actions/upload-artifact` / `actions/download-artifact`.
- Keep run isolation — a job only ever sees artefacts from its own run.
- Preserve all genuine dependency caching unchanged.

**Non-Goals:**
- Changing what is built, tested, or pushed.
- Touching the Docker Buildx `type=gha` build cache or the Maven/Gradle setup caches.
- Optimizing artifact size/retention beyond a sensible short retention.

## Decisions

**Use `actions/upload-artifact@v4` + `actions/download-artifact@v4`, pinned by SHA.**
The repo pins every action to a commit SHA with a version comment; follow that convention. v4 is required for per-job artifact isolation and the faster upload/download backend.

**One named artifact per logical output, not one combined artifact.**
Consumers need different subsets (`run-unit-tests` needs build dir + generated clients + frontend; `build-docker-image-and-run-itests` needs the JAR + later the build dir). Separate artifacts (`gradle-build`, `generated-java-clients`, `frontend-artefacts`, `zac-jar`, `docker-image`) let each consumer download only what it needs and map 1:1 to today's cache entries.

**Artifact names need no run suffix.**
Unlike the cache (global across runs), artifacts are already scoped to the run — `download-artifact` only sees artefacts uploaded within the same run. So names drop the `<repo>-<ref>-<run_number>` suffix the cache keys carried.

**Short retention.**
Set `retention-days` low (e.g. 1) since these are throwaway intra-run handoffs; avoids accumulating storage.

**Path fidelity.**
Upload/download `path:` values mirror the existing cache `path:` blocks exactly so the same files land in the same locations for downstream steps (notably the JaCoCo re-download of `build` in the itest job, and the multi-path frontend artefact).

Alternatives considered: keep cache but add explicit eviction — rejected, still misuses the cache and risks colliding with dependency caches. Single tarball artifact — rejected, forces consumers to download unneeded data.

## Risks / Trade-offs

- **Artifact contains many small files (e.g. `node_modules`)** → upload/download can be slower than cache for huge trees. Mitigation: artifacts are zipped by the action; acceptable for intra-run handoff, and `node_modules` was already cached as-is.
- **`download-artifact` default unpacks into the working dir** → must set `path:` to restore to original location. Mitigation: set explicit `path:` matching the upload layout per artifact.
- **Conditional upload of `docker-image.tar`** (only on `main`/`hotfix/*`) → the matching download in `push-docker-image` is already gated by the same condition, so no missing-artifact failures. Mitigation: keep the existing `if:` guards on both ends.

## Migration Plan

1. In `build`, replace the four `actions/cache/save` steps with `upload-artifact` steps.
2. In `run-unit-tests`, replace the three `actions/cache/restore` steps with `download-artifact` steps.
3. In `build-docker-image-and-run-itests`, replace the JAR restore and the later build-dir restore with `download-artifact`; replace the `actions/cache/save` of `docker-image.tar` with `upload-artifact`.
4. In `push-docker-image`, replace the `docker-image.tar` restore with `download-artifact`.
5. Validate on a PR run that all jobs pass and artifacts appear under the run.

Rollback: revert the workflow file; cache-based handoff is restored immediately on next run.
