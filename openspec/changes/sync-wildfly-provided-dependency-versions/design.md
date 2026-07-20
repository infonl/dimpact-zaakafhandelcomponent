## Context

`gradle/libs.versions.toml` had a marker-delimited block pinning ~9 dependencies whose versions must exactly match what the currently-used WildFly release bundles: `eclipse-microprofile-rest-client-api`, `eclipse-microprofile-config-api`, `eclipse-microprofile-fault-tolerance-api`, `eclipse-microprofile-health-api`, `smallrye-health`, `hibernate-validator`, `jakarta-jakartaee`, `jboss-resteasy-multipart-provider`, `wildfly-security-elytron-http-oidc`. `docs/development/updatingDependencies.md` step 7 told the developer to manually cross-reference WildFly's own `pom.xml`/module files on GitHub for each of these on every upgrade.

An earlier iteration of this design (see the "verify against the published channel manifest" approach) built a Gradle task that downloaded WildFly's channel manifest YAML and diffed it against the hand-pinned versions, failing `check` on drift and offering an auto-update task. That approach worked, but it was solving the wrong layer of the problem: it kept the versions manually declared in `gradle/libs.versions.toml` and only automated *checking* them. Investigating further (prompted by the question "can this not come from a WildFly BOM instead?") found that WildFly publishes proper Maven BOMs for exactly this purpose:

- `org.wildfly.bom:wildfly-ee:<version>` — core Jakarta EE / server dependencies (covers `hibernate-validator`, `jboss-resteasy-multipart-provider`, `wildfly-security-elytron-http-oidc`)
- `org.wildfly.bom:wildfly-expansion:<version>` — MicroProfile layer (covers all 4 `eclipse-microprofile-*` APIs)

Gradle can import a Maven BOM directly as a `platform()` dependency; once imported, a dependency declared without a version gets it from the BOM. This was verified hands-on for `wildfly.version = 41.0.0.Final`: importing both BOMs on the `providedCompile` configuration and dropping the explicit `version.ref` from the 7 covered `gradle/libs.versions.toml` entries resolves to the exact same versions previously hand-pinned (`./gradlew dependencies --configuration providedCompile` confirmed `9.1.1.Final`, `7.0.2.Final`, `2.9.2.Final`, `3.1.1`, `4.1.2`, `4.0.1`, `4.0`), and a full `compileKotlin`/`compileJava`/`compileTestKotlin`, `war`, and targeted `test` run all pass unchanged. This is strictly better than the manifest-verification approach: there is nothing left to keep in sync for those 7 dependencies, because there is no longer a second copy of the version to drift.

`jakarta-jakartaee` and `smallrye-health` are not in either BOM:
- `jakarta-jakartaee` (`jakarta.platform:jakarta.jakartaee-api`) tracks the Jakarta EE platform spec version WildFly implements (e.g. `11.0.0` for Jakarta EE 11), not an individually-versioned WildFly component — it isn't the kind of thing a component BOM would list, and it only changes on a Jakarta EE spec bump, not every WildFly patch release.
- `smallrye-health` is only used as `testImplementation`, as the reference MicroProfile Health implementation for unit tests that don't run inside a full WildFly (already documented in the existing code comment) — it does not need to match the runtime exactly.

Separately, we confirmed (via `git grep`) that none of the 9 dependencies can simply be removed: the codebase directly imports vendor-specific classes with no standard equivalent (`org.hibernate.validator.constraints.Length`; `org.wildfly.security.http.oidc.OidcPrincipal`/`OidcSecurityContext`; `org.jboss.resteasy.annotations.providers.multipart.MultipartForm`, `org.jboss.resteasy.plugins.providers.multipart.{InputPart,MultipartFormDataOutput}`, `org.jboss.resteasy.annotations.{Body,ResponseObject}`). Removing these dependencies would require rewriting that production code, which is out of scope.

Separately, the WFLY-20810 proposal (`wildfly:image` goal / bootable JAR on plain OpenJDK cloud images) that triggered this investigation has no bearing on dependency versions — it's a packaging/deployment concern (see `proposal.md`). It is not part of this design.

## Goals / Non-Goals

**Goals:**
- Eliminate hand-copying WildFly-provided dependency versions for the dependencies WildFly's own BOMs cover, by sourcing them directly from those BOMs at build time.
- Keep `pom.xml`'s `wildfly.version` as the single source of truth for "which WildFly release" — the Gradle side reads it, it does not duplicate it.
- Reduce the manual-verification surface documented in `updatingDependencies.md` to the 2 dependencies that genuinely cannot come from a BOM.

