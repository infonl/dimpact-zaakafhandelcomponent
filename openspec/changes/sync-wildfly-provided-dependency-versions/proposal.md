## Why

Every WildFly upgrade required a developer to manually look up and hand-copy ~9 dependency versions (MicroProfile APIs, Hibernate Validator, RESTEasy multipart provider, Elytron OIDC, SmallRye Health) into the `# Versions of dependencies provided by WildFly` block in `gradle/libs.versions.toml`, cross-referencing WildFly's own `pom.xml` / module files on GitHub by hand. This is tedious, error-prone, and easy to forget or get subtly wrong (a mismatched version can cause hard-to-diagnose `NoSuchMethodError`/`LinkageError` failures at runtime inside WildFly, since these dependencies must exactly match what the server itself provides).

WildFly publishes proper Maven BOMs (`org.wildfly.bom:wildfly-ee` and `org.wildfly.bom:wildfly-expansion`) that pin the exact version of every dependency it provides, and Gradle can import a Maven BOM directly as a `platform()` dependency. Once imported, dependencies declared without an explicit version get their version from the BOM automatically. This was verified hands-on: 7 of our 9 "provided by WildFly" dependencies (all 4 MicroProfile APIs, Hibernate Validator, RESTEasy multipart provider, WildFly Elytron OIDC) are covered by these BOMs, and importing them and dropping the explicit versions resolves to exactly the same versions that were previously hand-maintained (confirmed via `./gradlew dependencies --configuration providedCompile`), with a full compile, WAR build, and targeted test run all passing unchanged. This eliminates hand-copying almost entirely, rather than just automating the copy step.

The remaining 2 dependencies (`jakarta-jakartaee`, `smallrye-health`) are not covered by either BOM and stay manually pinned, each for a distinct, low-risk reason documented in `gradle/libs.versions.toml`.

We also confirmed the 9 dependencies genuinely cannot simply be removed: `git grep` shows the codebase directly imports vendor-specific classes with no standard equivalent — `org.hibernate.validator.constraints.Length`, `org.wildfly.security.http.oidc.OidcPrincipal`/`OidcSecurityContext`, and several `org.jboss.resteasy.*` annotations (`@MultipartForm`, `@Body`, `@ResponseObject`, `InputPart`, `MultipartFormDataOutput`). Dropping these dependencies would require rewriting that production code to avoid the vendor extensions, which is out of scope here.

Note: the WFLY-20810 proposal (bootable JAR packaging for the cloud / `wildfly:image` goal) referenced as the trigger for this investigation is unrelated to dependency versioning — it only changes how the bootable JAR is turned into a container image and has no effect on dependency version management. It does not need to be adopted for this change, and is out of scope here.

## What Changes

- Import WildFly's own published BOMs (`org.wildfly.bom:wildfly-ee` and `org.wildfly.bom:wildfly-expansion`, both at the `wildfly.version` pinned in `pom.xml`) as Gradle `platform()` dependencies on the `providedCompile` configuration in `build.gradle.kts`.
- Drop the explicit `version.ref` from the 7 `gradle/libs.versions.toml` library entries now covered by those BOMs (`eclipse-microprofile-rest-client-api`, `eclipse-microprofile-config-api`, `eclipse-microprofile-health-api`, `eclipse-microprofile-fault-tolerance-api`, `hibernate-validator`, `jboss-resteasy-multipart-provider`, `wildfly-security-elytron-http-oidc`), so their version is supplied by the BOM instead of hand-copied.
- Keep `jakarta-jakartaee` and `smallrye-health` explicitly pinned, each with a comment explaining why it is not BOM-covered.
- Add a small helper in `build.gradle.kts` that reads `wildfly.version` from `pom.xml`, so the WildFly version used for the BOM import stays a single source of truth rather than being duplicated.
- Update `docs/development/updatingDependencies.md` (WildFly upgrade steps) and the comments in `gradle/libs.versions.toml` to describe the new, much smaller manual surface.

## Capabilities

### New Capabilities
- `wildfly-provided-dependency-versions`: build-time behavior that sources the version of most WildFly-provided dependencies from WildFly's own published BOMs, and documents the small number of exceptions that remain manually pinned.

### Modified Capabilities
- none (no existing spec covers build dependency tooling)

## Impact

- Affected files: `build.gradle.kts` (BOM platform imports on `providedCompile`, `wildflyVersion` helper), `gradle/libs.versions.toml` (drop 7 `version.ref`s, updated comments), `docs/development/updatingDependencies.md`.
- No new build-time dependency: this uses Gradle's built-in Maven BOM/platform support, no YAML parsing or extra tooling needed.
- Network access: resolving the two BOM POMs requires Maven repository access at build time, via the existing Maven Central / `public-jboss` repositories already configured for the project.
- No impact on runtime behavior or the produced bootable JAR — `providedCompile` dependencies are compile-time only and are already excluded from the WAR; verified the WAR's contents are unchanged.
- No impact on the WFLY-20810 cloud-image packaging proposal — unrelated, out of scope.
