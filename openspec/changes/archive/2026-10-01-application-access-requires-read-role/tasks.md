## 1. Read-role flag

- [x] 1.1 Add `hasReadApplicationRole: Boolean = false` to `LoggedInUser` and to the `createLoggedInUser` test fixture, and verify `./gradlew compileKotlin` succeeds
- [x] 1.2 Inject `PolicyService` into `UserPrincipalFilter` and set `hasReadApplicationRole` when building the `LoggedInUser`: `false` without calling OPA when the user holds no application role, otherwise whether a role per zaaktype or an overall role is in `policyService.readLeesrollen()`. Add the flag to the login log line. Verify with the unit tests in 3.1

## 2. Filter

- [x] 2.1 Replace the non-admin check `hasAnyApplicationRole` in `RequestAuthorizationFilter` with `user.hasReadApplicationRole`, without injecting `PolicyService`. Leave the admin branch and public paths unchanged. Update the class KDoc line on general access. Verify with the unit tests in 3.2
- [x] 2.2 Add `, 2026 INFO.nl` to the SPDX header of each touched file only where `INFO.nl` is missing, and verify the headers by reading them

## 3. Tests

- [x] 3.1 Extend `UserPrincipalFilterTest` with a mocked `PolicyService`, covering: only `brp_zoeken` for a zaaktype and as overall role gives `false`; `brp_zoeken` for one zaaktype and `raadpleger` for another gives `true`; each read role on its own gives `true`; `behandelaar` as overall role gives `true`; only `systeemrol_behandelaar_alle_zaaktypen` gives `false`; no functional roles gives `false` without calling PABC or OPA. Stub `readLeesrollen()` in existing tests that now call it. Verify `./gradlew test --tests "nl.info.zac.authentication.UserPrincipalFilterTest"` passes
- [x] 3.2 Update `RequestAuthorizationFilterTest`: a user with the flag set is allowed on `/app/home` and a REST path; a user with only non-read roles and the flag unset gets 403 on both; a user without roles gets 403; the admin and public path tests stay. Verify `./gradlew test --tests "nl.info.zac.authentication.RequestAuthorizationFilterTest"` passes
- [ ] 3.3 Add Keycloak test user `zonderleesrol1` with a new functional role `zonder_leesrol_domein_test_1`, which PABC maps to only `brp_zoeken` and `zaakspecifiek_geautoriseerd` in domein test 1. Add an `AppContainerTest` scenario: the ZAC base URI gives 403 with the no-permission page, a REST endpoint gives 403, and sign-out still redirects. Add the user to the `IdentityRestServiceTest` users list. Verify with the integration tests in 4.2

## 4. Verification

- [x] 4.1 Run `./gradlew spotlessApply detektApply`, then `./gradlew detekt`, and verify it reports no issues
- [x] 4.2 Run the integration tests (`./gradlew buildDockerImage itest`) and verify none fail, so users with read roles keep access
- [ ] 4.3 In the local Docker Compose stack, log in as a user who holds only `brp_zoeken`, and verify that ZAC shows the "U heeft geen toestemming om deze pagina te bekijken." page with only a log-out button, and that the log-out button works
- [x] 4.4 Run `openspec validate application-access-requires-read-role --strict` and verify it passes