**Non-Goals:**
- Not adopting WFLY-20810 / `wildfly:image` — unrelated to this change.
- Not auto-bumping `wildfly.version` itself — upgrading WildFly is still a deliberate, reviewed decision (galleon layers, WildFly release notes, etc. still need human review per `updatingDependencies.md`).
- Not removing any of the 9 WildFly-provided dependencies — confirmed via `git grep` that the codebase directly uses vendor-specific classes from `hibernate-validator`, `resteasy-multipart-provider`, and `wildfly-elytron-http-oidc` that have no standard equivalent.
- Not building custom Gradle tasks to verify/sync versions (the earlier approach) — superseded by the BOM import, which needs no verification because there is nothing to drift.
- Not replacing Renovate for any dependency it already manages; this only covers the WildFly-provided block, which Renovate deliberately does not touch.

## Decisions

**Use WildFly's own Maven BOMs as Gradle platforms, not a manifest-verification task.** Rationale: a BOM import means the version is never copied into our repo at all — there is nothing to verify, update, or drift. This is simpler and more robust than parsing WildFly's channel manifest and diffing it against a hand-maintained copy. Alternative considered (and initially implemented, then discarded): a `verifyWildflyProvidedDependencyVersions`/`updateWildflyProvidedDependencyVersions` Gradle task pair that downloaded `org.wildfly.channels:wildfly[-ee]:<version>:manifest@yaml` and diffed it against `gradle/libs.versions.toml`. Discarded once the BOM approach was confirmed to work, since it addresses the same problem one layer higher with no ongoing verification burden.

**Which BOMs to import**: `org.wildfly.bom:wildfly-ee` and `org.wildfly.bom:wildfly-expansion`, both at `wildfly.version`. These two together cover 7 of the 9 WildFly-provided dependencies (confirmed by inspecting their published POMs on Maven Central). `org.wildfly.bom:wildfly` (the older, non-suffixed BOM) was considered and rejected — it stopped being published past `33.0.2.Final`, so it does not cover our current WildFly version.

**Where the BOMs are imported**: as `platform(...)` dependencies on the `providedCompile` configuration in the existing `dependencies {}` block in `build.gradle.kts`, alongside the dependencies they cover. Alternative considered: importing them on a broader configuration (e.g. `implementation`) — rejected because these BOM-covered libraries are all `providedCompile` (WildFly supplies them at runtime), and scoping the platform import to the same configuration keeps its influence limited to exactly the dependencies it's meant to version.

**How the WildFly version is obtained**: parse `pom.xml`'s `<wildfly.version>` property with `javax.xml.parsers.DocumentBuilderFactory` at build-script-configuration time (kept from the earlier iteration of this design, since the rationale for a single source of truth still applies). Alternative considered: duplicating the version into `gradle.properties` or the TOML — rejected for the same reason as before, it would create a second source of truth.

**Dependency declarations without a version**: the 7 BOM-covered `gradle/libs.versions.toml` `[libraries]` entries drop their `version.ref` entirely (Gradle version catalogs allow a library entry with only `group`/`name`), so their version is supplied solely by the imported platform. Alternative considered: keeping a `version.ref` "for documentation" — rejected, since a value that is no longer authoritative is worse than no value: it invites someone to "fix" it back into a hand-copied constant.

**The 2 remaining manual entries**: `jakarta-jakartaee` and `smallrye-health` keep an explicit `version.ref`, each with an inline comment (in `gradle/libs.versions.toml`) explaining why it isn't BOM-covered, and `updatingDependencies.md` is updated to only mention these two for manual verification.

## Risks / Trade-offs

- [A future WildFly release stops publishing `wildfly-ee`/`wildfly-expansion`, or splits/renames them] → the `providedCompile` dependency resolution would fail loudly at build time (unresolvable platform coordinate) rather than silently using a stale version, so this fails safe; would need a follow-up change to adjust the BOM coordinates.
- [Importing a platform onto `providedCompile` could in principle influence the resolved version of some other, unrelated transitive dependency shared with the BOM] → verified empirically: `compileKotlin`, `compileJava`, `compileTestKotlin`, `war`, and the targeted unit tests covering the vendor-specific usages all pass unchanged, and the built WAR's contents are unchanged (`providedCompile` is excluded from packaging as before).
- [`jakarta-jakartaee` and `smallrye-health` stay manual] → documented as known, explicit gaps in both `design.md` here and in `updatingDependencies.md`, with the specific reason for each, not left implicit.

## Migration Plan

1. Add the `platform(...)` imports for `wildfly-ee` and `wildfly-expansion` to the `providedCompile` block in `build.gradle.kts`, and drop `version.ref` from the 7 covered `gradle/libs.versions.toml` entries.
2. Confirm `./gradlew dependencies --configuration providedCompile` resolves the expected versions, and that `compileKotlin`/`compileJava`/`compileTestKotlin`, `war`, and the affected unit tests pass (done during this design).
3. Update `docs/development/updatingDependencies.md` step 7 to reflect the smaller manual surface (only `jakarta-jakartaee` and `smallrye-health`).
4. No rollback concerns beyond reverting the commit — the change is build-configuration only and does not affect runtime behavior or the produced bootable JAR.

## Open Questions

- None outstanding.
