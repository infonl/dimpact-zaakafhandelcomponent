## 1. Role constant

- [x] 1.1 Add `SYSTEEMROL_BEHANDELAAR_ALLE_ZAAKTYPEN("systeemrol_behandelaar_alle_zaaktypen")` to `ZacApplicationRole` and verify `./gradlew compileKotlin` succeeds

## 2. Filter

- [x] 2.1 Inject `PolicyService` into `RequestAuthorizationFilter` through the constructor, and verify `./gradlew compileKotlin` succeeds
- [x] 2.2 Replace the non-admin check `hasAnyApplicationRole` with a read-role check, in this order: refuse when the user holds no application role, admit when the user holds `systeemrol_behandelaar_alle_zaaktypen` (per zaaktype or overall), otherwise admit only when a role per zaaktype or an overall role is in `policyService.readLeesrollen()`. Leave the admin branch and public paths unchanged. Update the class KDoc line on general access. Verify with the unit tests in 3.1
- [x] 2.3 Add `, 2026 INFO.nl` to the SPDX header of each touched file only where `INFO.nl` is missing, and verify the headers by reading them

## 3. Tests

- [x] 3.1 Extend `RequestAuthorizationFilterTest` with a mocked `PolicyService`, covering the scenarios in `specs/application-access/spec.md`: only `brp_zoeken` gives 403; only `brp_zoeken` and `zaakspecifiek_geautoriseerd` gives 403; no roles gives 403 without calling OPA; `brp_zoeken` for one zaaktype and `raadpleger` for another is allowed; each read role on its own is allowed; a read role as overall role is allowed; only `systeemrol_behandelaar_alle_zaaktypen` is allowed without calling OPA; only `brp_zoeken` on `/rest/admin/` gives 403; `beheerder` on `/rest/admin/` is allowed; only `brp_zoeken` on `/sign-out` and `/assets/` with GET is allowed. Update existing tests that now need the `PolicyService` stub. Verify `./gradlew test --tests "nl.info.zac.authentication.RequestAuthorizationFilterTest"` passes

## 4. Verification

- [x] 4.1 Run `./gradlew spotlessApply detektApply`, then `./gradlew detekt`, and verify it reports no issues
- [x] 4.2 Run the integration tests (`./gradlew buildDockerImage itest`) and verify none fail, so users with read roles keep access
- [ ] 4.3 In the local Docker Compose stack, log in as a user who holds only `brp_zoeken`, and verify that ZAC shows the "U heeft geen toestemming om deze pagina te bekijken." page with only a log-out button, and that the log-out button works
- [x] 4.4 Run `openspec validate application-access-requires-read-role --strict` and verify it passes
