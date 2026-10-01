## 1. Read-role based search filter

- [x] 1.1 Add a `leesrollen` set (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, `beheerder`) to `rollen.rego`, use it in the `lezen` rules of the zaak, taak and document policies, and let `SearchService` read it from OPA through `PolicyService.readLeesrollen()`; verify with `opa test` and a `PolicyServiceTest` case
- [x] 1.2 Change `SearchService.getAllowedZaaktypenFilterQuery` to only admit zaaktypen whose roles, united with `overallRoles`, contain a read role, keeping the non-existing-zaaktype fallback when none remain; verify `./gradlew compileKotlin` succeeds
- [x] 1.3 Add `SearchServiceTest` cases: only `brp_zoeken` for one zaaktype and `behandelaar` for another (only the latter in the filter query); only `zaakspecifiek_geautoriseerd` for a zaaktype (zaaktype excluded, zaakspecifiek filter unchanged for the rest); read role mixed with non-read roles (included); overall `raadpleger` with only `brp_zoeken` for a zaaktype (included); no read role at all (non-existing-zaaktype filter); verify with `./gradlew test --tests "nl.info.zac.search.SearchServiceTest"`
- [x] 1.4 Confirm existing `SearchServiceTest` scenarios for users with read roles still pass unchanged, proving users with a read role see the same results as before

## 2. Remove the NOT_AUTHORISED_TO_LEZEN koppel reason

- [x] 2.1 Remove `NOT_AUTHORISED_TO_LEZEN` from `ZaakNotLinkableReason` and the `!to.lezen` branch from `gerelateerdNotLinkableReason`, dropping its now unused found-zaak argument; redefine `canBeRelatedTo` as `to.lezen && gerelateerdNotLinkableReason() == null`; verify `./gradlew compileKotlin` succeeds
- [x] 2.2 Update `ZaakLinkDataTest`: the gerelateerd reason only reports a missing `koppelen` right on the current zaak, while `canBeRelatedTo` returns `false` for an unreadable found zaak; verify with `./gradlew test --tests "nl.info.zac.zaak.model.ZaakLinkDataTest"`
- [x] 2.3 Check `ZaakKoppelenRestServiceTest` (and any other test referencing the removed value) still compiles and passes, adding a case that `linkZaak` with relation type GERELATEERD to an unreadable zaak throws a policy exception if none exists; verify with `./gradlew test --tests "*ZaakKoppelenRestService*"`
- [x] 2.4 Regenerate the OpenAPI spec and frontend types (`./gradlew generateOpenApiSpec`, then the frontend type generation) and verify the generated enum no longer contains `NOT_AUTHORISED_TO_LEZEN`

## 3. Frontend

- [x] 3.1 Remove `zaak.koppelen.niet-koppelbaar.NOT_AUTHORISED_TO_LEZEN` from `src/main/app/src/assets/i18n/nl.json` and `en.json`; verify with `grep -r NOT_AUTHORISED_TO_LEZEN src/main/app/src` returning nothing outside generated files
- [x] 3.2 Remove `NOT_AUTHORISED_TO_LEZEN` from the reason list in `zaak-link.component.spec.ts`; verify with `npm test -- zaak-link.component` and `npm run lint` in `src/main/app`

## 4. Integration and documentation

- [x] 4.1 Add a Keycloak/PABC test user `behandelaar1brpzoeker2` (behandelaar in domein test 1, only `brp_zoeken` in domein test 2 via the new `brp_zoeker_domein_test_2` functional role and `brp-zoekers-test-2` group), update the `IdentityServiceTest` user and group fixtures, and add `SearchRestServiceLeesrechtTest` proving that the zaak of zaaktype test 1 is absent from that user's search results and Koppelen list while a zaak of zaaktype test 2 is found; verify with `./gradlew itest --tests "*SearchRestServiceLeesrechtTest" --tests "*IdentityServiceTest"` after building the Docker image
- [x] 4.2 Update `docs/solution-architecture/accessControlPolicies.md` to state that zoekresultaten and werklijsten only include zaaktypen for which the user holds a read role; verify the section reads consistently with the `lezen` rules
- [x] 4.3 Run `./gradlew spotlessApply detektApply` and `./gradlew test`; verify both succeed
