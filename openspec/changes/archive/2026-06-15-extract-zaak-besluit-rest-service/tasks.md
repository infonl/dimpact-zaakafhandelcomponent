## 1. Create ZaakBesluitRestService

- [x] 1.1 Create `src/main/kotlin/nl/info/zac/app/zaak/ZaakBesluitRestService.kt` as a `@Singleton` JAX-RS service with `@Path("zaken")`, injecting only the dependencies needed by the six besluit endpoints: `BrcClientService`, `DecisionService`, `EventingService`, `loggedInUserInstance`, `PolicyService`, `RestDecisionConverter`, `ZaakHistoryLineConverter`, `ZaakService`, `ZrcClientService`, `ZtcClientService`
- [x] 1.2 Move `listBesluitenForZaakUUID` (`GET /besluit/zaakUuid/{zaakUuid}`) from `ZaakRestService` to `ZaakBesluitRestService`, keeping identical signature and authorization logic
- [x] 1.3 Move `createBesluit` (`POST /besluit`) from `ZaakRestService` to `ZaakBesluitRestService`, keeping identical signature and authorization logic
- [x] 1.4 Move `updateBesluit` (`PUT /besluit`) from `ZaakRestService` to `ZaakBesluitRestService`, keeping identical signature and authorization logic
- [x] 1.5 Move `intrekkenBesluit` (`PUT /besluit/intrekken`) from `ZaakRestService` to `ZaakBesluitRestService`, keeping identical signature and authorization logic
- [x] 1.6 Move `listBesluitHistorie` (`GET /besluit/{uuid}/historie`) from `ZaakRestService` to `ZaakBesluitRestService`, keeping identical signature
- [x] 1.7 Move `listBesluittypes` (`GET /besluittypes/{zaaktypeUUID}`) from `ZaakRestService` to `ZaakBesluitRestService`, keeping identical signature and authorization logic

## 2. Update ZaakRestService

- [x] 2.1 Remove the six moved endpoint methods from `ZaakRestService`
- [x] 2.2 Remove unused imports in `ZaakRestService` that were only needed by the removed endpoints (e.g. `CollectionUtils` if no longer used)
- [x] 2.3 Verify that `ZaakRestService` still compiles and all remaining dependencies are still referenced

## 3. Unit Tests

- [x] 3.1 Create `src/test/kotlin/nl/info/zac/app/zaak/ZaakBesluitRestServiceTest.kt` using Kotest `BehaviorSpec` + MockK, with only the dependencies needed by `ZaakBesluitRestService`
- [x] 3.2 Add `Given/When/Then` test for `listBesluitenForZaakUUID`: authorized user returns list of `RestDecision`
- [x] 3.3 Add `Given/When/Then` test for `listBesluitenForZaakUUID`: unauthorized user (no `lezen`) throws policy exception
- [x] 3.4 Add `Given/When/Then` test for `createBesluit`: authorized user creates besluit and screen event is sent
- [x] 3.5 Add `Given/When/Then` test for `createBesluit`: unauthorized user (no `vastleggenBesluit`) throws policy exception
- [x] 3.6 Add `Given/When/Then` test for `updateBesluit`: authorized user updates besluit and screen event is sent
- [x] 3.7 Add `Given/When/Then` test for `updateBesluit`: unauthorized user (no `vastleggenBesluit`) throws policy exception
- [x] 3.8 Add `Given/When/Then` test for `intrekkenBesluit`: authorized user on open zaak withdraws besluit and screen event is sent
- [x] 3.9 Add `Given/When/Then` test for `intrekkenBesluit`: unauthorized user (no `behandelen` or closed zaak) throws policy exception
- [x] 3.10 Add `Given/When/Then` test for `listBesluitHistorie`: returns converted audit trail history lines
- [x] 3.11 Add `Given/When/Then` test for `listBesluittypes`: authorized user returns filtered besluit types
- [x] 3.12 Add `Given/When/Then` test for `listBesluittypes`: unauthorized user (no `zakenTaken`) throws policy exception

## 4. Verify

- [x] 4.1 Run `./gradlew :test --tests "nl.info.zac.app.zaak.ZaakBesluitRestServiceTest"` and confirm all pass
- [x] 4.2 Run `./gradlew :test --tests "nl.info.zac.app.zaak.ZaakRestService*"` and confirm existing unit tests still pass
- [x] 4.3 Run `./gradlew compileKotlin` to verify no compilation errors
