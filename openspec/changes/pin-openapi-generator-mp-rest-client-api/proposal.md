## Why

`jboss-logmanager` is checked against "the module actually provisioned under `modules/system/layers/base/org/jboss/logmanager/main` in a built WildFly server" per the comment in `libs.versions.toml` and `updatingDependencies.md`. Today that check is manual and easy to forget; the built WildFly server (installed locally via `scripts/wildfly/install-wildfly.sh`) already contains the actual bundled `jboss-logmanager` module version, so this can be verified with a script instead of by hand.

Unlike the other manually-versioned "dependencies provided by WildFly" entries, `org.jboss.logmanager:jboss-logmanager` is not in the "Dependencies provided by WildFly" disabled `packageRule` in `renovate.json`, so Renovate still opens ad hoc bump PRs for it (confirmed: commit `33a36b18b`, "update dependency org.jboss.logmanager:jboss-logmanager to v3") - PRs that skip the verification check entirely, since they bump toward whatever is latest on Maven Central rather than what the target WildFly version actually bundles.

(A second gap was originally suspected for `openapi-generator-eclipse-microprofile-rest-client-api`, pinned to `3.0` because the OpenAPI Generator library does not support newer Eclipse MicroProfile Rest Client API versions. Investigation during implementation found no gap there: this catalog entry has no `[libraries]` entry referencing it via `version.ref` - it's used only as a raw template string in `build.gradle.kts`, not as a real Gradle dependency coordinate - so Renovate's gradle/version-catalog manager cannot resolve a package identity for it and has never touched it (confirmed: unchanged across the entire git history of `libs.versions.toml`, including many renovate-bot commits that bumped sibling entries before they moved under WildFly BOM coverage). No Renovate rule is needed; a comment documenting this is added instead, in case the entry is ever wired to a `[libraries]` entry in the future.)

## What Changes

- Add a script that, given an installed WildFly server directory (as produced by `scripts/wildfly/install-wildfly.sh`), reads the actual `jboss-logmanager` module version from its `module.xml` and compares it to the `jboss-logmanager` version in `gradle/libs.versions.toml`, failing with a clear message on mismatch.
- Document the script's usage in `updatingDependencies.md`, replacing the current manual instruction for `jboss-logmanager` with a pointer to running the script.
- Fold `org.jboss.logmanager:jboss-logmanager` into the existing "Dependencies provided by WildFly" disabled `packageRule` in `renovate.json`, stopping Renovate from opening bump PRs for it that skip the verification script.
- Add a comment next to `openapi-generator-eclipse-microprofile-rest-client-api` in `gradle/libs.versions.toml` explaining why it needs no Renovate rule today, and what would change that.

## Capabilities

None — this is tooling/CI configuration only; no ZAC application behavior changes.

## Impact

- New script under `scripts/wildfly/` for the jboss-logmanager version check.
- `docs/development/updatingDependencies.md` — updated instructions.
- `gradle/libs.versions.toml` — one added comment, no version change.
- `renovate.json` — `jboss-logmanager` added to the existing WildFly-provided-dependencies disabled `packageRule`.
- No impact on production code, runtime behavior, or APIs.
