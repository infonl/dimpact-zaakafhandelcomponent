## Context

See `proposal.md` - Why. `jboss-logmanager`'s required manual check against the built WildFly server's bundled module is not automated anywhere.

`scripts/wildfly/install-wildfly.sh` already installs WildFly locally into `wildfly-$WILDFLY_VERSION/` (relative to the repo root) using Galleon, reading the WildFly version from `pom.xml`. `scripts/python/` already hosts Python dependency-checking scripts (`scripts/python/dependencies/versions.py`) with an established venv setup (`init-pyenv.sh`).

The originally suspected second gap - Renovate proposing incompatible bumps for `openapi-generator-eclipse-microprofile-rest-client-api` - turned out not to exist (see `proposal.md` - Why): that catalog entry has no `[libraries]` reference, so Renovate's gradle manager cannot manage it. No Renovate rule is needed for it; only a documenting comment.

## Goals / Non-Goals

**Goals:**
- Give developers a scriptable, repeatable way to verify the `jboss-logmanager` version in `libs.versions.toml` against what a built WildFly server actually bundles, replacing the manual instruction in `updatingDependencies.md`.
- Document why `openapi-generator-eclipse-microprofile-rest-client-api` needs no Renovate rule today, so a future maintainer doesn't have to re-derive it.

**Non-Goals:**
- Not attempting to automate the `jakarta-jakartaee` check - no WildFly BOM or bundled artifact tracks this Jakarta EE spec-version claim (confirmed by inspecting the `wildfly-ee` and `wildfly-expansion` BOM poms for WildFly 41.0.1.Final - neither lists `jakarta.platform:jakarta.jakartaee-api`), so there is nothing machine-readable to diff against. Out of scope for this change.
- Not wiring the jboss-logmanager check into CI as a required, always-run gate. It runs on demand (locally or as an optional CI step), the same way the existing Python dependency scripts do, since it depends on a locally-installed WildFly server that isn't otherwise part of every build.
- Not adding a Renovate `packageRule` for `openapi-generator-eclipse-microprofile-rest-client-api` - there is nothing for such a rule to do today (see Context).

## Decisions

**jboss-logmanager verification script**: a shell script (`scripts/wildfly/verify-jboss-logmanager-version.sh`), matching the existing shell-script style of `scripts/wildfly/install-wildfly.sh` rather than the Python scripts, since it operates directly on the WildFly install produced by that script and needs no external HTTP calls (unlike the Python `dependencies/versions.py`, which queries external release-notes sources). It:
1. Locates the installed WildFly server directory (`wildfly-$WILDFLY_VERSION`, same convention as `install-wildfly.sh`).
2. Reads the bundled version from `modules/system/layers/base/org/jboss/logmanager/main/module.xml` (the module's `<resource-root path="..."/>` jar filename encodes the version, e.g. `jboss-logmanager-2.1.19.Final.jar`).
3. Reads the `jboss-logmanager` version from `gradle/libs.versions.toml`.
4. Compares the two, printing a clear match/mismatch message and exiting non-zero on mismatch.

Alternative considered: extend the Python `dependencies/versions.py` script instead - rejected because that script's purpose is checking *latest available* versions of external components, whereas this check is a *consistency* check against an already-installed local WildFly server, a different concern better kept as a small, dependency-free shell script.

## Risks / Trade-offs

- [WildFly's module directory layout or `module.xml` format changes across WildFly major versions, breaking the parsing] → Mitigation: script only needs to keep working across the WildFly versions actually in use; a parsing failure should produce a clear error rather than a false pass/fail, so a broken parse is caught immediately during manual WildFly upgrades.
- [Someone later wires `openapi-generator-eclipse-microprofile-rest-client-api` to a `[libraries]` entry, making it Renovate-manageable again, without noticing the earlier investigation] → Mitigation: the added comment states the current no-`[libraries]`-reference fact directly, so it goes stale (and prompts a fix) the moment that changes, rather than being an inference someone has to re-derive.

## Migration Plan

Not applicable - additive tooling change, no rollback concerns beyond reverting the script and documentation changes.
