## 1. Role constant

## 1. Filter

- [x] 1.1 Inject `PolicyService` into `RequestAuthorizationFilter` through the constructor, and verify `./gradlew compileKotlin` succeeds
- [x] 1.2 Replace the non-admin check `hasAnyApplicationRole` with a read-role check: refuse when the user holds no application role, without calling OPA, otherwise admit only when a role per zaaktype or an overall role is in `policyService.readLeesrollen()`. Leave the admin branch and public paths unchanged. Update the class KDoc line on general access. Verify with the unit tests in 2.1
- [x] 1.3 Add `, 2026 INFO.nl` to the SPDX header of each touched file only where `INFO.nl` is missing, and verify the headers by reading them

## 2. Tests

- [x] 2.1 Extend `RequestAuthorizationFilterTest` with a mocked `PolicyService`, covering the scenarios in `specs/application-access/spec.md`: only `brp_zoeken` gives 403; only `brp_zoeken` and `zaakspecifiek_geautoriseerd` gives 403; no roles gives 403 without calling OPA; `brp_zoeken` for one zaaktype and `raadpleger` for another is allowed; each read role on its own is allowed; a read role as overall role is allowed; only `systeemrol_behandelaar_alle_zaaktypen` for a zaaktype gives 403; only `brp_zoeken` on `/rest/admin/` gives 403; `beheerder` on `/rest/admin/` is allowed; only `brp_zoeken` on `/sign-out` and `/assets/` with GET is allowed. Update existing tests that now need the `PolicyService` stub. Verify `./gradlew test --tests "nl.info.zac.authentication.RequestAuthorizationFilterTest"` passes

## 3. Verification

- [x] 3.1 Run `./gradlew spotlessApply detektApply`, then `./gradlew detekt`, and verify it reports no issues
- [x] 3.2 Run the integration tests (`./gradlew buildDockerImage itest`) and verify none fail, so users with read roles keep access
- [ ] 3.3 In the local Docker Compose stack, log in as a user who holds only `brp_zoeken`, and verify that ZAC shows the "U heeft geen toestemming om deze pagina te bekijken." page with only a log-out button, and that the log-out button works
- [x] 3.4 Run `openspec validate application-access-requires-read-role --strict` and verify it passes
