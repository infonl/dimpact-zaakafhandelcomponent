## Context

The frontend unit test suite has 205 Jest spec files. In CI the `run-unit-tests` job calls `./gradlew ... test npmRunTestCoverage jacocoTestReport`, where `npmRunTestCoverage` runs `npm run test:report` → `ng test --coverage`. This runs all 205 specs sequentially in a single process with no explicit `maxWorkers` setting and collects coverage simultaneously — the slowest possible configuration. Result: ~11 minutes wall-clock time.

Current state:
- `jest.config.js` has no `maxWorkers` or `workerThreads` setting
- `npm run test:report` uses `ng test --coverage` (Angular builder, which delegates to Jest)
- Coverage collection (`collectCoverage: true`) is bundled into the same run
- No sharding — all specs run in one CI job
- `test:ci` script is broken (`npx jest -t` with no filter argument)

GitHub-hosted runners have 4 vCPUs; Jest defaults to ~50% of CPUs (2 workers). Explicit configuration and sharding are the levers.

## Goals / Non-Goals

**Goals:**
- Reduce frontend unit test wall-clock time in CI from ~11 min to ~4 min
- Make local `npm run test:report` faster by adding explicit `maxWorkers` to jest.config.js
- Split 205 specs into 3 parallel CI shards, each running ~68 specs
- Merge sharded coverage reports before uploading to Codecov
- Fix the broken `test:ci` script

**Non-Goals:**
- Migrate from Jest to Vitest (separate initiative if needed)
- Change test logic or add/remove tests
- Parallelize backend unit tests (already handled in a prior change)

## Decisions

### Decision 1: Jest `--shard` over a matrix of test file globs

Jest 29.x has built-in `--shard=<index>/<total>` support. It divides the test file list evenly and each shard collects its own coverage. Alternative: manual glob patterns per job — fragile when files are added. **Choice: `--shard`** because it auto-balances and requires zero maintenance as the test suite grows.

### Decision 2: 3 shards

205 specs / 3 ≈ 68 per shard. GitHub Actions runners have 4 vCPUs, so 3 parallel jobs each using `maxWorkers=4` is the sweet spot. 4 shards would push below 60s per job and the overhead (job spin-up ~30s, artifact download ~20s) would dominate. **Choice: 3 shards**.

### Decision 3: Separate coverage merge step, not in each shard

Each shard produces a partial `coverage-summary.json` and lcov files. Codecov natively merges flag-partitioned uploads, but to get a single correct summary we use `npx nyc merge` (or `jest --merge-coverage`) in a dedicated merge job. Alternative: upload raw per-shard lcov to Codecov and let it merge server-side — simpler but less reliable. **Choice: upload per-shard lcov directly to Codecov with the same flag** — Codecov merges partial uploads within the same commit automatically, so no explicit merge step is needed.

### Decision 4: Drive shards from GitHub Actions matrix, not Gradle

The CI step that runs `npmRunTestCoverage` is in a Gradle `NpmTask`. Adding shard support to Gradle would require parameterised tasks — possible but adds complexity. Instead, replace the single `run-unit-tests` job with a matrix job `run-frontend-unit-tests` that calls `npx jest --coverage --shard=${{ matrix.shard }}/${{ env.SHARD_TOTAL }}` directly (bypassing the `ng test` builder to use Jest CLI directly, which supports `--shard`). The existing `run-unit-tests` job continues to run backend unit tests (`./gradlew test jacocoTestReport`) without the `npmRunTestCoverage` task. **Choice: matrix job with direct Jest CLI**.

### Decision 5: `maxWorkers=4` in jest.config.js

Setting `maxWorkers` explicitly in `jest.config.js` ensures both local runs and CI runs use all available CPUs on 4-vCPU machines. Local developer machines typically have 8+ cores; Jest will use 4 workers regardless — safe and consistent. CI runners also have 4 vCPUs, so this is optimal there too.

## Risks / Trade-offs

- **Shard imbalance** → Some shards may be faster/slower than others if specs vary in duration. Jest's `--shard` distributes by file count, not execution time. Mitigation: 3 shards is coarse enough that imbalance is unlikely to dominate; revisit if >50% imbalance observed.
- **Coverage merge correctness** → Sharded coverage files cover disjoint test sets; if Codecov merges them per-commit the union should be correct. Risk: if a commit upload is partial (one shard fails), coverage appears lower. Mitigation: use `if: ${{ !cancelled() }}` on upload steps to always upload partial data; Codecov will flag incomplete coverage.
- **Angular builder bypass** → Using `npx jest` directly instead of `ng test` skips the Angular builder. The builder mostly just sets up jest.config.js resolution — since we configure everything in jest.config.js explicitly, this is safe. Risk: Angular-builder-specific options (e.g., tsconfig override) are lost. Mitigation: pass `--config jest.config.js` explicitly to Jest CLI.
- **Broken `test:ci` script** → Fixing it to `jest --coverage --maxWorkers=4 --shard=...` requires callers to pass the shard argument. Mitigation: the CI matrix passes it; local `test:report` is a separate script that doesn't shard.

## Migration Plan

1. Update `jest.config.js` — add `maxWorkers: 4`
2. Update `package.json` — fix `test:ci`, add `test:shard` script
3. Update `build.gradle.kts` — remove `npmRunTestCoverage` from the `test` task chain (keep backend test tasks)
4. Update CI workflow — split `run-unit-tests` into two jobs:
   - `run-backend-unit-tests`: runs `./gradlew test jacocoTestReport`
   - `run-frontend-unit-tests`: matrix (shard: [1, 2, 3]), runs sharded Jest with coverage
5. Update downstream `push-docker-image` `needs:` to reference both new job names
6. Rollback: revert jest.config.js and workflow YAML — no data loss risk

## Open Questions

- Should `test:report` (used locally) also be updated to use `maxWorkers=4` explicitly, or rely on jest.config.js? → Rely on jest.config.js (single source of truth).
- Should we keep `npmRunTestCoverage` Gradle task for local use? → Keep it but it will now use the updated jest.config.js with `maxWorkers=4` automatically.
