## 1. Filter

- [x] 1.1 Inject `PolicyService` into `RequestAuthorizationFilter` through its constructor, and add the guarded page rules from the design to its companion object: `/documenten`, `/productaanvragen` (path itself and below) and `/informatie-objecten` (exact, with or without trailing slash) need werklijst `inbox`; `/zaken/create` needs overige `startenZaak`. Verify `./gradlew compileKotlin` succeeds
- [x] 1.2 In `authorizeUser`, after the read role check and the admin check, return `Authorization.FORBIDDEN` when the path matches a guarded page rule and `policyService.readWerklijstRechten()` or `policyService.readOverigeRechten()` gives `false` for its right. Make no OPA call for any other path. Update the class KDoc with one line on the guarded pages. Verify with the unit tests in 2.1
- [x] 1.4 Match the admin prefixes and the guarded page rules on `servletPath + pathInfo` instead of the raw request URI, so percent-encoded and `;`-parameterised variants match too. Verify with unit tests for `/%64ocumenten/inbox`, `/documenten/inbox;view=list`, `/zaken/%63reate`, `/zaken/create;view=form`, `/%61dmin/settings` and `/admin;view=list/settings`, and with `AppContainerTest` scenarios for `/%61dmin`, `/admin;view=list`, `/%64ocumenten/inbox` and `/documenten/inbox;view=list`
- [x] 1.3 Add `, 2026 INFO.nl` to the SPDX header of each touched file only where `INFO.nl` is missing, and verify the headers by reading them

## 2. Tests

- [x] 2.1 Add `RequestAuthorizationFilterPageRightsTest`, a separate class because detekt reports `RequestAuthorizationFilterTest` as too large, with a mocked `PolicyService`: a user without `inbox` gets a plain 403 on `/documenten/inbox`, `/documenten/ontkoppelde`, `/documenten`, `/productaanvragen/inbox` and `/informatie-objecten`; a user with `inbox` is allowed on them; a user without `startenZaak` gets 403 on `/zaken/create` and one with it is allowed; `/informatie-objecten/<uuid>`, `/zaken/werkvoorraad`, `/documentenx` and a REST path are allowed without calling `PolicyService`; a user without a read role gets the base role page on `/documenten/inbox` without calling `PolicyService`. Pass the mock to every existing `RequestAuthorizationFilter()` construction. Verify `./gradlew test --tests "nl.info.zac.authentication.RequestAuthorizationFilter*"` passes
- [x] 2.2 Add `AppContainerTest` scenarios next to the `/admin` ones: `/documenten/inbox` and `/productaanvragen/inbox` give 403 with the "U heeft geen toestemming om deze pagina te bekijken." page and a home button for `BEHANDELAAR_1`, and 200 for `COORDINATOR_1`; `/zaken/create` gives 403 for `RAADPLEGER_1` and 200 for `BEHANDELAAR_1`. Verify with the integration tests in 3.2

## 3. Verification

- [x] 3.1 Run `./gradlew spotlessApply detektApply`, then `./gradlew detekt`, and verify it reports no issues
- [x] 3.2 Run `./gradlew buildDockerImage itest` and verify no integration test fails
- [ ] 3.3 In the local kind stack, log in as a `behandelaar`, open `/documenten/inbox` by URL, and verify ZAC shows the "U heeft geen toestemming om deze pagina te bekijken." page and its home button leads to the dashboard; then log in as a `coordinator` and verify the inbox loads
- [x] 3.4 Run `openspec validate page-access-by-role --strict` and verify it passes
