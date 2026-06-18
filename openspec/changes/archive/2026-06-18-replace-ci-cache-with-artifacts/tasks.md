## 1. Pin artifact actions

- [x] 1.1 Resolve the commit SHA for `actions/upload-artifact@v4` and add it with a `# v4.x.x` version comment (match repo pinning convention)
- [x] 1.2 Resolve the commit SHA for `actions/download-artifact@v4` and add it with a `# v4.x.x` version comment

## 2. `build` job — upload outputs

- [x] 2.1 Replace "Cache Gradle build artefacts" (`actions/cache/save`, path `build`) with `upload-artifact` named `gradle-build`, `retention-days: 1`
- [x] 2.2 Replace "Cache generated Java clients" (path `src/generated`) with `upload-artifact` named `generated-java-clients`
- [x] 2.3 Replace "Cache frontend build artefacts": tar `src/main/app/node_modules` + `src/main/app/src/generated/types` into `frontend-artefacts.tar.gz` (tar preserves dotfiles like `node_modules/.bin` and exec bits that `upload-artifact` would drop), then `upload-artifact` named `frontend-artefacts`
- [x] 2.4 Replace "Cache built ZAC JAR" (path `target/zaakafhandelcomponent.jar`) with `upload-artifact` named `zac-jar`

## 3. `run-unit-tests` job — download outputs

- [x] 3.1 Replace "Restore Gradle build artefacts" with `download-artifact` name `gradle-build`, `path: build`
- [x] 3.2 Replace "Restore built frontend artefacts": `download-artifact` name `frontend-artefacts`, then extract `frontend-artefacts.tar.gz` at the workspace root (restores original frontend paths with dotfiles + permissions)
- [x] 3.3 Replace "Restore generated Java clients" with `download-artifact` name `generated-java-clients`, `path: src/generated`

## 4. `build-docker-image-and-run-itests` job

- [x] 4.1 Replace "Restore built ZAC JAR" with `download-artifact` name `zac-jar`, `path: target`
- [x] 4.2 Replace the later "Restore Gradle build artefacts" (for JaCoCo) with `download-artifact` name `gradle-build`, `path: build`
- [x] 4.3 Replace "Cache ZAC Docker Image" (`actions/cache/save`, path `docker-image.tar`) with `upload-artifact` named `docker-image`, keeping the `if: main || hotfix/*` guard

## 5. `push-docker-image` job

- [x] 5.1 Replace "Restore Docker Image" with `download-artifact` name `docker-image` (path = repo root, where `docker load` expects `docker-image.tar`)

## 6. Verify

- [x] 6.1 Confirm no `actions/cache/save` or `actions/cache/restore` steps remain for run-scoped outputs (Maven/Gradle/Buildx caches untouched)
- [x] 6.2 Lint/validate the workflow YAML (e.g. `actionlint` or a syntax check)
- [ ] 6.3 Trigger a PR run and confirm all jobs pass and the five artifacts appear under the run
