## 1. jboss-logmanager verification script

- [x] 1.1 Create `scripts/wildfly/verify-jboss-logmanager-version.sh` that locates the installed `wildfly-$WILDFLY_VERSION` directory (same convention as `scripts/wildfly/install-wildfly.sh`), reads the bundled `jboss-logmanager` jar version from `modules/system/layers/base/org/jboss/logmanager/main/module.xml`, reads the `jboss-logmanager` version from `gradle/libs.versions.toml`, and exits non-zero with a clear message on mismatch (zero and a clear message on match).
- [x] 1.2 Add the SPDX header required for `.sh` files per project convention, and verify the script runs successfully (exit 0) against a freshly installed local WildFly server where the catalog version is up to date.
- [x] 1.3 Verify the script correctly detects and reports a mismatch by temporarily editing the `jboss-logmanager` version in `gradle/libs.versions.toml` to a wrong value, running the script, confirming it exits non-zero, then reverting the edit.

## 2. Documentation

- [x] 2.1 Update `docs/development/updatingDependencies.md`'s WildFly upgrade section to replace the manual `jboss-logmanager` module-checking instruction with a pointer to running `scripts/wildfly/verify-jboss-logmanager-version.sh`.
- [x] 2.2 Add a comment next to `openapi-generator-eclipse-microprofile-rest-client-api` in `gradle/libs.versions.toml` noting it has no `[libraries]` entry referencing it, so Renovate cannot and does not manage it (verified: unchanged across all history despite sibling entries being bumped by renovate-bot before they moved under WildFly BOM coverage) - if it's ever wired to a `[libraries]` entry, a Renovate disable rule must be added at that point, matching the existing "Dependencies provided by WildFly" packageRule style.
