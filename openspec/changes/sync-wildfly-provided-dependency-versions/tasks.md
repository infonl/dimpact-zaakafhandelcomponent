## 1. Investigate

- [x] 1.1 Confirm WFLY-20810 (the trigger for this investigation) has no bearing on dependency versions
- [x] 1.2 Confirm WildFly's channel manifests (`org.wildfly.channels:wildfly[-ee]`) match our currently-pinned versions, as a fallback design
- [x] 1.3 Confirm WildFly publishes Maven BOMs (`org.wildfly.bom:wildfly-ee`, `org.wildfly.bom:wildfly-expansion`) covering 7 of the 9 WildFly-provided dependencies, by inspecting their published POMs
- [x] 1.4 Confirm (via `git grep`) that none of the 9 WildFly-provided dependencies can simply be removed — the codebase directly imports vendor-specific classes with no standard equivalent

## 2. Import the WildFly BOMs as Gradle platforms

- [x] 2.1 Add a `readWildflyVersion` helper in `build.gradle.kts` that parses `pom.xml`'s `<wildfly.version>` property
- [x] 2.2 Add `providedCompile(platform("org.wildfly.bom:wildfly-ee:$wildflyVersion"))` and `providedCompile(platform("org.wildfly.bom:wildfly-expansion:$wildflyVersion"))` to the existing `dependencies {}` block
- [x] 2.3 Drop `version.ref` from the 7 BOM-covered `gradle/libs.versions.toml` `[libraries]` entries (`eclipse-microprofile-rest-client-api`, `eclipse-microprofile-config-api`, `eclipse-microprofile-health-api`, `eclipse-microprofile-fault-tolerance-api`, `hibernate-validator`, `jboss-resteasy-multipart-provider`, `wildfly-security-elytron-http-oidc`)
- [x] 2.4 Remove the now-unused version entries for those 7 dependencies from the `[versions]` block, keeping `jakarta-jakartaee` and `smallrye-health` with an explanatory comment each

## 3. Verify

- [x] 3.1 Run `./gradlew dependencies --configuration providedCompile` and confirm all 7 BOM-covered dependencies resolve to the previously-pinned versions
- [x] 3.2 Run `./gradlew compileKotlin compileJava compileTestKotlin` and confirm it succeeds
- [x] 3.3 Run `./gradlew war` and confirm it succeeds and the WAR does not contain any of the `providedCompile` dependencies
- [x] 3.4 Run targeted unit tests exercising the vendor-specific usages (`KlantRestService`, `UserPrincipalFilter`, `TaskRestService`, `EnkelvoudigInformatieObjectRestService`) and confirm they pass
- [x] 3.5 Run `./gradlew detekt` and confirm `build.gradle.kts` has no new findings

## 4. Documentation

- [x] 4.1 Update `docs/development/updatingDependencies.md` step 7 (Upgrade WildFly application server) to describe the BOM-based approach and the smaller manual surface (`jakarta-jakartaee`, `smallrye-health` only)
- [x] 4.2 Update the comments around the WildFly-provided blocks in `gradle/libs.versions.toml` to explain the BOM import and the reason for each remaining manual entry
